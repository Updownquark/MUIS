package org.muis.core.event;

import org.muis.core.MuisElement;
import org.muis.core.mgr.ImmutableChildList;

/** Represents the change of an element's list of children */
public class ChildEvent implements MuisEvent {
	/** Filters child events */
	public static final ChildEventCondition child = ChildEventCondition.child;

	/** The type of a {@link ChildEvent} */
	public static enum ChildEventType {
		/** Represents the addition of a child to an element */
		ADD,
		/** Represents the removeal of a child from an element */
		REMOVE,
		/** Represents the movement of a child in an element from one index to another */
		MOVE;

		/** @return Whether this is the {@link #ADD} event type */
		public boolean isAdd() {
			return this == ADD;
		}

		/** @return Whether this is the {@link #REMOVE} event type */
		public boolean isRemove() {
			return this == REMOVE;
		}

		/** @return Whether this is the {@link #MOVE} event type */
		public boolean isMove() {
			return this == MOVE;
		}
	}

	private final MuisElement theParent;
	private final MuisElement theChild;

	private final int theIndex;
	private final int theMoveFromIndex;

	private final ChildEvent.ChildEventType theType;

	private final ImmutableChildList<MuisElement> thePreChange;
	private final ImmutableChildList<MuisElement> thePostChange;

	/**
	 * @param parent The parent on which the change occurred
	 * @param ch The child that was added, removed, or moved
	 * @param index The index that the child was added to, removed from, or moved to
	 * @param moveFromIndex The index that the child was moved to. Same as {@code index} if this event is not of type
	 *            {@link ChildEventType#MOVE}.
	 * @param type The type of the event
	 * @param preChange The elements in the list immediately before the change
	 * @param postChange The elements in the list immediately after the change
	 */
	public ChildEvent(MuisElement parent, MuisElement ch, int index, int moveFromIndex, ChildEvent.ChildEventType type,
		ImmutableChildList<MuisElement> preChange, ImmutableChildList<MuisElement> postChange) {
		theParent = parent;
		theChild = ch;
		theIndex = index;
		theMoveFromIndex = moveFromIndex;
		theType = type;
		thePreChange = preChange;
		thePostChange = postChange;
	}

	/** @return The parent whose child list was changed */
	@Override
	public MuisElement getElement() {
		return theParent;
	}

	@Override
	public MuisEvent getCause() {
		return null;
	}

	/** @return The child that was added, removed, or moved */
	public MuisElement getChild() {
		return theChild;
	}

	/** @return The index that the child was added to, removed from, or moved to */
	public int getIndex() {
		return theIndex;
	}

	/**
	 * @return The index that the child was moved from. Same as {@link #getIndex()} if this event is not of type
	 *         {@link ChildEventType#MOVE}
	 */
	public int getMoveFromIndex() {
		return theMoveFromIndex;
	}

	/** @return The type of this event */
	public ChildEvent.ChildEventType getType() {
		return theType;
	}

	/** @return The elements in the list immediately before the change */
	public ImmutableChildList<MuisElement> getPreChangeContent() {
		return thePreChange;
	}

	/** @return The elements in the list immediately after the change */
	public ImmutableChildList<MuisElement> getPostChangeContent() {
		return thePostChange;
	}

	@Override
	public boolean isOverridden() {
		return false;
	}
}