package org.muis.core.event;

import org.muis.core.event.boole.TPAnd;
import org.muis.core.event.boole.TypedPredicate;

public class KeyBoardEventCondition extends UserEventCondition<KeyBoardEvent> {
	public static TypedPredicate<MuisEvent, KeyBoardEvent> base = new TypedPredicate<MuisEvent, KeyBoardEvent>() {
		@Override
		public KeyBoardEvent cast(MuisEvent value) {
			return value instanceof KeyBoardEvent ? (KeyBoardEvent) value : null;
		}
	};

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

	public static final PressPredicate predicatePress = new PressPredicate(true);
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

	public KeyBoardEventCondition press() {
		if(Boolean.TRUE.equals(isPress))
			return this;
		KeyBoardEventCondition ret = clone();
		ret.isPress = true;
		return ret;
	}

	public KeyBoardEventCondition release() {
		if(Boolean.FALSE.equals(isPress))
			return this;
		KeyBoardEventCondition ret = clone();
		ret.isPress = false;
		return ret;
	}
}
