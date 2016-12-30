package org.quick.base.model;

import org.observe.Observable;
import org.quick.core.model.SelectableDocumentModel;

public interface AdjustableFormatter<T> extends QuickFormatter<T> {
	T increment(T value);
	String isIncrementEnabled(T value);
	T decrement(T value);
	String isDecrementEnabled(T value);

	public interface Factory<T> {
		AdjustableFormatter<T> create(SelectableDocumentModel doc, Observable<?> until);
	}
}
