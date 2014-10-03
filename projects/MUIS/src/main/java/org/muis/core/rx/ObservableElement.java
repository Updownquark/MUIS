package org.muis.core.rx;

class ObservableElement<T> extends DefaultObservable<T> {
	private Observer<T> theController;
	private T theValue;

	ObservableElement(T value) {
		theValue = value;
		theController = control(subscriber -> {
			subscriber.onNext(theValue);
		});
	}

	void set(T newValue) {
		theValue = newValue;
		theController.onNext(newValue);
	}

	void remove() {
		theController.onCompleted(theValue);
	}
}