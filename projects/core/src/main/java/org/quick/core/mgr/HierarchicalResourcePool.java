package org.quick.core.mgr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.observe.Observable;
import org.observe.Subscription;
import org.qommons.Transactable;
import org.qommons.Transaction;
import org.qommons.collect.ListenerList;

public class HierarchicalResourcePool implements ObservableResourcePool {
	public static final RootResourcePool ROOT = new RootResourcePool();

	private final Object theSubject;
	private HierarchicalResourcePool theParent;
	private boolean isLocallyActive;
	private boolean isParentActive;
	private boolean isActuallyActive;
	private final List<HierarchicalResourcePool> theChildren;
	private final Transactable theLock;
	private final ListenerList<PooledSubscription<?, ?>> theSubscriptions;

	public HierarchicalResourcePool(Object subject, HierarchicalResourcePool parent, Transactable lock, boolean active) {
		theSubject = subject;
		theParent = parent;
		theLock = lock;
		isLocallyActive = active;
		theChildren = new ArrayList<>();
		theSubscriptions = ListenerList.build().allowReentrant().withFastSize(false).build();
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
			theSubscriptions.forEach(resource -> //
			resource.setActive(actuallyActive));
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
	public <R, S> PooledResourceBuilder<R, S> build(Function<? super R, ? extends S> subscribe, R listener) {
		return new PRBImpl<>(subscribe, listener);
	}

	@Override
	public String toString() {
		return theSubject + " resPool";
	}

	private class PRBImpl<R, S> implements PooledResourceBuilder<R, S> {
		private final Function<? super R, ? extends S> theSubscribe;
		private final R theListener;
		private List<Consumer<? super R>> theOnSubscribe;

		PRBImpl(Function<? super R, ? extends S> subscribe, R listener) {
			theSubscribe = subscribe;
			theListener = listener;
		}

		@Override
		public PooledResourceBuilder<R, S> onSubscribe(Consumer<? super R> listener) {
			if (theOnSubscribe == null)
				theOnSubscribe = new LinkedList<>();
			theOnSubscribe.add(listener);
			return this;
		}

		@Override
		public Subscription unsubscribe(BiConsumer<? super R, ? super S> unsubscribe) {
			return new PooledSubscription<>(theListener, theSubscribe, //
				theOnSubscribe == null ? Collections.emptyList() : theOnSubscribe, //
				unsubscribe);
		}
	}

	private class PooledSubscription<S, R> implements Subscription {
		private final R theListener;
		private final Function<? super R, ? extends S> theSubscribe;
		private final List<Consumer<? super R>> theOnSubscribe;
		private final BiConsumer<? super R, ? super S> theUnsubscribe;
		private final Runnable thePoolRemove;
		private S theSubscription;

		PooledSubscription(R listener, Function<? super R, ? extends S> subscribe, List<Consumer<? super R>> onSubscribe,
			BiConsumer<? super R, ? super S> unsubscribe) {
			theListener = listener;
			theSubscribe = subscribe;
			theOnSubscribe = onSubscribe;
			theUnsubscribe = unsubscribe;
			thePoolRemove = theSubscriptions.add(this, false);
		}

		void setActive(boolean active) {
			if (active) {
				for (Consumer<? super R> onSubscribe : theOnSubscribe)
					onSubscribe.accept(theListener);
				theSubscription = theSubscribe.apply(theListener);
			} else {
				theUnsubscribe.accept(theListener, theSubscription);
				theSubscription = null;
			}
		}

		@Override
		public void unsubscribe() {
			thePoolRemove.run();
			try (Transaction t = HierarchicalResourcePool.this.lock(false, null)) {
				if (isActive()) {
					theUnsubscribe.accept(theListener, theSubscription);
					theSubscription = null;
				}
			}
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
