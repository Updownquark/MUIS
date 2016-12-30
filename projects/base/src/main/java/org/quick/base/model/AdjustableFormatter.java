package org.quick.base.model;

import org.observe.Observable;
import org.quick.core.model.QuickDocumentModel;

public interface AdjustableFormatter<T> extends QuickFormatter<T> {
	T increment(T value);
	String isIncrementEnabled(T value);
	T decrement(T value);
	String isDecrementEnabled(T value);

	public interface Factory<T> extends QuickFormatter.Factory<T> {
		@Override
		AdjustableFormatter<T> create(QuickDocumentModel doc, Observable<?> until);
	}
}
