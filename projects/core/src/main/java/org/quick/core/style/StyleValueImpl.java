package org.quick.core.style;

import java.util.Objects;

import org.observe.Observable;
import org.observe.ObservableValue;
import org.observe.ObservableValueEvent;
import org.observe.Observer;
import org.observe.Subscription;
import org.qommons.Causable;
import org.qommons.Transaction;
import org.quick.core.QuickException;
import org.quick.core.mgr.QuickMessageCenter;
import org.quick.util.QuickUtils;

import com.google.common.reflect.TypeToken;

public class StyleValueImpl<T> implements StyleValue<T> {
	private final StyleAttribute<T> theAttribute;
	private final ObservableValue<? extends T> theValue;
	private final QuickMessageCenter theMessageCenter;

	/**
	 * @param attribute The style attribute that this value is for
	 * @param value The value for the attribute
	 * @param msg The message center to log values from the observable that are not acceptable for the style attribute
	 */
	public StyleValueImpl(StyleAttribute<T> attribute, ObservableValue<? extends T> value, QuickMessageCenter msg) {
		theAttribute = attribute;
		theValue = value;
		theMessageCenter = msg;
	}

	@Override
	public StyleAttribute<T> getAttribute() {
		return theAttribute;
	}

	@Override
	public TypeToken<T> getType() {
		return theAttribute.getType().getType();
	}

	@Override
	public T get() {
		T value = theValue.get();
		if (!theAttribute.canAccept(value)) {
			theMessageCenter.info("Value " + value + " from observable " + theValue + " is unacceptable for style attribute " + theAttribute
				+ ". Using default value.");
			return theAttribute.getDefault();
		}
		if (getType().getRawType().isInstance(value))
			return value;
		else if (QuickUtils.isAssignableFrom(getType(), theValue.getType()))
			return QuickUtils.convert(getType(), value);
		else {
			try {
				return theAttribute.getType().cast((TypeToken<T>) theValue.getType(), value);
			} catch (QuickException e) {
				theMessageCenter.error("Value " + value + " from observable " + theValue + " is unacceptable for style attribute "
					+ theAttribute + ". Using default value.", e);
				return theAttribute.getDefault();
			}
		}
	}

	@Override
	public StyleAttributeEvent<T> createInitialEvent(T value, Object cause) {
		return new StyleAttributeEvent<>(theAttribute, true, null, value, cause);
	}

	@Override
	public StyleAttributeEvent<T> createChangeEvent(T oldVal, T newVal, Object cause) {
		return new StyleAttributeEvent<>(theAttribute, false, oldVal, newVal, cause);
	}

	@Override
	public Observable<ObservableValueEvent<T>> changes() {
		return new StyleValueChanges();
	}

	StyleAttributeEvent<T> wrap(ObservableValueEvent<? extends T> event) {
		if (event.isInitial())
			return createInitialEvent(QuickUtils.convert(getType(), event.getNewValue()), event);
		else
			return createChangeEvent(QuickUtils.convert(getType(), event.getOldValue()), QuickUtils.convert(getType(), event.getNewValue()),
				event.getCause());
	}

	@Override
	public int hashCode() {
		return theValue.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != getClass())
			return false;
		StyleValueImpl<?> sv = (StyleValueImpl<?>) obj;
		return Objects.equals(theValue, sv.theValue);
	}

	@Override
	public String toString() {
		return theValue.toString();
	}

	private class StyleValueChanges implements Observable<ObservableValueEvent<T>> {
		@Override
		public Subscription subscribe(Observer<? super ObservableValueEvent<T>> observer) {
			return theValue.changes().act(event -> {
				StyleAttributeEvent<T> styleEvent;
				if (theAttribute.canAccept(event.getNewValue())) {
					styleEvent = wrap(event);
				} else {
					theMessageCenter.info("Value " + event.getNewValue() + " from observable " + theValue
						+ " is unacceptable for style attribute " + theAttribute + ". Using default value.");
					if (theAttribute.canAccept(event.getOldValue()))
						styleEvent = createChangeEvent(event.getOldValue(), theAttribute.getDefault(), event);
					else
						styleEvent = null; // Stay at default value.
				}
				if (styleEvent != null) {
					try (Transaction t = Causable.use(styleEvent)) {
						observer.onNext(styleEvent);
					}
				}
			});
		}

		@Override
		public boolean isSafe() {
			return theValue.changes().isSafe();
		}

		@Override
		public Transaction lock() {
			return theValue.lock();
		}

		@Override
		public Transaction tryLock() {
			return theValue.tryLock();
		}
	}
}
