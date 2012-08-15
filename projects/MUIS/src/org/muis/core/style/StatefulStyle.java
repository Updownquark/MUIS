package org.muis.core.style;

/**
 * An extension of MuisStyle that may have different attribute settings depending on a state. MuisStyle query methods on an implementation
 * of this class return the value for the element's current state, if it has one, or the base state (where no states are active) otherwise.
 */
public interface StatefulStyle extends MuisStyle {
	/**
	 * @param state The state to get the settings for
	 * @return A style that represents this style's setting when the state is as given
	 */
	MuisStyle getStyleFor(String... state);

	/** @return All style attributes that are set for any condition in this style specifically */
	Iterable<StyleAttribute<?>> allLocal();

	/** @return All style attributes that are set for any condition in this style or any of its dependents */
	Iterable<StyleAttribute<?>> allAttrs();

	/**
	 * @param attr The attribute to get the expressions for
	 * @return The expression/value combinations that are set in this style for the given attribute
	 */
	<T> StyleExpressionValue<T> [] getExpressions(StyleAttribute<T> attr);
}
