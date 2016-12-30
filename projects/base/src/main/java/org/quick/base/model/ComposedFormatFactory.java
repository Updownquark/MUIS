package org.quick.base.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.observe.Observable;
import org.qommons.BiTuple;
import org.quick.core.model.MutableDocumentModel;
import org.quick.core.model.QuickDocumentModel;
import org.quick.core.model.QuickDocumentModel.ContentChangeEvent;
import org.quick.core.model.SelectableDocumentModel;

import com.google.common.reflect.TypeToken;

public abstract class ComposedFormatFactory<T> implements AdjustableFormatter.Factory<T> {
	public interface FormatComponent<T, S> extends QuickFormatter<S> {
		S getComponent(T value);

		S increment(S value);
		String isIncrementEnabled(S value);
		S getDecrement(S value);
		String isDecrementEnabled(S value);

		int getStart();
		int length();
	}

	public abstract TypeToken<T> getFormatType();
	public abstract TypeToken<? extends T> getParseType();

	protected abstract List<FormatComponent<T, ?>> getComponents(QuickDocumentModel doc) throws QuickParseException;
	protected abstract T assemble(List<?> components) throws IllegalArgumentException;

	@Override
	public AdjustableFormatter<T> create(QuickDocumentModel doc, Observable<?> until) {
		return new ComposedFormatter(doc, until);
	}

	public class ComposedFormatter implements AdjustableFormatter<T> {
		private final QuickDocumentModel theDoc;
		private final Observable<?> theUntil;
		private List<SubFormat<?>> theSubFormats;
		private QuickParseException theCurrentError;
		private T theCurrentValue;

		ComposedFormatter(QuickDocumentModel doc, Observable<?> until) {
			theDoc = doc;
			theUntil = until;
			reset();
			theDoc.changes().takeUntil(until).act(change -> {
				if (theSubFormats == null)
					reset();
				if (change instanceof ContentChangeEvent) {
					ContentChangeEvent contentChange = (ContentChangeEvent) change;
					int posDiff = change.getEndIndex() - change.getStartIndex();
					if (contentChange.isRemove())
						posDiff = -posDiff;
					boolean accounted = false;
					// See if this change can be handled in one sub-format sequence
					for (SubFormat<?> sub : theSubFormats) {
						if (accounted) {
							sub.start += posDiff;
							continue;
						}
						int preEnd = change.getEndIndex();
						if (!contentChange.isRemove())
							preEnd -= posDiff;
						if (sub.start <= change.getStartIndex() && sub.start + sub.length >= preEnd) {
							int newLen = sub.length + posDiff;
							try {
								sub.component.parse(theDoc.subSequence(sub.start, sub.start + newLen));
								sub.length = newLen;
								accounted = true;
							} catch (QuickParseException e) {
								// We'll give other sequences a chance maybe, otherwise we'll reset and get the error there
							}
						}
					}
					if (!accounted)
						reset();
				} else {
					for (SubFormat<?> sub : theSubFormats) {
						if (sub.start < change.getEndIndex() && sub.start + sub.length < change.getStartIndex()) {
							try {
								sub.component.parse(theDoc.subSequence(sub.start, sub.start + sub.length));
							} catch (QuickParseException e) {
								reset();
								break;
							}
						}
					}
				}
			});
		}

		private void reset() {
			try {
				theSubFormats = getComponents(theDoc).stream().map(c -> new SubFormat<>(c, theDoc)).collect(Collectors.toList());
				for (SubFormat<?> sub : theSubFormats)
					if (sub.error != null)
						theCurrentError = sub.error;
				if (theCurrentError == null)
					theCurrentValue = assemble(theSubFormats.stream().map(s -> s.value).collect(Collectors.toList()));
			} catch (QuickParseException e) {
				theCurrentError = e;
			}
		}

		@Override
		public TypeToken<T> getFormatType() {
			return ComposedFormatFactory.this.getFormatType();
		}

