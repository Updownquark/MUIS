package org.muis.core.event;

public class SizeNeedsChangedEvent extends MuisEvent<Void> {
	private final SizeNeedsChangedEvent theCause;

	private boolean isHandled;

	public SizeNeedsChangedEvent(SizeNeedsChangedEvent cause) {
		super(org.muis.core.MuisConstants.Events.SIZE_NEEDS_CHANGED, null);
		theCause = cause;
	}

	public SizeNeedsChangedEvent getCause() {
		return theCause;
	}

	public SizeNeedsChangedEvent getRoot() {
		SizeNeedsChangedEvent ret = this;
		while(ret.theCause != null)
			ret = theCause;
		return ret;
	}

	public boolean isHandled() {
		return isHandled;
	}

	public void handle() {
		isHandled = true;
		if(theCause != null)
			theCause.handle();
	}
}
