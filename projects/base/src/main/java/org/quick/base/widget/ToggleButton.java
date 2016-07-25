package org.quick.base.widget;

import org.observe.ObservableAction;
import org.observe.SettableValue;
import org.quick.base.BaseConstants;
import org.quick.core.mgr.StateEngine;
import org.quick.core.model.ModelAttributes;
import org.quick.core.tags.State;
import org.quick.core.tags.StateSupport;

/**
 * A button that has a {@link org.quick.base.BaseConstants.States#SELECTED selected} state and modifies a boolean attribute value based on
 * that state
 */
@StateSupport({@State(name = BaseConstants.States.SELECTED_NAME, priority = BaseConstants.States.SELECTED_PRIORITY)})
public class ToggleButton extends Button {
	private StateEngine.StateController theSelectedController;

	private SettableValue<Boolean> theValue;

	/** Creates a toggle button */
	public ToggleButton() {
		theSelectedController = state().control(BaseConstants.States.SELECTED);
		theSelectedController.link(theValue);
	}

	@Override
	protected ObservableAction createAction() {
		theValue = atts().require(new Object(), ModelAttributes.selected).asSettable();
		return theValue.assignmentTo(theValue.mapV(v -> !v));
	}
}
