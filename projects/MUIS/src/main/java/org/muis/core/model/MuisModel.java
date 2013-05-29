package org.muis.core.model;

/** Contains values and sub models that define how MUIS widgets present and alter their data */
public interface MuisModel {
	/**
	 * @param name The name of the sub-model to get
	 * @return The sub-model with the given name, or null if no such sub-model exists in this model
	 */
	MuisModel getModel(String name);

	/**
	 * @param <T> The compile-time type of the value
	 * @param name The name of the value to get
	 * @param type The run-time type of the value
	 * @return The value with the given name, or null if no such value exists in this model
	 * @throws ClassCastException If the given value exists in this model, but is not of the given type
	 */
	<T> MuisModelValue<? extends T> getValue(String name, Class<T> type) throws ClassCastException;
}
