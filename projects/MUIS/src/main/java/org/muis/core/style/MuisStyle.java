package org.muis.core.style;

/** Governs the set of properties that define how MUIS elements of different types render themselves */
public interface MuisStyle extends Iterable<StyleAttribute<?>> {
	/** @return The styles, in order, that this style depends on for attributes not set directly in this style */
	MuisStyle [] getDependencies();

	/**
	 * @param attr The attribute to check
	 * @return Whether the attribute is set directly in this style
	 */
	boolean isSet(StyleAttribute<?> attr);

	/**
	 * @param attr The attribute to check
	 * @return Whether the attribute is set in this style or one of its ancestors
	 */
	boolean isSetDeep(StyleAttribute<?> attr);

	/**
	 * @param <T> The type of attribute to get the value of
	 * @param attr The attribute to get the value of
	 * @return The value of the attribute set directly in this style, or null if it is not set
	 */
	<T> T getLocal(StyleAttribute<T> attr);

	/** @return An iterable for attributes set locally in this style */
	Iterable<StyleAttribute<?>> localAttributes();

	/**
	 * Gets the value of the attribute in this style or its dependencies. This style is checked first, then dependencies are checked. If the
	 * attribute is not set in this style or its dependencies, then the attribute's default value is returned.
	 *
	 * @param <T> The type of attribute to get the value of
	 * @param attr The attribute to get the value of
	 * @return The value of the attribute in this style's scope
	 */
	<T> T get(StyleAttribute<T> attr);

	/** @param listener The listener to be notified when attribute data in this style changes */
	void addListener(StyleListener listener);

	/** @param listener The listener to remove from notification */
	void removeListener(StyleListener listener);
}
