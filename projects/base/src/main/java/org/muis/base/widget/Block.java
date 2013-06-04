package org.muis.base.widget;

import org.muis.core.MuisLayout;

/** A very simple building block for MUIS documents. A block is a rectangle that allows custom layouts to layout arbitrary content. */
public class Block extends org.muis.core.LayoutContainer {
	@Override
	protected MuisLayout getDefaultLayout() {
		return new org.muis.base.layout.SimpleLayout();
	}
}
