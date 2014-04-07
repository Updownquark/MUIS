package org.muis.core.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.muis.core.event.ChildEvent.ChildEventType;
import org.muis.core.event.boole.TPAnd;
import org.muis.core.event.boole.TPOr;
import org.muis.core.event.boole.TypedPredicate;

/** Allows advanced filtering on {@link ChildEvent}s */
public class ChildEventCondition implements MuisEventCondition<ChildEvent>, Cloneable {
	/** Filters events of this type */
	public static final TypedPredicate<MuisEvent, ChildEvent> base = new TypedPredicate<MuisEvent, ChildEvent>() {
		@Override
		public ChildEvent cast(MuisEvent value) {
			return value instanceof ChildEvent ? (ChildEvent) value : null;
		}
	};

	/** Filters {@link ChildEvent}s based on their {@link ChildEvent#getType() type} */
	public static class ChildEventTypePredicate implements TypedPredicate<ChildEvent, ChildEvent> {
		private final ChildEventType theType;

		private ChildEventTypePredicate(ChildEventType type) {
			theType = type;
		}

		@Override
		public ChildEvent cast(ChildEvent value) {
			return value.getType() == theType ? value : null;
		}
	}

	/** A map of all {@link ChildEventType}s to {@link ChildEventTypePredicate}s that filter on that type */
	public static final Map<ChildEventType, ChildEventTypePredicate> types;

	static {
		Map<ChildEventType, ChildEventTypePredicate> t = new java.util.LinkedHashMap<>();
		for(ChildEventType type : ChildEventType.values())
			t.put(type, new ChildEventTypePredicate(type));
		types = Collections.unmodifiableMap(t);
	}

	/**
	 * @param eventTypes The child event types to filter on
	 * @return A filter that filters events for any of the given types
	 */
	public static TPOr<ChildEvent> or(ChildEventType... eventTypes) {
		List<ChildEventTypePredicate> preds = new ArrayList<>();
		for(ChildEventType type : eventTypes)
			preds.add(types.get(type));
		return new TPOr<>(preds);
	}

	/**
	 * @param eventTypes The child event types to filter on
	 * @return A filter that filters events for any of the given types
	 */
	public static TPOr<ChildEvent> orTypes(Iterable<ChildEventType> eventTypes) {
		List<ChildEventTypePredicate> preds = new ArrayList<>();
		for(ChildEventType type : eventTypes)
			preds.add(types.get(type));
		return new TPOr<>(preds);
	}

	/** Filters child events */
	public static final ChildEventCondition child = new ChildEventCondition();

	private List<ChildEventType> theTypes;

	private ChildEventCondition() {
		theTypes = null;
	}

	@Override
	public TypedPredicate<MuisEvent, ChildEvent> getTester() {
		TypedPredicate<MuisEvent, ChildEvent> ret = base;
		if(theTypes != null) {
			if(theTypes.size() == 1)
				ret = new TPAnd<>(base, types.get(theTypes.get(0)));
			else
				ret = new TPAnd<>(base, orTypes(theTypes));
		}
		return ret;
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

	@Override
	protected ChildEventCondition clone() {
		try {
			return (ChildEventCondition) super.clone();
		} catch(CloneNotSupportedException e) {
			throw new IllegalStateException(e);
		}
	}
}
