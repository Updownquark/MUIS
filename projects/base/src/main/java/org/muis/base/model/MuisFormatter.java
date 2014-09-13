package org.muis.base.model;

import org.muis.core.model.MuisDocumentModel;
import org.muis.core.model.MutableDocumentModel;

/**
 * Knows how to format and parse objects of a certain type
 *
 * @param <T> The type of objects that this formatter can parse and format
 */
public interface MuisFormatter<T> {
	void setText(T value, MutableDocumentModel doc);

	void append(T value, MutableDocumentModel doc);

	T parse(MuisDocumentModel doc);
}
