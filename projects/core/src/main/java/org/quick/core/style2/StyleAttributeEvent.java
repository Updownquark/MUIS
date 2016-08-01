package org.quick.core.style2;

import org.observe.ObservableValueEvent;

public class StyleAttributeEvent<T> extends ObservableValueEvent<T> {
	public StyleAttributeEvent(StyleValue<T> observable, boolean initial, T oldValue, T newValue, Object cause) {
		super(observable, initial, oldValue, newValue, cause);
	}

	@Override
	public StyleValue<T> getObservable() {
		return (StyleValue<T>) super.getObservable();
	}
}
