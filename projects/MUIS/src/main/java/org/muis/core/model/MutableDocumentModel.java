package org.muis.core.model;

/** A modifiable document model */
public interface MutableDocumentModel extends MuisDocumentModel, Appendable {
	@Override
	public MutableDocumentModel append(CharSequence csq);

	@Override
	public MutableDocumentModel append(CharSequence csq, int start, int end);

	@Override
	public MutableDocumentModel append(char c);

	/**
	 * Deletes characters from this document
	 * 
	 * @param start The index of the start of the sequence to remove, inclusive
	 * @param end The index of the end of the sequence to remove, exclusive
	 * @return This model, for chaining
	 */
	MutableDocumentModel delete(int start, int end);

	/**
	 * Sets the content for this model
	 * 
	 * @param text The text to set
	 * @return This model, for chaining
	 */
	MutableDocumentModel setText(String text);
}
