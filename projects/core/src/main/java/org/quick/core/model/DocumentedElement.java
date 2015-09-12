package org.quick.core.model;

/** A {@link org.quick.core.QuickElement} with a {@link QuickDocumentModel} */
public interface DocumentedElement {
	/** @return This element's text document */
	public QuickDocumentModel getDocumentModel();
}
