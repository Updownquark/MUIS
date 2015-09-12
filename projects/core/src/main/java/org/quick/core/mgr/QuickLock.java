package org.quick.core.mgr;

import org.quick.core.QuickElement;

public class QuickLock implements AutoCloseable {
	private final QuickElement [] theElements;

	private final Object theType;

	private volatile QuickLocker theLocker;

	public QuickLock(Object type, QuickElement... els) {
		theElements = els;
		theType = type;
	}

	public QuickElement [] getElements() {
		return theElements.clone();
	}

	public Object getType() {
		return theType;
	}

	public boolean isActive() {
		return theLocker != null;
	}

	void setLocker(QuickLocker locker) {
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
