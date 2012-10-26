package org.muis.core.style;

/**
 * An extension of MuisStyle that may have different attribute settings depending on a condition of some type.
 *
 * @param <S> The type of the conditional style set
 * @param <E> The type of expression the style set supports
 */
public interface ConditionalStyle<S extends ConditionalStyle<S, E>, E extends StyleExpression<E>> {
	/**
	 * @return The styles that this style depends on for attribute values when an attribute's value is not set in this style directly for a
	 *         state
	 */
	S [] getConditionalDependencies();

	/** @return All style attributes that are set for any condition in this style specifically */
	Iterable<StyleAttribute<?>> allLocal();

	/** @return All style attributes that are set for any condition in this style or any of its dependents */
	Iterable<StyleAttribute<?>> allAttrs();

	/**
	 * @param <T> The type of the attribute
	 * @param attr The attribute to get the expressions for
	 * @return The expression/value combinations that are set in this style locally for the given attribute
	 */
	<T> StyleExpressionValue<E, T> [] getLocalExpressions(StyleAttribute<T> attr);

	/**
	 * @param <T> The type of the attribute
	 * @param attr The attribute to get the expressions for
	 * @return The expression/value combinations that are set in this style or any of its dependencies for the given attribute
	 */
	<T> StyleExpressionValue<E, T> [] getExpressions(StyleAttribute<T> attr);

	/** @param listener The listener to be notified when the effective value of any style attribute in this style changes for any state */
	void addListener(StyleExpressionListener<S, E> listener);

	/** @param listener The listener to remove */
	void removeListener(StyleExpressionListener<S, E> listener);
}
