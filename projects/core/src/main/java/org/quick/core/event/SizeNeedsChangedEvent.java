package org.quick.core.event;

import org.quick.core.QuickElement;

/**
 * An event communicating that an element's size needs may have changed and its parent or an ancestor may need to lay its contents out again
 */
public class SizeNeedsChangedEvent implements QuickEvent {
	/** Filters for this type of event */
	public static final java.util.function.Function<QuickEvent, SizeNeedsChangedEvent> sizeNeeds = value -> {
		return value instanceof SizeNeedsChangedEvent ? (SizeNeedsChangedEvent) value : null;
	};

	private final QuickElement theElement;
	private final SizeNeedsChangedEvent theCause;

	private boolean isHandled;

	/**
	 * @param element The element that this event is being fired in
	 * @param cause The event from a child that may have prompted this event (may be null)
	 */
	public SizeNeedsChangedEvent(QuickElement element, SizeNeedsChangedEvent cause) {
		theElement = element;
		theCause = cause;
	}

	@Override
	public QuickElement getElement() {
		return theElement;
	}

	/** @return The event from a child that may have prompted this event (may be null) */
	@Override
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

	@Override
	public boolean isOverridden() {
		return isHandled;
	}

	@Override
	public String toString() {
		return "Size needs changed for " + theElement;
	}
}
