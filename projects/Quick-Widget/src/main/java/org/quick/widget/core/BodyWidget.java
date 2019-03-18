package org.quick.widget.core;

import org.quick.core.BodyElement;
import org.quick.widget.core.layout.LayerLayout;
import org.quick.widget.core.layout.QuickWidgetLayout;

public class BodyWidget<E extends BodyElement> extends LayoutContainerWidget<E> {
	@Override
	protected QuickWidgetLayout getDefaultLayout() {
		return new LayerLayout();
	}
}
