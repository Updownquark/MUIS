package org.muis.core.event;

import org.muis.core.event.boole.TPAnd;
import org.muis.core.event.boole.TypedPredicate;

public class PositionedUserEventCondition<E extends PositionedUserEvent> extends UserEventCondition<E> {
	protected static TypedPredicate<MuisEvent, PositionedUserEvent> base = new TypedPredicate<MuisEvent, PositionedUserEvent>() {
		@Override
		public PositionedUserEvent cast(MuisEvent value) {
			return value instanceof PositionedUserEvent ? (PositionedUserEvent) value : null;
		}
	};

	public static final PositionedUserEventCondition<PositionedUserEvent> positioned = new PositionedUserEventCondition<>();

	protected PositionedUserEventCondition() {
	}

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
