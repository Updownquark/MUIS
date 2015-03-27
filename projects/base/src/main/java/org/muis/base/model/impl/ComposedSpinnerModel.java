package org.muis.base.model.impl;

import org.muis.core.model.MuisActionListener;
import org.observe.ObservableValue;

/**
 * A spinner model composed of pre-assembled pieces: the value, incrementer, and decrementer components
 *
 * @param <T> The type of value manipulated by the spinner model
 */
public class ComposedSpinnerModel<T> implements org.muis.base.model.SpinnerModel<T> {
	private final ObservableValue<T> theValue;

	private final MuisActionListener theIncrementer;

	private final MuisActionListener theDecrementer;

	/**
	 * @param value The value for the spinner
	 * @param inc The incrementer action for the spinner
	 * @param dec The decrementer action for the spinner
	 */
	public ComposedSpinnerModel(ObservableValue<T> value, MuisActionListener inc, MuisActionListener dec) {
		theValue = value;
		theIncrementer = inc;
		theDecrementer = dec;
	}

	@Override
	public ObservableValue<? extends T> getValue() {
		return theValue;
	}

	@Override
	public MuisActionListener getIncrement() {
		return theIncrementer;
	}

	@Override
	public MuisActionListener getDecrement() {
		return theDecrementer;
	}
}
