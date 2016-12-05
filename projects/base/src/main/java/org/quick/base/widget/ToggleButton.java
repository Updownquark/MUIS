package org.quick.base.widget;

import org.observe.ObservableAction;
import org.observe.SettableValue;
import org.quick.base.BaseConstants;
import org.quick.core.QuickConstants;
import org.quick.core.QuickException;
import org.quick.core.mgr.StateEngine;
import org.quick.core.model.ModelAttributes;
import org.quick.core.tags.AcceptAttribute;
import org.quick.core.tags.QuickElementType;
import org.quick.core.tags.State;

import com.google.common.reflect.TypeToken;

/**
 * A button that has a {@link org.quick.base.BaseConstants.States#SELECTED selected} state and modifies a boolean attribute value based on
 * that state
 */
@QuickElementType(//
	attributes = { //
		@AcceptAttribute(declaringClass = ModelAttributes.class, field = "selected", required = true)//
	}, states = { //
		@State(name = BaseConstants.States.SELECTED_NAME, priority = BaseConstants.States.SELECTED_PRIORITY)//
	})
public class ToggleButton extends Button {
	private StateEngine.StateController theSelectedController;

	private SettableValue<Boolean> theValue;

	/** Creates a toggle button */
	public ToggleButton() {
		theSelectedController = state().control(BaseConstants.States.SELECTED);
		life().runWhen(() -> {
			if (atts().getHolder(ModelAttributes.action).get() == null) {
				try {
					atts().set(ModelAttributes.action, ObservableAction.nullAction(TypeToken.of(Object.class), null));
				} catch (QuickException e) {
					msg().error("Could not set default value for action", e);
				}
			}
		}, QuickConstants.CoreStage.STARTUP.toString(), -1);
		life().runWhen(() -> {
			theSelectedController.link(theValue);
		}, QuickConstants.CoreStage.INITIALIZED.toString(), 1);
	}

	@Override
	protected ObservableAction<?> createAction() {
		theValue = atts().getHolder(ModelAttributes.selected).asSettable();
		theValue = theValue.mapV(theValue.getType(), b -> b == null ? false : b, b -> b,
			true);
		return ObservableAction.and(TypeToken.of(Object.class), ObservableAction.flatten(atts().getHolder(ModelAttributes.action)),
			theValue.assignmentTo(theValue.mapV(v -> !v)));
	}
}
