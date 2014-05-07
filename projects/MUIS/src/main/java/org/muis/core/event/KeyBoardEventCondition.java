package org.muis.core.event;

import org.muis.core.event.boole.TPAnd;
import org.muis.core.event.boole.TypedPredicate;

/** Allows advanced filtering on {@link KeyBoardEvent}s */
public class KeyBoardEventCondition extends UserEventCondition<KeyBoardEvent> {
	/** Filters all {@link KeyBoardEvent}s */
	@SuppressWarnings("hiding")
	public static TypedPredicate<MuisEvent, KeyBoardEvent> base = value -> {
		return value instanceof KeyBoardEvent ? (KeyBoardEvent) value : null;
	};

	/** Filters {@link KeyBoardEvent}s based on their {@link KeyBoardEvent#wasPressed() pressed} attribute */
	public static class PressPredicate implements TypedPredicate<KeyBoardEvent, KeyBoardEvent> {
		private final boolean isPress;

		private PressPredicate(boolean press) {
			isPress = press;
		}

		@Override
		public KeyBoardEvent cast(KeyBoardEvent value) {
			return value.wasPressed() == isPress ? value : null;
		}
	}

	/** Filters key pressed events */
	public static final PressPredicate predicatePress = new PressPredicate(true);
	/** Filters key released events */
	public static final PressPredicate predicateRelease = new PressPredicate(false);

	/** Filters key events that have not been {@link UserEvent#use() used} */
	public static final KeyBoardEventCondition key = new KeyBoardEventCondition(null);

	private Boolean isPress;

	private KeyBoardEventCondition(Boolean press) {
		isPress = press;
	}

	@Override
	public TypedPredicate<MuisEvent, KeyBoardEvent> getTester() {
		TypedPredicate<MuisEvent, KeyBoardEvent> kbeTester = base;
		if(isPress != null)
			kbeTester = new TPAnd<>(kbeTester, isPress ? predicatePress : predicateRelease);
		return new TPAnd<>(getUserTester(), kbeTester);
	}

	@Override
	public KeyBoardEventCondition withUsed() {
		return (KeyBoardEventCondition) super.withUsed();
	}

	@Override
	protected KeyBoardEventCondition clone() {
		return (KeyBoardEventCondition) super.clone();
	}

	/** @return A new condition the same as this one that accepts key pressed events */
	public KeyBoardEventCondition press() {
		if(Boolean.TRUE.equals(isPress))
			return this;
		KeyBoardEventCondition ret = clone();
		ret.isPress = true;
		return ret;
	}

	/** @return A new condition the same as this one that accepts key released events */
	public KeyBoardEventCondition release() {
		if(Boolean.FALSE.equals(isPress))
			return this;
		KeyBoardEventCondition ret = clone();
		ret.isPress = false;
		return ret;
	}
}
