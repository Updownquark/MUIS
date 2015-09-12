package org.quick.base.widget;

import org.observe.ObservableValue;
import org.quick.base.BaseConstants;
import org.quick.core.QuickConstants;
import org.quick.core.event.QuickEvent;
import org.quick.core.event.StateChangedEvent;
import org.quick.core.event.UserEvent;
import org.quick.core.model.ModelAttributes;
import org.quick.core.tags.State;
import org.quick.core.tags.StateSupport;
import org.quick.util.QuickUtils;

/**
 * A button that has a {@link org.quick.base.BaseConstants.States#SELECTED selected} state and modifies a boolean model value based on that
 * state and fires no actions
 */
@StateSupport({@State(name = BaseConstants.States.SELECTED_NAME, priority = BaseConstants.States.SELECTED_PRIORITY)})
public class ToggleButton extends Button {
	private org.quick.core.mgr.StateEngine.StateController theSelectedController;

	private org.quick.core.model.WidgetRegistration theRegistration;

	/** Creates a toggle button */
	public ToggleButton() {
		super(false);
		life().runWhen(
			() -> {
				ObservableValue<ObservableValue<?>> mv = atts().accept(new Object(), ModelAttributes.value);
				mv.act(evt -> {
					if(theRegistration != null)
						theRegistration.unregister();
					if(evt.getValue() instanceof org.quick.core.model.WidgetRegister)
						theRegistration = ((org.quick.core.model.WidgetRegister) evt.getValue()).register(this);
					if(evt.getValue() != null) {
						if(!evt.getValue().getType().canAssignTo(Boolean.TYPE)) {
							msg().error("Toggle button backed by non-boolean model: " + evt.getObservable().getType(), "modelValue",
								evt.getValue());
							return;
						}
						((ObservableValue<Boolean>) evt.getValue()).takeUntil(mv).act(evt2 -> {
							if(state().is(BaseConstants.States.SELECTED) != evt2.getValue())
								theSelectedController.set(evt2.getValue(), QuickUtils.getUserEvent(evt2));
						});
					} else
						setEnabled(true, QuickUtils.getUserEvent(evt));
				});
				theSelectedController = state().control(BaseConstants.States.SELECTED);
				events().filterMap(StateChangedEvent.state(BaseConstants.States.SELECTED)).act(event -> {
					QuickEvent cause = event.getCause();
					if(event.getValue())
						valueChanged(true, cause instanceof UserEvent ? (UserEvent) cause : null);
					else
						valueChanged(false, cause instanceof UserEvent ? (UserEvent) cause : null);
				});
			}, QuickConstants.CoreStage.INITIALIZED.toString(), 1);
	}

	@Override
	protected void action(UserEvent cause) {
		if(!state().is(BaseConstants.States.ENABLED))
			return;
		theSelectedController.set(!state().is(BaseConstants.States.SELECTED), cause);
	}

	private void valueChanged(boolean value, UserEvent cause) {
		ObservableValue<Boolean> modelValue = (ObservableValue<Boolean>) atts().get(ModelAttributes.value);
		if(modelValue == null)
			return;
		if(!(modelValue instanceof org.observe.SettableValue) || modelValue.getType().canAssignTo(Boolean.TYPE))
			return;
		if(modelValue.get().booleanValue() == value)
			return;
		try {
			((org.observe.SettableValue<Boolean>) modelValue).set(value, cause);
		} catch(RuntimeException e) {
			msg().error("Model value threw exception for value \"" + value + "\"", e);
		}
		if(modelValue.get().booleanValue() != value)
			theSelectedController.set(modelValue.get(), null);
	}
}
