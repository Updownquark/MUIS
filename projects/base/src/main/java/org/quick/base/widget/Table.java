package org.quick.base.widget;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.observe.ObservableValueEvent;
import org.observe.Subscription;
import org.observe.collect.CollectionChangeEvent.ElementChange;
import org.observe.collect.ObservableCollection;
import org.qommons.ArrayUtils;
import org.qommons.Transaction;
import org.qommons.collect.ElementId;
import org.quick.core.*;
import org.quick.core.QuickConstants.CoreStage;
import org.quick.core.layout.*;
import org.quick.core.mgr.AttributeManager2;
import org.quick.core.model.ModelAttributes;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.prop.QuickPropertyType;
import org.quick.core.style.LayoutStyle;
import org.quick.core.style.LengthUnit;
import org.quick.core.style.Size;
import org.quick.core.tags.AcceptAttribute;
import org.quick.core.tags.QuickElementType;
import org.quick.core.tags.Template;
import org.quick.widget.base.layout.BaseLayoutUtils;
import org.quick.widget.core.Point;
import org.quick.widget.core.QuickElementCapture;
import org.quick.widget.core.Rectangle;
import org.quick.widget.core.layout.SimpleSizeGuide;
import org.quick.widget.core.layout.SizeGuide;

@QuickElementType(attributes = { //
	@AcceptAttribute(declaringClass = Table.class, field = "rows", required = true), //
	@AcceptAttribute(declaringClass = Table.class, field = "columns", required = false), //
	@AcceptAttribute(declaringClass = LayoutAttributes.class, field = "orientation", required = true, defaultValue = "VERTICAL"), //
	@AcceptAttribute(declaringClass = Table.class, field = "rowValueModelName", required = true, defaultValue = "row"), //
	@AcceptAttribute(declaringClass = Table.class, field = "rowValueElementNameName", required = true, defaultValue = "rowElement") //
})
@Template(location = "../../../../table.qts")
public class Table extends QuickTemplate {
	public static final QuickAttribute<ObservableCollection<?>> rows = QuickAttribute.build("rows", ModelAttributes.collectionType).build();
	public static final QuickAttribute<ObservableCollection<?>> columns = QuickAttribute.build("columns", ModelAttributes.collectionType)
		.build();
	public static final QuickAttribute<String> rowValueModelName = QuickAttribute.build("row-value", QuickPropertyType.string).build();
	public static final QuickAttribute<String> rowElementModelName = QuickAttribute.build("row-element", QuickPropertyType.string)
		.build();
	public static final QuickAttribute<String> columnValueModelName = QuickAttribute.build("column-value", QuickPropertyType.string)
		.build();
	public static final QuickAttribute<String> columnElementModelName = QuickAttribute.build("column-element", QuickPropertyType.string)
		.build();
	public static final QuickAttribute<Size> rowHeight = QuickAttribute.build("row-height", LayoutAttributes.sizeType).build();
	public static final QuickAttribute<Size> rowWidth = QuickAttribute.build("row-width", LayoutAttributes.sizeType).build();

	private AttachPoint<TableColumn> theColumnAP;
	private Size theConfiguredRowSize;
	private List<RowSizer> theRowSizes;
	private SimpleSizeGuide theRowSizer;
	private boolean isRowSizeDirty;

	private ObservableCollection<?> theRows;
	private ObservableCollection<?> theColumns;

