package org.quick.base.widget;

import org.quick.base.BaseConstants;
import org.quick.core.model.ModelAttributes;
import org.quick.core.tags.AcceptAttribute;
import org.quick.core.tags.QuickElementType;
import org.quick.core.tags.State;

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
}
