package org.quick.core.event;


/**
 * Allows advanced filtering on {@link PositionedUserEvent}s
 *
 * @param <E> The sub-type of event being filtered
 */
public class PositionedUserEventCondition<E extends PositionedUserEvent> extends UserEventCondition<E> {
	/** Filters all {@link PositionedUserEvent}s */
	@SuppressWarnings("hiding")
	protected static java.util.function.Function<QuickEvent, PositionedUserEvent> base = value -> {
		return value instanceof PositionedUserEvent ? (PositionedUserEvent) value : null;
	};

	/** Filters all {@link UserEvent#isUsed() unused} {@link PositionedUserEvent}s */
	public static final PositionedUserEventCondition<PositionedUserEvent> positioned = new PositionedUserEventCondition<>();

	/** Constructor for subclasses */
	protected PositionedUserEventCondition() {
	}

	/**
	 * @param event The event to filter
	 * @return The event if it meets the positioned part of this condition; null otherwise
	 */
	protected PositionedUserEvent positionedApply(QuickEvent event) {
		return base.apply(userApply(event));
	}

	/* Should be overridden by subclass */
	@Override
	public E apply(QuickEvent evt) {
		return (E) positionedApply(evt);
	}

	@Override
	protected PositionedUserEventCondition<E> clone() {
		return (PositionedUserEventCondition<E>) super.clone();
	}

	@Override
	public PositionedUserEventCondition<E> withUsed() {
		return (PositionedUserEventCondition<E>) super.withUsed();
	}
}
