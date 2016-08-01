package org.quick.core.style2;

import java.util.Objects;

import org.observe.ObservableValue;
import org.observe.ObservableValueEvent;
import org.observe.Observer;
import org.observe.Subscription;
import org.observe.util.ObservableUtils;
import org.quick.core.style.StyleAttribute;

import com.google.common.reflect.TypeToken;

public class StyleConditionValue<T> implements ObservableValue<T> {
	private final StyleAttribute<T> theAttribute;
	private final StyleCondition theCondition;
	private final ObservableValue<? extends T> theValue;

	public StyleConditionValue(StyleAttribute<T> attribute, StyleCondition condition, ObservableValue<? extends T> value) {
		theAttribute = attribute;
		theCondition = condition;
		theValue = value;
	}

	public StyleAttribute<T> getAttribute() {
		return theAttribute;
	}

	public StyleCondition getCondition() {
		return theCondition;
	}

	@Override
	public TypeToken<T> getType() {
		return theAttribute.getType().getType();
	}

	@Override
	public T get() {
		return theValue.get();
	}

	@Override
	public Subscription subscribe(Observer<? super ObservableValueEvent<T>> observer) {
		return theValue.subscribe(new Observer<ObservableValueEvent<? extends T>>() {
			@Override
			public <V extends ObservableValueEvent<? extends T>> void onNext(V event) {
				if (theAttribute.canAccept(event.getValue()))
					observer.onNext(ObservableUtils.wrap(event, StyleConditionValue.this));
				else if (theAttribute.canAccept(event.getOldValue()))
					observer.onNext(createChangeEvent(event.getOldValue(), theAttribute.getDefault(), event));
				// else Nothing. Stay at default value.
			}

			@Override
			public <V extends ObservableValueEvent<? extends T>> void onCompleted(V event) {
				// Not sure what this would mean, but I guess we'll propagate it
				if (theAttribute.canAccept(event.getValue()))
					observer.onCompleted(ObservableUtils.wrap(event, StyleConditionValue.this));
				else
					observer.onCompleted(createChangeEvent(theAttribute.getDefault(), theAttribute.getDefault(), event));
			}
		});
	}


	@Override
	public boolean isSafe() {
		return theValue.isSafe();
	}

	@Override
	public int hashCode() {
		int ret = 0;
		if (theCondition != null)
			ret += theCondition.hashCode();
		ret *= 13;
		if (theValue != null)
			ret += theValue.hashCode();
		return ret;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof StyleConditionValue))
			return false;
		StyleConditionValue<?> sev = (StyleConditionValue<?>) obj;
		return Objects.equals(theCondition, sev.theCondition) && Objects.equals(theValue, sev.theValue);
	}

	@Override
	public String toString() {
		return theCondition + "=" + theValue;
	}
}
