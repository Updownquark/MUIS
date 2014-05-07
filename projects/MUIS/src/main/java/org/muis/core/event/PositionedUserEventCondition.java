package org.muis.core.event;

import org.muis.core.event.boole.TPAnd;
import org.muis.core.event.boole.TypedPredicate;

/**
 * Allows advanced filtering on {@link PositionedUserEvent}s
 *
 * @param <E> The sub-type of event being filtered
 */
public class PositionedUserEventCondition<E extends PositionedUserEvent> extends UserEventCondition<E> {
	/** Filters all {@link PositionedUserEvent}s */
	@SuppressWarnings("hiding")
	protected static TypedPredicate<MuisEvent, PositionedUserEvent> base = value -> {
		return value instanceof PositionedUserEvent ? (PositionedUserEvent) value : null;
	};

	/** Filters all {@link UserEvent#isUsed() unused} {@link PositionedUserEvent}s */
	public static final PositionedUserEventCondition<PositionedUserEvent> positioned = new PositionedUserEventCondition<>();

	/** Constructor for subclasses */
	protected PositionedUserEventCondition() {
	}

	/** @return The superclass filter, to be AND-ed by subclasses */
	protected TypedPredicate<MuisEvent, PositionedUserEvent> getPositionedTester() {
		return new TPAnd<>(getUserTester(), base);
	}

	/* Should be overridden by subclass */
	@Override
	public TypedPredicate<MuisEvent, E> getTester() {
		return (TypedPredicate<MuisEvent, E>) getPositionedTester();
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
