package org.quick.core.style;

import java.util.Objects;

import org.observe.ObservableValue;
import org.observe.ObservableValueEvent;
import org.observe.Observer;
import org.observe.Subscription;
import org.observe.util.ObservableUtils;
import org.quick.core.mgr.QuickMessageCenter;

import com.google.common.reflect.TypeToken;

/**
 * An attribute's value within a {@link QuickStyle}
 *
 * @param <T> The type of the attribute that this value is for
 */
public class StyleValue<T> implements ObservableValue<T> {
	private final StyleAttribute<T> theAttribute;
	private final ObservableValue<? extends T> theValue;
	private final QuickMessageCenter theMessageCenter;

	/**
	 * @param attribute The style attribute that this value is for
	 * @param value The value for the attribute
	 * @param msg The message center to log values from the observable that are not acceptable for the style attribute
	 */
	public StyleValue(StyleAttribute<T> attribute, ObservableValue<? extends T> value, QuickMessageCenter msg) {
		theAttribute = attribute;
		theValue = value;
		theMessageCenter = msg;
	}

	/** @return The style attribute that this value is for */
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
		if (theAttribute.canAccept(value))
			return value;
		else {
			theMessageCenter.info("Value " + value + " from observable " + theValue + " is unacceptable for style attribute " + theAttribute
				+ ". Using default value.");
			return theAttribute.getDefault();
		}
	}

	@Override
	public StyleAttributeEvent<T> createInitialEvent(T value) {
		return new StyleAttributeEvent<>(this, true, null, value, null);
	}

	@Override
	public StyleAttributeEvent<T> createChangeEvent(T oldVal, T newVal, Object cause) {
		return new StyleAttributeEvent<>(this, false, oldVal, newVal, cause);
	}

	@Override
	public Subscription subscribe(Observer<? super ObservableValueEvent<T>> observer) {
		return theValue.subscribe(new Observer<ObservableValueEvent<? extends T>>() {
			@Override
			public <V extends ObservableValueEvent<? extends T>> void onNext(V event) {
				if (theAttribute.canAccept(event.getValue()))
					observer.onNext(ObservableUtils.wrap(event, StyleValue.this));
				else {
					theMessageCenter.info("Value " + event.getValue() + " from observable " + theValue
						+ " is unacceptable for style attribute " + theAttribute + ". Using default value.");
					if (theAttribute.canAccept(event.getOldValue()))
						observer.onNext(createChangeEvent(event.getOldValue(), theAttribute.getDefault(), event));
					// else Nothing. Stay at default value.
				}
			}

			@Override
			public <V extends ObservableValueEvent<? extends T>> void onCompleted(V event) {
				// Not sure what this would mean, but I guess we'll propagate it
				if (theAttribute.canAccept(event.getValue()))
					observer.onCompleted(ObservableUtils.wrap(event, StyleValue.this));
				else {
					theMessageCenter.info("Value " + event.getValue() + " from observable " + theValue
						+ " is unacceptable for style attribute " + theAttribute + ". Using default value.");
					observer.onCompleted(createChangeEvent(theAttribute.getDefault(), theAttribute.getDefault(), event));
				}
			}
		});
	}


	@Override
	public boolean isSafe() {
		return theValue.isSafe();
	}

	@Override
	public int hashCode() {
		return theValue.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj==null || obj.getClass()!=getClass())
			return false;
		StyleValue<?> sv = (StyleValue<?>) obj;
		return Objects.equals(theValue, sv.theValue);
	}

	@Override
	public String toString() {
		return theValue.toString();
	}
}
