package org.muis.core.style;

import org.muis.core.rx.Observable;

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

	/** @return An observable for changes in this style's expressions */
	Observable<StyleExpressionEvent<S, E, ?>> expressions();
}
