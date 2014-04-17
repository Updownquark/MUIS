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
}
