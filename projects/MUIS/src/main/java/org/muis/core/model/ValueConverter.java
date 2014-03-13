package org.muis.core.model;

/**
 * Converts or mutates a value from one type to anther
 * 
 * @param <F> The type that this converter can convert from
 * @param <T> The type that this converter can convert to
 */
public interface ValueConverter<F, T> {
	/**
	 * @param value The value to convert
	 * @return The converted value
	 */
	T convert(F value);
}
