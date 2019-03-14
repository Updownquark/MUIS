package org.quick.base.widget;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;

import org.observe.ObservableValueEvent;
import org.observe.SettableValue;
import org.observe.SimpleSettableValue;
import org.observe.collect.CollectionChangeType;
import org.observe.collect.ObservableCollection;
import org.observe.util.TypeTokens;
import org.qommons.collect.ElementId;
import org.quick.core.QuickConstants.CoreStage;
import org.quick.core.QuickTemplate;
import org.quick.core.layout.LayoutAttributes;
import org.quick.core.layout.Orientation;
import org.quick.core.mgr.AttributeManager2;
import org.quick.core.model.ModelAttributes;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.prop.QuickPropertyType;
import org.quick.core.style.Size;
import org.quick.core.tags.AcceptAttribute;
import org.quick.core.tags.QuickElementType;
import org.quick.core.tags.Template;

@QuickElementType(attributes = { //
	@AcceptAttribute(declaringClass = Table.class, field = "rows", required = true), //
	@AcceptAttribute(declaringClass = Table.class, field = "columns", required = false), //
	@AcceptAttribute(declaringClass = LayoutAttributes.class, field = "orientation", required = true, defaultValue = "VERTICAL"), //
	@AcceptAttribute(declaringClass = Table.class, field = "rowValueModelName", required = true, defaultValue = "row"), //
	@AcceptAttribute(declaringClass = Table.class, field = "rowValueElementNameName", required = true, defaultValue = "rowElement") //
})
@Template(location = "../../../../table.qts")
public class Table<R, C> extends QuickTemplate {
	public static final QuickAttribute<ObservableCollection<?>> rows = QuickAttribute.build("rows", ModelAttributes.collectionType).build();
	public static final QuickAttribute<ObservableCollection<?>> columns = QuickAttribute.build("columns", ModelAttributes.collectionType)
		.build();
	public static final QuickAttribute<String> rowValueModelName = QuickAttribute.build("row-value", QuickPropertyType.string).build();
	public static final QuickAttribute<String> rowElementModelName = QuickAttribute.build("row-element", QuickPropertyType.string).build();
	public static final QuickAttribute<String> columnValueModelName = QuickAttribute.build("column-value", QuickPropertyType.string)
		.build();
	public static final QuickAttribute<String> columnElementModelName = QuickAttribute.build("column-element", QuickPropertyType.string)
		.build();
	public static final QuickAttribute<Size> rowHeight = QuickAttribute.build("row-height", LayoutAttributes.sizeType).build();
	public static final QuickAttribute<Size> rowWidth = QuickAttribute.build("row-width", LayoutAttributes.sizeType).build();

	private AttachPoint<TableColumn<R, C>> theColumnAP;
	private ObservableCollection<R> theRows;
	private ObservableCollection<C> theColumnValues;
	private ObservableCollection<TableColumn<R, C>> theColumns;
	private final SettableValue<ElementId> theSelectedRow;
	private final SettableValue<ElementId> theHoveredRow;
	private final SettableValue<ElementId> theSelectedColumn;
	private final SettableValue<ElementId> theHoveredColumn;

	private String theColumnElementModelName;
	private String theColumnValueModelName;

