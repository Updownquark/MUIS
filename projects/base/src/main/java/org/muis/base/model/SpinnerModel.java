package org.muis.base.model;

import org.muis.core.model.MuisActionListener;
import org.muis.core.rx.ObservableValue;

/**
 * The model that controls a {@link org.muis.base.widget.Spinner} widget
 * 
 * @param <T> The type of value manipulated by the model
 */
public interface SpinnerModel<T> {
	/** @return The spinner's value */
	ObservableValue<? extends T> getValue();

	/** @return The action that increments the spinner's value */
	MuisActionListener getIncrement();

	/** @return The action that decrements the spinner's value */
	MuisActionListener getDecrement();
}
