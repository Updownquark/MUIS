package org.quick.base.model;

import org.observe.ObservableAction;
import org.observe.ObservableValue;
import org.observe.collect.ObservableOrderedCollection;
import org.quick.core.model.QuickDocumentModel;

public abstract class ComposedFormatter<T> implements AdjustableFormatter<T> {
	public interface FormatComponent<T, S> {
		S getComponent(QuickDocumentModel doc);
		ObservableAction<S> getIncrement();
		ObservableAction<S> getDecrement();
	}

	abstract ObservableOrderedCollection<FormatComponent<T, ?>> getComponents(QuickDocumentModel doc);

	abstract ObservableValue<T> assemble(ObservableOrderedCollection<?> components);

	@Override
	public QuickFormatter<T> getFormat() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObservableAction<T> incrementFor(QuickDocumentModel doc) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObservableAction<T> decrementFor(QuickDocumentModel doc) {
		// TODO Auto-generated method stub
		return null;
	}
}
