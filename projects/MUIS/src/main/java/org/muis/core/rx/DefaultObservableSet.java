package org.muis.core.rx;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A set whose content can be observed. This set is immutable in that none of its methods, including {@link Set} methods, can modify its
 * content (Set modification methods will throw {@link UnsupportedOperationException}). To modify the list content, use {@link #control()}
 * to obtain a set controller. This controller can be modified and these modifications will be reflected in this set and will be propagated
 * to subscribers.
 *
 * @param <E> The type of element in the set
 */
public class DefaultObservableSet<E> extends AbstractSet<E> implements ObservableSet<E> {
	private LinkedHashMap<E, ObservableElement<E>> theValues;

	private ReentrantReadWriteLock theLock;
	private AtomicBoolean hasIssuedController = new AtomicBoolean(false);
	private DefaultObservable<Observable<E>> theBackingObservable;
	private Observer<Observable<E>> theBackingController;
	private DefaultObservable<Void> theChangeObservable;
	private Observer<Void> theChangeController;

	/** Creates the list */
	public DefaultObservableSet() {
		theValues = new LinkedHashMap<>();

		theLock = new ReentrantReadWriteLock();
		theBackingObservable = new DefaultObservable<>();
		theChangeObservable = new DefaultObservable<>();
		theChangeController = theChangeObservable.control(null);
	}

	/**
	 * @param action The action to perform under a lock
	 * @param write Whether to perform the action under a write lock or a read lock
	 */
	protected void doLocked(Runnable action, boolean write) {
		Lock lock = write ? theLock.writeLock() : theLock.readLock();
		lock.lock();
		try {
			action.run();
			theChangeController.onNext(null);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Obtains the controller for this list. Only one call can be made to this method. All calls after the first will throw an
	 * {@link IllegalStateException}.
	 *
	 * @return The list to control this list's data.
	 */
	public Set<E> control(org.muis.core.rx.DefaultObservable.OnSubscribe<Observable<E>> onSubscribe) {
		if(hasIssuedController.getAndSet(true))
			throw new IllegalStateException("This observable set is already controlled");
		theBackingController = theBackingObservable.control(subscriber -> {
			doLocked(() -> {
				for(ObservableElement<E> el : theValues.values())
					subscriber.onNext(el);
			}, false);
			onSubscribe.onsubscribe(subscriber);
		});
		return new ObservableSetController();
	}

	@Override
	public Observable<Void> changes() {
		return theChangeObservable;
	}

	@Override
	public Subscription<Observable<E>> subscribe(Observer<? super Observable<E>> observer) {
		return theBackingObservable.subscribe(observer);
	}

	@Override
	public int size() {
		return theValues.size();
	}

	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			private final Iterator<E> backing = theValues.keySet().iterator();

			@Override
			public boolean hasNext() {
				boolean [] ret = new boolean[1];
				doLocked(() -> {
					ret[0] = backing.hasNext();
				}, false);
				return ret[0];
			}

			@Override
			public E next() {
				Object [] ret = new Object[1];
				doLocked(() -> {
					ret[0] = backing.next();
				}, false);
				return (E) ret[0];
			}
		};
	}

	@Override
	protected DefaultObservableSet<E> clone() throws CloneNotSupportedException {
		DefaultObservableSet<E> ret = (DefaultObservableSet<E>) super.clone();
		ret.theValues = (LinkedHashMap<E, ObservableElement<E>>) theValues.clone();
		for(Map.Entry<E, ObservableElement<E>> entry : theValues.entrySet())
			entry.setValue(new ObservableElement<>(entry.getKey()));
		ret.theLock = new ReentrantReadWriteLock();
		ret.hasIssuedController = new AtomicBoolean(false);
		ret.theBackingObservable = new DefaultObservable<>();
		return ret;
	}

	private class ObservableSetController extends AbstractSet<E> {
		@Override
		public Iterator<E> iterator() {
			return new Iterator<E>() {
				private Iterator<Map.Entry<E, ObservableElement<E>>> backing = theValues.entrySet().iterator();

				@Override
				public boolean hasNext() {
					boolean [] ret = new boolean[1];
					doLocked(() -> {
						ret[0] = backing.hasNext();
					}, false);
					return ret[0];
				}

				@Override
				public E next() {
					Object [] ret = new Object[1];
					doLocked(() -> {
						ret[0] = backing.next();
					}, false);
					return (E) ret[0];
				}

				@Override
				public void remove() {
					doLocked(() -> {
						backing.remove();
					}, true);
				}
			};
		}

		@Override
		public int size() {
			return DefaultObservableSet.this.size();
		}

		@Override
		public boolean contains(Object o) {
			boolean [] ret = new boolean[1];
			doLocked(() -> {
				ret[0] = theValues.containsKey(o);
			}, false);
			return ret[0];
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			boolean [] ret = new boolean[1];
			doLocked(() -> {
				ret[0] = theValues.keySet().containsAll(c);
			}, false);
			return ret[0];
		}

		@Override
		public Object [] toArray() {
			Object [][] ret = new Object[1][];
			doLocked(() -> {
				ret[0] = theValues.keySet().toArray();
			}, false);
			return ret[0];
		}

		@Override
		public <T> T [] toArray(T [] a) {
			Object [][] ret = new Object[1][];
			doLocked(() -> {
				ret[0] = theValues.keySet().toArray(a);
			}, false);
			return (T []) ret[0];
		}

		@Override
		public boolean add(E e) {
			boolean [] ret = new boolean[1];
			doLocked(() -> {
				if(!theValues.containsKey(e))
					return;
				ret[0] = true;
				ObservableElement<E> el = new ObservableElement<>(e);
				theValues.put(e, el);
				theBackingController.onNext(el);
			}, true);
			return ret[0];
		}

		@Override
		public boolean remove(Object o) {
			boolean [] ret = new boolean[1];
			doLocked(() -> {
				ObservableElement<E> el = theValues.remove(o);
				if(el == null)
					return;
				ret[0] = true;
				el.remove();
			}, true);
			return ret[0];
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			boolean [] ret = new boolean[1];
			doLocked(() -> {
				for(Object o : c) {
					ObservableElement<E> el = theValues.remove(o);
					if(el == null)
						continue;
					ret[0] = true;
					el.remove();
				}
			}, true);
			return ret[0];
		}

		@Override
		public boolean addAll(Collection<? extends E> c) {
			boolean [] ret = new boolean[1];
			doLocked(() -> {
				for(E add : c) {
					if(!theValues.containsKey(add))
						continue;
					ret[0] = true;
					ObservableElement<E> el = new ObservableElement<>(add);
					theValues.put(add, el);
					theBackingController.onNext(el);
				}
			}, true);
			return ret[0];
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			boolean [] ret = new boolean[1];
			doLocked(() -> {
				Iterator<Map.Entry<E, ObservableElement<E>>> iter = theValues.entrySet().iterator();
				while(iter.hasNext()) {
					Map.Entry<E, ObservableElement<E>> entry = iter.next();
					if(c.contains(entry.getKey()))
						continue;
					ret[0] = true;
					ObservableElement<E> el = entry.getValue();
					iter.remove();
					el.remove();
				}
			}, true);
			return ret[0];
		}

		@Override
		public void clear() {
			doLocked(() -> {
				Iterator<ObservableElement<E>> iter = theValues.values().iterator();
				while(iter.hasNext()) {
					ObservableElement<E> el = iter.next();
					iter.remove();
					el.remove();
				}
			}, true);
		}
	}
}
