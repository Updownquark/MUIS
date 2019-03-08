package org.quick.widget.core.event;

import java.util.function.Function;

/**
 * Allows advanced filtering on {@link UserEvent}s
 *
 * @param <E> The sub-type of events to filter
 */
public class UserEventCondition<E extends UserEvent> implements QuickEventCondition<E>, Cloneable {
	/** Filters all {@link UserEvent}s */
	public static Function<QuickWidgetEvent, UserEvent> base = value -> {
		return value instanceof UserEvent ? (UserEvent) value : null;
	};

	/** Filters {@link UserEvent#isUsed() unused} {@link UserEvent}s */
	public static Function<UserEvent, UserEvent> notWithUsed = value -> {
		return value.isUsed() ? null : value;
	};

	/** Filters {@link UserEvent#isUsed() unused} {@link UserEvent}s */
	public static final UserEventCondition<UserEvent> user = new UserEventCondition<>();

	private boolean isWithUsed;

	/** Constructor for subclasses */
	protected UserEventCondition() {
		isWithUsed = false;
	}

	/* Should be overridden by subclass */
	@Override
	public E apply(QuickWidgetEvent t) {
		return (E) userApply(t);
	}

	/**
	 * @param event The event to filter
	 * @return The event if it meets the user part of this condition; null otherwise
	 */
	protected UserEvent userApply(QuickWidgetEvent event) {
		UserEvent evt = base.apply(event);
		if(evt == null)
			return null;
		if(!isWithUsed && evt.isUsed())
			return null;
		return evt;
	}

	/** @return A filter that accepts events that are {@link UserEvent#isUsed() used} as well as unused. */
	public UserEventCondition<E> withUsed() {
		if(isWithUsed)
			return this;
		UserEventCondition<E> ret = clone();
		ret.isWithUsed = true;
		return ret;
	}

	@Override
	protected UserEventCondition<E> clone() {
		UserEventCondition<E> ret;
		try {
			ret = (UserEventCondition<E>) super.clone();
		} catch(CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
		return ret;
	}
}
