package org.muis.base.widget;

/**
 * A label is a container intended for text-only, but this is not enforced. It differs from block only in that its default layout may be
 * different (flow by default) and its style sheet attributes may be different (margin and padding are typically 0)
 */
public class Label extends org.muis.core.LayoutContainer {
	@Override
	protected org.muis.core.MuisLayout getDefaultLayout() {
		return new org.muis.base.layout.FlowLayout();
	}
}
