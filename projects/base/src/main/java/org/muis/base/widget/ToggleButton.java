package org.muis.base.widget;

import org.muis.base.BaseConstants;
import org.muis.core.event.AttributeChangedEvent;
import org.muis.core.event.MuisEvent;
import org.muis.core.event.UserEvent;
import org.muis.core.mgr.MuisState;
import org.muis.core.model.ModelAttributes;
import org.muis.core.model.MuisModelValue;
import org.muis.core.model.MuisModelValueEvent;
import org.muis.core.tags.State;
import org.muis.core.tags.StateSupport;

/**
 * A button that has a {@link org.muis.base.BaseConstants.States#SELECTED selected} state and modifies a boolean model value based on that
 * state and fires no actions
 */
@StateSupport({@State(name = BaseConstants.States.SELECTED_NAME, priority = BaseConstants.States.SELECTED_PRIORITY)})
public class ToggleButton extends Button {
	private org.muis.core.mgr.StateEngine.StateController theSelectedController;

	private org.muis.core.model.MuisModelValueListener<Boolean> theValueListener;

	private org.muis.core.model.WidgetRegistration theRegistration;

	/** Creates a toggle button */
	public ToggleButton() {
		super(false);
		theValueListener = new org.muis.core.model.MuisModelValueListener<Boolean>() {
			@Override
			public void valueChanged(MuisModelValueEvent<? extends Boolean> evt) {
				if(state().is(BaseConstants.States.SELECTED) == evt.getNewValue().booleanValue())
					return;
				theSelectedController.setActive(evt.getNewValue(), evt.getUserEvent());
			}
		};
		atts().accept(new Object(), ModelAttributes.value);
		addListener(org.muis.core.MuisConstants.Events.ATTRIBUTE_CHANGED,
			new org.muis.core.event.AttributeChangedListener<MuisModelValue<?>>(ModelAttributes.value) {
				@Override
				public void attributeChanged(AttributeChangedEvent<MuisModelValue<?>> event) {
					setModelValue(event.getOldValue(), event.getValue());
				}
			});
		setModelValue(null, atts().get(ModelAttributes.value));
		theSelectedController = state().control(BaseConstants.States.SELECTED);
		state().addListener(BaseConstants.States.SELECTED, new org.muis.core.mgr.StateEngine.StateListener() {
			@Override
			public void entered(MuisState state, MuisEvent<?> cause) {
				valueChanged(true, cause instanceof UserEvent ? (UserEvent) cause : null);
			}

			@Override
			public void exited(MuisState state, MuisEvent<?> cause) {
				valueChanged(false, cause instanceof UserEvent ? (UserEvent) cause : null);
			}
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
			if(newValue.getType() != Boolean.class && newValue.getType() != Boolean.TYPE) {
				msg().error("Toggle button backed by non-boolean model: " + newValue.getType(), "modelValue", newValue);
				return;
			}
			((MuisModelValue<Boolean>) newValue).addListener(theValueListener);
			if(newValue instanceof org.muis.core.model.WidgetRegister)
				theRegistration = ((org.muis.core.model.WidgetRegister) newValue).register(this);
			setEnabled(newValue.isMutable(), null);
		} else {
			setEnabled(true, null);
		}
	}

	private void valueChanged(boolean value, UserEvent cause) {
		MuisModelValue<Boolean> modelValue = (MuisModelValue<Boolean>) atts().get(ModelAttributes.value);
		if(modelValue == null)
			return;
		if(modelValue.getType() != Boolean.class && modelValue.getType() != Boolean.TYPE)
			return;
		if(modelValue.get().booleanValue() == value)
			return;
		try {
			modelValue.set(value, cause);
		} catch(RuntimeException e) {
			msg().error("Model value threw exception for value \"" + value + "\"", e);
			theSelectedController.setActive(modelValue.get(), null);
		}
	}
}
