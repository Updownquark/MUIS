package org.muis.test.model;

import org.muis.core.MuisElement;

/** A simple MUIS model for the button test */
public class ButtonTestModel {
	private int theCount;
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
		body.getStyle().getSelf().set(org.muis.core.style.BackgroundStyles.color, color);
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
}
