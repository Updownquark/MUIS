package org.muis.base.widget;

import org.muis.core.MuisLayout;
import org.muis.core.annotations.MuisAttrConsumer;
import org.muis.core.annotations.MuisAttrType;
import org.muis.core.annotations.NeededAttr;

/** Implements a button. Buttons can be set to toggle mode or normal mode. Buttons are containers that may have any type of content in them. */
@MuisAttrConsumer(attrs = {@NeededAttr(
	name = "layout",
	type = MuisAttrType.INSTANCE,
	valueType = MuisLayout.class,
	initValue = "base:simple")})
public class Button extends org.muis.core.LayoutContainer
{
}
