package org.muis.base.model;

import org.muis.core.model.MuisDocumentModel;
import org.muis.core.model.MutableDocumentModel;

/**
 * Knows how to format and parse objects of a certain type
 *
 * @param <T> The type of objects that this formatter can parse and format
 */
public interface MuisFormatter<T> {
	/**
	 * Formats a value into a MUIS document
	 * 
	 * @param value The value to write
	 * @param doc The document to write the value into
	 */
	void append(T value, MutableDocumentModel doc);

	/**
	 * Parses a value out of a MUIS document
	 * 
	 * @param doc The document to read the value from
	 * @return The parsed value
	 * @throws MuisParseException If a value could not be parsed from the document
	 */
	T parse(MuisDocumentModel doc) throws MuisParseException;
}