		@Override
		public TypeToken<? extends T> getParseType() {
			return ComposedFormatFactory.this.getParseType();
		}

		@Override
		public void append(MutableDocumentModel doc, T value) {
			for (SubFormat<?> sub : theSubFormats)
				append(doc, value, sub);
		}

		private <S> void append(MutableDocumentModel doc, T value, SubFormat<S> sub) {
			sub.component.append(doc, sub.component.getComponent(value));
		}

		@Override
		public void adjust(MutableDocumentModel doc, T value) {
			doc.clear();
			append(doc, value);
		}

		@Override
		public T parse(QuickDocumentModel doc) throws QuickParseException {
			if (doc != theDoc)
				throw new QuickParseException(getClass().getName() + " is dedicated to a single document", 0, doc.length());
			if (theCurrentError != null)
				throw theCurrentError;
			return theCurrentValue;
		}

		@Override
		public T increment(T value) {
			BiTuple<T, String> result = attempt(true, true);
			if (result.getValue2() != null)
				throw new IllegalArgumentException(result.getValue2());
			return result.getValue1();
		}

		@Override
		public String isIncrementEnabled(T value) {
			return attempt(true, false).getValue2();
		}

		@Override
		public T decrement(T value) {
			BiTuple<T, String> result = attempt(false, true);
			if (result.getValue2() != null)
				throw new IllegalArgumentException(result.getValue2());
			return result.getValue1();
		}

		@Override
		public String isDecrementEnabled(T value) {
			return attempt(false, false).getValue2();
		}

		private BiTuple<T, String> attempt(boolean inc, boolean enact) {
			if (theSubFormats == null)
				return new BiTuple<>(null, "Content error");
			if (!(theDoc instanceof SelectableDocumentModel))
				return new BiTuple<>(null, "Document is not selectable");
			int cursor = ((SelectableDocumentModel) theDoc).getCursor();
			List<Object> subValues = new ArrayList<>(theSubFormats.size());
			for (int i = 0; i < theSubFormats.size(); i++)
				subValues.add(theSubFormats.get(i).value);
			String message = null;
			T newValue = null;
			boolean success = false;
			for (int i = 0; i < theSubFormats.size(); i++) {
				SubFormat<?> sub = theSubFormats.get(i);
				if (sub.start <= cursor && sub.start + sub.length >= cursor) {
					String msg = getMessageFor(sub, inc);
					if (msg != null) {
						message = msg;
						continue;
					}
					Object newSubValue = getNewValueFor(sub, inc);
					Object oldSubValue = subValues.set(i, newSubValue);
					try {
						newValue = assemble(subValues);
						success = true;
						if (enact)
							((SubFormat<Object>) sub).value = newSubValue;
						break;
					} catch (IllegalArgumentException e) {
						message = e.getMessage();
						subValues.set(i, oldSubValue);
					}
					break;
				}
			}
			if (success) {
				if (enact)
					theCurrentValue = newValue;
				return new BiTuple<>(newValue, null);
			} else
				return new BiTuple<>(null, message);
		}

		private <S> String getMessageFor(SubFormat<S> sub, boolean inc) {
			return inc ? sub.component.isIncrementEnabled(sub.value) : sub.component.isDecrementEnabled(sub.value);
		}

		private <S> S getNewValueFor(SubFormat<S> sub, boolean inc) {
			return inc ? sub.component.increment(sub.value) : sub.component.getDecrement(sub.value);
		}
	}

	class SubFormat<S> {
		final FormatComponent<T, S> component;
		int start;
		int length;
		S value;
		QuickParseException error;

		SubFormat(FormatComponent<T, S> component, QuickDocumentModel doc) {
			this.component = component;
			start = component.getStart();
			length = component.length();
			try {
				value = component.parse(doc.subSequence(start, start + length));
			} catch (QuickParseException e) {
				error = e;
			}
		}
	}
}
