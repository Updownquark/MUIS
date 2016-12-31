package org.quick.base.model;

import org.qommons.Transaction;
import org.quick.core.model.MutableDocumentModel;
import org.quick.core.model.QuickDocumentModel;
import org.quick.core.model.SelectableDocumentModel;

/**
 * A simple version of {@link QuickFormatter} that just operates on pure text and attempts to preserve selection
 *
 * @param <T> The type of value that this formatter understands
 */
public interface SimpleFormatter<T> extends QuickFormatter<T> {
	/**
	 * @param value The value to format
	 * @return The formatted value
	 */
	String format(T value);

	/**
	 * @param text The text to parse
	 * @return The parsed value
	 * @throws QuickParseException If an error occurs parsing the text
	 */
	T parse(String text) throws QuickParseException;

	@Override
	default void append(MutableDocumentModel doc, T value) {
		doc.append(format(value));
	}

	@Override
	default void adjust(MutableDocumentModel doc, T value) {
		String text = format(value);
		if (doc.toString().equals(text))
			return;
		try (Transaction t = doc.holdForWrite(null)) {
			if (!(doc instanceof SelectableDocumentModel)) {
				doc.clear();
				doc.append(text);
				return;
			}
			SelectableDocumentModel selDoc = (SelectableDocumentModel) doc;
			int cursor = selDoc.getCursor();
			int anchor = selDoc.getSelectionAnchor();
			if (cursor == anchor) {
				if (cursor == doc.length()) {
					doc.clear();
					doc.append(text);
					return;
				}
				if (cursor == 0) {
					doc.clear();
					doc.append(text);
					selDoc.setSelection(0, 0);
					return;
				}
			}

			int min = Math.min(cursor, anchor);
			int max = Math.max(cursor, anchor);
			String oldDoc = doc.toString();
			if (text.startsWith(oldDoc.substring(0, max))) {
				doc.delete(max, oldDoc.length());
				doc.append(text.substring(max));
				selDoc.setSelection(anchor, cursor);
			} else if (text.endsWith(oldDoc.substring(min))) {
				int diff = text.length() - oldDoc.length();
				doc.delete(0, min);
				doc.insert(0, text.substring(0, min));
				selDoc.setSelection(anchor + diff, cursor + diff);
			} else if (text.startsWith(oldDoc.substring(0, min))) {
				doc.delete(min, oldDoc.length());
				doc.append(text.substring(min));
				selDoc.setSelection(min, min);
			} else if (text.endsWith(oldDoc.substring(max))) {
				int diff = text.length() - oldDoc.length();
				doc.delete(0, max);
				doc.insert(0, text.substring(0, max));
				selDoc.setSelection(max + diff, max + diff);
			} else {
				doc.clear();
				doc.append(text);
			}
		}
	}

	@Override
	default T parse(QuickDocumentModel doc) throws QuickParseException {
		return parse(doc.toString());
	}

	/**
	 * A simple version of {@link AdjustableFormatter} that just operates on pure text
	 *
	 * @param <T> The type of value that this formatter understands
	 */
	interface SimpleAdjustableFormatter<T> extends SimpleFormatter<T>, AdjustableFormatter<T> {}
}
