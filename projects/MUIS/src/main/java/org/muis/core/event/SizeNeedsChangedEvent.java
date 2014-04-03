package org.muis.core.event;

import org.muis.core.MuisElement;
import org.muis.core.event.boole.TypedPredicate;

/**
 * An event communicating that an element's size needs may have changed and its parent or an ancestor may need to lay its contents out again
 */
public class SizeNeedsChangedEvent implements MuisEvent {
	/** Filters for this type of event */
	public static final TypedPredicate<MuisEvent, SizeNeedsChangedEvent> sizeNeeds = new TypedPredicate<MuisEvent, SizeNeedsChangedEvent>() {
		@Override
		public SizeNeedsChangedEvent cast(MuisEvent value) {
			return value instanceof SizeNeedsChangedEvent ? (SizeNeedsChangedEvent) value : null;
		}
	};

	private final MuisElement theElement;
	private final SizeNeedsChangedEvent theCause;

	private boolean isHandled;

	/**
	 * @param element The element that this event is being fired in
	 * @param cause The event from a child that may have prompted this event (may be null)
	 */
	public SizeNeedsChangedEvent(MuisElement element, SizeNeedsChangedEvent cause) {
		theElement = element;
		theCause = cause;
	}

	@Override
	public MuisElement getElement() {
		return theElement;
	}

	/** @return The event from a child that may have prompted this event (may be null) */
	public SizeNeedsChangedEvent getCause() {
		return theCause;
	}

	/** @return The original event that may have ultimately resulted in this event (will not be null, may be <code>this</code>) */
	public SizeNeedsChangedEvent getRoot() {
		SizeNeedsChangedEvent ret = this;
		while(ret.theCause != null)
			ret = theCause;
		return ret;
	}

	/** @return Whether a layout operation has been accomplished or queued as a result of this event */
	public boolean isHandled() {
		return isHandled;
	}

	/** Marks that a layout operation has been accomplished or queued as a result of this event */
	public void handle() {
		isHandled = true;
		if(theCause != null)
			theCause.handle();
	}
}
