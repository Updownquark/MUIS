package org.muis.core.rx;

import prisms.lang.Type;

class ObservableElementImpl<T> extends DefaultObservableValue<T> {
	private Observer<ObservableValueEvent<T>> theController;
	private final Type theType;
	private T theValue;

	ObservableElementImpl(Type type, T value) {
		theType = type;
		theValue = value;
		theController = control(null);
	}

	void set(T newValue) {
		T oldValue = theValue;
		theValue = newValue;
		theController.onNext(new ObservableValueEvent<T>(this, oldValue, newValue, null));
	}

	void remove() {
		theController.onCompleted(new ObservableValueEvent<T>(this, theValue, theValue, null));
	}

	@Override
	public Type getType() {
		return theType;
	}

	@Override
	public T get() {
		return theValue;
	}
}