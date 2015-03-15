package org.muis.core.model;

import org.muis.rx.ObservableValue;

/** Contains values and sub models that define how MUIS widgets present and alter their data */
public interface MuisAppModel {
	/**
	 * @param name The name of the sub-model to get
	 * @return The sub-model with the given name, or null if no such sub-model exists in this model
	 */
	MuisAppModel getSubModel(String name);

	/**
	 * @param <T> The (compile-time) type of the widget model to get
	 * @param name The name of the widget model to get
	 * @param modelType The (run-time) type of the widget model to get
	 * @return The widget model with the given name, or null if no widget model with the given name exists in this model
	 * @throws ClassCastException If the given widget model exists in this model, but is not of the given type
	 */
	<T extends MuisWidgetModel> T getWidgetModel(String name, Class<T> modelType) throws ClassCastException;

	/**
	 * @param <T> The compile-time type of the value
	 * @param name The name of the value to get
	 * @param type The type of the value to be checked at run-time, or null to get the value without type checking
	 * @return The value with the given name, or null if no such value exists in this model
	 * @throws ClassCastException If the given value exists in this model, but is not of the given type
	 */
	<T> ObservableValue<? extends T> getValue(String name, Class<T> type) throws ClassCastException;

	/**
	 * @param name The name of the action to get the listener for
	 * @return The action listener for the given action, or null if this model is not listening to the given action
	 */
	MuisActionListener getAction(String name);
}
