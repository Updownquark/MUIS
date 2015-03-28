package org.muis.core.style;

import static org.muis.core.style.StyleExpressionValue.STYLE_EXPRESSION_COMPARE;

import org.observe.collect.*;

import prisms.lang.Type;

/**
 * An extension of MuisStyle that may have different attribute settings depending on a condition of some type.
 *
 * @param <S> The type of the conditional style set
 * @param <E> The type of expression the style set supports
 */
public interface ConditionalStyle<S extends ConditionalStyle<S, E>, E extends StyleExpression<E>> {
	/** @return The type of expressions that this conditional style uses */
	Type getExpressionType();

	/**
	 * @return The styles that this style depends on for attribute values when an attribute's value is not set in this style directly for a
	 *         state
	 */
	ObservableList<S> getConditionalDependencies();

	/** @return All style attributes that are set for any condition in this style specifically */
	ObservableSet<StyleAttribute<?>> allLocal();

	/**
	 * @param <T> The type of the attribute
	 * @param attr The attribute to get the expressions for
	 * @return The expression/value combinations that are set in this style locally for the given attribute, sorted by decreasing priority
	 */
	<T> ObservableOrderedCollection<StyleExpressionValue<E, T>> getLocalExpressions(StyleAttribute<T> attr);

	/** @return All style attributes that are set for any condition in this style or any of its dependents */
	default ObservableSet<StyleAttribute<?>> allAttrs() {
		DefaultObservableSet<ObservableSet<StyleAttribute<?>>> ret = new DefaultObservableSet<>(new Type(ObservableSet.class, new Type(
			StyleAttribute.class)));
		java.util.Set<ObservableSet<StyleAttribute<?>>> controller = ret.control(null);
		controller.add(allLocal());
		controller.add(ObservableSet.flatten(getConditionalDependencies().map(depend -> depend.allAttrs())));
		return new org.observe.util.ObservableSetWrapper<StyleAttribute<?>>(ObservableSet.flatten(ret)) {
			@Override
			public String toString() {
				return "All attributes for " + ConditionalStyle.this;
			}
		};
	}

	/**
	 * @param <T> The type of the attribute
	 * @param attr The attribute to get the expressions for
	 * @return The expression/value combinations that are set in this style or any of its dependencies for the given attribute, sorted by
	 *         decreasing priority
	 */
	default <T> ObservableOrderedCollection<StyleExpressionValue<E, T>> getExpressions(StyleAttribute<T> attr) {
		DefaultObservableList<ObservableOrderedCollection<StyleExpressionValue<E, T>>> ret = new DefaultObservableList<>(
			new Type(ObservableOrderedCollection.class, new Type(StyleExpressionValue.class, new Type(Object.class, true), attr.getType()
				.getType())));
		java.util.List<ObservableOrderedCollection<StyleExpressionValue<E, T>>> controller = ret.control(null);
		controller.add(getLocalExpressions(attr));
		controller.add(ObservableOrderedCollection.flatten(getConditionalDependencies().map(depend -> depend.getExpressions(attr)),
			STYLE_EXPRESSION_COMPARE));
		return new org.observe.util.ObservableOrderedCollectionWrapper<StyleExpressionValue<E, T>>(ObservableOrderedCollection.flatten(ret,
			STYLE_EXPRESSION_COMPARE)) {
			@Override
			public String toString() {
				return attr + " expressions for " + ConditionalStyle.this;
			}
		};
	}
}
