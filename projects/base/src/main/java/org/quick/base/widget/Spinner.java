package org.quick.base.widget;

import org.observe.ObservableAction;
import org.quick.base.BaseAttributes;
import org.quick.base.layout.TextEditLayout;
import org.quick.core.QuickTemplate;
import org.quick.core.QuickTextElement;
import org.quick.core.model.ModelAttributes;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.tags.AcceptAttribute;
import org.quick.core.tags.QuickElementType;
import org.quick.core.tags.Template;

/** A text box with up and down arrows to increment or decrement the value */
@Template(location = "../../../../spinner.qml")
@QuickElementType(
	attributes = { //
		@AcceptAttribute(declaringClass = Spinner.class, field = "increment", required = true), //
		@AcceptAttribute(declaringClass = Spinner.class, field = "decrement", required = true), //
		@AcceptAttribute(declaringClass = ModelAttributes.class, field = "value"), //
		@AcceptAttribute(declaringClass = BaseAttributes.class, field = "format"), //
		@AcceptAttribute(declaringClass = BaseAttributes.class, field = "document"), //
		@AcceptAttribute(declaringClass = BaseAttributes.class, field = "rich"), //
		@AcceptAttribute(declaringClass = TextEditLayout.class, field = "charLengthAtt"), //
		@AcceptAttribute(declaringClass = TextEditLayout.class, field = "charRowsAtt"), //
		@AcceptAttribute(declaringClass = QuickTextElement.class, field = "multiLine") //
	})
public class Spinner extends QuickTemplate {
	/** The increment attribute, specifying the action to perform when the user clicks the up arrow */
	public static final QuickAttribute<ObservableAction<?>> increment = QuickAttribute.build("increment", ModelAttributes.actionType)
		.build();
	/** The decrement attribute, specifying the action to perform when the user clicks the down arrow */
	public static final QuickAttribute<ObservableAction<?>> decrement = QuickAttribute.build("decrement", ModelAttributes.actionType)
		.build();

	/** Creates a spinner */
	public Spinner() {
	}
}
