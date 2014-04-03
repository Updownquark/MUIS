package org.muis.core.event;

import org.muis.core.MuisElement;
import org.muis.core.event.boole.TypedPredicate;

public class ElementMovedEvent implements MuisEvent {
	public static final TypedPredicate<MuisEvent, ElementMovedEvent> moved = new TypedPredicate<MuisEvent, ElementMovedEvent>() {
		@Override
		public ElementMovedEvent cast(MuisEvent value) {
			return value instanceof ElementMovedEvent ? (ElementMovedEvent) value : null;
		}
	};

	private final MuisElement theElement;

	private final MuisElement theOldParent;
	private final MuisElement theNewParent;

	public ElementMovedEvent(MuisElement element, MuisElement oldParent, MuisElement newParent) {
		theElement = element;
		theOldParent = oldParent;
		theNewParent = newParent;
	}

	@Override
	public MuisElement getElement() {
		return theElement;
	}

	public MuisElement getOldParent() {
		return theOldParent;
	}

	public MuisElement getNewParent() {
		return theNewParent;
	}
}
