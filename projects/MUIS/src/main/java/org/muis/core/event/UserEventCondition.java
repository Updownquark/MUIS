package org.muis.core.event;

import org.muis.core.event.boole.TPAnd;
import org.muis.core.event.boole.TypedPredicate;

public class UserEventCondition<E extends UserEvent> implements MuisEventCondition<E>, Cloneable {
	public static TypedPredicate<MuisEvent, UserEvent> base = new TypedPredicate<MuisEvent, UserEvent>() {
		@Override
		public UserEvent cast(MuisEvent value) {
			return value instanceof UserEvent ? (UserEvent) value : null;
		}
	};

	public static TypedPredicate<UserEvent, UserEvent> notWithUsed = new TypedPredicate<UserEvent, UserEvent>() {
		@Override
		public UserEvent cast(UserEvent value) {
			return value.isUsed() ? null : value;
		}
	};

	public static final UserEventCondition<UserEvent> user = new UserEventCondition();

	private boolean isWithUsed;

	protected UserEventCondition() {
		isWithUsed = false;
	}

	protected TypedPredicate<MuisEvent, UserEvent> getUserTester() {
		TypedPredicate<MuisEvent, UserEvent> ret = base;
		if(!isWithUsed)
			ret = new TPAnd<>(ret, notWithUsed);
		return ret;
	}

	/* Should be overridden by subclass */
	@Override
	public TypedPredicate<MuisEvent, E> getTester() {
		return (TypedPredicate<MuisEvent, E>) getUserTester();
	}

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