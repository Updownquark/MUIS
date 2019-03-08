package org.quick.widget.core.event;

/** Allows advanced filtering on {@link KeyBoardEvent}s */
public class KeyBoardEventCondition extends UserEventCondition<KeyBoardEvent> {
	/** Filters all {@link KeyBoardEvent}s */
	@SuppressWarnings("hiding")
	public static java.util.function.Function<QuickWidgetEvent, KeyBoardEvent> base = value -> {
		return value instanceof KeyBoardEvent ? (KeyBoardEvent) value : null;
	};

	/** Filters key events that have not been {@link UserEvent#use() used} */
	public static final KeyBoardEventCondition key = new KeyBoardEventCondition(null);

	private Boolean isPress;

	private KeyBoardEventCondition(Boolean press) {
		isPress = press;
	}

	@Override
	public KeyBoardEvent apply(QuickWidgetEvent evt) {
		KeyBoardEvent kbEvt = base.apply(userApply(evt));
		if(kbEvt == null)
			return null;
		if(isPress != null && kbEvt.wasPressed() != isPress.booleanValue())
			return null;
		return kbEvt;
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
