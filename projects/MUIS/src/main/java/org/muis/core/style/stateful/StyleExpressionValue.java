package org.muis.core.style.stateful;


/**
 * Represents a state expression-value combination
 *
 * @param <T> The type of the attribute that this expression value is for
 */
public class StyleExpressionValue<T> {
	private final StateExpression theExpression;

	private final T theValue;

	/**
	 * @param exp The state expression to wrap
	 * @param value The attribute value to wrap
	 */
	public StyleExpressionValue(StateExpression exp, T value) {
		theExpression = exp;
		theValue = value;
	}

	/** @return The state expression wrapped by this object */
	public StateExpression getExpression() {
		return theExpression;
	}

	/** @return The attribute value wrapped by this object */
	public T getValue() {
		return theValue;
	}
}
