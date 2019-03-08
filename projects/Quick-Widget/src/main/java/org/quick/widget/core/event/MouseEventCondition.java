package org.quick.widget.core.event;

import java.util.ArrayList;
import java.util.List;

import org.quick.widget.core.event.MouseEvent.ButtonType;
import org.quick.widget.core.event.MouseEvent.MouseEventType;

/** Allows advanced filtering on {@link MouseEvent}s */
public class MouseEventCondition extends PositionedUserEventCondition<MouseEvent> {
	/** Filters all {@link MouseEvent}s */
	@SuppressWarnings("hiding")
	public static java.util.function.Function<QuickWidgetEvent, MouseEvent> base = value -> {
		return value instanceof MouseEvent ? (MouseEvent) value : null;
	};

	/** Filters mouse events that have not been {@link UserEvent#use() used} */
	public static final MouseEventCondition mouse = new MouseEventCondition();

	private List<MouseEventType> theTypes;
	private List<ButtonType> theButtons;
	private Integer theClickCount;

	private MouseEventCondition() {
		theTypes = null;
		theButtons = null;
		theClickCount = null;
	}

	@Override
	public MouseEvent apply(QuickWidgetEvent evt) {
		MouseEvent mouseEvt = base.apply(userApply(evt));
		if(mouseEvt == null)
			return null;
		if(theTypes != null && !theTypes.contains(mouseEvt.getType()))
			return null;
		if(theButtons != null && !theButtons.contains(mouseEvt.getButton()))
			return null;
		if(theClickCount != null && mouseEvt.getClickCount() != theClickCount.intValue())
			return null;
		return mouseEvt;
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
