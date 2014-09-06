package org.muis.base.model;

import org.muis.core.model.MuisDocumentModel;

public interface MuisFormatter<T> {
	void append(T value, MuisDocumentModel doc);

	T parse(MuisDocumentModel doc);
}
