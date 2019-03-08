package org.quick.widget.base;

import org.quick.base.widget.SimpleContainer;
import org.quick.widget.core.QuickTemplateWidget;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.QuickWidgetDocument;

public abstract class SimpleContainerWidget extends QuickTemplateWidget {
	public SimpleContainerWidget(QuickWidgetDocument doc, SimpleContainer element, QuickWidget parent) {
		super(doc, element, parent);
	}

	@Override
	public SimpleContainer getElement() {
		return (SimpleContainer) super.getElement();
	}
}
