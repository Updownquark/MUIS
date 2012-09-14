package org.muis.core.style;

/**
 * Represents an attribute value that is specific to a {@link NamedStyleGroup group}, an element {@link TypedStyleGroup type}, or a
 * {@link org.muis.core.mgr.MuisState state}
 *
 * @param <E> The type of the element that this value is for
 * @param <T> The type of the style attribute that this value is for
 */
public class StyleGroupTypeExpressionValue<E extends org.muis.core.MuisElement, T> {
	private String theGroupName;

	private Class<E> theType;

	private StateExpression theExpression;

	private T theValue;

	/**
	 * @param groupName The name of the group that this value is for
	 * @param type The element type that this value is for
	 * @param exp The state expression to wrap
	 * @param value The attribute value to wrap
	 */
	public StyleGroupTypeExpressionValue(String groupName, Class<E> type, StateExpression exp, T value) {
		theGroupName = groupName;
		if(type == null)
			type = (Class<E>) org.muis.core.MuisElement.class;
		else if(!org.muis.core.MuisElement.class.isAssignableFrom(type))
			throw new IllegalArgumentException("Only subtypes of " + org.muis.core.MuisElement.class.getSimpleName()
				+ " may be used for typed style groups");
		theType = type;
		theExpression = exp;
		theValue = value;
	}

	/** @return The name of the group that this value is for */
	public String getGroupName() {
		return theGroupName;
	}

	/** @return The element type that this value is for */
	public Class<E> getType() {
		return theType;
	}

	/** @return The state expression that this value is for */
	public StateExpression getExpression() {
		return theExpression;
	}

	/** @return The attribute value */
	public T getValue() {
		return theValue;
	}
}
