package org.quick.base.widget;

import org.quick.core.QuickLayout;

/** A very simple building block for MUIS documents. A block is a rectangle that allows custom layouts to layout arbitrary content. */
public class Block extends org.quick.core.LayoutContainer {
	@Override
	protected QuickLayout getDefaultLayout() {
		return new org.quick.base.layout.SimpleLayout();
	}
}
