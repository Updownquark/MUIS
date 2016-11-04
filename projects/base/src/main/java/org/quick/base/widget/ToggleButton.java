package org.quick.base.widget;

import org.observe.ObservableAction;
import org.observe.SettableValue;
import org.quick.base.BaseConstants;
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
@QuickElementType(attributes = { @AcceptAttribute(declaringClass = ModelAttributes.class, field = "selected") },
	states = { @State(name = BaseConstants.States.SELECTED_NAME, priority = BaseConstants.States.SELECTED_PRIORITY) })
public class ToggleButton extends Button {
	private StateEngine.StateController theSelectedController;

	private SettableValue<Boolean> theValue;

	/** Creates a toggle button */
	public ToggleButton() {
		theSelectedController = state().control(BaseConstants.States.SELECTED);
		theSelectedController.link(theValue);
	}

	@Override
	protected ObservableAction<?> createAction() {
		theValue = atts().getHolder(ModelAttributes.selected).asSettable();
		return ObservableAction.and(TypeToken.of(Object.class), ObservableAction.flatten(atts().getHolder(ModelAttributes.action)),
			theValue.assignmentTo(theValue.mapV(v -> !v)));
	}
}
