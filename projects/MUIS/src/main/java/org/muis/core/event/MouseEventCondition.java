package org.muis.core.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.muis.core.event.MouseEvent.ButtonType;
import org.muis.core.event.MouseEvent.MouseEventType;
import org.muis.core.event.boole.TPAnd;
import org.muis.core.event.boole.TPOr;
import org.muis.core.event.boole.TypedPredicate;

/** Allows advanced filtering on {@link MouseEvent}s */
public class MouseEventCondition extends PositionedUserEventCondition<MouseEvent> {
	/** Filters all {@link MouseEvent}s */
	@SuppressWarnings("hiding")
	public static TypedPredicate<MuisEvent, MouseEvent> base = new TypedPredicate<MuisEvent, MouseEvent>() {
		@Override
		public MouseEvent cast(MuisEvent value) {
			return value instanceof MouseEvent ? (MouseEvent) value : null;
		}
	};

	/** Filters mouse events that have not been {@link UserEvent#use() used} */
	public static final MouseEventCondition mouse = new MouseEventCondition();

	/** Filters {@link MouseEvent}s based on their {@link MouseEvent#getType() type} attribute */
	public static class MouseEventTypedPredicate implements TypedPredicate<MouseEvent, MouseEvent> {
		private final MouseEventType theType;

		private MouseEventTypedPredicate(MouseEventType type) {
			theType = type;
		}

		/** @return The mouse event type that this predicate filters */
		public MouseEventType getType() {
			return theType;
		}

		@Override
		public MouseEvent cast(MouseEvent value) {
			return value.getType() == theType ? value : null;
		}
	}

	/** Filters {@link MouseEvent}s based on their {@link MouseEvent#getButton() buttonType} attribute */
	public static class ButtonTypedPredicate implements TypedPredicate<MouseEvent, MouseEvent> {
		private final ButtonType theButton;

		private ButtonTypedPredicate(ButtonType type) {
			theButton = type;
		}

		/** @return The mouse button that this predicate filters */
		public ButtonType getButton() {
			return theButton;
		}

		@Override
		public MouseEvent cast(MouseEvent value) {
			return value.getButton() == theButton ? value : null;
		}
	}

	/** Filters {@link MouseEvent}s based on their {@link MouseEvent#getClickCount() clickCount} attribute */
	public static class ClickCountPredicate implements TypedPredicate<MouseEvent, MouseEvent> {
		private final int theClickCount;

		/** @param count The click count to filter on */
		public ClickCountPredicate(int count) {
			theClickCount=count;
		}

		/** @return The click count that this predicate filters */
		public int getClickCount(){
			return theClickCount;
		}

		@Override
		public MouseEvent cast(MouseEvent value) {
			return value.getClickCount()==theClickCount ? value : null;
		}
	}

	/** A map of all mouse event types to predicates that filter on them */
	public static final Map<MouseEventType, MouseEventTypedPredicate> types;
	/** A map of all mouse buttons to predicates that filter on them */
	public static final Map<ButtonType, ButtonTypedPredicate> buttons;
	/** Fitlers single clicks */
	public static final ClickCountPredicate singleClick = new ClickCountPredicate(1);
	/** Fitlers double clicks */
	public static final ClickCountPredicate doubleClick = new ClickCountPredicate(2);

	static {
		Map<MouseEventType, MouseEventTypedPredicate> t = new java.util.LinkedHashMap<>();
		for(MouseEventType type : MouseEventType.values())
			t.put(type, new MouseEventTypedPredicate(type));
		types = Collections.unmodifiableMap(t);

		Map<ButtonType, ButtonTypedPredicate> b = new java.util.LinkedHashMap<>();
		for(ButtonType type : ButtonType.values())
			b.put(type, new ButtonTypedPredicate(type));
		buttons = Collections.unmodifiableMap(b);
	}

	/**
	 * @param eventTypes The mouse event types to filter
	 * @return A condition that filters mouse events of any of the given types
	 */
	public static TPOr<MouseEvent> or(MouseEventType... eventTypes) {
		List<MouseEventTypedPredicate> preds = new ArrayList<>();
		for(MouseEventType type : eventTypes)
			preds.add(types.get(type));
		return new TPOr<MouseEvent>(preds);
	}

	/**
	 * @param eventTypes The mouse event types to filter
	 * @return A condition that filters mouse events of any of the given types
	 */
	public static TPOr<MouseEvent> orTypes(Iterable<MouseEventType> eventTypes) {
		List<MouseEventTypedPredicate> preds = new ArrayList<>();
		for(MouseEventType type : eventTypes)
			preds.add(types.get(type));
		return new TPOr<MouseEvent>(preds);
	}

	/**
	 * @param buttonTypes The mouse buttons to filter
	 * @return A condition that filters mouse events for any of the given buttons
	 */
	public static TPOr<MouseEvent> or(ButtonType... buttonTypes) {
		List<ButtonTypedPredicate> preds = new ArrayList<>();
		for(ButtonType type : buttonTypes)
			preds.add(buttons.get(type));
		return new TPOr<MouseEvent>(preds);
	}

	/**
	 * @param buttonTypes The mouse buttons to filter
	 * @return A condition that filters mouse events for any of the given buttons
	 */
	public static TPOr<MouseEvent> orButtons(Iterable<ButtonType> buttonTypes) {
		List<ButtonTypedPredicate> preds = new ArrayList<>();
		for(ButtonType type : buttonTypes)
			preds.add(buttons.get(type));
		return new TPOr<MouseEvent>(preds);
	}

