package org.quick.core.style2;

import org.observe.ObservableValue;
import org.observe.collect.ObservableSet;
import org.quick.core.QuickElement;
import org.quick.core.style.StyleAttribute;

public interface QuickStyle {
	/** @return The element that this style belongs to */
	QuickElement getElement();

	/**
	 * @param attr The attribute to check
	 * @return Whether the attribute is set directly in this style
	 */
	boolean isSetLocal(StyleAttribute<?> attr);

	/**
	 * @param attr The attribute to check
	 * @return Whether the attribute is set in this style or one of its dependencies
	 */
	boolean isSet(StyleAttribute<?> attr);

	/** @return Attributes set locally in this style */
	ObservableSet<StyleAttribute<?>> localAttributes();

	/** @return Attributes set in this style or any of its dependencies */
	ObservableSet<StyleAttribute<?>> attributes();

	/**
	 * @param <T> The type of attribute to get the value of
	 * @param attr The attribute to get the value of
	 * @return The value of the attribute set directly in this style, or null if it is not set
	 */
	<T> ObservableValue<T> getLocal(StyleAttribute<T> attr);

	/**
	 * Gets the value of the attribute in this style
	 *
	 * @param <T> The type of attribute to get the value of
	 * @param attr The attribute to get the value of
	 * @param withDefault Whether to return the default value if no value is set for the attribute in this style or its dependencies
	 * @return The observable value of the attribute in this style's scope
	 */
	<T> ObservableValue<T> get(StyleAttribute<T> attr, boolean withDefault);

	/**
	 * Short-hand for {@link #get(StyleAttribute, boolean) get}(attr, true)
	 *
	 * @param attr The attribute to get the value of
	 * @return The observable value of the attribute in this style's scope
	 */
	default <T> ObservableValue<T> get(StyleAttribute<T> attr) {
		return get(attr, true);
	}
}
