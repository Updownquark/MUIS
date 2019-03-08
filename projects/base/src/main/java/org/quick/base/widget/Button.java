package org.quick.base.widget;

import org.quick.base.BaseConstants;
import org.quick.core.model.ModelAttributes;
import org.quick.core.tags.AcceptAttribute;
import org.quick.core.tags.QuickElementType;
import org.quick.core.tags.State;

/**
 * Implements a button. Buttons can be set to toggle mode or normal mode. Buttons are containers that may have any type of content in them.
 */
@QuickElementType(//
	attributes = { //
		@AcceptAttribute(declaringClass = ModelAttributes.class, field = "action", required = true)//
	}, states = { //
		@State(name = BaseConstants.States.DEPRESSED_NAME, priority = BaseConstants.States.DEPRESSED_PRIORITY), //
		@State(name = BaseConstants.States.ENABLED_NAME, priority = BaseConstants.States.ENABLED_PRIORITY)//
	})
public class Button extends SimpleContainer {
	/** Creates a button */
	public Button() {
		setFocusable(true);
	}

	/** @return The panel containing the contents of this button */
	public Block getContentPane() {
		return (Block) getElement(getTemplate().getAttachPoint("contents")).get();
	}
}
