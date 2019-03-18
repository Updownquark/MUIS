package org.quick.widget.base;

import org.quick.base.widget.Block;
import org.quick.widget.base.layout.SimpleLayout;
import org.quick.widget.core.LayoutContainerWidget;
import org.quick.widget.core.layout.QuickWidgetLayout;

public class BlockWidget<E extends Block> extends LayoutContainerWidget<E> {
	@Override
	protected QuickWidgetLayout getDefaultLayout() {
		return new SimpleLayout();
	}
}
