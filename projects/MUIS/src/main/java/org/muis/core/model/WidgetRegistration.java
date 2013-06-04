package org.muis.core.model;

/** Allows unregistration by a registered widget */
public interface WidgetRegistration {
	/**
	 * Unregisters the registered widget
	 * 
	 * @see WidgetRegister#register(org.muis.core.MuisElement)
	 */
	void unregister();
}
