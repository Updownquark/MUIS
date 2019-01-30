package org.quick.base.widget;

import java.util.*;

import org.observe.collect.CollectionChangeEvent.ElementChange;
import org.observe.collect.ObservableCollection;
import org.qommons.Transaction;
import org.quick.core.QuickConstants.CoreStage;
import org.quick.core.QuickTemplate;
import org.quick.core.layout.LayoutAttributes;
import org.quick.core.layout.SimpleSizeGuide;
import org.quick.core.model.ModelAttributes;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.prop.QuickPropertyType;
import org.quick.core.tags.AcceptAttribute;
import org.quick.core.tags.QuickElementType;
import org.quick.core.tags.Template;

@QuickElementType(attributes = { //
	@AcceptAttribute(declaringClass = Table.class, field = "rows", required = true), //
	@AcceptAttribute(declaringClass = Table.class, field = "columns", required = false), //
	@AcceptAttribute(declaringClass = Table.class, field = "rowValueModelName", required = true, defaultValue = "rowValue"), //
	@AcceptAttribute(declaringClass = LayoutAttributes.class, field = "orientation", required = true, defaultValue = "VERTICAL")
})
@Template(location = "../../../../table.qts")
public class Table extends QuickTemplate {
	public static final QuickAttribute<ObservableCollection<?>> rows = QuickAttribute.build("rows", ModelAttributes.collectionType).build();
	public static final QuickAttribute<ObservableCollection<?>> columns = QuickAttribute.build("columns", ModelAttributes.collectionType)
		.build();
	public static final QuickAttribute<String> rowValueModelName = QuickAttribute.build("row-value-model-name", QuickPropertyType.string)
		.build();
	public static final QuickAttribute<String> columnValueModelName = QuickAttribute
		.build("column-value-model-name", QuickPropertyType.string).build();

	private AttachPoint<TableColumn> theColumnAP;
	private List<List<SimpleSizeGuide>> theCellSizes;
	private SimpleSizeGuide rowSizer;

	public Table() {
		rowSizer = new SimpleSizeGuide();
		life().runWhen(() -> {
			// The value of the rows and columns attributes cannot be changed dynamically
			// This is to ensure that the types, which are relied upon by the columns, never change
			// The values of these attributes *can* be a flattened collection that is switched out internally
			// because this cannot affect the type
			ObservableCollection<?> _rows = atts().get(rows).get();
			atts().get(rows).changes().noInit().act(evt -> {
				if (evt.getNewValue() != _rows) {
					msg().error(rows.getName() + " attribute cannot be changed");
					atts().setValue(rows, _rows, null);
				}
			});
			ObservableCollection<?> cols = atts().get(columns).get();
			if (cols != null)
				atts().accept(columnValueModelName, this, aa -> aa.required().init("columnValue"));
			atts().get(columns).changes().noInit().act(evt -> {
				if (evt.getNewValue() != cols) {
					msg().error(columns.getName() + " attribute cannot be changed");
					atts().setValue(columns, cols, null);
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
					switch (evt.type) {
					case add:
						for (ElementChange<?> el : evt.elements) {
							List<SimpleSizeGuide> rowSizes = new ArrayList<>();
							rowSizes.addAll(Collections.nCopies(cols.size(), null));
							theCellSizes.add(el.index, rowSizes);
						}
						sizeNeedsChanged = true;
						break;
					case remove:
						for (ElementChange<?> el : evt.getElements())
							theCellSizes.remove(el.index);
						sizeNeedsChanged = true;
						break;
					case set:
						for (ElementChange<?> el : evt.elements) {
							List<SimpleSizeGuide> rowSizes = theCellSizes.get(el.index);
							for (int c = 0; c < cols.size(); c++) {
								if (rowSizes.get(c) == null)
									continue;
								SimpleSizeGuide size = cols.get(c).getCellSize(el.newValue);
								if (!rowSizes.get(c).equals(size)) {
									rowSizes.set(c, size);
									sizeNeedsChanged = true;
								}
							}
						}
					}
				}
				if (sizeNeedsChanged)
					sizeNeedsChanged();
			});
			cols.subscribe(evt -> {
				boolean sizeNeedsChanged = false;
				try (Transaction t = _rows.lock(false, evt)) {
					switch (evt.getType()) {
					case add:
						for (List<SimpleSizeGuide> rowSize : theCellSizes)
							rowSize.add(evt.getIndex(), null);
						sizeNeedsChanged = true;
						break;
					case remove:
						for (List<SimpleSizeGuide> rowSize : theCellSizes)
							rowSize.remove(evt.getIndex());
						sizeNeedsChanged = true;
						break;
					case set:
						Iterator<?> rowIter = _rows.iterator();
						Iterator<List<SimpleSizeGuide>> sizeIter = theCellSizes.iterator();
						while (sizeIter.hasNext()) {
							Object row = rowIter.next();
							List<SimpleSizeGuide> rowSize = sizeIter.next();
							if (rowSize.get(evt.getIndex()) == null)
								continue;
							SimpleSizeGuide size = evt.getNewValue().getCellSize(row);
							if (!rowSize.get(evt.getIndex()).equals(size)) {
								rowSize.set(evt.getIndex(), size);
								sizeNeedsChanged = true;
							}
						}
						break;
					}
				}
				if (sizeNeedsChanged)
					sizeNeedsChanged();
			}, true);
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
			}
			// TODO Populate the row-headers' rows with the columns attribute content (if set) or the actual column children (otherwise)
		}, CoreStage.INIT_CHILDREN, 1);

		// TODO Plumb in a way for the table columns to learn the sizes of each row value
	}
}
