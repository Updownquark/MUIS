package org.quick.core.style;

import static org.quick.core.style.StyleExpressionValue.STYLE_EXPRESSION_COMPARE;

import org.observe.collect.ObservableCollection;
import org.observe.collect.ObservableList;
import org.observe.collect.ObservableOrderedCollection;
import org.observe.collect.ObservableSet;
import org.observe.collect.impl.ObservableArrayList;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

/**
 * An extension of QuickStyle that may have different attribute settings depending on a condition of some type.
 *
 * @param <S> The type of the conditional style set
 * @param <E> The type of expression the style set supports
 */
public interface ConditionalStyle<S extends ConditionalStyle<S, E>, E extends StyleExpression<E>> {
	/** @return The type of expressions that this conditional style uses */
	TypeToken<E> getExpressionType();

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
		ObservableArrayList<ObservableCollection<StyleAttribute<?>>> ret = new ObservableArrayList<>(
			new TypeToken<ObservableCollection<StyleAttribute<?>>>() {});
		ret.add(allLocal());
		ret.add(ObservableCollection.flatten(getConditionalDependencies().map(depend -> depend.allAttrs())));
		return new org.observe.util.ObservableSetWrapper<StyleAttribute<?>>(
			ObservableSet.unique(ObservableCollection.flatten(ret), Object::equals), false) {
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
		ObservableArrayList<ObservableOrderedCollection<StyleExpressionValue<E, T>>> ret = new ObservableArrayList<>(
			new TypeToken<ObservableOrderedCollection<StyleExpressionValue<E, T>>>() {}
				.where(new TypeParameter<E>() {}, getExpressionType()).where(new TypeParameter<T>() {}, attr.getType().getType()));
		ret.add(getLocalExpressions(attr));
		ret.add(ObservableOrderedCollection.flatten(getConditionalDependencies().map(depend -> depend.getExpressions(attr))));
		return new org.observe.util.ObservableOrderedCollectionWrapper<StyleExpressionValue<E, T>>(ObservableOrderedCollection.flatten(ret),
			false) {
			@Override
			public String toString() {
				return attr + " expressions for " + ConditionalStyle.this;
			}
		}.sorted(STYLE_EXPRESSION_COMPARE);
	}
}
