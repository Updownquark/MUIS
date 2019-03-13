package org.quick.widget.base;

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
import org.quick.base.widget.Table;
import org.quick.base.widget.TableColumn;
import org.quick.core.QuickConstants.CoreStage;
import org.quick.core.layout.LayoutAttributes;
import org.quick.core.layout.LayoutGuideType;
import org.quick.core.layout.Orientation;
import org.quick.core.style.LayoutStyle;
import org.quick.core.style.LengthUnit;
import org.quick.core.style.Size;
import org.quick.widget.base.layout.BaseLayoutUtils;
import org.quick.widget.core.Point;
import org.quick.widget.core.QuickElementCapture;
import org.quick.widget.core.QuickTemplateWidget;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.QuickWidgetDocument;
import org.quick.widget.core.Rectangle;
import org.quick.widget.core.layout.SimpleSizeGuide;
import org.quick.widget.core.layout.SizeGuide;

import com.google.common.reflect.TypeToken;

public class TableWidget<R, C> extends QuickTemplateWidget {
	private Size theConfiguredRowSize;
	private List<RowSizer> theRowSizes;
	private SimpleSizeGuide theRowSizer;
	private boolean isRowSizeDirty;

	public TableWidget(QuickWidgetDocument doc, Table<R, C> element, QuickWidget parent) {
		super(doc, element, parent);
		theRowSizer = new SimpleSizeGuide();

		getElement().life().runWhen(() -> {
			// TODO size needs changed and rowSize=dirty when margin or padding changes
			// Accept row-height for a vertical table and row-width for a horizontal one
			getElement().atts().get(LayoutAttributes.orientation).changes().act(new Consumer<ObservableValueEvent<Orientation>>() {
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
					}
					if (evt.getNewValue().isVertical())
						rowDimSub = getElement().atts().get(Table.rowHeight).noInitChanges().act(rowDimAction);
					else
						rowDimSub = getElement().atts().get(Table.rowWidth).noInitChanges().act(rowDimAction);
				}
			});
		}, CoreStage.INIT_SELF, 1);
		getElement().life().runWhen(() -> {
			// TODO Probably need to keep a full row x column cache table of row sizers to optimize size recalculations due to model changes
			ObservableCollection<?> _rows = getElement().getRows();
			ObservableCollection<? extends TableColumnWidget<R, C>> cols = getColumns();
			_rows.changes().act(evt -> {
				boolean sizeNeedsChanged = false;
				try (Transaction t = getElement().getContentLocker().lock(false, evt)) {
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
			// TODO Populate the row-headers' rows with the columns attribute content (if set) or the actual column children (otherwise)
		}, CoreStage.INIT_CHILDREN, 1);
	}

	@Override
	public Table<R, C> getElement() {
		return (Table<R, C>) super.getElement();
	}

	public ObservableCollection<TableColumnWidget<R, C>> getColumns() {
		return getElement().getColumns().flow()
			.map(new TypeToken<TableColumn<R, C>>() {}, col -> (TableColumnWidget<R, C>) getChild(col), opts -> opts.cache(false))
			.collectPassive();
	}

	private class RowSizer {
		Object theRow;
		final List<SizeGuide> columnSizes;
		int position;
		int size;

		RowSizer(Object row, List<? extends TableColumnWidget<R, C>> columns) {
			theRow = row;
			columnSizes = new ArrayList<>(columns.size());
			for (TableColumnWidget<R, C> col : columns)
				columnSizes.add(theConfiguredRowSize == null ? col.getCellSize(theRow) : null);
		}

		boolean setRow(Object row, List<? extends TableColumnWidget<R, C>> columns) {
			theRow = row;
			if (theConfiguredRowSize != null)
				return false;
			for (int i = 0; i < columnSizes.size(); i++)
				columnSizes.set(i, columns.get(i).getCellSize(theRow));
			return true;
		}

		boolean columnAdded(int index, TableColumnWidget<R, C> column) {
			columnSizes.add(index, theConfiguredRowSize == null ? column.getCellSize(theRow) : null);
			return true;
		}

		boolean columnRemoved(int index) {
			columnSizes.remove(index);
			return true;
		}

		boolean columnChanged(int index, TableColumnWidget<R, C> column) {
			columnSizes.set(index, theConfiguredRowSize == null ? column.getCellSize(theRow) : null);
			return theConfiguredRowSize == null;
		}
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
		int pos = getElement().atts().get(LayoutAttributes.orientation).get().isVertical() ? point.y : point.x;
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

	private void calcRowSizes() {
		Orientation axis = getElement().atts().get(LayoutAttributes.orientation).get();
		int rowDim = bounds().get(axis).getSize();
		int margin = getElement().getStyle().get(LayoutStyle.margin).get().evaluate(rowDim);
		Size padding = getElement().getStyle().get(LayoutStyle.padding).get();
		Size rowSize = theConfiguredRowSize;
		try (Transaction t = getElement().getRows().lock(false, null)) {
			int rowCount = getElement().getRows().size();
			if (rowCount == 0)
				return;
			if (rowSize != null) {
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
					for (int r = 0; r < rowCount; r++) {
						theRowSizes.get(r).position = Math.round(pos);
						pos += rowSizePixels;
						theRowSizes.get(r).size = Math.round(pos) - theRowSizes.get(r).position;
						pos += padPixels;
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
			} else {
				// TODO Not implemented
			}
		}
	}

	@Override
	public SizeGuide getSizer(Orientation orientation) {
		ObservableCollection<?> tableRows = getElement().atts().get(Table.rows).get();
		ObservableCollection<TableColumnWidget<R, C>> tableColumns = getColumns();
		try (Transaction rowT = tableRows.lock(false, null); Transaction colT = tableColumns.lock(false, null)) {
			Orientation axis = getElement().atts().get(LayoutAttributes.orientation).get();
			if (axis == orientation) {
				if (tableRows.isEmpty())
					return new SimpleSizeGuide(0, 0, 0, 0, Integer.MAX_VALUE);
				Size rowSize = getElement().atts().get(axis.isVertical() ? Table.rowHeight : Table.rowWidth).get();
				if (rowSize != null) {
					if (rowSize.getUnit() == LengthUnit.percent) {
						// If the row size is a percent, then it tells us nothing about how big the rows would like to be
						return new SimpleSizeGuide(0, 0, 300, Integer.MAX_VALUE, Integer.MAX_VALUE);
					} else {
						Size margin = getElement().getStyle().get(LayoutStyle.margin).get();
						Size padding = getElement().getStyle().get(LayoutStyle.padding).get();
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
						Size margin = getElement().getStyle().get(LayoutStyle.margin).get();
						Size padding = getElement().getStyle().get(LayoutStyle.padding).get();
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
		ObservableCollection<? extends TableColumnWidget<R, C>> tableColumns = getColumns();
		try (Transaction colT = tableColumns.lock(false, null)) {
			for (TableColumn column : tableColumns)
				column.doLayout();
			Orientation orient = getElement().atts().get(LayoutAttributes.orientation).get();
			int rowDim = bounds().get(orient).getSize();
			int colDim = bounds().get(orient.opposite()).getSize();
			int margin = getElement().getStyle().get(LayoutStyle.margin).get().evaluate(colDim);
			int padding = getElement().getStyle().get(LayoutStyle.padding).get().evaluate(colDim);
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

	@Override
	public QuickElementCapture[] paintChildren(Graphics2D graphics, Rectangle area) {
		if (getElement().getColumnValues() == null)
			return super.paintChildren(graphics, area);
		else {
			// TODO Render the stamped columns
			// Return the element capture for the hovered cell
			return null;
		}
	}
}
