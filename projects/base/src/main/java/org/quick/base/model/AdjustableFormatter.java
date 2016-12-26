package org.quick.base.model;

import org.observe.Observable;
import org.observe.ObservableAction;
import org.quick.core.model.SelectableDocumentModel;

public interface AdjustableFormatter<T> {
	QuickFormatter<T> getFormat();

	ObservableAction<T> getIncrement();
	ObservableAction<T> getDecrement();

	public interface Factory<T> {
		AdjustableFormatter<T> create(SelectableDocumentModel doc, Observable<?> until);
	}
}