	public Table() {
		theSelectedRow = new SimpleSettableValue<>(TypeTokens.get().of(ElementId.class), true, getAttributeLocker(), null)
			.filterAccept(v -> {
				if (v == null)
					return null;
				try {
					theRows.getElement(v);
					return null;
				} catch (NoSuchElementException e) {
					return "Element " + v + " is not a row in this table";
				}
			});
		theHoveredRow = new SimpleSettableValue<>(TypeTokens.get().of(ElementId.class), true, getAttributeLocker(), null)
			.filterAccept(v -> {
				if (v == null)
					return null;
				try {
					theRows.getElement(v);
					return null;
				} catch (NoSuchElementException e) {
					return "Element " + v + " is not a row in this table";
				}
			});
		theSelectedColumn = new SimpleSettableValue<>(TypeTokens.get().of(ElementId.class), true, getAttributeLocker(), null)
			.filterAccept(v -> {
				if (v == null)
					return null;
				try {
					theColumns.getElement(v);
					return null;
				} catch (NoSuchElementException e) {
					return "Element " + v + " is not a column in this table";
				}
			});
		theHoveredColumn = new SimpleSettableValue<>(TypeTokens.get().of(ElementId.class), true, getAttributeLocker(), null)
			.filterAccept(v -> {
				if (v == null)
					return null;
				try {
					theColumns.getElement(v);
					return null;
				} catch (NoSuchElementException e) {
					return "Element " + v + " is not a column in this table";
				}
			});
		life().runWhen(() -> {
			// The value of the rows and columns attributes cannot be changed dynamically
			// This is to ensure that the types, which are relied upon by the columns, never change
			// The values of these attributes *can* be a flattened collection that is switched out internally
			// because this cannot affect the type
			ensureConstant(rows);
			ensureConstant(columns);
			ensureConstant(rowValueModelName);
			ensureConstant(rowElementModelName);
			theRows = (ObservableCollection<R>) atts().get(rows).get();
			theColumnValues = (ObservableCollection<C>) atts().get(columns).get();
			if (theColumnValues != null) {
				theColumnValueModelName = atts().accept(columnValueModelName, this, aa -> aa.required().init("column")).get();
				theColumnElementModelName = atts().accept(columnElementModelName, this, aa -> aa.required().init("columnElement")).get();
				ensureConstant(columnValueModelName);
				ensureConstant(columnElementModelName);
			}
			// Accept row-height for a vertical table and row-width for a horizontal one
			atts().get(LayoutAttributes.orientation).changes().act(new Consumer<ObservableValueEvent<Orientation>>() {
				private AttributeManager2.AttributeAcceptance rowDimAcc;

				@Override
				public void accept(ObservableValueEvent<Orientation> evt) {
					if (!evt.isInitial() && evt.getOldValue() != evt.getNewValue()) {
						rowDimAcc.reject();
					}
					if (evt.getNewValue().isVertical())
						atts().accept(rowHeight, Table.this, acc -> rowDimAcc = acc);
					else
						atts().accept(rowWidth, Table.this, acc -> rowDimAcc = acc);
				}
			});
		}, CoreStage.INIT_SELF, -1);
		// TODO There are several ObservableCollection.onChange calls here, which may be memory leaks if the table is disposed
		// and the row/column values are not
		life().runWhen(() -> {
			theColumnAP = (AttachPoint<TableColumn<R, C>>) getTemplate().getAttachPoint("column");
			// TODO Probably need to keep a full row x column cache table of row sizers to optimize size recalculations due to model changes
			theColumns = (ObservableCollection<TableColumn<R, C>>) getContainer(theColumnAP).getContent();
			if (theColumnValues != null) {
				// TODO One day, it might be better to make the columns virtual when they are variable
				// I.e. instead of creating a new physical table column for each column value, publish the template and the column values
				// and let the implementation create stamped columns
				// Columns should usually be less numerous or variable than rows though, so this is good enough for now
				// And it might be good enough forever

				// Ensure there's initially one column
				if (theColumns.size() != 1) {
					msg().fatal(
						"When column values are specified in the columns attribute, only one column child may be specified, but found "
							+ theColumns.size());
					return;
				}
				TableColumn<R, C> templateColumn = theColumns.getFirst();
				theColumns.clear();
				boolean[] areCVsChanging = new boolean[1];
				theColumnValues.subscribe(evt -> {
					areCVsChanging[0] = true;
					try {
						switch (evt.getType()) {
						case add:
							TableColumn<R, C> newColumn = templateColumn.copy(Table.this, null);
							newColumn.setColumnValue(theColumnValues.mutableElement(evt.getElementId()), theColumnElementModelName,
								theColumnValueModelName);
							theColumns.add(evt.getIndex(), newColumn);
							break;
						case remove:
							theColumns.remove(evt.getIndex());
							break;
						case set:
							theColumns.get(evt.getIndex()).updateColumnValue();
							break;
						}
					} finally {
						areCVsChanging[0] = false;
					}
				}, true);
				theColumns.onChange(evt -> {
					if (!areCVsChanging[0]) {
						msg().error("Column children should never be added externally when column values are set");
					}
				});
			} else {
				// TODO Dynamically ensure the columns are sorted by their header attribute (type End: leading or trailing)
			}
			theRows.onChange(evt -> {
				if (evt.getType() == CollectionChangeType.remove) {
					if (evt.getOldValue().equals(theSelectedRow.get()))
						theSelectedRow.set(null, evt);
					if (evt.getOldValue().equals(theHoveredRow.get()))
						theHoveredRow.set(null, evt);
				}
			});
			theColumns.onChange(evt -> {
				switch (evt.getType()) {
				case add:
					evt.getNewValue().initColumnId(evt.getElementId());
					break;
				case remove:
					if (evt.getOldValue().equals(theSelectedColumn.get()))
						theSelectedColumn.set(null, evt);
					if (evt.getOldValue().equals(theHoveredColumn.get()))
						theHoveredColumn.set(null, evt);
					break;
				case set:
					if (evt.getOldValue() != evt.getNewValue())
						evt.getNewValue().initColumnId(evt.getElementId());
					break;
				}
			});
			// TODO Populate the row-headers' rows with the columns attribute content (if set) or the actual column children (otherwise)
		}, CoreStage.INIT_CHILDREN, 1);
	}

	public ObservableCollection<R> getRows() {
		return theRows;
	}

	public ObservableCollection<C> getColumnValues() {
		return theColumnValues;
	}

	public ObservableCollection<TableColumn<R, C>> getColumns() {
		return theColumns;
	}

	public void setSelected(ElementId columnId, ElementId row, Object cause) {
		if (!Objects.equals(row, theSelectedRow.get()))
			theSelectedRow.set(row, cause);
		if (!Objects.equals(columnId, theSelectedColumn.get()))
			theSelectedColumn.set(columnId, cause);
	}

	public void setHovered(ElementId columnId, ElementId row, Object cause) {
		if (!Objects.equals(columnId, theSelectedRow.get()))
			theHoveredRow.set(row, cause);
		if (!Objects.equals(columnId, theHoveredColumn.get()))
			theHoveredColumn.set(columnId, cause);
	}

	public SettableValue<ElementId> getSelectedRow() {
		return theSelectedRow;
	}

	public SettableValue<ElementId> getHoveredRow() {
		return theHoveredRow;
	}

	public SettableValue<ElementId> getSelectedColumn() {
		return theSelectedColumn;
	}

	public SettableValue<ElementId> getHoveredColumn() {
		return theHoveredColumn;
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
