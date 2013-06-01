package org.muis.test.model;

import org.muis.core.MuisElement;

/** A simple MUIS model for the button test */
public class ButtonTestModel {
	/**
	 * Called when the user clicks the big button
	 * 
	 * @param element The button
	 */
	public void clicked(MuisElement element) {
		System.out.println("Clicked!");
	}
}
