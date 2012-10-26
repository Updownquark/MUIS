package org.muis.core.tags;

/**
 * Tells {@link org.muis.core.MuisTemplate} that an attach point from the super class should not be specifiable from source XML. The attach
 * point can still be specified in the template XML for the subclass.
 */
public @interface AttachPointOverride {
	/** The name of the attach point to override */
	String name();
}
