package org.quick.base.model.impl;

import org.observe.ObservableValue;
import org.quick.core.model.QuickActionListener;

/**
 * A spinner model composed of pre-assembled pieces: the value, incrementer, and decrementer components
 *
 * @param <T> The type of value manipulated by the spinner model
 */
public class ComposedSpinnerModel<T> implements org.quick.base.model.SpinnerModel<T> {
	private final ObservableValue<T> theValue;

	private final QuickActionListener theIncrementer;

	private final QuickActionListener theDecrementer;

	/**
	 * @param value The value for the spinner
	 * @param inc The incrementer action for the spinner
	 * @param dec The decrementer action for the spinner
	 */
	public ComposedSpinnerModel(ObservableValue<T> value, QuickActionListener inc, QuickActionListener dec) {
		theValue = value;
		theIncrementer = inc;
		theDecrementer = dec;
	}

	@Override
	public ObservableValue<? extends T> getValue() {
		return theValue;
	}

	@Override
	public QuickActionListener getIncrement() {
		return theIncrementer;
	}

	@Override
	public QuickActionListener getDecrement() {
		return theDecrementer;
	}
}
