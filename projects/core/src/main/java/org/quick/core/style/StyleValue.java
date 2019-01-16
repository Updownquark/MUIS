package org.quick.core.style;

import org.observe.ObservableValue;

/**
 * An attribute's value within a {@link QuickStyle}
 *
 * @param <T> The type of the attribute that this value is for
 */
public interface StyleValue<T> extends ObservableValue<T> {
	/** @return The style attribute that this value is for */
	StyleAttribute<T> getAttribute();
}
