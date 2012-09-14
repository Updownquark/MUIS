package org.muis.core.style;

/**
 * Represents a style sheet in MUIS that can be populated with style attribute values that are potentially specific to a
 * {@link NamedStyleGroup group}, {@link TypedStyleGroup type}, and/or {@link org.muis.core.mgr.MuisState state}
 */
public interface StyleSheet {
	/**
	 * @return The style sheetss that this style sheet depends on for attribute values when an attribute's value is not set in this style
	 *         sheet directly for a state
	 */
	StyleSheet [] getStyleSheetDependencies();

	/** @return All style attributes that are set for any condition in this style sheet specifically */
	Iterable<StyleAttribute<?>> allLocal();

	/** @return All style attributes that are set for any condition in this style sheet or any of its dependents */
	Iterable<StyleAttribute<?>> allAttrs();

	/**
	 * @param <T> The type of the attribute
	 * @param attr The attribute to get the expressions for
	 * @return The group/type/expression/value combinations that are set in this style sheet locally for the given attribute
	 */
	<T> StyleGroupTypeExpressionValue<?, T> [] getLocalExpressions(StyleAttribute<T> attr);

	/**
	 * @param <T> The type of the attribute
	 * @param attr The attribute to get the expressions for
	 * @return The group/type/expression/value combinations that are set in this style sheet or any of its dependencies for the given
	 *         attribute
	 */
	<T> StyleGroupTypeExpressionValue<?, T> [] getExpressions(StyleAttribute<T> attr);

	/**
	 * @param listener The listener to be notified when the effective value of any style attribute in this style sheet changes for any
	 *            group, type or state
	 */
	void addListener(StyleGroupTypeExpressionListener listener);

	/** @param listener The listener to remove */
	void removeListener(StyleGroupTypeExpressionListener listener);
}
