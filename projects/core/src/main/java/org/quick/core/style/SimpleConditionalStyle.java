package org.quick.core.style;

import org.observe.ObservableValue;
import org.observe.collect.ObservableList;
import org.observe.collect.ObservableSet;
import org.observe.collect.impl.ObservableHashSet;
import org.quick.core.QuickException;
import org.quick.core.mgr.QuickMessageCenter;

import com.google.common.reflect.TypeToken;

/**
 * Implements ConditionalStyle without dependencies
 *
 * @param <S> The type of style set
 * @param <E> The type of expression supported by the style set
 */
public abstract class SimpleConditionalStyle<S extends ConditionalStyle<S, E>, E extends StyleExpression<E>> implements
	ConditionalStyle<S, E> {
	private final QuickMessageCenter theMessageCenter;
	private java.util.concurrent.ConcurrentHashMap<StyleAttribute<?>, StyleValueHolder<E, ?>> theAttributes;

	private ObservableSet<StyleAttribute<?>> theObservableAtts;
	private ObservableSet<StyleAttribute<?>> theAttController;

	/** @param msg The message center to report style value validation errors to */
	protected SimpleConditionalStyle(QuickMessageCenter msg) {
		theMessageCenter = msg;
		theAttributes = new java.util.concurrent.ConcurrentHashMap<>();
		theAttController = new ObservableHashSet<>(new TypeToken<StyleAttribute<?>>() {});
		theObservableAtts = theAttController.immutable();
	}

	@Override
	public ObservableSet<StyleAttribute<?>> allLocal() {
		return theObservableAtts;
	}

	@Override
	public <T> ObservableList<StyleExpressionValue<E, T>> getLocalExpressions(StyleAttribute<T> attr) {
		StyleValueHolder<E, T> holder = (StyleValueHolder<E, T>) theAttributes.get(attr);
		if(holder == null) {
			holder = new StyleValueHolder<>(this, attr, null);
			theAttributes.put(attr, holder);
			theAttController.add(attr);
		}
		return holder;
	}

	/**
	 * @param <T> The type of the attribute
	 * @see MutableConditionalStyle#set(StyleAttribute, StyleExpression, ObservableValue) Implemented to make extensions of this class
	 *      easily support {@link MutableConditionalStyle}
	 * @param attr The attribute to set the value of
	 * @param value The value to set for the attribute
	 * @return This style, for chaining
	 * @throws ClassCastException If the given value is not recognized by the attribute
	 * @throws IllegalArgumentException If the given value is invalid for the given attribute
	 */
	protected <T> SimpleConditionalStyle<S, E> set(StyleAttribute<T> attr, ObservableValue<T> value)
		throws ClassCastException, IllegalArgumentException {
		return set(attr, null, value);
	}

	/**
	 * @see MutableConditionalStyle#set(StyleAttribute, StyleExpression, ObservableValue) Implemented to make extensions of this class
	 *      easily support {@link MutableConditionalStyle}
	 * @param <T> The type of the attribute
	 * @param attr The attribute to set the value of
	 * @param exp The state expression for the value to be active for
	 * @param value The value to set for the attribute
	 * @return This style, for chaining
	 * @throws ClassCastException If the given value is not recognized by the attribute
	 * @throws IllegalArgumentException If the given value is invalid for the given attribute
	 */
	protected <T> SimpleConditionalStyle<S, E> set(StyleAttribute<T> attr, E exp, ObservableValue<T> value)
		throws ClassCastException, IllegalArgumentException {
		if(value == null) {
			clear(attr, exp);
			return this;
		}
		if(attr == null)
			throw new NullPointerException("Cannot set the value of a null attribute");
		if (!attr.getType().canAccept(value.getType()))
			throw new ClassCastException(value.getType() + " instance " + value + " cannot be set for attribute " + attr + " of type "
				+ attr.getType());
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
		T value2;
		try {
			value2 = attr.getType().cast(attr.getType().getType(), value);
		} catch (QuickException e) {
			throw new ClassCastException(e.getMessage());
		}
		if(attr.getValidator() != null)
			try {
				attr.getValidator().assertValid(value2);
			} catch(org.quick.core.QuickException e) {
				throw new IllegalArgumentException(e.getMessage());
			}
		return value2;
	}

	private <T> void setValue(StyleAttribute<T> attr, E exp, ObservableValue<T> value) {
		StyleExpressionValue<E, T> sev = createStyleExpressionValue(attr, exp, value);
		StyleValueHolder<E, T> holder = (StyleValueHolder<E, T>) theAttributes.get(attr);
		if(holder == null) {
			holder = new StyleValueHolder<>(this, attr, sev);
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
	protected <T> StyleExpressionValue<E, T> createStyleExpressionValue(StyleAttribute<T> attr, E exp, ObservableValue<T> value) {
		return new StyleExpressionValue<>(exp, value, new SafeStyleValue<>(attr, value, theMessageCenter));
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
}
