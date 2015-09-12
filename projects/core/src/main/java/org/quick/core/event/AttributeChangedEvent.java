package org.quick.core.event;

import java.util.function.Function;

import org.quick.core.QuickAttribute;
import org.quick.core.QuickElement;
import org.quick.core.event.boole.TypedPredicate;
import org.quick.core.mgr.AttributeManager.AttributeHolder;

/**
 * Fired when a new value is set on an attribute
 *
 * @param <T> The type of the attribute
 */
public abstract class AttributeChangedEvent<T> extends QuickPropertyEvent<T> {
	/** Filters events of this type */
	@SuppressWarnings("hiding")
	public static final Function<QuickEvent, AttributeChangedEvent<?>> base = value -> {
		return value instanceof AttributeChangedEvent && !((AttributeChangedEvent<?>) value).isOverridden() ? (AttributeChangedEvent<?>) value
			: null;
	};

	/**
	 * A filter for attribute change events on a particular attribute
	 *
	 * @param <T> The type of the attribute
	 */
	public static class AttributeTypedPredicate<T> implements TypedPredicate<AttributeChangedEvent<?>, AttributeChangedEvent<T>> {
		private final QuickAttribute<T> theAttribute;

		private AttributeTypedPredicate(QuickAttribute<T> att) {
			theAttribute = att;
		}

		/** @return The attribute that this filter accepts events for */
		public QuickAttribute<T> getAttribute() {
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
	public static <T> Function<QuickEvent, AttributeChangedEvent<T>> att(QuickAttribute<T> attr) {
		return event -> {
			AttributeChangedEvent<?> attEvt = base.apply(event);
			if(attEvt == null)
				return null;
			if(attEvt.getAttribute() != attr)
				return null;
			return (AttributeChangedEvent<T>) attEvt;
		};
	}

	private final QuickAttribute<T> theAttr;

	/**
	 * @param element The element whose attribute changed
	 * @param holder The attribute holder firing this event
	 * @param attr The attribute whose value changed
	 * @param initial Whether this represents the population of the initial value of an observable value in response to subscription
	 * @param oldValue The attribute's value before it was changed
	 * @param newValue The attribute's value after it was changed
	 * @param cause The cause of this event
	 */
	protected AttributeChangedEvent(QuickElement element, AttributeHolder<T> holder, QuickAttribute<T> attr, boolean initial, T oldValue,
		T newValue, QuickEvent cause) {
		super(element, holder, initial, oldValue, newValue, cause);
		theAttr = attr;
	}

	@Override
	public AttributeHolder<T> getObservable() {
		return (AttributeHolder<T>) super.getObservable();
	}

	/** @return The attribute whose value changed */
	public QuickAttribute<T> getAttribute() {
		return theAttr;
	}
}