	public Table() {
		theRowSizer = new SimpleSizeGuide();
		life().runWhen(() -> {
			// The value of the rows and columns attributes cannot be changed dynamically
			// This is to ensure that the types, which are relied upon by the columns, never change
			// The values of these attributes *can* be a flattened collection that is switched out internally
			// because this cannot affect the type
			ensureConstant(rows);
			ensureConstant(columns);
			ensureConstant(rowValueModelName);
			ensureConstant(rowElementModelName);
			ensureConstant(columnValueModelName);
			ensureConstant(columnElementModelName);
			theRows = atts().get(rows).get();
			theColumns = atts().get(columns).get();
			if (theColumns != null) {
				// TODO Remove this next line when we support variable columns
				msg().fatal("Variable column support has not yet been implemented");
				atts().accept(columnValueModelName, this, aa -> aa.required().init("column"));
				atts().accept(columnElementModelName, this, aa -> aa.required().init("columnElement"));
			}
			// TODO size needs changed and rowSize=dirty when margin or padding changes
			// Accept row-height for a vertical table and row-width for a horizontal one
			atts().get(LayoutAttributes.orientation).changes().act(new Consumer<ObservableValueEvent<Orientation>>() {
				private AttributeManager2.AttributeAcceptance rowDimAcc;
				private Subscription rowDimSub;
				private Consumer<ObservableValueEvent<Size>> rowDimAction = evt -> {
					theConfiguredRowSize = evt.getNewValue();
					if (!evt.isInitial()) {
						isRowSizeDirty = true;
						sizeNeedsChanged();
					}
				};

				@Override
				public void accept(ObservableValueEvent<Orientation> evt) {
					if (!evt.isInitial() && evt.getOldValue() != evt.getNewValue()) {
						rowDimSub.unsubscribe();
						rowDimAcc.reject();
					}
					if (evt.getNewValue().isVertical())
						rowDimSub = atts().accept(rowHeight, Table.this, acc -> rowDimAcc = acc).noInitChanges().act(rowDimAction);
					else
						rowDimSub = atts().accept(rowWidth, Table.this, acc -> rowDimAcc = acc).noInitChanges().act(rowDimAction);
				}
			});
		}, CoreStage.INIT_SELF, 1);
		life().runWhen(() -> {
			theColumnAP = (AttachPoint<TableColumn>) getTemplate().getAttachPoint("column");
			// TODO Probably need to keep a full row x column cache table of row sizers to optimize size recalculations due to model changes
			ObservableCollection<?> _rows = atts().get(rows).get();
			ObservableCollection<? extends TableColumn> cols = getContainer(theColumnAP).getContent();
			_rows.changes().act(evt -> {
				boolean sizeNeedsChanged = false;
				try (Transaction t = getContentLocker().lock(false, evt)) {
					// TODO Can do better by propagating the size needs change along theRowSizes collection
					// instead of marking it dirty and calculating the whole thing again
					switch (evt.type) {
					case add:
						for (ElementChange<?> el : evt.elements)
							theRowSizes.add(el.index, new RowSizer(el.newValue, cols));
						sizeNeedsChanged = true;
						break;
					case remove:
						for (ElementChange<?> el : evt.getElements())
							theRowSizes.remove(el.index);
						sizeNeedsChanged = true;
						break;
					case set:
						for (ElementChange<?> el : evt.elements)
							sizeNeedsChanged |= theRowSizes.get(el.index).setRow(el.newValue, cols);
					}
				}
				if (sizeNeedsChanged) {
					isRowSizeDirty = true;
					sizeNeedsChanged();
				}
			});
			ObservableCollection<?> colValues = atts().get(columns).get();
			if (colValues != null) {
				// TODO Ensure there's initially one column
				// TODO Store the type of that column (or some means to create new columns of the same type, content, and attributes).
				if (!colValues.isEmpty()) {
					// TODO Set the column model value for the existing column, then duplicate it for each additional column
				} else {
					// TODO Remove the initial column
				}
				colValues.changes().noInit().act(evt -> {
					// TODO Add/remove/update columns
					// TODO Adjust the rowSizer as well since columns determine the row size
				});
			} else {
				// TODO Dynamically ensure the columns are sorted by their header attribute (type End: leading or trailing)
				cols.subscribe(evt -> {
					boolean sizeNeedsChanged = false;
					try (Transaction t = _rows.lock(false, evt)) {
						switch (evt.getType()) {
						case add:
							for (RowSizer rowSize : theRowSizes)
								sizeNeedsChanged |= rowSize.columnAdded(evt.getIndex(), evt.getNewValue());
							break;
						case remove:
							for (RowSizer rowSize : theRowSizes)
								sizeNeedsChanged |= rowSize.columnRemoved(evt.getIndex());
							break;
						case set:
							for (RowSizer rowSize : theRowSizes)
								sizeNeedsChanged |= rowSize.columnChanged(evt.getIndex(), evt.getNewValue());
							break;
						}
					}
					if (sizeNeedsChanged) {
						isRowSizeDirty = true;
						sizeNeedsChanged();
					}
				}, true);
			}
			// TODO Populate the row-headers' rows with the columns attribute content (if set) or the actual column children (otherwise)
		}, CoreStage.INIT_CHILDREN, 1);
	}

	private class RowSizer {
		Object theRow;
		final List<SizeGuide> columnSizes;
		int position;
		int size;

