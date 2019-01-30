package org.quick.core.mgr;

import java.util.ArrayList;
import java.util.List;

import org.observe.Observable;
import org.qommons.Lockable;
import org.qommons.Transaction;

public class HierarchicalResourcePool implements ObservableResourcePool {
	private HierarchicalResourcePool theParent;
	private int theAncestorCount;
	private int theActiveParents;
	private boolean isActive;
	private boolean isActuallyActive;
	private final List<HierarchicalResourcePool> theChildren;
	private final Lockable theLock;

	public HierarchicalResourcePool(HierarchicalResourcePool parent, Lockable lock, boolean active) {
		theParent = parent;
		theLock = lock;
		isActive = active;
		theChildren = new ArrayList<>();
		if (parent != null) {
			try (Transaction t = parent.lock()) {
				theAncestorCount = parent.theAncestorCount + 1;
				theActiveParents = parent.theActiveParents + (parent.isActive ? 1 : 0);
				parent.theChildren.add(this);
			}
		}
		checkActive();
	}

	public void setParent(HierarchicalResourcePool newParent) {
		try (Transaction t = Lockable.lockAll(theLock, theParent, newParent)) {
			theParent.theChildren.remove(this);
			if (newParent != null) {
				theAncestorCount = newParent.theAncestorCount + 1;
				theActiveParents = newParent.theActiveParents + (newParent.isActive ? 1 : 0);
				newParent.theChildren.add(this);
			} else
				theAncestorCount = theActiveParents = 0;
			theParent = newParent;
			checkActive();
		}
	}

	public void setActive(boolean active) {
		try (Transaction t = theLock.lock()) {
			if (isActive == active)
				return;
			isActive = active;
		}
		updateActiveParentCount(active ? 1 : -1);
	}

	private void updateActiveParentCount(int diff) {
		try (Transaction t = theLock.lock()) {
			theActiveParents += diff;
			checkActive();
		}
		for (HierarchicalResourcePool child : theChildren)
			child.updateActiveParentCount(diff);
	}

	private boolean checkActive() {
		boolean actuallyActive = isActive && theActiveParents == theAncestorCount;
		if (isActuallyActive == actuallyActive) {
		} else if (actuallyActive) {
			// TODO re-add all subscriptions
		} else {
			// TODO remove all subscriptions
		}
	}

	@Override
	public boolean isLockSupported() {
		return theLock.isLockSupported();
	}

	@Override
	public Transaction lock() {
		return theLock.lock();
	}

	@Override
	public Transaction tryLock() {
		return theLock.tryLock();
	}

	@Override
	public <T> Observable<T> pool(Observable<T> resource) {
		// TODO Auto-generated method stub
		return null;
	}
}
