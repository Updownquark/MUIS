package org.quick.core.event;


import org.observe.ObservableValue;
import org.observe.ObservableValueEvent;
import org.quick.core.QuickElement;
import org.quick.core.event.boole.TypedPredicate;

/**
 * Represents an event that occurs when a property changes value.
 *
 * @param <T> The type of the property that was changed
 */
public abstract class QuickPropertyEvent<T> extends ObservableValueEvent<T> implements QuickEvent {
	/** Filters for property events */
	public static final TypedPredicate<QuickEvent, QuickPropertyEvent<?>> base = value -> {
		return value instanceof QuickPropertyEvent && !((QuickPropertyEvent<?>) value).isOverridden() ? (QuickPropertyEvent<?>) value : null;
	};

	private final QuickElement theElement;

	/**
	 * @param element The element that this event is being fired in
	 * @param observable The observable that is firing this event
	 * @param initial Whether this represents the population of the initial value of an observable value in response to subscription
	 * @param oldValue The value of the property before it was changed
	 * @param newValue The current value of the property after it has been changed
	 * @param cause The cause of this event
	 */
	public QuickPropertyEvent(QuickElement element, ObservableValue<T> observable, boolean initial, T oldValue, T newValue, QuickEvent cause) {
		super(observable, initial, oldValue, newValue, cause);
		theElement = element;
	}

	@Override
	public QuickElement getElement() {
		return theElement;
	}

	@Override
	public QuickEvent getCause() {
		return (QuickEvent) super.getCause();
	}

	/**
	 * A property event is overridden when the value of the property on the same property center is changed again while the event from the
	 * old change is still being fired. Overridden property events should stop being propagated to listeners because listeners using the
	 * {@link #getValue() value} on the event will be using outdated information.
	 *
	 * @return Whether this event is overridden.
	 */
	@Override
	public abstract boolean isOverridden();
}