		RowSizer(Object row, List<? extends TableColumn> columns) {
			theRow = row;
			columnSizes = new ArrayList<>(columns.size());
			for (TableColumn col : columns)
				columnSizes.add(theConfiguredRowSize == null ? col.getCellSize(theRow) : null);
		}

		boolean setRow(Object row, List<? extends TableColumn> columns) {
			theRow = row;
			if (theConfiguredRowSize != null)
				return false;
			for (int i = 0; i < columnSizes.size(); i++)
				columnSizes.set(i, columns.get(i).getCellSize(theRow));
			return true;
		}

		boolean columnAdded(int index, TableColumn column) {
			columnSizes.add(index, theConfiguredRowSize == null ? column.getCellSize(theRow) : null);
			return true;
		}

		boolean columnRemoved(int index) {
			columnSizes.remove(index);
			return true;
		}

		boolean columnChanged(int index, TableColumn column) {
			columnSizes.set(index, theConfiguredRowSize == null ? column.getCellSize(theRow) : null);
			return theConfiguredRowSize == null;
		}
	}

	public ObservableCollection<?> getRows() {
		return atts().get(rows).get();
	}

	public int getRowPosition(int row) {
		if (isRowSizeDirty)
			calcRowSizes();
		return theRowSizes.get(row).position;
	}

	public int getRowLength(int row) {
		if (isRowSizeDirty)
			calcRowSizes();
		return theRowSizes.get(row).size;
	}

	public int getRowAt(Point point) {
		int pos = atts().get(LayoutAttributes.orientation).get().isVertical() ? point.y : point.x;
		return ArrayUtils.binarySearch(theRowSizes, sz -> {
			int diff = pos - sz.position;
			if (diff < 0)
				return -1;
			else if (diff >= sz.size)
				return 1;
			else
				return 0;
		});
	}

	protected void setSelected(TableColumn column, ElementId row) {
		for (TableColumn col : getContainer(theColumnAP).getContent()) {
			col.setSelectedRow(row);
			if (col != column)
				col.setSelected(false);
		}
	}

	private void calcRowSizes(){
		Orientation axis = atts().get(LayoutAttributes.orientation).get();
		int rowDim = bounds().get(axis).getSize();
		int margin = getStyle().get(LayoutStyle.margin).get().evaluate(rowDim);
		Size padding = getStyle().get(LayoutStyle.padding).get();
		Size rowSize = theConfiguredRowSize;
		try(Transaction t=theRows.lock(false, null)){
			int rowCount = theRows.size();
			if(rowCount==0)
				return;
			if(rowSize!=null){
				float rowSizePixels = rowSize.evaluate(rowDim);
				float padPixels = padding.evaluate(rowDim);
				float padMult;
				float total = rowSizePixels * rowCount;
				if (total >= rowDim) {
					padMult = 0;
					rowSizePixels *= rowDim / total;
				} else {
					float space = margin * 2 + padPixels * (rowCount - 1);
					if (total + space > rowDim)
						padMult = (rowDim - total) / space;
					else
						padMult = 1;
				}
				if ((padMult != 0 && padMult != 1) || (rowSizePixels != (int) rowSizePixels)) {
					padPixels *= padMult;
					float pos = margin * padMult;
					for(int r=0;r<rowCount;r++){
						theRowSizes.get(r).position=Math.round(pos);
						pos+=rowSizePixels;
						theRowSizes.get(r).size=Math.round(pos)-theRowSizes.get(r).position;
						pos+=padPixels;
					}
				} else if (padMult != 0 || (padPixels != (int) padPixels)) {
					int rowSizePix = (int) rowSizePixels;
					int pos = margin;
					float totalPadding = 0;
					int totalPadInt = 0;
					for (int r = 0; r < rowCount; r++) {
						theRowSizes.get(r).position = pos;
						theRowSizes.get(r).size = rowSizePix;
						pos += rowSizePix;
						totalPadding += padPixels;
						int rowPad = Math.round(totalPadding - totalPadInt);
						pos += rowPad;
						totalPadInt += rowPad;
					}
				} else {
					int rowSizePix = (int) rowSizePixels;
					int padPix = (int) padPixels;
					int pos = margin;
					for (int r = 0; r < rowCount; r++) {
						theRowSizes.get(r).position = pos;
						theRowSizes.get(r).size = rowSizePix;
						pos += rowSizePix + padPix;
					}
				}
			} else{
				//TODO Not implemented
			}
		}
	}

