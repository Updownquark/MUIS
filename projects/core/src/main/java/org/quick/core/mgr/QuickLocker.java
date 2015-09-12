package org.quick.core.mgr;

import java.util.HashSet;

import org.quick.core.QuickElement;

public class QuickLocker {
	private final Object theSync;

	private java.util.HashMap<Object, HashSet<QuickElement>> theElementSets;

	private java.util.ArrayList<QuickLock> theLocks;

	public QuickLocker() {
		theSync = new Object();
		theElementSets = new java.util.HashMap<>();
		theLocks = new java.util.ArrayList<>();
	}

	public void lock(QuickLock lock) {
		boolean locked = false;
		do
			synchronized(theSync) {
				if(canLockSynced(lock.getElements())) {
					lockSynced(lock);
					locked = true;
				}
			}
		while(!locked);
	}

	public boolean lockIfUnlocked(QuickLock lock) {
		boolean locked = false;
		synchronized(theSync) {
			if(canLockSynced(lock.getElements())) {
				locked = true;
				lockSynced(lock);
			}
		}
		return locked;
	}

	public boolean canLock(QuickLock lock) {
		synchronized(theSync) {
			return canLockSynced(lock.getElements());
		}
	}

	public boolean isLocked(Object type, QuickElement element) {
		synchronized(theSync) {
			HashSet<QuickElement> set = theElementSets.get(type);
			return set == null ? false : set.contains(element);
		}
	}

	void unlock(QuickLock lock) {
		synchronized(theSync) {
			unlockSynced(lock);
		}
	}

	private boolean canLockSynced(Object type, QuickElement... els) {
		HashSet<QuickElement> set = theElementSets.get(type);
		if(set == null)
			return true;
		for(QuickElement el : els)
			if(set.contains(el))
				return false;
		return true;
	}

	public void lockSynced(QuickLock lock) {
		HashSet<QuickElement> set = theElementSets.get(lock.getType());
		if(set == null) {
			set = new HashSet<>();
			theElementSets.put(lock.getType(), set);
		}
		theLocks.add(lock);
		for(QuickElement el : lock.getElements())
			set.add(el);
		lock.setLocker(this);
	}

	private void unlockSynced(QuickLock lock) {
		if(theLocks.remove(lock)) {
			HashSet<QuickElement> set = theElementSets.get(lock.getType());
			for(QuickElement el : lock.getElements())
				set.remove(el);
		}
	}

	/**
	 * Locks the hierarchy from an ancestor to a parent. If used with {@link QuickElement#CHILDREN_LOCK_TYPE}, this will ensure that the path
	 * from ancestor to descendant remains constant while the lock is held.
	 *
	 * @param type The type of the lock
	 * @param ancestor The ancestor to lock from
	 * @param descendant The descendant to lock to--this element itself is not locked
	 * @param doLock Whether to enforce the lock immediately or merely create the lock instance
	 * @return The lock instance
	 */
	public QuickLock lockFrom(Object type, QuickElement ancestor, QuickElement descendant, boolean doLock) {
		java.util.ArrayList<QuickElement> els = new java.util.ArrayList<>();
		descendant = descendant.getParent();
		while(descendant != null && descendant != ancestor)
			els.add(descendant);
		if(descendant == null)
			throw new IllegalArgumentException("Elements are not related by descent");
		QuickLock ret = new QuickLock(type, els.toArray(new QuickElement[els.size()]));
		if(doLock)
			lock(ret);
		return ret;
	}

	public QuickLock lock(Object type, QuickElement element, boolean doLock) {
		QuickLock ret = new QuickLock(type, element);
		if(doLock)
			lock(ret);
		return ret;
	}
}
