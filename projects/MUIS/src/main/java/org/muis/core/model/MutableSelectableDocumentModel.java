package org.muis.core.model;

/** A mutable, selectable document model which provides some extra utility methods */
public interface MutableSelectableDocumentModel extends MutableDocumentModel, SelectableDocumentModel {
	/**
	 * Inserts a character sequence at this model's cursor
	 *
	 * @param csq The character sequence to insert
	 * @return This model, for chaining
	 */
	MutableSelectableDocumentModel insert(CharSequence csq);

	/**
	 * Inserts a character at this model's cursor
	 *
	 * @param c The character to insert
	 * @return This model, for chaining
	 */
	MutableSelectableDocumentModel insert(char c);

	/**
	 * Inserts a character sequence
	 *
	 * @param offset The index at which to insert the character sequence
	 * @param csq The character sequence to insert
	 * @return This model, for chaining
	 */
	MutableSelectableDocumentModel insert(int offset, CharSequence csq);

	/**
	 * Inserts a character
	 *
	 * @param offset The index at which to insert the character
	 * @param c The character to insert
	 * @return This model, for chaining
	 */
	MutableSelectableDocumentModel insert(int offset, char c);

	/**
	 * Deletes characters from this document
	 *
	 * @param start The index of the start of the sequence to remove, inclusive
	 * @param end The index of the end of the sequence to remove, exclusive
	 * @return This model, for chaining
	 */
	MutableSelectableDocumentModel delete(int start, int end);
}
