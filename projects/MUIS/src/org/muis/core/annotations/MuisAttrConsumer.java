package org.muis.core.annotations;

/** Tags MUIS types like layouts and textures that require or can use certain attributes */
public @interface MuisAttrConsumer
{
	/** @return The attributes that this annotation's tagged type needs or can use */
	NeededAttr [] attrs();

	/** @return The attributes that this annotation's tagged type needs or can use on an element's children */
	NeededAttr [] childAttrs() default {};

	/** @return The default action to perform when one of the configured attributes changes */
	MuisActionType action() default MuisActionType.none;
}
