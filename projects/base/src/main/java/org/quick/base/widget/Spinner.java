package org.quick.base.widget;

import static org.quick.base.layout.TextEditLayout.charLengthAtt;
import static org.quick.base.layout.TextEditLayout.charRowsAtt;
import static org.quick.core.QuickTextElement.multiLine;

import org.observe.ObservableAction;
import org.quick.base.BaseAttributes;
import org.quick.base.model.QuickFormatter;
import org.quick.core.QuickTemplate;
import org.quick.core.model.ModelAttributes;
import org.quick.core.model.QuickDocumentModel;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.tags.ModelAttribute;
import org.quick.core.tags.Template;

/** A text box with up and down arrows to increment or decrement the value */
@Template(location = "../../../../spinner.qck", //
	attributes = { //
		@ModelAttribute(name = "value", type = Object.class), //
		@ModelAttribute(name = "increment", type = ObservableAction.class), //
		@ModelAttribute(name = "decrement", type = ObservableAction.class), //
		@ModelAttribute(name = "format", type = QuickFormatter.class), //
		@ModelAttribute(name = "length", type = Integer.class), //
		@ModelAttribute(name = "rows", type = Integer.class), //
		@ModelAttribute(name = "multi-line", type = Boolean.class), //
		@ModelAttribute(name = "document", type = QuickDocumentModel.class), //
		@ModelAttribute(name = "rich", type = Boolean.class)
	})
public class Spinner extends QuickTemplate {
	/** The increment attribute, specifying the action to perform when the user clicks the up arrow */
	public static final QuickAttribute<ObservableAction> increment = QuickAttribute.build("increment", ModelAttributes.actionType).build();
	/** The decrement attribute, specifying the action to perform when the user clicks the down arrow */
	public static final QuickAttribute<ObservableAction> decrement = QuickAttribute.build("decrement", ModelAttributes.actionType).build();

	/** Creates a spinner */
	public Spinner() {
		life().runWhen(() -> {
			Object accepter = new Object();
			atts().require(accepter, increment, decrement)//
				.accept(accepter, ModelAttributes.value, BaseAttributes.format, charLengthAtt, charRowsAtt, multiLine,
					BaseAttributes.document, BaseAttributes.rich);
		}, org.quick.core.QuickConstants.CoreStage.INIT_SELF.toString(), 1);
	}
}
