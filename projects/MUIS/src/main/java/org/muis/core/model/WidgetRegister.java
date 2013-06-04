package org.muis.core.model;

import org.muis.core.MuisElement;

/** Allows widgets to register themselves with a model or value to make themselves available to the application code */
public interface WidgetRegister {
	/**
	 * @param widget The widget to register
	 * @return A registration object that allows for unregistration
	 */
	WidgetRegistration register(MuisElement widget);

	/** @return The elements registered with this register */
	public java.util.List<MuisElement> registered();
}
