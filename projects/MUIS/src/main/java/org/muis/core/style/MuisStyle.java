package org.muis.core.style;

import org.muis.core.MuisElement;
import org.muis.core.event.MuisEvent;
import org.observe.Observable;
import org.observe.ObservableValue;
import org.observe.ObservableValueEvent;
import org.observe.collect.ObservableCollection;
import org.observe.collect.ObservableList;
import org.observe.collect.ObservableSet;
import org.observe.collect.impl.ObservableArrayList;
import org.observe.util.ObservableUtils;

import prisms.lang.Type;

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

	/** @return The element that this style belongs to. May be null. */
	default MuisElement getElement() {
		return null;
	}

	/**
	 * @param <T> The type of the event to map
	 * @param attr The style attribute to map the event for
	 * @param event The observable event representing a change to the attribute in this style
	 * @return The style attribute event to fire for the change
	 */
	default <T> StyleAttributeEvent<T> mapEvent(StyleAttribute<T> attr, ObservableValueEvent<T> event) {
		MuisStyle root;
		if(event instanceof StyleAttributeEvent) {
			root = ((StyleAttributeEvent<T>) event).getRootStyle();
			attr = ((StyleAttributeEvent<T>) event).getAttribute();
		} else if(event.getCause() instanceof StyleAttributeEvent)
			root = ((StyleAttributeEvent<T>) event.getCause()).getRootStyle();
		else
			root = this;
		if(attr == null)
			return null;
		MuisEvent cause = null;
		if(event instanceof MuisEvent)
			cause = (MuisEvent) event;
		else if(event.getCause() instanceof MuisEvent)
			cause = (MuisEvent) event.getCause();
		return new StyleAttributeEvent<>(getElement(), root, this, attr, event.getOldValue(), event.getValue(), cause);
	}

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
		ObservableArrayList<ObservableCollection<StyleAttribute<?>>> ret = new ObservableArrayList<>(new Type(
			ObservableCollection.class, new Type(StyleAttribute.class)));
		ret.add(localAttributes());
		ret.add(ObservableCollection.flatten(getDependencies().map(depend -> depend.attributes())));
		return new org.observe.util.ObservableSetWrapper<StyleAttribute<?>>(ObservableSet.unique(ObservableCollection.flatten(ret
			.immutable()))) {
			@Override
			public String toString() {
				return "All attributes for " + MuisStyle.this;
			}
		};
	}

	/**
	 * Gets the value of the attribute in this style or its dependencies. This style is checked first, then dependencies are checked. If the
	 * attribute is not set in this style or its dependencies and {@code withDefault} is true, then the attribute's default value is
	 * returned.
	 *
	 * @param <T> The type of attribute to get the value of
	 * @param attr The attribute to get the value of
	 * @param withDefault Whether to return the default value if no value is set for the attribute in this style or its dependencies
	 * @return The observable value of the attribute in this style's scope
	 */
	default <T> ObservableValue<T> get(StyleAttribute<T> attr, boolean withDefault) {
		ObservableValue<T> dependValue = ObservableUtils.flattenListValues(attr.getType().getType(),
			getDependencies().map(depend -> depend.get(attr, false))).getFirst();
		return new org.observe.util.ObservableValueWrapper<T>(getLocal(attr).combineV(null, (T local, T depend) -> {
			if(local != null)
				return local;
			else if(depend != null)
				return depend;
			else if(withDefault)
				return attr.getDefault();
			else
				return null;
		}, dependValue, true).mapEvent(event -> mapEvent(attr, event))) {
			@Override
			public String toString() {
				return MuisStyle.this + ".get(" + attr + ")";
			}
		};
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
	default <T> ObservableValue<T> get(StyleAttribute<T> attr) {
		return get(attr, true);
	}

	/** @return An observable that fires a {@link StyleAttributeEvent} for every local attribute change in this style */
	default Observable<StyleAttributeEvent<?>> localChanges() {
		Observable<StyleAttributeEvent<?>> localChanges = ObservableCollection.fold(localAttributes().map(attr -> getLocal(attr)))
			.noInit().map(event -> (StyleAttributeEvent<?>) event);
		return localChanges;
	}

	/** @return An observable that fires a {@link StyleAttributeEvent} for every attribute whose value is cleared from this style locally */
	default Observable<StyleAttributeEvent<?>> localRemoves() {
		return localAttributes().removes().map(event -> mapEvent(null, event));
	}

	/** @return An observable that fires a {@link StyleAttributeEvent} for every change affecting attribute values in this style */
	default Observable<StyleAttributeEvent<?>> allChanges() {
		Observable<StyleAttributeEvent<?>> localChanges = localChanges();
		Observable<StyleAttributeEvent<?>> depends = ObservableCollection
			.fold(getDependencies().map(dep -> dep.allChanges()))
			// Don't propagate dependency changes that are overridden in this style
			.filter(event -> !isSet(event.getAttribute()))
			.map(
				event -> {
					return new StyleAttributeEvent<>(null, event.getRootStyle(), this, (StyleAttribute<Object>) event.getAttribute(), event
						.getOldValue(), event.getValue(), event);
				});
		return new org.observe.util.ObservableWrapper<StyleAttributeEvent<?>>(Observable.or(localChanges, localRemoves(), depends)) {
			@Override
			public String toString() {
				return "All changes in " + MuisStyle.this;
			}
		};
	}

	/**
	 * @param attrs The attributes to watch
	 * @return An observable that fires a {@link StyleAttributeEvent} for every change affecting the given attribute values in this style
	 */
	default Observable<StyleAttributeEvent<?>> watch(StyleAttribute<?>... attrs) {
		return new org.observe.util.ObservableWrapper<StyleAttributeEvent<?>>(ObservableCollection
			.fold(ObservableSet.constant(new Type(StyleAttribute.class, new Type(Object.class, true)), attrs).map(attr -> get(attr)))
			.noInit().map(event -> (StyleAttributeEvent<?>) event)) {
			@Override
			public String toString() {
				StringBuilder ret = new StringBuilder();
				ret.append(MuisStyle.this).append(".watch(");
				for(int i = 0; i < attrs.length; i++) {
					if(i < attrs.length - 1)
						ret.append(", ");
					ret.append(attrs[i]);
				}
				ret.append(')');
				return ret.toString();
			}
		};
	}
}
