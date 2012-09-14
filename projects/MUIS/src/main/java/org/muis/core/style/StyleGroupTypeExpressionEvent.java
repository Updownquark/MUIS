package org.muis.core.style;

/**
 * Represents the change to a single potentially group-, type-, or expression-dependent style attribute in a style sheet
 *
 * @param <E> The element type that the attribute was changed for
 * @param <T> The type of attribute that was changed
 */
public class StyleGroupTypeExpressionEvent<E extends org.muis.core.MuisElement, T> {
	private StyleSheet theRootStyle;

	private StyleSheet theLocalStyle;

	private final StyleAttribute<T> theAttribute;

	private final String theGroupName;

	private Class<E> theType;

	private final StateExpression theExpr;

	/**
	 * Creates a style attribute event
	 *
	 * @param root The style sheet that the change was in
	 * @param local The style sheet that is firing the event
	 * @param attr The attribute whose value was changed
	 * @param groupName The name of the group that the attribute was changed for
	 * @param type The element type that the attribute was changed for
	 * @param expr The state expression under which the attribute value changed
	 */
	public StyleGroupTypeExpressionEvent(StyleSheet root, StyleSheet local, StyleAttribute<T> attr, String groupName, Class<E> type,
		StateExpression expr) {
		theRootStyle = root;
		theLocalStyle = local;
		theAttribute = attr;
		theGroupName = groupName;
		theType = type;
		theExpr = expr;
	}

	/** @return The style that the attribute was changed in */
	public StyleSheet getRootStyle() {
		return theRootStyle;
	}

	/** @return The style that is firing the event */
	public StyleSheet getLocalStyle() {
		return theLocalStyle;
	}

	/** @return The attribute whose value was changed */
	public StyleAttribute<T> getAttribute() {
		return theAttribute;
	}

	/** @return The name of the group that the attribute was changed for */
	public String getGroupName() {
		return theGroupName;
	}

	/** @return The element type that the attribute was changed for */
	public Class<E> getType() {
		return theType;
	}

	/** @return The state under which the attribute value changed in the style */
	public StateExpression getExpression() {
		return theExpr;
	}
}
