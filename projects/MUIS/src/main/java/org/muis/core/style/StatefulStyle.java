package org.muis.core.style;


/**
 * An extension of MuisStyle that may have different attribute settings depending on a state. MuisStyle query methods on an implementation
 * of this class return the value for the element's current state, if it has one, or the base state (where no states are active) otherwise.
 */
public interface StatefulStyle {
	/**
	 * @return The styles that this style depends on for attribute values when an attribute's value is not set in this style directly for a
	 *         state
	 */
	StatefulStyle [] getStatefulDependencies();

	/** @return All style attributes that are set for any condition in this style specifically */
	Iterable<StyleAttribute<?>> allLocal();

	/** @return All style attributes that are set for any condition in this style or any of its dependents */
	Iterable<StyleAttribute<?>> allAttrs();

	/**
	 * @param <T> The type of the attribute
	 * @param attr The attribute to get the expressions for
	 * @return The expression/value combinations that are set in this style locally for the given attribute
	 */
	<T> StyleExpressionValue<T> [] getLocalExpressions(StyleAttribute<T> attr);

	/**
	 * @param <T> The type of the attribute
	 * @param attr The attribute to get the expressions for
	 * @return The expression/value combinations that are set in this style or any of its dependencies for the given attribute
	 */
	<T> StyleExpressionValue<T> [] getExpressions(StyleAttribute<T> attr);

	/** @param listener The listener to be notified when the effective value of any style attribute in this style changes for any state */
	void addListener(StyleExpressionListener listener);

	/** @param listener The listener to remove */
	void removeListener(StyleExpressionListener listener);
}
