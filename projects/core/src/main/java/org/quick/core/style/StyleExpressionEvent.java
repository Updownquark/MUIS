package org.quick.core.style;


/**
 * Represents the change to a single potentially expression-dependent style attribute in a stateful style
 *
 * @param <E> The type of expression for which the attribute's value was changed
 * @param <T> The type of attribute that was changed
 * @param <S> The type of style that the attribute was changed in
 */
public class StyleExpressionEvent<S extends ConditionalStyle<S, E>, E extends StyleExpression<E>, T> {
	private S theRootStyle;

	private S theLocalStyle;

	private final StyleAttribute<T> theAttribute;

	private final E theExpr;

	/**
	 * Creates a style attribute event
	 * 
	 * @param root The style that the change was in
	 * @param local The style that is firing the event
	 * @param attr The attribute whose value was changed
	 * @param expr The expression under which the attribute value changed
	 */
	public StyleExpressionEvent(S root, S local, StyleAttribute<T> attr, E expr) {
		theRootStyle = root;
		theLocalStyle = local;
		theAttribute = attr;
		theExpr = expr;
	}

	/** @return The style that the attribute was changed in */
	public S getRootStyle() {
		return theRootStyle;
	}

	/** @return The style that is firing the event */
	public S getLocalStyle() {
		return theLocalStyle;
	}

	/** @return The attribute whose value was changed */
	public StyleAttribute<T> getAttribute() {
		return theAttribute;
	}

	/** @return The expression under which the attribute value changed in the style */
	public E getExpression() {
		return theExpr;
	}
}
