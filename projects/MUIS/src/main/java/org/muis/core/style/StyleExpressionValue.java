package org.muis.core.style;

import org.muis.core.rx.ObservableValue;
import org.muis.core.rx.ObservableValueEvent;
import org.muis.core.rx.Observer;
import org.muis.core.rx.Subscription;

/**
 * A style value that is contingent upon a boolean expression of some type
 *
 * @param <E> The type of expression that must evaluate to true for an expression value of a given type in order for the value to be applied
 * @param <V> The type of style value
 */
public class StyleExpressionValue<E extends StyleExpression<E>, V> implements ObservableValue<V> {
	private final E theExpression;
	private final ObservableValue<? extends V> theValue;

	/**
	 * @param expression The expression that must be true for this value to be active
	 * @param value The value for the attribute to enact if the expression is true
	 */
	public StyleExpressionValue(E expression, V value) {
		this(expression, ObservableValue.constant(value));
	}

	/**
	 * @param expression The expression that must be true for this value to be active
	 * @param value The value for the attribute to enact if the expression is true
	 */
	public StyleExpressionValue(E expression, ObservableValue<? extends V> value) {
		theExpression = expression;
		theValue = value;
	}

	/** @return The expression determining when this value applies */
	public E getExpression() {
		return theExpression;
	}

	@Override
	public prisms.lang.Type getType() {
		return theValue.getType();
	}

	@Override
	public V get() {
		return theValue.get();
	}

	@Override
	public Subscription<ObservableValueEvent<V>> subscribe(Observer<? super ObservableValueEvent<V>> observer) {
		return theValue.mapV(value -> value).subscribe(observer);
	}

	@Override
	public int hashCode() {
		int ret = 0;
		if(theExpression != null)
			ret += theExpression.hashCode();
		ret *= 13;
		if(theValue != null)
			ret += theValue.hashCode();
		return ret;
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof StyleExpressionValue))
			return false;
		StyleExpressionValue<?, ?> sev = (StyleExpressionValue<?, ?>) obj;
		return prisms.util.ArrayUtils.equals(theExpression, sev.theExpression) && prisms.util.ArrayUtils.equals(theValue, sev.theValue);
	}

	@Override
	public String toString() {
		return theExpression + "=" + theValue;
	}
}
