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
	 * @return Whether the attribute is set in this style or one of its dependencies
	 */
	boolean isSet(StyleAttribute<?> attr);

	/** @return Attributes set in this style or any of its dependencies */
	ObservableSet<StyleAttribute<?>> attributes();

	/**
	 * Gets the value of the attribute in this style
	 *
	 * @param <T> The type of attribute to get the value of
	 * @param attr The attribute to get the value of
	 * @param withDefault Whether to return the default value if no value is set for the attribute in this style or its dependencies
	 * @return The observable value of the attribute in this style's scope
	 */
	<T> T get(StyleAttribute<T> attr, boolean withDefault);

	/**
	 * Short-hand for {@link #get(StyleAttribute, boolean) get}(attr, true)
	 *
	 * @param attr The attribute to get the value of
	 * @return The observable value of the attribute in this style's scope
	 */
	default <T> T get(StyleAttribute<T> attr) {
		return get(attr, true);
	}

	/**
	 * Gets the value of the attribute in this style
	 *
	 * @param <T> The type of attribute to get the value of
	 * @param attr The attribute to get the value of
	 * @param withDefault Whether to return the default value if no value is set for the attribute in this style or its dependencies
	 * @return The observable value of the attribute in this style's scope
	 */
	<T> ObservableValue<T> observe(StyleAttribute<T> attr, boolean withDefault);
}
