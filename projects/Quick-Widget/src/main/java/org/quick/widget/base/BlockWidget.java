package org.quick.widget.base;

import org.quick.base.widget.Block;
import org.quick.widget.base.layout.SimpleLayout;
import org.quick.widget.core.LayoutContainerWidget;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.QuickWidgetDocument;
import org.quick.widget.core.layout.QuickWidgetLayout;

public class BlockWidget extends LayoutContainerWidget {
	public BlockWidget(QuickWidgetDocument doc, Block element, QuickWidget parent) {
		super(doc, element, parent);
	}

	@Override
	public Block getElement() {
		return (Block) super.getElement();
	}

	@Override
	protected QuickWidgetLayout getDefaultLayout() {
		return new SimpleLayout();
	}
}
