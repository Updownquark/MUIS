package org.muis.core.style;

/**
 * A style value that is contingent upon a boolean expression of some type
 *
 * @param <E> The type of expression that must evaluate to true for an expression value of a given type in order for the value to be applied
 * @param <V> The type of style value
 */
public class StyleExpressionValue<E extends StyleExpression<E>, V> {
	private final E theExpression;

	private final V theValue;

	/**
	 * @param expression The expression that must be true for this value to be active
	 * @param value The value for the attribute to enact if the expression is true
	 */
	public StyleExpressionValue(E expression, V value) {
		theExpression = expression;
		theValue = value;
	}
	/** @return The expression determining when this value applies */
	public E getExpression() {
		return theExpression;
	}

	/** @return The style value that is applied when the expression evaluates true */
	public V getValue() {
		return theValue;
	}
}