	private List<MouseEventType> theTypes;
	private List<ButtonType> theButtons;
	private Integer theClickCount;

	private MouseEventCondition() {
		theTypes = null;
		theButtons = null;
		theClickCount = null;
	}

	@Override
	public TypedPredicate<MuisEvent, MouseEvent> getTester() {
		TypedPredicate<MuisEvent, MouseEvent> mousePred = base;
		if(theTypes != null) {
			if(theTypes.size() == 1)
				mousePred = new TPAnd<>(base, types.get(theTypes.get(0)));
			else
				mousePred = new TPAnd<>(base, orTypes(theTypes));
		}
		if(theButtons != null) {
			if(theButtons.size() == 1)
				mousePred = new TPAnd<>(base, buttons.get(theButtons.get(0)));
			else
				mousePred = new TPAnd<>(base, orButtons(theButtons));
		}
		if(theClickCount != null) {
			switch (theClickCount) {
			case 1:
				mousePred = new TPAnd<>(base, singleClick);
				break;
			case 2:
				mousePred = new TPAnd<>(base, doubleClick);
				break;
			default:
				mousePred = new TPAnd<>(base, new ClickCountPredicate(theClickCount));
				break;
			}
		}
		return new TPAnd<>(getPositionedTester(), mousePred);
	}

	@Override
	protected MouseEventCondition clone() {
		MouseEventCondition ret = (MouseEventCondition) super.clone();
		if(ret.theTypes != null)
			ret.theTypes = new ArrayList<>(ret.theTypes);
		return ret;
	}

	/**
	 * @param eventTypes The event types to add
	 * @return A condition like this one that accepts {@link MouseEvent}s of the given types
	 */
	public MouseEventCondition addTypes(MouseEventType... eventTypes) {
		if(theTypes != null) {
			boolean hasAll = true;
			for(MouseEventType type : eventTypes)
				if(!theTypes.contains(type)) {
					hasAll = false;
					break;
				}
			if(hasAll)
				return this;
		}
		MouseEventCondition ret = clone();
		if(ret.theTypes == null)
			ret.theTypes = new ArrayList<>();
		for(MouseEventType type : eventTypes)
			if(!ret.theTypes.contains(type))
				ret.theTypes.add(type);
		return ret;
	}

	/**
	 * @param buttonTypes The buttons to add
	 * @return A condition like this one that accepts {@link MouseEvent}s for the given buttons
	 */
	public MouseEventCondition addButtons(ButtonType... buttonTypes) {
		if(theButtons != null) {
			boolean hasAll = true;
			for(ButtonType type : buttonTypes)
				if(!theButtons.contains(type)) {
					hasAll = false;
					break;
				}
			if(hasAll)
				return this;
		}
		MouseEventCondition ret = clone();
		if(ret.theButtons == null)
			ret.theButtons = new ArrayList<>();
		for(ButtonType type : buttonTypes)
			if(!ret.theButtons.contains(type))
				ret.theButtons.add(type);
		return ret;
	}

	/**
	 * @param count The click count
	 * @return A condition like this on that accepts {@link MouseEvent}s with given click count
	 */
	public MouseEventCondition cc(int count) {
		if(count <= 0)
			throw new IllegalArgumentException("Click count must be at least 1");
		if(theClickCount != null && theClickCount != count)
			return this;
		MouseEventCondition ret = clone();
		ret.theClickCount = count;
		return ret;
	}

	/** @return A condition like this one that accepts {@link MouseEventType#clicked} events */
	public MouseEventCondition click() {
		return addTypes(MouseEventType.clicked);
	}

	/** @return A condition like this one that accepts {@link MouseEventType#pressed} events */
	public MouseEventCondition down() {
		return addTypes(MouseEventType.pressed);
	}

	/** @return A condition like this one that accepts {@link MouseEventType#released} events */
	public MouseEventCondition up() {
		return addTypes(MouseEventType.released);
	}

	/** @return A condition like this one that accepts {@link MouseEventType#moved} events */
	public MouseEventCondition move() {
		return addTypes(MouseEventType.moved);
	}

	/** @return A condition like this one that accepts {@link MouseEventType#entered} events */
	public MouseEventCondition enter() {
		return addTypes(MouseEventType.entered);
	}

	/** @return A condition like this one that accepts {@link MouseEventType#exited} events */
	public MouseEventCondition exit() {
		return addTypes(MouseEventType.exited);
	}

	/** @return A condition like this one that accepts mouse events from the {@link ButtonType#left} button */
	public MouseEventCondition left() {
		return addButtons(ButtonType.left);
	}

	/** @return A condition like this one that accepts mouse events from the {@link ButtonType#right} button */
	public MouseEventCondition right() {
		return addButtons(ButtonType.right);
	}

	/** @return A condition like this one that accepts mouse events from the {@link ButtonType#middle} button */
	public MouseEventCondition middle() {
		return addButtons(ButtonType.middle);
	}

	/** @return A condition like this one that accepts mouse events from an {@link ButtonType#other} button */
	public MouseEventCondition other() {
		return addButtons(ButtonType.other);
	}

	/**@return A condition like this one that accepts a double click */
	public MouseEventCondition dbl() {
		return cc(2);
	}

	@Override
	public MouseEventCondition withUsed() {
		return (MouseEventCondition) super.withUsed();
	}
}
