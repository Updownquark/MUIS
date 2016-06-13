package org.quick.base.model;

import org.quick.core.model.QuickDocumentModel;
import org.quick.core.model.MutableDocumentModel;

/**
 * Knows how to format and parse objects of a certain type
 *
 * @param <T> The type of objects that this formatter can parse and format
 */
public interface QuickFormatter<T> {
	/**
	 * Formats a value into a Quick document
	 * 
	 * @param value The value to write
	 * @param doc The document to write the value into
	 */
	void append(T value, MutableDocumentModel doc);

	/**
	 * Parses a value out of a Quick document
	 * 
	 * @param doc The document to read the value from
	 * @return The parsed value
	 * @throws QuickParseException If a value could not be parsed from the document
	 */
	T parse(QuickDocumentModel doc) throws QuickParseException;
}
