package org.muis.core.model;

/** A modifiable document model */
public interface MutableDocumentModel extends MuisDocumentModel, Appendable {
	@Override
	MutableDocumentModel append(CharSequence csq);

	@Override
	MutableDocumentModel append(CharSequence csq, int start, int end);

	@Override
	MutableDocumentModel append(char c);
}