	@Override
	public SizeGuide getSizer(Orientation orientation) {
		ObservableCollection<?> tableRows = atts().get(rows).get();
		ObservableCollection<? extends TableColumn> tableColumns = getContainer(theColumnAP).getContent();
		try (Transaction rowT = tableRows.lock(false, null); Transaction colT = tableColumns.lock(false, null)) {
			Orientation axis = atts().get(LayoutAttributes.orientation).get();
			if (axis == orientation) {
				if (tableRows.isEmpty())
					return new SimpleSizeGuide(0, 0, 0, 0, Integer.MAX_VALUE);
				Size rowSize = atts().get(axis.isVertical() ? rowHeight : rowWidth).get();
				if (rowSize != null) {
					if (rowSize.getUnit() == LengthUnit.percent) {
						// If the row size is a percent, then it tells us nothing about how big the rows would like to be
						return new SimpleSizeGuide(0, 0, 300, Integer.MAX_VALUE, Integer.MAX_VALUE);
					} else {
						Size margin = getStyle().get(LayoutStyle.margin).get();
						Size padding = getStyle().get(LayoutStyle.padding).get();
						int rowCount = tableRows.size();
						int size = Math.round(rowSize.getValue() * rowCount);
						int minSize = size;
						size += (rowCount - 1) * padding.evaluate(size);
						size += margin.evaluate(size) * 2;
						return new SimpleSizeGuide(minSize, size, size, size, Integer.MAX_VALUE);
					}
				} else
					return new SizeGuide.GenericSizeGuide() {
						@Override
						public int get(LayoutGuideType type, int crossSize, boolean csMax) {
							if (crossSize == bounds().get(axis.opposite()).getSize())
								return theRowSizer.get(type, crossSize, csMax); // TODO rowSizer is not populated yet
							return 0; // TODO Not implemented
						}

						@Override
						public int getBaseline(int size) {
							return 0;
						}
					};
			} else {
				return new SizeGuide.GenericSizeGuide() {
					@Override
					public int get(LayoutGuideType type, int crossSize, boolean csMax) {
						Size margin = getStyle().get(LayoutStyle.margin).get();
						Size padding = getStyle().get(LayoutStyle.padding).get();
						int size = BaseLayoutUtils.getBoxLayoutSize(tableColumns, orientation, type, bounds().get(axis).getSize(), false,
							padding, padding);
						size += margin.evaluate(size) * 2;
						return size;
					}

					@Override
					public int getBaseline(int size) {
						return 0;
					}
				};
			}
		}
	}

	@Override
	public void doLayout() {
		// If the columns are declared, then the columns themselves are stamped and there's nothing to lay out
		// Otherwise, the columns are physical children and we can lay them out appropriately
		if (theColumns == null) {
			ObservableCollection<? extends TableColumn> tableColumns = getContainer(theColumnAP).getContent();
			try (Transaction colT = tableColumns.lock(false, null)) {
				for (TableColumn column : tableColumns)
					column.doLayout();
				Orientation orient = atts().get(LayoutAttributes.orientation).get();
				int rowDim = bounds().get(orient).getSize();
				int colDim = bounds().get(orient.opposite()).getSize();
				int margin = getStyle().get(LayoutStyle.margin).get().evaluate(colDim);
				int padding = getStyle().get(LayoutStyle.padding).get().evaluate(colDim);
				int pos = margin;
				for (TableColumn column : tableColumns) {
					int colWidth = column.bounds().get(orient.opposite()).getSize();
					if (orient.isVertical())
						column.bounds().setBounds(pos, margin, colWidth, rowDim);
					else
						column.bounds().setBounds(margin, pos, rowDim, colWidth);
					pos += padding + colWidth;
				}
			}
		}
		// Don't call the super
	}

	@Override
	public QuickElementCapture[] paintChildren(Graphics2D graphics, Rectangle area) {
		if (theColumns == null)
			return super.paintChildren(graphics, area);
		else {
			// TODO Render the stamped columns
			// Return the element capture for the hovered cell
			return null;
		}
	}

	private <T> void ensureConstant(QuickAttribute<T> att) {
		atts().get(att).noInitChanges().act(new Consumer<ObservableValueEvent<T>>() {
			T initValue;

			@Override
			public void accept(ObservableValueEvent<T> evt) {
				if (evt.isInitial())
					initValue = evt.getNewValue();
				else if (initValue != evt.getNewValue())
					throw new IllegalStateException(att + " cannot be changed");
			}
		});
	}
}
