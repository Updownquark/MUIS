package org.quick.core.prop;

import java.util.Collection;

import org.observe.ObservableValue;

/** Contains information needed to parse property text */
public interface ExpressionContext {
	/**
	 * @param name The name of the variable
	 * @return The value of the variable, or null if no such variable with the given name exists in this context
	 */
	ObservableValue<?> getVariable(String name);

	/**
	 * @param <T> The type of the collection to add the functions to
	 * @param name The name of the function
	 * @param functions The list to add all this context's functions with the given name to
	 * @return The list
	 */
	<T extends Collection<ExpressionFunction<?>>> T getFunctions(String name, T functions);

	/**
	 * @param <T> The type of the collection to add the units to
	 * @param name The name of the unit
	 * @param units The list to add all this context's units with the given name to
	 * @return The list
	 */
	<T extends Collection<Unit<?, ?>>> T getUnits(String name, T units);
}
