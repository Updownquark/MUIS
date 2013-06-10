package org.muis.core.event;

/**
 * An event communicating that an element's size needs may have changed and its parent or an ancestor may need to lay its contents out again
 */
public class SizeNeedsChangedEvent extends MuisEvent<Void> {
	private final SizeNeedsChangedEvent theCause;

	private boolean isHandled;

	/** @param cause The event from a child that may have prompted this event (may be null) */
	public SizeNeedsChangedEvent(SizeNeedsChangedEvent cause) {
		super(org.muis.core.MuisConstants.Events.SIZE_NEEDS_CHANGED, null);
		theCause = cause;
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
