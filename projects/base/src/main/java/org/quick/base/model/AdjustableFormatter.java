package org.quick.base.model;

import org.observe.ObservableAction;
import org.quick.core.model.QuickDocumentModel;

public interface AdjustableFormatter<T> {
	QuickFormatter<T> getFormat();

	ObservableAction<T> getIncrement();
	ObservableAction<T> getDecrement();

	public interface Factory<T> {
		AdjustableFormatter<T> create(QuickDocumentModel doc);
	}
}
