package org.muis.core.style;

import org.muis.core.rx.*;

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
	 * @return The expression/value combinations that are set in this style locally for the given attribute
	 */
	<T> ObservableList<StyleExpressionValue<E, T>> getLocalExpressions(StyleAttribute<T> attr);

	/** @return All style attributes that are set for any condition in this style or any of its dependents */
	default ObservableSet<StyleAttribute<?>> allAttrs() {
		DefaultObservableSet<ObservableSet<StyleAttribute<?>>> ret = new DefaultObservableSet<>(new Type(ObservableSet.class, new Type(
			StyleAttribute.class)));
		java.util.Set<ObservableSet<StyleAttribute<?>>> controller = ret.control(null);
		controller.add(allLocal());
		controller.add(ObservableCollection.flatten(getConditionalDependencies().mapC(depend -> depend.allAttrs())));
		return ObservableCollection.flatten(ret);
	}

	/**
	 * @param <T> The type of the attribute
	 * @param attr The attribute to get the expressions for
	 * @return The expression/value combinations that are set in this style or any of its dependencies for the given attribute
	 */
	default <T> ObservableList<StyleExpressionValue<E, T>> getExpressions(StyleAttribute<T> attr) {
		DefaultObservableList<ObservableList<StyleExpressionValue<E, T>>> ret = new DefaultObservableList<>(new Type(ObservableList.class,
			new Type(StyleExpressionValue.class, new Type(Object.class, true), attr.getType().getType())));
		java.util.List<ObservableList<StyleExpressionValue<E, T>>> controller = ret.control(null);
		controller.add(getLocalExpressions(attr));
		controller.add(ObservableList.flatten(getConditionalDependencies().mapC(depend -> depend.getExpressions(attr))));
		return ObservableList.flatten(ret);
	}
}
