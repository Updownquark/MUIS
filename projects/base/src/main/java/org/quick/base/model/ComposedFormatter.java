package org.quick.base.model;

import org.observe.ObservableAction;
import org.observe.ObservableValue;
import org.observe.collect.ObservableOrderedCollection;
import org.quick.core.model.MutableDocumentModel;
import org.quick.core.model.QuickDocumentModel;

import com.google.common.reflect.TypeToken;

public abstract class ComposedFormatter<T> implements AdjustableFormatter<T> {
	public interface FormatComponent<T, S> {
		S getComponent(QuickDocumentModel doc, int[] position);
		ObservableAction<S> getIncrement();
		ObservableAction<S> getDecrement();
	}

	public abstract TypeToken<T> getFormatType();

	public abstract TypeToken<? extends T> getParseType();

	abstract ObservableOrderedCollection<FormatComponent<T, ?>> getComponents(QuickDocumentModel doc);

	abstract ObservableValue<T> assemble(ObservableOrderedCollection<?> components);

	@Override
	public QuickFormatter<T> getFormat() {
		return new QuickFormatter<T>() {
			@Override
			public TypeToken<T> getFormatType() {
				return ComposedFormatter.this.getFormatType();
			}

			@Override
			public void append(MutableDocumentModel doc, T value) {
				// TODO Auto-generated method stub

			}

			@Override
			public void adjust(MutableDocumentModel doc, T value) {
				// TODO Auto-generated method stub
				QuickFormatter.super.adjust(doc, value);
			}

			@Override
			public TypeToken<? extends T> getParseType() {
				return ComposedFormatter.this.getParseType();
			}

			@Override
			public T parse(QuickDocumentModel doc) throws QuickParseException {
				// TODO Auto-generated method stub
				return null;
			}
		};
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
