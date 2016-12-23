package org.quick.base.model;

import org.observe.ObservableAction;
import org.observe.collect.ObservableOrderedCollection;
import org.quick.core.model.MutableDocumentModel;
import org.quick.core.model.QuickDocumentModel;

import com.google.common.reflect.TypeToken;

public abstract class ComposedFormatter<T> implements AdjustableFormatter<T> {
	public interface FormatComponent<T, S> {
		S getComponent(QuickDocumentModel doc, int[] position);
		void replaceIn(QuickDocumentModel doc, int start, int end);
		ObservableAction<S> getIncrement();
		ObservableAction<S> getDecrement();
	}

	public abstract TypeToken<T> getFormatType();
	public abstract TypeToken<? extends T> getParseType();

	abstract ObservableOrderedCollection<FormatComponent<T, ?>> getComponents(QuickDocumentModel doc);

	abstract T assemble(ObservableOrderedCollection<?> components);

	@Override
	public QuickFormatter<T> getFormat() {
		return new QuickFormatter<T>() {
			@Override
			public TypeToken<T> getFormatType() {
				return ComposedFormatter.this.getFormatType();
			}

			@Override
			public TypeToken<? extends T> getParseType() {
				return ComposedFormatter.this.getParseType();
			}

			@Override
			public void insert(MutableDocumentModel doc, int start, T value) {
				// TODO Auto-generated method stub

			}

			@Override
			public void adjust(MutableDocumentModel doc, int start, int end, T value) {
			}

			@Override
			public T parse(QuickDocumentModel doc) throws QuickParseException {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}

	@Override
	public ObservableAction<T> getIncrement() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObservableAction<T> getDecrement() {
		// TODO Auto-generated method stub
		return null;
	}
}
