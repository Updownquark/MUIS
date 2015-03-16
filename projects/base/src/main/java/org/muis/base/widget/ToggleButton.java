package org.muis.base.widget;

import org.muis.base.BaseConstants;
import org.muis.core.MuisConstants;
import org.muis.core.event.MuisEvent;
import org.muis.core.event.StateChangedEvent;
import org.muis.core.event.UserEvent;
import org.muis.core.model.ModelAttributes;
import org.muis.core.tags.State;
import org.muis.core.tags.StateSupport;
import org.muis.rx.ObservableValue;
import org.muis.util.MuisUtils;

/**
 * A button that has a {@link org.muis.base.BaseConstants.States#SELECTED selected} state and modifies a boolean model value based on that
 * state and fires no actions
 */
@StateSupport({@State(name = BaseConstants.States.SELECTED_NAME, priority = BaseConstants.States.SELECTED_PRIORITY)})
public class ToggleButton extends Button {
	private org.muis.core.mgr.StateEngine.StateController theSelectedController;

	private org.muis.core.model.WidgetRegistration theRegistration;

	/** Creates a toggle button */
	public ToggleButton() {
		super(false);
		life().runWhen(
			() -> {
				ObservableValue<ObservableValue<?>> mv = atts().accept(new Object(), ModelAttributes.value);
				mv.act(evt -> {
					if(theRegistration != null)
						theRegistration.unregister();
					if(evt.getValue() instanceof org.muis.core.model.WidgetRegister)
						theRegistration = ((org.muis.core.model.WidgetRegister) evt.getValue()).register(this);
					if(evt.getValue() != null) {
						if(!evt.getValue().getType().canAssignTo(Boolean.TYPE)) {
							msg().error("Toggle button backed by non-boolean model: " + evt.getObservable().getType(), "modelValue",
								evt.getValue());
							return;
						}
						((ObservableValue<Boolean>) evt.getValue()).takeUntil(mv).act(evt2 -> {
							if(state().is(BaseConstants.States.SELECTED) != evt2.getValue())
								theSelectedController.set(evt2.getValue(), MuisUtils.getUserEvent(evt2));
						});
					} else
						setEnabled(true, MuisUtils.getUserEvent(evt));
				});
				theSelectedController = state().control(BaseConstants.States.SELECTED);
				events().filterMap(StateChangedEvent.state(BaseConstants.States.SELECTED)).act(event -> {
					MuisEvent cause = event.getCause();
					if(event.getValue())
						valueChanged(true, cause instanceof UserEvent ? (UserEvent) cause : null);
					else
						valueChanged(false, cause instanceof UserEvent ? (UserEvent) cause : null);
				});
			}, MuisConstants.CoreStage.INITIALIZED.toString(), 1);
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
		if(!(modelValue instanceof org.muis.rx.SettableValue) || modelValue.getType().canAssignTo(Boolean.TYPE))
			return;
		if(modelValue.get().booleanValue() == value)
			return;
		try {
			((org.muis.rx.SettableValue<Boolean>) modelValue).set(value, cause);
		} catch(RuntimeException e) {
			msg().error("Model value threw exception for value \"" + value + "\"", e);
		}
		if(modelValue.get().booleanValue() != value)
			theSelectedController.set(modelValue.get(), null);
	}
}
