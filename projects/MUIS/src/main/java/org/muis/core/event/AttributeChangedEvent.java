package org.muis.core.event;

import java.util.function.Function;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisElement;
import org.muis.core.event.boole.TypedPredicate;
import org.muis.core.mgr.AttributeManager.AttributeHolder;

/**
 * Fired when a new value is set on an attribute
 *
 * @param <T> The type of the attribute
 */
public abstract class AttributeChangedEvent<T> extends MuisPropertyEvent<T> {
	/** Filters events of this type */
	@SuppressWarnings("hiding")
	public static final Function<MuisEvent, AttributeChangedEvent<?>> base = value -> {
		return value instanceof AttributeChangedEvent && !((AttributeChangedEvent<?>) value).isOverridden() ? (AttributeChangedEvent<?>) value
			: null;
	};

	/**
	 * A filter for attribute change events on a particular attribute
	 *
	 * @param <T> The type of the attribute
	 */
	public static class AttributeTypedPredicate<T> implements TypedPredicate<AttributeChangedEvent<?>, AttributeChangedEvent<T>> {
		private final MuisAttribute<T> theAttribute;

		private AttributeTypedPredicate(MuisAttribute<T> att) {
			theAttribute = att;
		}

		/** @return The attribute that this filter accepts events for */
		public MuisAttribute<T> getAttribute() {
			return theAttribute;
		}

		@Override
		public AttributeChangedEvent<T> cast(AttributeChangedEvent<?> value) {
			if(value.getAttribute() == theAttribute)
				return (AttributeChangedEvent<T>) value;
			else
				return null;
		}
	}

	/**
	 * @param <T> The type of the attribute
	 * @param attr The attribute to listen for
	 * @return A filter for change events to the given attribute
	 */
	public static <T> Function<MuisEvent, AttributeChangedEvent<T>> att(MuisAttribute<T> attr) {
		return event -> {
			AttributeChangedEvent<?> attEvt = base.apply(event);
			if(attEvt == null)
				return null;
			if(attEvt.getAttribute() != attr)
				return null;
			return (AttributeChangedEvent<T>) attEvt;
		};
	}

	private final MuisAttribute<T> theAttr;

	/**
	 * @param element The element whose attribute changed
	 * @param holder The attribute holder firing this event
	 * @param attr The attribute whose value changed
	 * @param oldValue The attribute's value before it was changed
	 * @param newValue The attribute's value after it was changed
	 * @param cause The cause of this event
	 */
	public AttributeChangedEvent(MuisElement element, AttributeHolder<T> holder, MuisAttribute<T> attr, T oldValue, T newValue,
		MuisEvent cause) {
		super(element, holder, oldValue, newValue, cause);
		theAttr = attr;
	}

	@Override
	public AttributeHolder<T> getObservable() {
		return (AttributeHolder<T>) super.getObservable();
	}

	/** @return The attribute whose value changed */
	public MuisAttribute<T> getAttribute() {
		return theAttr;
	}
}
