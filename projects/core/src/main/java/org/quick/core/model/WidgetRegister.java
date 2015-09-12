package org.quick.core.model;

import org.quick.core.QuickElement;

/** Allows widgets to register themselves with a model or value to make themselves available to the application code */
public interface WidgetRegister {
	/**
	 * @param widget The widget to register
	 * @return A registration object that allows for unregistration
	 */
	WidgetRegistration register(QuickElement widget);

	/** @return The elements registered with this register */
	public java.util.List<QuickElement> registered();
}
