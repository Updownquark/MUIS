package org.muis.core.style.stateful;

import org.muis.core.style.StyleAttribute;

/**
 * Represents the change to a single potentially expression-dependent style attribute in a stateful style
 * 
 * @param <T> The type of attribute that was changed
 */
public class StyleExpressionEvent<T> {
	private StatefulStyle theRootStyle;

	private StatefulStyle theLocalStyle;

	private final StyleAttribute<T> theAttribute;

	private final StateExpression theExpr;

	/**
	 * Creates a style attribute event
	 *
	 * @param root The style that the change was in
	 * @param local The style that is firing the event
	 * @param attr The attribute whose value was changed
	 * @param expr The state expression under which the attribute value changed
	 */
	public StyleExpressionEvent(StatefulStyle root, StatefulStyle local, StyleAttribute<T> attr, StateExpression expr) {
		theRootStyle = root;
		theLocalStyle = local;
		theAttribute = attr;
		theExpr = expr;
	}

	/** @return The style that the attribute was changed in */
	public StatefulStyle getRootStyle() {
		return theRootStyle;
	}

	/** @return The style that is firing the event */
	public StatefulStyle getLocalStyle() {
		return theLocalStyle;
	}

	/** @return The attribute whose value was changed */
	public StyleAttribute<T> getAttribute() {
		return theAttribute;
	}

	/** @return The state under which the attribute value changed in the style */
	public StateExpression getExpression() {
		return theExpr;
	}
}
