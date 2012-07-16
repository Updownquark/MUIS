package org.muis.core.annotations;

/** Tags MUIS types like textures that require or can use certain style attributes */
public @interface MuisStyeAttrConsumer
{
	/** @return The style attributes that this annotation's tagged type needs or can use */
	NeededAttr [] attrs();

	/** @return The default action to perform when one of the configured style attributes changes */
	MuisActionType action() default MuisActionType.paint;
}
