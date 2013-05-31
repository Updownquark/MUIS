package org.muis.core.model;

/**
 * A getter for a value
 * 
 * @param <T> The compile time type of the value returned by this getter
 */
public interface Getter<T> {
	/** @return The run time type of the value returned by this getter */
	Class<T> getType();

	/**
	 * @return This getter's value
	 * @throws IllegalStateException If an error occurs accessing the value
	 */
	T get() throws IllegalStateException;
}
