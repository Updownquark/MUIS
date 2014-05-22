package org.muis.base.widget;

import org.muis.base.BaseConstants;
import org.muis.core.event.*;
import org.muis.core.model.ModelAttributes;
import org.muis.core.model.MuisModelValue;
import org.muis.core.tags.State;
import org.muis.core.tags.StateSupport;

/**
 * A button that has a {@link org.muis.base.BaseConstants.States#SELECTED selected} state and modifies a boolean model value based on that
 * state and fires no actions
 */
@StateSupport({@State(name = BaseConstants.States.SELECTED_NAME, priority = BaseConstants.States.SELECTED_PRIORITY)})
public class ToggleButton extends Button {
	private org.muis.core.mgr.StateEngine.StateController theSelectedController;

	private org.muis.core.rx.ObservableValueListener<Boolean> theValueListener;

	private org.muis.core.model.WidgetRegistration theRegistration;

	/** Creates a toggle button */
	public ToggleButton() {
		super(false);
		theValueListener = evt -> {
			if(state().is(BaseConstants.States.SELECTED) == evt.getNewValue().booleanValue())
				return;
			theSelectedController.setActive(evt.getNewValue(), org.muis.core.model.MuisModelValueEvent.getUserEvent(evt));
		};
		atts().accept(new Object(), ModelAttributes.value);
		events().listen(AttributeChangedEvent.att(ModelAttributes.value), (AttributeChangedEvent<MuisModelValue<?>> event) -> {
			setModelValue(event.getOldValue(), event.getValue());
		});
		setModelValue(null, atts().get(ModelAttributes.value));
		theSelectedController = state().control(BaseConstants.States.SELECTED);
		events().listen(StateChangedEvent.state(BaseConstants.States.SELECTED), (StateChangedEvent event) -> {
			MuisEvent cause = event.getCause();
			if(event.getValue())
				valueChanged(true, cause instanceof UserEvent ? (UserEvent) cause : null);
			else
				valueChanged(false, cause instanceof UserEvent ? (UserEvent) cause : null);
		});
	}

	@Override
	protected void action(UserEvent cause) {
		if(!state().is(BaseConstants.States.ENABLED))
			return;
		theSelectedController.setActive(!state().is(BaseConstants.States.SELECTED), cause);
	}

	private void setModelValue(MuisModelValue<?> oldValue, MuisModelValue<?> newValue) {
		if(oldValue != null) {
			oldValue.removeListener(theValueListener);
			if(theRegistration != null)
				theRegistration.unregister();
		}
		if(newValue != null) {
			if(!newValue.getType().canAssignTo(Boolean.TYPE)) {
				msg().error("Toggle button backed by non-boolean model: " + newValue.getType(), "modelValue", newValue);
				return;
			}
			((MuisModelValue<Boolean>) newValue).addListener(theValueListener);
			if(newValue instanceof org.muis.core.model.WidgetRegister)
				theRegistration = ((org.muis.core.model.WidgetRegister) newValue).register(this);
			// setEnabled(newValue.isMutable(), null); TODO
			theSelectedController.setActive((Boolean) newValue.get(), null);
		} else {
			setEnabled(true, null);
		}
	}

	private void valueChanged(boolean value, UserEvent cause) {
		MuisModelValue<Boolean> modelValue = (MuisModelValue<Boolean>) atts().get(ModelAttributes.value);
		if(modelValue == null)
			return;
		if(modelValue.getType().canAssignTo(Boolean.TYPE))
			return;
		if(modelValue.get().booleanValue() == value)
			return;
		try {
			modelValue.set(value, cause);
		} catch(RuntimeException e) {
			msg().error("Model value threw exception for value \"" + value + "\"", e);
		}
		if(modelValue.get().booleanValue() != value)
			theSelectedController.setActive(modelValue.get(), null);
	}
}
