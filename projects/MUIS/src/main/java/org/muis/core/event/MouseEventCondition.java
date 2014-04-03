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

public class MouseEventCondition extends PositionedUserEventCondition<MouseEvent> {
	public static TypedPredicate<MuisEvent, MouseEvent> base = new TypedPredicate<MuisEvent, MouseEvent>() {
		@Override
		public MouseEvent cast(MuisEvent value) {
			return value instanceof MouseEvent ? (MouseEvent) value : null;
		}
	};

	/** Matches all events of type {@link MouseEvent} */
	public static final MouseEventCondition mouse = new MouseEventCondition();

	public static class MouseEventTypedPredicate implements TypedPredicate<MouseEvent, MouseEvent> {
		private final MouseEventType theType;

		private MouseEventTypedPredicate(MouseEventType type) {
			theType = type;
		}

		public MouseEventType getType() {
			return theType;
		}

		@Override
		public MouseEvent cast(MouseEvent value) {
			return value.getMouseEventType() == theType ? value : null;
		}
	}

	public static class ButtonTypedPredicate implements TypedPredicate<MouseEvent, MouseEvent> {
		private final ButtonType theButton;

		private ButtonTypedPredicate(ButtonType type) {
			theButton = type;
		}

		public ButtonType getButton() {
			return theButton;
		}

		@Override
		public MouseEvent cast(MouseEvent value) {
			return value.getButtonType() == theButton ? value : null;
		}
	}

	public static final Map<MouseEventType, MouseEventTypedPredicate> types;
	public static final Map<ButtonType, ButtonTypedPredicate> buttons;

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

	public static TPOr<MouseEvent> or(MouseEventType... eventTypes) {
		List<MouseEventTypedPredicate> preds = new ArrayList<>();
		for(MouseEventType type : eventTypes)
			preds.add(types.get(type));
		return new TPOr<MouseEvent>(preds);
	}

	public static TPOr<MouseEvent> orTypes(Iterable<MouseEventType> eventTypes) {
		List<MouseEventTypedPredicate> preds = new ArrayList<>();
		for(MouseEventType type : eventTypes)
			preds.add(types.get(type));
		return new TPOr<MouseEvent>(preds);
	}

	public static TPOr<MouseEvent> or(ButtonType... buttonTypes) {
		List<ButtonTypedPredicate> preds = new ArrayList<>();
		for(ButtonType type : buttonTypes)
			preds.add(buttons.get(type));
		return new TPOr<MouseEvent>(preds);
	}

	public static TPOr<MouseEvent> orButtons(Iterable<ButtonType> buttonTypes) {
		List<ButtonTypedPredicate> preds = new ArrayList<>();
		for(ButtonType type : buttonTypes)
			preds.add(buttons.get(type));
		return new TPOr<MouseEvent>(preds);
	}

	private List<MouseEventType> theTypes;
	private List<ButtonType> theButtons;
	private int theMinClicks;

	private MouseEventCondition() {
		theTypes = null;
		theButtons = null;
		theMinClicks = 0;
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
		return new TPAnd<>(getPositionedTester(), mousePred);
	}

	@Override
	protected MouseEventCondition clone() {
		MouseEventCondition ret = (MouseEventCondition) super.clone();
		if(ret.theTypes != null)
			ret.theTypes = new ArrayList<>(ret.theTypes);
		return ret;
	}

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

	public MouseEventCondition cc(int count) {
		if(theMinClicks >= count)
			return this;
		MouseEventCondition ret = clone();
		ret.theMinClicks = count;
		return ret;
	}

	public MouseEventCondition click() {
		return addTypes(MouseEventType.clicked);
	}

	public MouseEventCondition down() {
		return addTypes(MouseEventType.pressed);
	}

	public MouseEventCondition up() {
		return addTypes(MouseEventType.released);
	}

	public MouseEventCondition move() {
		return addTypes(MouseEventType.moved);
	}

	public MouseEventCondition enter() {
		return addTypes(MouseEventType.entered);
	}

	public MouseEventCondition exit() {
		return addTypes(MouseEventType.exited);
	}

	public MouseEventCondition left() {
		return addButtons(ButtonType.LEFT);
	}

	public MouseEventCondition right() {
		return addButtons(ButtonType.RIGHT);
	}

	public MouseEventCondition middle() {
		return addButtons(ButtonType.MIDDLE);
	}

	public MouseEventCondition other() {
		return addButtons(ButtonType.OTHER);
	}

	public MouseEventCondition dbl() {
		return cc(2);
	}

	@Override
	public MouseEventCondition withUsed() {
		return (MouseEventCondition) super.withUsed();
	}
}
