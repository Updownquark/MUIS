package org.muis.base.model;

/**
 * Validates values
 * 
 * @param <T> The type of values that this validator knows how to validate
 */
public interface Validator<T> {
	/**
	 * @param value The value to validate
	 * @return An error message or null, if the value is valid
	 */
	String validate(T value);
}
