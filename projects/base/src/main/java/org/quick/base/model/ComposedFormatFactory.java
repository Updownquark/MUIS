package org.quick.base.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.observe.*;
import org.qommons.BiTuple;
import org.quick.core.model.MutableDocumentModel;
import org.quick.core.model.QuickDocumentModel;
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
	public AdjustableFormatter<T> create(SelectableDocumentModel doc, Observable<?> until) {
		return new ComposedFormatter(doc, until);
	}

	public class ComposedFormatter implements AdjustableFormatter<T> {
		private final SelectableDocumentModel theDoc;
		private final Observable<?> theUntil;
		private List<SubFormat<?>> theSubFormats;
		private QuickParseException theCurrentError;
		private T theCurrentValue;

		ComposedFormatter(SelectableDocumentModel doc, Observable<?> until) {
			theDoc = doc;
			theUntil = until;
			reset();
			theDoc.changes().takeUntil(until).act(change -> {
				// TODO
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
		public QuickFormatter<T> getFormat() {
			return new QuickFormatter<T>() {
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
					if(theCurrentError!=null)
						throw theCurrentError;
					return theCurrentValue;
				}
			};
		}

		@Override
		public ObservableAction<T> getIncrement() {
			return changeAction(true);
		}

		@Override
		public ObservableAction<T> getDecrement() {
			return changeAction(false);
		}

		private ObservableAction<T> changeAction(boolean inc) {
			return new ObservableAction<T>() {
				@Override
				public TypeToken<T> getType() {
					return getFormatType();
				}

				@Override
				public ObservableValue<String> isEnabled() {
					return new ObservableValue<String>(){
						@Override
						public boolean isSafe() {
							return theDoc.changes().isSafe();
						}

						@Override
						public TypeToken<String> getType() {
							return TypeToken.of(String.class);
						}

						@Override
						public String get() {
							return attempt(false).getValue2();
						}

						@Override
						public Subscription subscribe(Observer<? super ObservableValueEvent<String>> observer) {
							String[] msg = new String[] { get() };
							Subscription sub = theDoc.changes().takeUntil(theUntil).act(change -> {
								String newMsg = get();
								if (!Objects.equals(msg[0], newMsg)) {
									observer.onNext(createChangeEvent(msg[0], newMsg, change));
									msg[0] = newMsg;
								}
							});
							observer.onNext(createInitialEvent(msg[0]));
							return sub;
						}
					};
				}

				private BiTuple<T, String> attempt(boolean enact) {
					if (theSubFormats == null)
						return new BiTuple<>(null, "Content error");
					int cursor = theDoc.getCursor();
					List<Object> subValues = new ArrayList<>(theSubFormats.size());
					for (int i = 0; i < theSubFormats.size(); i++)
						subValues.add(theSubFormats.get(i).value);
					String message = null;
					T value = null;
					boolean success = false;
					for (int i = 0; i < theSubFormats.size(); i++) {
						SubFormat<?> sub = theSubFormats.get(i);
						if (sub.start <= cursor && sub.start + sub.length >= cursor) {
							String msg = getMessageFor(sub);
							if (msg != null) {
								message = msg;
								continue;
							}
							Object newValue = getNewValueFor(sub);
							Object oldValue = subValues.set(i, newValue);
							try {
								value = assemble(subValues);
								success = true;
								if (enact)
									((SubFormat<Object>) sub).value = newValue;
								break;
							} catch (IllegalArgumentException e) {
								message = e.getMessage();
								subValues.set(i, oldValue);
							}
							break;
						}
					}
					if (success) {
						if (enact)
							theCurrentValue = value;
						return new BiTuple<>(value, null);
					} else
						return new BiTuple<>(null, message);
				}

				private <S> String getMessageFor(SubFormat<S> sub) {
					return inc ? sub.component.isIncrementEnabled(sub.value) : sub.component.isDecrementEnabled(sub.value);
				}

				private <S> S getNewValueFor(SubFormat<S> sub) {
					return inc ? sub.component.increment(sub.value) : sub.component.getDecrement(sub.value);
				}

				@Override
				public T act(Object cause) throws IllegalStateException {
					BiTuple<T, String> result = attempt(true);
					if (result.getValue2() != null)
						throw new IllegalStateException(result.getValue2());
					return result.getValue1();
				}
			};
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
