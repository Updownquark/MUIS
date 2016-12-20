package org.quick.base.model;

import org.observe.ObservableAction;
import org.quick.core.model.QuickDocumentModel;

public interface AdjustableFormatter<T> {
	QuickFormatter<T> getFormat();
	ObservableAction<T> incrementFor(QuickDocumentModel doc);
	ObservableAction<T> decrementFor(QuickDocumentModel doc);
}
