package org.quick.core.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.quick.core.QuickElement;
import org.quick.core.event.ChildEvent.ChildEventType;

/** Allows advanced filtering on {@link ChildEvent}s */
public class ChildEventCondition implements QuickEventCondition<ChildEvent>, Cloneable {
	/** Filters events of this type */
	public static final Function<QuickEvent, ChildEvent> base = value -> {
		return value instanceof ChildEvent ? (ChildEvent) value : null;
	};

	/** Filters child events */
	public static final ChildEventCondition child = new ChildEventCondition();

	private List<ChildEventType> theTypes;

	private QuickElement theChild;

	private ChildEventCondition() {
		theTypes = null;
	}

	@Override
	public ChildEvent apply(QuickEvent event) {
		if(!(event instanceof ChildEvent))
			return null;
		ChildEvent chEvt = (ChildEvent) event;
		if(theTypes != null && !theTypes.contains(chEvt.getType()))
			return null;
		if(theChild != null && theChild != chEvt.getChild())
			return null;
		return chEvt;
	}

	/**
	 * @param eventTypes The event types to add
	 * @return A filter that has the same parameters as this condition but with the given event types
	 */
	public ChildEventCondition addTypes(ChildEventType... eventTypes) {
		if(theTypes != null) {
			boolean hasAll = true;
			for(ChildEventType type : eventTypes)
				if(!theTypes.contains(type)) {
					hasAll = false;
					break;
				}
			if(hasAll)
				return this;
		}
		ChildEventCondition ret = clone();
		if(ret.theTypes == null)
			ret.theTypes = new ArrayList<>();
		for(ChildEventType type : eventTypes)
			if(!ret.theTypes.contains(type))
				ret.theTypes.add(type);
		return ret;
	}

	/** @return A new condition that accepts add-typed child events */
	public ChildEventCondition add() {
		return addTypes(ChildEventType.ADD);
	}

	/** @return A new condition that accepts remove-typed child events */
	public ChildEventCondition remove() {
		return addTypes(ChildEventType.REMOVE);
	}

	/** @return A new condition that accepts move-typed child events */
	public ChildEventCondition move() {
		return addTypes(ChildEventType.MOVE);
	}

	/**
	 * @param childElement The child to filter on
	 * @return A filter for events on the given child
	 */
	public ChildEventCondition forChild(QuickElement childElement) {
		ChildEventCondition copy = clone();
		copy.theChild = childElement;
		return copy;
	}

	@Override
	protected ChildEventCondition clone() {
		try {
			return (ChildEventCondition) super.clone();
		} catch(CloneNotSupportedException e) {
			throw new IllegalStateException(e);
		}
	}
}
