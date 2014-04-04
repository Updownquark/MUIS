package org.muis.core.event;

import org.muis.core.MuisElement;
import org.muis.core.event.boole.TypedPredicate;

/** Signifies the transfer of an element from one parent to another */
public class ElementMovedEvent implements MuisEvent {
	/** Filters events of this type */
	public static final TypedPredicate<MuisEvent, ElementMovedEvent> moved = new TypedPredicate<MuisEvent, ElementMovedEvent>() {
		@Override
		public ElementMovedEvent cast(MuisEvent value) {
			return value instanceof ElementMovedEvent ? (ElementMovedEvent) value : null;
		}
	};

	private final MuisElement theElement;

	private final MuisElement theOldParent;
	private final MuisElement theNewParent;

	/**
	 * @param element The element that was moved
	 * @param oldParent The parent of the element prior to the move
	 * @param newParent The parent of the element after the move
	 */
	public ElementMovedEvent(MuisElement element, MuisElement oldParent, MuisElement newParent) {
		theElement = element;
		theOldParent = oldParent;
		theNewParent = newParent;
	}

	@Override
	public MuisElement getElement() {
		return theElement;
	}

	/** @return The parent of the element prior to the move */
	public MuisElement getOldParent() {
		return theOldParent;
	}

	/** @return THe parent of the element after the move */
	public MuisElement getNewParent() {
		return theNewParent;
	}
}
