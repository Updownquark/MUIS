package org.quick.base.widget;

import java.util.function.Consumer;

import org.observe.*;
import org.observe.util.TypeTokens;
import org.quick.base.BaseAttributes;
import org.quick.base.layout.TextEditLayout;
import org.quick.base.model.AdjustableFormatter;
import org.quick.core.QuickTemplate;
import org.quick.core.QuickTextElement;
import org.quick.core.event.KeyBoardEvent;
import org.quick.core.model.ModelAttributes;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.tags.AcceptAttribute;
import org.quick.core.tags.QuickElementType;
import org.quick.core.tags.Template;

import com.google.common.reflect.TypeToken;

/** A text box with up and down arrows to increment or decrement the value */
@Template(location = "../../../../spinner.qts")
@QuickElementType(attributes = { //
	@AcceptAttribute(declaringClass = Spinner.class, field = "increment"), //
	@AcceptAttribute(declaringClass = Spinner.class, field = "decrement"), //
	@AcceptAttribute(declaringClass = ModelAttributes.class, field = "value", required = true), //
	@AcceptAttribute(declaringClass = BaseAttributes.class, field = "format"), //
	@AcceptAttribute(declaringClass = BaseAttributes.class, field = "formatFactory"), //
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
		life().runWhen(() -> {
			setButtonAction(true);
			setButtonAction(false);
		}, org.quick.core.QuickConstants.CoreStage.INIT_CHILDREN.toString(), 1);
		events().filterMap(KeyBoardEvent.key).act(new Consumer<KeyBoardEvent>() {
			private Button.ClickControl theUpControl;
			private Button.ClickControl theDownControl;

			@Override
			public void accept(KeyBoardEvent event) {
				theUpControl = Button.release(theUpControl, event);
				theDownControl = Button.release(theDownControl, event);
				if (Boolean.TRUE.equals(atts().get(QuickTextElement.multiLine).get())) {
					return;
				}
				if (event.getKeyCode() == KeyBoardEvent.KeyCode.UP_ARROW) {
					if (event.wasPressed())
						theUpControl = getAdjust(true).press(event);
				} else if (event.getKeyCode() == KeyBoardEvent.KeyCode.DOWN_ARROW) {
					if (event.wasPressed())
						theDownControl = getAdjust(false).press(event);
				}
			}
		});
	}

	/** @return This spinner's text field */
	protected TextField getTextField() {
		return (TextField) getElement(getTemplate().getAttachPoint("text")).get();
	}

	/**
	 * @param up Whether to get the up or down button
	 * @return The button adjuster (up or down) for this spinner
	 */
	protected Button getAdjust(boolean up) {
		return (Button) getElement(getTemplate().getAttachPoint(up ? "up" : "down")).get();
	}

	private void setButtonAction(boolean up) {
		QuickAttribute<ObservableAction<?>> incOrDec = up ? increment : decrement;
		TextField textField = getTextField();
		SettableValue<Object> value = textField.getValue();
		try {
			ObservableValue<ObservableAction<?>> actionObs = atts().get(incOrDec).combine(//
				TypeTokens.get().keyFor(ObservableAction.class).parameterized(() -> new TypeToken<ObservableAction<?>>() {}),
				(action, format, val) -> {
					if (action != null)
						return action;
					else if (format instanceof AdjustableFormatter) {
						AdjustableFormatter<Object> adjFormat = (AdjustableFormatter<Object>) format;
						if (val != null) {
							return new ObservableAction<Object>() {
								@Override
								public TypeToken<Object> getType() {
									return adjFormat.getFormatType();
								}

								@Override
								public Object act(Object cause) {
									String enabled = isEnabled().get();
									if (enabled != null)
										throw new IllegalStateException(enabled);
									Object newValue;
									if (incOrDec == increment)
										newValue = adjFormat.increment(val);
									else
										newValue = adjFormat.decrement(val);
									value.set(newValue, cause);
									return newValue;
								}

								@Override
								public ObservableValue<String> isEnabled() {
									ObservableValue<String> opEnabled;
									ObservableValue<String> adjustEnabled;
									if (incOrDec == increment) {
										opEnabled = ObservableValue.of(TypeTokens.get().STRING, adjFormat.isIncrementEnabled(val));
										adjustEnabled = ObservableValue.of(TypeTokens.get().STRING,
											value.isAcceptable(adjFormat.increment(val)));
									} else {
										opEnabled = ObservableValue.of(TypeTokens.get().STRING, adjFormat.isDecrementEnabled(val));
										adjustEnabled = ObservableValue.of(TypeTokens.get().STRING,
											value.isAcceptable(adjFormat.decrement(val)));
									}
									return ObservableValue.firstValue(TypeToken.of(String.class), v -> v != null, value.isEnabled(),
										opEnabled, adjustEnabled);
								}
							};
						} else
							return ObservableAction.disabled(TypeToken.of(Object.class), "No value");
					} else
						return ObservableAction.disabled(TypeToken.of(Object.class), "No " + incOrDec.getName() + " action set");
				}, textField.atts().get(BaseAttributes.format), value, null);
			actionObs = actionObs.refresh(Observable.flatten(textField.getDocumentModel().value().map(doc -> doc.simpleChanges())));
			getAdjust(up).atts().get(ModelAttributes.action).set(ObservableAction.flatten(actionObs), null);
		} catch (IllegalArgumentException e) {
			msg().error("Could not set action for " + (up ? "up" : "down") + " button", e);
		}
	}
}
