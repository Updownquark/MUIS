package org.quick.widget.core;

import org.quick.core.BodyElement;
import org.quick.widget.core.layout.LayerLayout;
import org.quick.widget.core.layout.QuickWidgetLayout;

public class BodyWidget extends LayoutContainerWidget<BodyElement> {
	@Override
	protected QuickWidgetLayout getDefaultLayout() {
		return new LayerLayout();
	}
}
