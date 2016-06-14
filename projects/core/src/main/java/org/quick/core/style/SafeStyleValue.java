package org.quick.core.style;

import org.observe.ObservableValue;
import org.observe.ObservableValueEvent;
import org.observe.Observer;
import org.observe.Subscription;
import org.quick.core.QuickException;
import org.quick.core.mgr.QuickMessageCenter;

import com.google.common.reflect.TypeToken;

public class SafeStyleValue<T> implements ObservableValue<T> {
	private final StyleAttribute<T> theAttr;
	private final ObservableValue<T> theWrapped;
	private final QuickMessageCenter theMessageCenter;

	public SafeStyleValue(StyleAttribute<T> attr, ObservableValue<T> wrap, QuickMessageCenter msg) {
		theAttr = attr;
		theWrapped = wrap;
		theMessageCenter = msg;
	}

	@Override
	public Subscription subscribe(Observer<? super ObservableValueEvent<T>> observer) {
		return theWrapped.subscribe(new Observer<ObservableValueEvent<T>>() {
			boolean isOverridden;

			@Override
			public <V extends ObservableValueEvent<T>> void onNext(V value) {
				if (theAttr.getValidator() != null) {
					try {
						theAttr.getValidator().assertValid(value.getValue());
						observer.onNext(createChangeEvent(getPreValue(value.getOldValue()), value.getValue(), value.getCause()));
						isOverridden = false;
					} catch (QuickException e) {
						theMessageCenter.error(
							(value.isInitial() ? "Initial" : "Updated") + " value for " + theAttr + " " + value.getValue() + " is invalid",
							e);
						if (!isOverridden)
							observer.onNext(createChangeEvent(getPreValue(value.getOldValue()), theAttr.getDefault(), value.getCause()));
						isOverridden = true;
					}
				} else
					observer.onNext(createChangeEvent(value.getOldValue(), value.getValue(), value.getCause()));
			}

			T getPreValue(T old) {
				if (isOverridden)
					return theAttr.getDefault();
				else
					return old;
			}
		});
	}

	@Override
	public boolean isSafe() {
		return theWrapped.isSafe();
	}

	@Override
	public TypeToken<T> getType() {
		return theWrapped.getType();
	}

	@Override
	public T get() {
		T value = theWrapped.get();
		if (theAttr.getValidator() != null)
			try {
				theAttr.getValidator().assertValid(value);
			} catch (QuickException e) {
				theMessageCenter.error("Value for " + theAttr + " " + value + " is invalid", e);
				return theAttr.getDefault();
			}
		return value;
	}
}
