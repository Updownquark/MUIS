package org.quick.base.model;

import org.observe.ObservableValue;
import org.quick.core.model.QuickActionListener;

/**
 * The model that controls a {@link org.quick.base.widget.Spinner} widget
 *
 * @param <T> The type of value manipulated by the model
 */
public interface SpinnerModel<T> {
	/** @return The spinner's value */
	ObservableValue<? extends T> getValue();

	/** @return The action that increments the spinner's value */
	QuickActionListener getIncrement();

	/** @return The action that decrements the spinner's value */
	QuickActionListener getDecrement();
}
