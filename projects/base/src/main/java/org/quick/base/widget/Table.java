package org.quick.base.widget;

import org.observe.collect.ObservableCollection;
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
			// TODO Probably need to keep a full row x column cache table of row sizers to optimize size recalculations due to model changes
			atts().get(rows).get().changes().act(evt -> {
				// TODO Use the columns to figure out how the row model change affects the cached sizer.
				sizeNeedsChanged();
			});
			ObservableCollection<?> cols = atts().get(columns).get();
			if (cols != null) {
				// TODO Ensure there's initially one column
				// TODO Store the type of that column (or some means to create new columns of the same type and attributes).
				if (!cols.isEmpty()) {
					// TODO Set the column model value for the existing column, then duplicate it for each additional column
				} else {
					// TODO Remove the initial column
				}
				cols.changes().noInit().act(evt -> {
					// TODO Add/remove/update columns
					// TODO Adjust the rowSizer as well since columns determine the row size
				});
			}
			// TODO Dynamically ensure the columns are sorted by their header attribute (type End: leading or trailing)
			// TODO Populate the row-headers' rows with the columns attribute content (if set) or the actual column children (otherwise)
		}, CoreStage.INIT_CHILDREN, 1);

		// TODO Plumb in a way for the table columns to learn the sizes of each row value
	}
}
