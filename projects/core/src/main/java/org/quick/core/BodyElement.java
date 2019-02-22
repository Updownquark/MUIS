package org.quick.core;

import org.quick.core.QuickConstants.CoreStage;

/** The root element in a Quick document */
public class BodyElement extends LayoutContainer {
	/** Creates a body element */
	public BodyElement() {
		setFocusable(false);
		life().runWhen(() -> {
			getResourcePool().setParent(getDocument().getResourcePool());
		}, CoreStage.INIT_SELF, 1);
	}

	@Override
	protected QuickLayout getDefaultLayout() {
		return new LayerLayout();
	}
}
