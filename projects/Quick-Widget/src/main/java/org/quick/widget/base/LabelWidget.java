package org.quick.widget.base;

import org.quick.base.widget.Label;
import org.quick.widget.core.QuickTemplateWidget;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.QuickWidgetDocument;

public class LabelWidget extends QuickTemplateWidget {
	public LabelWidget(QuickWidgetDocument doc, Label element, QuickWidget parent) {
		super(doc, element, parent);
	}

	@Override
	public Label getElement() {
		return (Label) super.getElement();
	}
}
