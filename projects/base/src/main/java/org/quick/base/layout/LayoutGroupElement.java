package org.quick.base.layout;

import org.quick.core.QuickConstants;
import org.quick.core.QuickElement;
import org.quick.core.layout.LayoutAttributes;
import org.quick.core.layout.SimpleSizeGuide;
import org.quick.core.layout.SizeGuide;

public class LayoutGroupElement extends QuickElement {
	public LayoutGroupElement() {
		life().runWhen(() -> {
			atts().require(this, LayoutAttributes.direction).noInit()
				.act(v -> msg().error("Cannot modify the direction of a sub-group dynamically"));
		}, QuickConstants.CoreStage.STARTUP.toString(), 1);
	}

	@Override
	public SizeGuide getWSizer() {
		return new SimpleSizeGuide(0, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

	@Override
	public SizeGuide getHSizer() {
		return new SimpleSizeGuide(0, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

	@Override
	protected void doLayout() {}
}
