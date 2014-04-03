package org.muis.core.event;

import org.muis.core.event.boole.TypedPredicate;

public class FocusEventCondition extends UserEventCondition<FocusEvent> {
	public static TypedPredicate<MuisEvent, FocusEvent> base = new TypedPredicate<MuisEvent, FocusEvent>() {
		@Override
		public FocusEvent cast(MuisEvent value) {
			return value instanceof FocusEvent ? (FocusEvent) value : null;
		}
	};

	public static class FocusPredicate implements TypedPredicate<FocusEvent, FocusEvent> {
		private final boolean isFocus;

		private FocusPredicate(boolean focus) {
			isFocus = focus;
		}

		@Override
		public FocusEvent cast(FocusEvent value) {
			return value.isFocus() == isFocus ? value : null;
		}
	}

	public static final FocusPredicate predicateFocus = new FocusPredicate(true);
	public static final FocusPredicate predicateBlur = new FocusPredicate(false);

	/** Filters focus events that have not been {@link UserEvent#use() used} */
	public static final FocusEventCondition focusEvent = new FocusEventCondition(null);

	/** Filters focus gained events that have not been {@link UserEvent#use() used} */
	public static final FocusEventCondition focus = new FocusEventCondition(true);

	/** Filters focus lost events that have not been {@link UserEvent#use() used} */
	public static final FocusEventCondition blur = new FocusEventCondition(false);

	private final Boolean isFocus;

	private FocusEventCondition(Boolean focus) {
		isFocus = focus;
	}

	@Override
	public TypedPredicate<MuisEvent, FocusEvent> getTester() {
		TypedPredicate<MuisEvent, FocusEvent> focusTester = base;
		if(isFocus != null)
			focusTester = new org.muis.core.event.boole.TPAnd<>(focusTester, isFocus ? predicateFocus : predicateBlur);
		return new org.muis.core.event.boole.TPAnd<>(getUserTester(), focusTester);
	}

	@Override
	public FocusEventCondition withUsed() {
		return (FocusEventCondition) super.withUsed();
	}

	@Override
	protected FocusEventCondition clone() {
		return (FocusEventCondition) super.clone();
	}
}
