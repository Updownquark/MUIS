package org.muis.core.style;

import java.util.Set;

import org.muis.rx.ObservableValue;
import org.muis.rx.collect.DefaultObservableSet;
import org.muis.rx.collect.ObservableList;
import org.muis.rx.collect.ObservableSet;

import prisms.lang.Type;

/**
 * Implements ConditionalStyle without dependencies
 *
 * @param <S> The type of style set
 * @param <E> The type of expression supported by the style set
 */
public abstract class SimpleConditionalStyle<S extends ConditionalStyle<S, E>, E extends StyleExpression<E>> implements
	ConditionalStyle<S, E>, Cloneable {
	private java.util.concurrent.ConcurrentHashMap<StyleAttribute<?>, StyleValueHolder<E, ?>> theAttributes;

	private DefaultObservableSet<StyleAttribute<?>> theObservableAtts;
	private Set<StyleAttribute<?>> theAttController;

	/** Creates a SimpleStatefulStyle */
	protected SimpleConditionalStyle() {
		theAttributes = new java.util.concurrent.ConcurrentHashMap<>();
		theObservableAtts = new DefaultObservableSet<>(new Type(StyleAttribute.class, new Type(Object.class, true)));
		theAttController = theObservableAtts.control(null);
	}

	@Override
	public ObservableSet<StyleAttribute<?>> allLocal() {
		return theObservableAtts;
	}

	@Override
	public <T> ObservableList<StyleExpressionValue<E, T>> getLocalExpressions(StyleAttribute<T> attr) {
		StyleValueHolder<E, T> holder = (StyleValueHolder<E, T>) theAttributes.get(attr);
		if(holder == null) {
			holder = new StyleValueHolder<>(this, attr, getExpressionType(), null);
			theAttributes.put(attr, holder);
			theAttController.add(attr);
		}
		return holder;
	}

	/**
	 * @param <T> The type of the attribute
	 * @see MutableConditionalStyle#set(StyleAttribute, StyleExpression, Object) Implemented to make extensions of this class easily support
	 *      {@link MutableConditionalStyle}
	 * @param attr The attribute to set the value of
	 * @param value The value to set for the attribute
	 * @return This style, for chaining
	 * @throws ClassCastException If the given value is not recognized by the attribute
	 * @throws IllegalArgumentException If the given value is invalid for the given attribute
	 */
	protected <T> SimpleConditionalStyle<S, E> set(StyleAttribute<T> attr, T value) throws ClassCastException, IllegalArgumentException {
		return set(attr, null, value);
	}

	/**
	 * @see MutableConditionalStyle#set(StyleAttribute, StyleExpression, Object) Implemented to make extensions of this class easily support
	 *      {@link MutableConditionalStyle}
	 * @param <T> The type of the attribute
	 * @param attr The attribute to set the value of
	 * @param exp The state expression for the value to be active for
	 * @param value The value to set for the attribute
	 * @return This style, for chaining
	 * @throws ClassCastException If the given value is not recognized by the attribute
	 * @throws IllegalArgumentException If the given value is invalid for the given attribute
	 */
	protected <T> SimpleConditionalStyle<S, E> set(StyleAttribute<T> attr, E exp, T value) throws ClassCastException,
		IllegalArgumentException {
		if(value == null) {
			clear(attr, exp);
			return this;
		}
		if(attr == null)
			throw new NullPointerException("Cannot set the value of a null attribute");
		value = castAndValidate(attr, value);
		setValue(attr, exp, ObservableValue.constant(attr.getType().getType(), value));
		return this;
	}

	/**
	 * @see MutableConditionalStyle#set(StyleAttribute, StyleExpression, Object) Implemented to make extensions of this class easily support
	 *      {@link MutableConditionalStyle}
	 * @param <T> The type of the attribute
	 * @param attr The attribute to set the value of
	 * @param exp The state expression for the value to be active for
	 * @param value The value to set for the attribute
	 * @return This style, for chaining
	 * @throws ClassCastException If the given value is not recognized by the attribute
	 * @throws IllegalArgumentException If the given value is invalid for the given attribute
	 */
	protected <T> SimpleConditionalStyle<S, E> set(StyleAttribute<T> attr, E exp, ObservableValue<? extends T> value)
		throws ClassCastException, IllegalArgumentException {
		if(value == null) {
			clear(attr, exp);
			return this;
		}
		if(attr == null)
			throw new NullPointerException("Cannot set the value of a null attribute");
		if(!attr.getType().canCast(value.getType()))
			throw new ClassCastException(value.getType() + " instance " + value + " cannot be set for attribute " + attr + " of type "
				+ attr.getType());
		value = value.mapV(v -> castAndValidate(attr, v));
		setValue(attr, exp, value);
		return this;
	}

	/**
	 * Ensures that the given value can be set for a particular attribute
	 *
	 * @param <T> The type of the attribute
	 * @param attr The attribute to set a value for
	 * @param value The value to be set for the attribute
	 * @return The value to store for the attribute
	 * @throws ClassCastException If the given value is not recognized by the attribute
	 * @throws IllegalArgumentException If the given value is not valid for the attribute
	 */
	protected <T> T castAndValidate(StyleAttribute<T> attr, T value) throws ClassCastException, IllegalArgumentException {
		T value2 = attr.getType().cast(attr.getType().getType(), value);
		if(value2 == null)
			throw new ClassCastException(value.getClass().getName() + " instance " + value + " cannot be set for attribute " + attr
				+ " of type " + attr.getType());
		if(attr.getValidator() != null)
			try {
				attr.getValidator().assertValid(value2);
			} catch(org.muis.core.MuisException e) {
				throw new IllegalArgumentException(e.getMessage());
			}
		return value2;
	}

	private <T> void setValue(StyleAttribute<T> attr, E exp, ObservableValue<? extends T> value) {
		StyleExpressionValue<E, T> sev = createStyleExpressionValue(attr, exp, value);
		StyleValueHolder<E, T> holder = (StyleValueHolder<E, T>) theAttributes.get(attr);
		if(holder == null) {
			holder = new StyleValueHolder<>(this, attr, getExpressionType(), sev);
			theAttributes.put(attr, holder);
			theAttController.add(attr);
		} else
			holder.set(sev);
	}

	/**
	 * Creates a {@link StyleExpressionValue}
	 *
	 * @param <T> The type of the value
	 * @param attr The attribute to create the expression value for
	 * @param exp The expression that the value is valid for
	 * @param value The style value
	 * @return The style expression value for the expression and value
	 */
	protected <T> StyleExpressionValue<E, T> createStyleExpressionValuex(StyleAttribute<T> attr, E exp, T value) {
		return createStyleExpressionValue(attr, exp, ObservableValue.constant(attr.getType().getType(), value));
	}

	/**
	 * Creates a {@link StyleExpressionValue}
	 *
	 * @param <T> The type of the value
	 * @param attr The attribute to create the expression value for
	 * @param exp The expression that the value is valid for
	 * @param value The style value
	 * @return The style expression value for the expression and value
	 */
	protected <T> StyleExpressionValue<E, T> createStyleExpressionValue(StyleAttribute<T> attr, E exp, ObservableValue<? extends T> value) {
		return new StyleExpressionValue<>(exp, value);
	}

	/**
	 * @see MutableConditionalStyle#clear(StyleAttribute, StyleExpression) Implemented to make extensions of this class easily support
	 *      MutableConditionalStyle
	 * @param attr The attribute to clear the value of
	 * @return This style, for chaining
	 */
	protected SimpleConditionalStyle<S, E> clear(StyleAttribute<?> attr) {
		return clear(attr, null);
	}

	/**
	 * @see MutableConditionalStyle#clear(StyleAttribute, StyleExpression) Implemented to make extensions of this class easily support
	 *      MutableConditionalStyle
	 * @param attr The attribute to clear the value of
	 * @param exp The state expression to clear the value for
	 * @return This style, for chaining
	 */
	protected SimpleConditionalStyle<S, E> clear(StyleAttribute<?> attr, E exp) {
		StyleValueHolder<E, ?> holder = theAttributes.get(attr);
		if(holder != null)
			holder.remove(exp);
		if(holder.size() == 0) {
			theAttributes.remove(attr);
			theAttController.remove(attr);
		}
		return this;
	}

	@Override
	public SimpleConditionalStyle<S, E> clone() {
		SimpleConditionalStyle<S, E> ret;
		try {
			ret = (SimpleConditionalStyle<S, E>) super.clone();
		} catch(CloneNotSupportedException e) {
			throw new IllegalStateException(e);
		}
		ret.theAttributes = new java.util.concurrent.ConcurrentHashMap<>();
		for(java.util.Map.Entry<StyleAttribute<?>, StyleValueHolder<E, ?>> entry : theAttributes.entrySet())
			ret.theAttributes.put(entry.getKey(), entry.getValue().clone());
		// Don't add the listeners--ret is a new style
		return ret;
	}
}
