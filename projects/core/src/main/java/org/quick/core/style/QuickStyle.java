package org.quick.core.style;

import org.observe.ObservableValue;
import org.observe.collect.ObservableCollection;
import org.observe.collect.ObservableSet;
import org.quick.core.mgr.QuickState;

import com.google.common.reflect.TypeToken;

/** Defines style attribute values */
public interface QuickStyle {
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
	<T> ObservableValue<T> get(StyleAttribute<T> attr, boolean withDefault);

	/**
	 * Short-hand for {@link #get(StyleAttribute, boolean) get}(attr, true)
	 *
	 * @param <T> The type of the attribute
	 * @param attr The attribute to get the value of
	 * @return The observable value of the attribute in this style's scope
	 */
	default <T> ObservableValue<T> get(StyleAttribute<T> attr) {
		return get(attr, true);
	}

	/**
	 * @param extraStates The extra states to create a new style for
	 * @return A style that reflects what this style would look like if the given extra states were used in evaluating any conditional
	 *         values
	 */
	QuickStyle forExtraStates(ObservableCollection<QuickState> extraStates);

	/**
	 * @param state The extra state to create a new style for
	 * @return A style that reflects what this style would look like if the given extra state was used in evaluating any conditional values
	 */
	default QuickStyle forExtraState(QuickState state){
		return forExtraStates(ObservableCollection.constant(TypeToken.of(QuickState.class), state));
	}

	/**
	 * @param extraGroups The extra groups to create a new style for
	 * @return A style that reflects what this style would look like if the given extra groups were used in evaluating any conditional
	 *         values
	 */
	QuickStyle forExtraGroups(ObservableCollection<String> extraGroups);
}
