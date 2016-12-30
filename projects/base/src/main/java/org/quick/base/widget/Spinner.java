package org.quick.base.widget;

import org.observe.ObservableAction;
import org.observe.ObservableValue;
import org.quick.base.BaseAttributes;
import org.quick.base.layout.TextEditLayout;
import org.quick.base.model.AdjustableFormatter;
import org.quick.core.QuickException;
import org.quick.core.QuickTemplate;
import org.quick.core.QuickTextElement;
import org.quick.core.model.ModelAttributes;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.tags.AcceptAttribute;
import org.quick.core.tags.QuickElementType;
import org.quick.core.tags.Template;

import com.google.common.reflect.TypeToken;

/** A text box with up and down arrows to increment or decrement the value */
@Template(location = "../../../../spinner.qts")
@QuickElementType(
	attributes = { //
		@AcceptAttribute(declaringClass = Spinner.class, field = "increment"), //
		@AcceptAttribute(declaringClass = Spinner.class, field = "decrement"), //
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
		life().runWhen(()->{
			setButtonAction("up", increment);
			setButtonAction("down", decrement);
		}, org.quick.core.QuickConstants.CoreStage.INIT_CHILDREN.toString(), 1);
	}

	private void setButtonAction(String attach, QuickAttribute<ObservableAction<?>> incOrDec) {
		TextField textField = (TextField) getElement(getTemplate().getAttachPoint("text")).get();
		try {
			ObservableValue<ObservableAction<?>> iodA = atts().observe(incOrDec);
			ObservableAction<?>[] a = new ObservableAction[1];
			iodA.value().act(action -> //
			a[0] = action//
			);
			ObservableValue<ObservableAction<?>> actionObs = atts().observe(incOrDec).combineV(new TypeToken<ObservableAction<?>>() {},
				(action, format, value) -> {
					if (action != null)
						return action;
					else if (format instanceof AdjustableFormatter) {
						AdjustableFormatter<Object> adjFormat = (AdjustableFormatter<Object>) format;
						if (value != null) {
							return new ObservableAction<Object>() {
								@Override
								public TypeToken<Object> getType() {
									return adjFormat.getFormatType();
								}

								@Override
								public Object act(Object cause) {
									if (incOrDec == increment)
										return adjFormat.increment(value);
									else
										return adjFormat.decrement(value);
								}

								@Override
								public ObservableValue<String> isEnabled() {
									if (incOrDec == increment)
										return ObservableValue.constant(TypeToken.of(String.class), adjFormat.isIncrementEnabled(value));
									else
										return ObservableValue.constant(TypeToken.of(String.class), adjFormat.isDecrementEnabled(value));
								}
							};
						} else
							return ObservableAction.disabled(TypeToken.of(Object.class), "No value");
					} else
						return ObservableAction.disabled(TypeToken.of(Object.class), "No " + incOrDec.getName() + " action set");
				}, atts().observe(BaseAttributes.format), textField.getValue(), true);
			getElement(getTemplate().getAttachPoint(attach)).get().atts().set(ModelAttributes.action, ObservableAction.flatten(actionObs));
		} catch (QuickException e) {
			msg().error("Could not set action for " + attach + " button", e);
		}
	}
}
