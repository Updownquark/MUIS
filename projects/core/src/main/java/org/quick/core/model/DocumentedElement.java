package org.quick.core.model;

import org.observe.ObservableValue;

/** A {@link org.quick.core.QuickElement} with a {@link QuickDocumentModel} */
public interface DocumentedElement {
	/** @return This element's text document */
	public ObservableValue<QuickDocumentModel> getDocumentModel();
}
