package org.quick.widget.core.event;

/** Allows advanced filtering on {@link FocusEvent}s */
public class FocusEventCondition extends UserEventCondition<FocusEvent> {
	/** Filters all {@link FocusEvent}s */
	@SuppressWarnings("hiding")
	public static java.util.function.Function<QuickWidgetEvent, FocusEvent> base = value -> {
		return value instanceof FocusEvent ? (FocusEvent) value : null;
	};

	/** Filters focus events that have not been {@link UserEvent#use() used} */
	public static final FocusEventCondition focusEvent = new FocusEventCondition(null);

	/** Filters focus gained events that have not been {@link UserEvent#use() used} */
	public static final FocusEventCondition focus = new FocusEventCondition(true);

	/** Filters focus lost events that have not been {@link UserEvent#use() used} */
	public static final FocusEventCondition blur = new FocusEventCondition(false);

	private final Boolean isFocus;

	private FocusEventCondition(Boolean f) {
		isFocus = f;
	}

	@Override
	public FocusEvent apply(QuickWidgetEvent evt) {
		FocusEvent focusEvt = base.apply(userApply(evt));
		if(focusEvt == null)
			return null;
		if(isFocus != null && focusEvt.isFocus() != isFocus.booleanValue())
			return null;
		return focusEvt;
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
