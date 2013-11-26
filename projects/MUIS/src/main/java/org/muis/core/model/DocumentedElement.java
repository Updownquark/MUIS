package org.muis.core.model;

/** A {@link org.muis.core.MuisElement} with a {@link MuisDocumentModel} */
public interface DocumentedElement {
	/** @return This element's text document */
	public MuisDocumentModel getDocumentModel();
}
