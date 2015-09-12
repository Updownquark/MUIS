package org.quick.core.style;

/**
 * Represents an expression that a style attribute's value may be conditional on.
 *
 * @param <T> The type of the expression
 * @see ConditionalStyle
 */
public interface StyleExpression<T extends StyleExpression<?>> {
	/**
	 * @param expr The expression to compare to
	 * @return -1 if this expression is always false when {@code expr} is true, 1 if this expression is always true when {@code expr} is
	 *         true, or 0 if this expression's evaluation may be true or false if {@code expr} is true
	 */
	int getWhenTrue(T expr);

	/**
	 * @param expr The expression to compare to
	 * @return -1 if this expression is always false when {@code expr} is false, 1 if this expression is always true when {@code expr} is
	 *         false, or 0 if this expression's evaluation may be true or false if {@code expr} is false
	 */
	int getWhenFalse(T expr);

	/**
	 * @return The overall priority of this expression--values conditional on higher-priority expressions will take precedence over values
	 *         conditional on lower-priority expressions
	 */
	int getPriority();
}
