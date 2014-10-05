package org.muis.core.style;

import org.muis.core.rx.ObservableCollection;
import org.muis.core.rx.ObservableList;
import org.muis.core.rx.ObservableSet;
import org.muis.core.rx.ObservableValue;

/** Governs the set of properties that define how MUIS elements of different types render themselves */
public interface MuisStyle {
	/** @return The styles, in order, that this style depends on for attributes not set directly in this style */
	ObservableList<MuisStyle> getDependencies();

	/**
	 * @param attr The attribute to check
	 * @return Whether the attribute is set directly in this style
	 */
	boolean isSet(StyleAttribute<?> attr);

	/**
	 * @param <T> The type of attribute to get the value of
	 * @param attr The attribute to get the value of
	 * @return The value of the attribute set directly in this style, or null if it is not set
	 */
	<T> ObservableValue<T> getLocal(StyleAttribute<T> attr);

	/** @return Attributes set locally in this style */
	ObservableSet<StyleAttribute<?>> localAttributes();

	/**
	 * @param attr The attribute to check
	 * @return Whether the attribute is set in this style or one of its ancestors
	 */
	default boolean isSetDeep(StyleAttribute<?> attr) {
		if(isSet(attr))
			return true;
		for(MuisStyle depend : getDependencies())
			if(depend.isSetDeep(attr))
				return true;
		return false;
	}

	/** @return Attributes set in this style or any of its dependencies */
	default ObservableSet<StyleAttribute<?>> attributes() {
		org.muis.core.rx.CompoundObservableSet<StyleAttribute<?>> ret = new org.muis.core.rx.CompoundObservableSet<>();
		ret.addSet(localAttributes());
		ret.addSet(ObservableCollection.flatten(getDependencies().mapC(depend -> depend.attributes())));
		return ret;
	}

	/**
	 * Gets the value of the attribute in this style or its dependencies. This style is checked first, then dependencies are checked. If the
	 * attribute is not set in this style or its dependencies, then the attribute's default value is returned.
	 *
	 * @param <T> The type of attribute to get the value of
	 * @param attr The attribute to get the value of
	 * @param withDefault Whether to return the default value if no value is set for the attribute in this style or its dependencies
	 * @return The observable value of the attribute in this style's scope
	 */
	default <T> ObservableValue<T> get(StyleAttribute<T> attr, boolean withDefault) {
		ObservableValue<T> dependValue = org.muis.core.rx.ObservableUtils.first(attr.getType().getType(),
			getDependencies().mapC(depend -> depend.get(attr, false)), (T val) -> val);
		return getLocal(attr).composeV((T local, T depend) -> {
			if(local != null)
				return local;
			else if(depend != null)
				return depend;
			else if(withDefault)
				return attr.getDefault();
			else
				return null;
		}, dependValue);
	}

	default org.muis.core.rx.Observable<StyleAttributeEvent<?>> allChanges(){
		return attributes().mapC(attr->get(attr, true))
	}
}
