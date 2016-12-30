package org.quick.base.model;

import org.quick.core.model.MutableDocumentModel;
import org.quick.core.model.QuickDocumentModel;

/**
 * A simple version of {@link QuickFormatter} that just operates on pure text
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
		if (!doc.toString().equals(text))
			QuickFormatter.super.adjust(doc, value);
	}

	@Override
	default T parse(QuickDocumentModel doc) throws QuickParseException {
		return parse(doc.toString());
	}

	interface SimpleAdjustableFormatter<T> extends SimpleFormatter<T>, AdjustableFormatter<T> {}
}
