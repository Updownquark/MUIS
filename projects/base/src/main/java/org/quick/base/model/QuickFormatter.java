package org.quick.base.model;

import org.observe.Observable;
import org.quick.core.model.MutableDocumentModel;
import org.quick.core.model.QuickDocumentModel;

import com.google.common.reflect.TypeToken;

/**
 * Knows how to format and parse objects of a certain type
 *
 * @param <T> The type of objects that this formatter can parse and format
 */
public interface QuickFormatter<T> {
	/** @return The type of values that this formatter can format */
	TypeToken<T> getFormatType();

	/**
	 * Formats a value onto the end of a Quick document
	 *
	 * @param doc The document to write the value into
	 * @param value The value to write
	 */
	void append(MutableDocumentModel doc, T value);

	/**
	 * Formats a value into of a Quick document, replacing its current contents
	 *
	 * @param doc The document to write the value into
	 * @param value The value to write
	 */
	default void adjust(MutableDocumentModel doc, T value) {
		doc.clear();
		append(doc, value);
	}

	/** @return The type of values that this formatter can produce, or null if parsing is not supported */
	TypeToken<? extends T> getParseType();

	/**
	 * Parses a value out of a Quick document
	 *
	 * @param doc The document to read the value from
	 * @return The parsed value
	 * @throws QuickParseException If a value could not be parsed from the document
	 */
	T parse(QuickDocumentModel doc) throws QuickParseException;

	/**
	 * A factory that produces QuickFormatters that may keep state for their document
	 * 
	 * @param <T> The type of values that formatters produced by this factory understand
	 */
	interface Factory<T> {
		/**
		 * @param doc The document to create the formatter for
		 * @param until An observable that will fire when the formatter is no longer needed and should release its state
		 * @return The formatter
		 */
		QuickFormatter<T> create(QuickDocumentModel doc, Observable<?> until);
	}
}
