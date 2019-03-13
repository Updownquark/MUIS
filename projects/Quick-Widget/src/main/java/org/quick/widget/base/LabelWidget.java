package org.quick.widget.base;

import org.observe.ObservableValue;
import org.quick.base.widget.Label;
import org.quick.core.model.QuickDocumentModel;
import org.quick.widget.core.QuickTemplateWidget;
import org.quick.widget.core.QuickTextWidget;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.QuickWidgetDocument;
import org.quick.widget.core.RenderableDocumentModel;
import org.quick.widget.core.model.DocumentedElement;

public class LabelWidget extends QuickTemplateWidget implements DocumentedElement {
	public LabelWidget(QuickWidgetDocument doc, Label element, QuickWidget parent) {
		super(doc, element, parent);
	}

	@Override
	public Label getElement() {
		return (Label) super.getElement();
	}

	public QuickTextWidget getValue() {
		return (QuickTextWidget) getChild(getElement().getValue());
	}

	@Override
	public ObservableValue<QuickDocumentModel> getDocumentModel() {
		return getValue().getDocumentModel();
	}

	@Override
	public RenderableDocumentModel getRenderableDocument() {
		return getValue().getRenderableDocument();
	}
}
