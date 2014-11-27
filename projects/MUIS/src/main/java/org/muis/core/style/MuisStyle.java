package org.muis.core.style;

import java.util.Set;

import org.muis.core.event.MuisEvent;
import org.muis.core.rx.*;

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

	/**
	 * @param <T> The type of the event to map
	 * @param attr The style attribute to map the event for
	 * @param event The observable event representing a change to the attribute in this style
	 * @return The style attribute event to fire for the change
	 */
	default <T> StyleAttributeEvent<T> mapEvent(StyleAttribute<T> attr, ObservableValueEvent<T> event) {
		MuisStyle root;
		if(event instanceof StyleAttributeEvent)
			root = ((StyleAttributeEvent<T>) event).getRootStyle();
		else if(event.getCause() instanceof StyleAttributeEvent)
			root = ((StyleAttributeEvent<T>) event.getCause()).getRootStyle();
		else
			root = this;
		MuisEvent cause = null;
		if(event instanceof MuisEvent)
			cause = (MuisEvent) event;
		else if(event.getCause() instanceof MuisEvent)
			cause = (MuisEvent) event.getCause();
		return new StyleAttributeEvent<>(null, root, this, attr, event.getOldValue(), event.getValue(), cause);
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
		DefaultObservableSet<ObservableSet<StyleAttribute<?>>> ret = new DefaultObservableSet<>(new Type(ObservableSet.class,
			new Type(StyleAttribute.class)));
		Set<ObservableSet<StyleAttribute<?>>> controller = ret.control(null);
		controller.add(localAttributes());
		controller.add(ObservableSet.flatten(getDependencies().mapC(depend -> depend.attributes())));
		return new org.muis.util.ObservableSetWrapper<StyleAttribute<?>>(ObservableSet.flatten(ret)) {
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
			getDependencies().mapC(depend -> depend.get(attr, false))).find(attr.getType().getType(), val -> val);
		return new org.muis.util.ObservableValueWrapper<T>(getLocal(attr).combineV(null, (T local, T depend) -> {
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

	/** @return An observable that fires a {@link StyleAttributeEvent} for every attribute whose value is cleared from this style locally */
	default Observable<StyleAttributeEvent<?>> localRemoves() {
		return localAttributes().removes().map(event -> {
			MuisStyle root;
			StyleAttribute<Object> attr;
			Object oldVal;
			if(event.getCause() instanceof StyleAttributeEvent) {
				StyleAttributeEvent<Object> sae = (StyleAttributeEvent<Object>) event.getCause();
				root = sae.getRootStyle();
				attr = sae.getAttribute();
				oldVal = sae.getOldValue();
			} else
				throw new IllegalStateException("Cannot convert to StyleAttributeEvent: " + event);
			root = this;
			MuisEvent cause = null;
			if(event instanceof MuisEvent)
				cause = (MuisEvent) event;
			else if(event.getCause() instanceof MuisEvent)
				cause = (MuisEvent) event.getCause();
			return new StyleAttributeEvent<>(null, root, this, attr, oldVal, get(attr).get(), cause);
		});
	}

	/** @return An observable that fires a {@link StyleAttributeEvent} for every change affecting attribute values in this style */
	default Observable<StyleAttributeEvent<?>> allChanges() {
		 //Work-around for a ridiculous build error in eclipse
		@SuppressWarnings("cast")
		Observable<StyleAttributeEvent<?>> localChanges = ObservableCollection.fold(
			localAttributes().mapC(attr -> (Observable<?>) get(attr).skip(1))).map(event -> (StyleAttributeEvent<?>) event);
		Observable<StyleAttributeEvent<?>> depends = ObservableCollection
			.fold(getDependencies().mapC(dep -> dep.allChanges()))
			// Don't propagate dependency changes that are overridden in this style
			.filter(event -> get(event.getAttribute(), false) == null)
			.map(
				event -> {
					return new StyleAttributeEvent<>(null, event.getRootStyle(), this, (StyleAttribute<Object>) event.getAttribute(), event
						.getOldValue(), event.getValue(), event);
				});
		return new org.muis.util.ObservableWrapper<StyleAttributeEvent<?>>(Observable.or(localChanges, localRemoves(), depends)) {
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
		return new org.muis.util.ObservableWrapper<StyleAttributeEvent<?>>(ObservableCollection.fold(
			ObservableSet.constant(new Type(StyleAttribute.class, new Type(Object.class, true)), attrs).mapC(attr -> get(attr))).map(
			event -> (StyleAttributeEvent<?>) event)) {
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
