package org.muis.core.style;

import org.muis.core.rx.Observable;
import org.muis.core.rx.ObservableCollection;
import org.muis.core.rx.ObservableList;
import org.muis.core.rx.ObservableSet;

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
		org.muis.core.rx.CompoundObservableSet<StyleAttribute<?>> ret = new org.muis.core.rx.CompoundObservableSet<>();
		ret.addSet(allLocal());
		ret.addSet(ObservableCollection.flatten(getConditionalDependencies().mapC(depend -> depend.allAttrs())));
		return ret;
	}

	/**
	 * @param <T> The type of the attribute
	 * @param attr The attribute to get the expressions for
	 * @return The expression/value combinations that are set in this style or any of its dependencies for the given attribute
	 */
	default <T> ObservableList<StyleExpressionValue<E, T>> getExpressions(StyleAttribute<T> attr) {
		org.muis.core.rx.CompoundObservableList<StyleExpressionValue<E, T>> ret = new org.muis.core.rx.CompoundObservableList<>();
		ret.addList(getLocalExpressions(attr));
		ret.addList(ObservableList.flatten(getConditionalDependencies().mapC(depend -> depend.getExpressions(attr))));
		return ret;
	}

	/** @return An observable for changes in this style's expressions */
	Observable<StyleExpressionEvent<S, E, ?>> expressions();
}
