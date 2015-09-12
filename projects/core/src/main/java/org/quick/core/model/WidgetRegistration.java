package org.quick.core.model;

/** Allows unregistration by a registered widget */
public interface WidgetRegistration {
	/**
	 * Unregisters the registered widget
	 * 
	 * @see WidgetRegister#register(org.quick.core.QuickElement)
	 */
	void unregister();
}
