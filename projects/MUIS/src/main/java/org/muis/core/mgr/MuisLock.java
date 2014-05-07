package org.muis.core.mgr;

import org.muis.core.MuisElement;

public class MuisLock implements AutoCloseable {
	private final MuisElement [] theElements;

	private final Object theType;

	private volatile MuisLocker theLocker;

	public MuisLock(Object type, MuisElement... els) {
		theElements = els;
		theType = type;
	}

	public MuisElement [] getElements() {
		return theElements.clone();
	}

	public Object getType() {
		return theType;
	}

	public boolean isActive() {
		return theLocker != null;
	}

	void setLocker(MuisLocker locker) {
		theLocker = locker;
	}

	@Override
	public void close() {
		synchronized(theElements) {
			theLocker.unlock(this);
			theLocker = null;
		}
	}
}
