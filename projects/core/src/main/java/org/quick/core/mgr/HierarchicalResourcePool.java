package org.quick.core.mgr;

import java.util.ArrayList;
import java.util.List;

import org.observe.Observable;
import org.observe.Observer;
import org.observe.Subscription;
import org.qommons.Transactable;
import org.qommons.Transaction;
import org.qommons.collect.BetterHashMap;
import org.qommons.collect.BetterMap;
import org.qommons.collect.ListenerList;
import org.qommons.collect.RRWLockingStrategy;

public class HierarchicalResourcePool implements ObservableResourcePool {
	public static final RootResourcePool ROOT = new RootResourcePool();

	private final Object theSubject;
	private HierarchicalResourcePool theParent;
	private boolean isLocallyActive;
	private boolean isParentActive;
	private boolean isActuallyActive;
	private final List<HierarchicalResourcePool> theChildren;
	private final Transactable theLock;
	private final BetterMap<Observable<?>, PooledResource<?>> theResources;

	public HierarchicalResourcePool(Object subject, HierarchicalResourcePool parent, Transactable lock, boolean active) {
		theSubject = subject;
		theParent = parent;
		theLock = lock;
		isLocallyActive = active;
		theChildren = new ArrayList<>();
		theResources = BetterHashMap.build().identity().withLocking(new RRWLockingStrategy(lock)).buildMap();
		if (parent != null) {
			try (Transaction t = parent.lock(true, this)) {
				isParentActive = parent.isActuallyActive;
				parent.theChildren.add(this);
			}
		} else
			isParentActive = false;
		isActuallyActive = isParentActive && isLocallyActive;
	}

	void setRoot() {
		if (!(this instanceof RootResourcePool) || ROOT != null)
			throw new IllegalStateException(
				"Should only be called from the singleton " + RootResourcePool.class.getSimpleName() + " instance");
		isParentActive = true;
		isActuallyActive = isLocallyActive;
		// This is only called from the constructor, so there's no need to do any notifications
	}

	public void setParent(HierarchicalResourcePool newParent) {
		try (Transaction t = theLock.lock(true, null)) {
			if (theParent != null) {
				try (Transaction oldPT = theParent.lock(true, null)) {
					theParent.theChildren.remove(this);
				}
				theParent = null;
				if (newParent != null) {
					try (Transaction newPT = newParent.lock(true, null)) {
						newParent.theChildren.add(this);
					}
					theParent = newParent;
					isParentActive = newParent.isActuallyActive;
				} else
					isParentActive = false;
			}
			checkActive(t);
		}
	}

	public boolean isLocallyActive() {
		return isLocallyActive;
	}

	public boolean isActive() {
		return isActuallyActive;
	}

	public void setActive(boolean active) {
		Transaction t = theLock.lock(true, null);
		isLocallyActive = active;
		checkActive(t);
	}

	private void checkActive(Transaction writeT) {
		boolean actuallyActive = isLocallyActive && isParentActive;
		if (isActuallyActive == actuallyActive) {
			writeT.close();
			return;
		} else {
			isActuallyActive = actuallyActive;
			for (PooledResource<?> resource : theResources.values())
				resource.setConnected(actuallyActive);
			try (Transaction roT = theLock.lock(false, null)) {
				writeT.close();
				for (HierarchicalResourcePool child : theChildren)
					child.updateParentActive(actuallyActive);
			}
		}
	}

	private void updateParentActive(boolean active) {
		Transaction t = theLock.lock(true, null);
		isParentActive = active;
		checkActive(t);
	}

	@Override
	public boolean isLockSupported() {
		return theLock.isLockSupported();
	}

	@Override
	public Transaction lock(boolean write, Object cause) {
		return theLock.lock(write, cause);
	}

	@Override
	public Transaction tryLock(boolean write, Object cause) {
		return theLock.tryLock(write, cause);
	}

	@Override
	public <T> Observable<T> pool(Observable<T> resource) {
		return (PooledResource<T>) theResources.computeIfAbsent(resource, PooledResource::new);
	}

	@Override
	public String toString() {
		return theSubject + " resPool";
	}

	private class PooledResource<T> implements Observable<T> {
		private final Observable<T> theObservable;
		private final ListenerList<SubscribedObserver<? super T>> theObservers;

		PooledResource(Observable<T> observable) {
			theObservable = observable;
			theObservers = ListenerList.build().allowReentrant().withFastSize(false).build();
		}

		@Override
		public Subscription subscribe(Observer<? super T> observer) {
			SubscribedObserver<? super T> subObs = new SubscribedObserver<>(observer);
			try (Transaction t = theLock.lock(false, null)) {
				if (isActive())
					subObs.subscription = theObservable.subscribe(observer);
				Runnable listRemove = theObservers.add(subObs, true);
				return () -> {
					listRemove.run();
					subObs.unsubscribe();
				};
			}
		}

		@Override
		public boolean isSafe() {
			return theObservable.isSafe();
		}

		@Override
		public Transaction lock() {
			return theObservable.lock(); // TODO Is there a way to safely avoid locking the observable when the pool is inactive?
		}

		@Override
		public Transaction tryLock() {
			return theObservable.tryLock(); // TODO Is there a way to safely avoid locking the observable when the pool is inactive?
		}

		void setConnected(boolean connected) {
			theObservers.forEach(subObs -> {
				if (connected)
					subObs.subscription = theObservable.subscribe(subObs.observer);
				else
					subObs.unsubscribe();
			});
		}

		@Override
		public String toString() {
			return HierarchicalResourcePool.this + ": " + theObservable;
		}
	}

	private static class SubscribedObserver<T> {
		final Observer<? super T> observer;
		Subscription subscription;

		SubscribedObserver(Observer<? super T> observer) {
			this.observer = observer;
		}

		void unsubscribe() {
			Subscription sub = subscription;
			subscription = null;
			sub.unsubscribe();
		}
	}

	private static class RootResourcePool extends HierarchicalResourcePool {
		RootResourcePool() {
			super("ROOT", null, Transactable.NONE, true);
			setRoot();
		}

		@Override
		public void setParent(HierarchicalResourcePool newParent) {
			if (newParent != null)
				throw new IllegalArgumentException("The ROOT resource pool cannot have a parent");
		}

		@Override
		public void setActive(boolean active) {
			if (!active)
				throw new UnsupportedOperationException("The ROOT resource pool serves only as a global root and cannot be deactivated");
		}

		@Override
		public <T> Observable<T> pool(Observable<T> resource) {
			throw new UnsupportedOperationException(
				"The ROOT resource pool serves only as a global root and cannot be used to pool resources");
		}
	}
}
