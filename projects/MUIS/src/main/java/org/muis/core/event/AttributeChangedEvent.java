package org.muis.core.event;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisElement;
import org.muis.core.event.boole.TypedPredicate;

/**
 * Fired when a new value is set on an attribute
 *
 * @param <T> The type of the attribute
 */
public class AttributeChangedEvent<T> extends MuisPropertyEvent<T> {
	public static final TypedPredicate<MuisEvent, AttributeChangedEvent<?>> base = new TypedPredicate<MuisEvent, AttributeChangedEvent<?>>() {
		@Override
		public AttributeChangedEvent<?> cast(MuisEvent value) {
			return value instanceof AttributeChangedEvent ? (AttributeChangedEvent<?>) value : null;
		}
	};

	public static class AttributeTypedPredicate<T> implements TypedPredicate<AttributeChangedEvent<?>, AttributeChangedEvent<T>> {
		private final MuisAttribute<T> theAttribute;

		private AttributeTypedPredicate(MuisAttribute<T> att) {
			theAttribute = att;
		}

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

	public static <T> TypedPredicate<MuisEvent, AttributeChangedEvent<T>> att(MuisAttribute<T> attr) {
		return new org.muis.core.event.boole.TPAnd<>(base, new AttributeTypedPredicate<>(attr));
	}

	private final MuisAttribute<T> theAttr;

	/**
	 * @param element The element whose attribute changed
	 * @param attr The attribute whose value changed
	 * @param oldValue The attribute's value before it was changed
	 * @param newValue The attribute's value after it was changed
	 */
	public AttributeChangedEvent(MuisElement element, MuisAttribute<T> attr, T oldValue, T newValue) {
		super(element, oldValue, newValue);
		theAttr = attr;
	}

	/** @return The attribute whose value changed */
	public MuisAttribute<T> getAttribute() {
		return theAttr;
	}
}
