package org.quick.widget.base;

import org.observe.ObservableValue;
import org.quick.base.widget.Label;
import org.quick.core.model.QuickDocumentModel;
import org.quick.widget.core.QuickTemplateWidget;
import org.quick.widget.core.QuickTextWidget;
import org.quick.widget.core.RenderableDocumentModel;
import org.quick.widget.core.model.DocumentedElement;

public class LabelWidget<E extends Label> extends QuickTemplateWidget<E> implements DocumentedElement {
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
