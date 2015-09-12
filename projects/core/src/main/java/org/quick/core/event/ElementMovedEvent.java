package org.quick.core.event;

import org.quick.core.QuickElement;

/** Signifies the transfer of an element from one parent to another */
public class ElementMovedEvent implements QuickEvent {
	/** Filters events of this type */
	public static final java.util.function.Function<QuickEvent, ElementMovedEvent> moved = value -> {
		return value instanceof ElementMovedEvent ? (ElementMovedEvent) value : null;
	};

	private final QuickElement theElement;

	private final QuickElement theOldParent;
	private final QuickElement theNewParent;

	/**
	 * @param element The element that was moved
	 * @param oldParent The parent of the element prior to the move
	 * @param newParent The parent of the element after the move
	 */
	public ElementMovedEvent(QuickElement element, QuickElement oldParent, QuickElement newParent) {
		theElement = element;
		theOldParent = oldParent;
		theNewParent = newParent;
	}

	@Override
	public QuickElement getElement() {
		return theElement;
	}

	@Override
	public QuickEvent getCause() {
		return null;
	}

	/** @return The parent of the element prior to the move */
	public QuickElement getOldParent() {
		return theOldParent;
	}

	/** @return THe parent of the element after the move */
	public QuickElement getNewParent() {
		return theNewParent;
	}

	@Override
	public boolean isOverridden() {
		return theElement.getParent() != theNewParent;
	}

	@Override
	public String toString() {
		if(theOldParent == null)
			return theElement + " added to " + theNewParent;
		else if(theNewParent == null)
			return theElement + " removed from " + theOldParent;
		else
			return theElement + " moved from " + theOldParent + " to " + theNewParent;
	}
}
