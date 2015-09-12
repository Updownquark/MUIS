package org.quick.core;

/** The root element in a MUIS document */
public class BodyElement extends LayoutContainer {
	/** Creates a body element */
	public BodyElement() {
		setFocusable(false);
	}

	@Override
	protected QuickLayout getDefaultLayout() {
		return new LayerLayout();
	}
}
