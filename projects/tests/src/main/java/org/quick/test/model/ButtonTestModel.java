package org.quick.test.model;

import org.quick.core.QuickElement;
import org.quick.core.QuickException;
import org.quick.core.style.BackgroundStyle;

/** A simple Quick model for the button test */
public class ButtonTestModel {
	private int theCount;

	private org.quick.base.model.QuickButtonGroup theColorGroup;

	/** Creates the test model */
	public ButtonTestModel() {
		theColorGroup = new org.quick.base.model.QuickButtonGroup();
		theColorGroup.act(evt -> {
			org.quick.core.event.UserEvent ue = org.quick.util.QuickUtils.getUserEvent(evt);
			if(ue == null)
				return;
			try {
				ue.getDocument().getRoot().getStyle().getSelf()
					.set(BackgroundStyle.color, org.quick.core.style.Colors.parseColor(theColorGroup.get()));
			} catch(ClassCastException | IllegalArgumentException | QuickException e) {
				e.printStackTrace();
			}
		});
	}

	/**
	 * Called when the user clicks the big button
	 *
	 * @param element The button
	 */
	public void clicked(QuickElement element) {
		if(element == null)
			return;
		org.quick.core.BodyElement body = element.getDocument().getRoot();
		java.awt.Color color = getColor();
		body.getStyle().getSelf().set(BackgroundStyle.color, color);
		theCount++;
	}

	private java.awt.Color getColor() {
		switch (theCount % 3) {
		case 0:
			return org.quick.core.style.Colors.red;
		case 1:
			return org.quick.core.style.Colors.blue;
		case 2:
			return org.quick.core.style.Colors.green;
		}
		return org.quick.core.style.Colors.black;
	}

	/** @return The button group with the color selection */
	public org.quick.base.model.QuickButtonGroup getColorGroup() {
		return theColorGroup;
	}
}
