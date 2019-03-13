package org.quick.widget.core.model;

import org.observe.ObservableValue;
import org.quick.core.model.QuickDocumentModel;
import org.quick.widget.core.RenderableDocumentModel;

/** A {@link org.quick.core.QuickElement} with a {@link QuickDocumentModel} */
public interface DocumentedElement {
	/** @return This element's text document */
	public ObservableValue<QuickDocumentModel> getDocumentModel();

	public RenderableDocumentModel getRenderableDocument();
}
