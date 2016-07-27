package org.quick.base.model;

import com.google.common.reflect.TypeToken;

/**
 * Validates values
 *
 * @param <T> The type of values that this validator knows how to validate
 */
public interface Validator<T> {
	/** @return The type of values that this validator validates */
	TypeToken<T> getType();

	/**
	 * @param value The value to validate
	 * @return An error message or null, if the value is valid
	 */
	String validate(T value);
}
