package org.muis.test.model;

import org.muis.core.MuisElement;
import org.muis.core.MuisException;
import org.muis.core.model.MuisModelValueEvent;
import org.muis.core.style.BackgroundStyles;

/** A simple MUIS model for the button test */
public class ButtonTestModel {
	private int theCount;

	private org.muis.base.model.MuisButtonGroup theColorGroup;

	/** Creates the test model */
	public ButtonTestModel() {
		theColorGroup = new org.muis.base.model.MuisButtonGroup();
		theColorGroup.addListener(new org.muis.core.model.MuisModelValueListener<String>() {
			@Override
			public void valueChanged(MuisModelValueEvent<? extends String> evt) {
				if(evt.getUserEvent() == null)
					return;
				try {
					evt.getUserEvent().getDocument().getRoot().getStyle().getSelf()
						.set(BackgroundStyles.color, org.muis.core.style.Colors.parseColor(theColorGroup.get()));
				} catch(ClassCastException | IllegalArgumentException | MuisException e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Called when the user clicks the big button
	 *
	 * @param element The button
	 */
	public void clicked(MuisElement element) {
		if(element == null)
			return;
		org.muis.core.BodyElement body = element.getDocument().getRoot();
		java.awt.Color color = getColor();
		body.getStyle().getSelf().set(BackgroundStyles.color, color);
		theCount++;
	}

	private java.awt.Color getColor() {
		switch (theCount % 3) {
		case 0:
			return org.muis.core.style.Colors.red;
		case 1:
			return org.muis.core.style.Colors.blue;
		case 2:
			return org.muis.core.style.Colors.green;
		}
		return org.muis.core.style.Colors.black;
	}

	/** @return The button group with the color selection */
	public org.muis.base.model.MuisButtonGroup getColorGroup() {
		return theColorGroup;
	}
}
