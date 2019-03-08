package org.quick.core;

import org.quick.core.prop.QuickAttribute;
import org.quick.core.prop.QuickPropertyType;

/** A simple container element that lays its children out using an implementation of {@link QuickLayout} */
public class LayoutContainer extends QuickElement {
	/** The attribute that specifies the layout type for a layout container */
	public static QuickAttribute<QuickLayout> LAYOUT_ATTR = QuickAttribute
		.build("layout", QuickPropertyType.forTypeInstance(QuickLayout.class, null)).build();

	/** @return The QuickLayout that lays out this container's children */
	public QuickLayout getLayout() {
		return atts().get(LAYOUT_ATTR).get();
	}
}
