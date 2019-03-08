package org.quick.widget.core;

import org.quick.core.BodyElement;
import org.quick.widget.core.layout.LayerLayout;
import org.quick.widget.core.layout.QuickWidgetLayout;

public class BodyWidget extends LayoutContainerWidget {
	public BodyWidget(QuickWidgetDocument doc, BodyElement element) {
		super(doc, element, null);
	}

	@Override
	public BodyElement getElement() {
		return (BodyElement) super.getElement();
	}

	@Override
	protected QuickWidgetLayout getDefaultLayout() {
		return new LayerLayout();
	}
}
