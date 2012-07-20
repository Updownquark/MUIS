package org.muis.core;

import org.muis.core.annotations.MuisActionType;
import org.muis.core.annotations.MuisAttrConsumer;
import org.muis.core.annotations.MuisAttrType;
import org.muis.core.annotations.NeededAttr;

/** The root element in a MUIS document */
@MuisAttrConsumer(attrs = {@NeededAttr(name = "layout", type = MuisAttrType.INSTANCE, valueType = MuisLayout.class, initValue = "layer")},
	action = MuisActionType.layout)
public class BodyElement extends LayoutContainer
{
	/** Creates a body element */
	public BodyElement()
	{
		setFocusable(false);
	}
}
