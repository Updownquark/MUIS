package org.quick.core.model;

import org.quick.util.Transaction;

/** A modifiable document model */
public interface MutableDocumentModel extends QuickDocumentModel, Appendable {
	@Override
	public MutableDocumentModel append(CharSequence csq);

	@Override
	public MutableDocumentModel append(CharSequence csq, int start, int end);

	@Override
	public MutableDocumentModel append(char c);

	/**
	 * Clears this document's content
	 * 
	 * @return This model, for chaining
	 */
	public default QuickDocumentModel clear() {
		return delete(0, length());
	}

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

	/**
	 * Inserts a character sequence
	 *
	 * @param offset The index at which to insert the character sequence
	 * @param csq The character sequence to insert
	 * @return This model, for chaining
	 */
	MutableDocumentModel insert(int offset, CharSequence csq);

	/**
	 * Inserts a character
	 *
	 * @param offset The index at which to insert the character
	 * @param c The character to insert
	 * @return This model, for chaining
	 */
	MutableDocumentModel insert(int offset, char c);

	/** @return A transaction that prevents any other threads from modifying or accessing this document model until the transaction is closed */
	Transaction holdForWrite();
}
