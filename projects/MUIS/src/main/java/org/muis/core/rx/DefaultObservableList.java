package org.muis.core.rx;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A list whose content can be observed. This list is immutable in that none of its methods, including {@link List} methods, can modify its
 * content (List modification methods will throw {@link UnsupportedOperationException}). To modify the list content, use {@link #control()}
 * to obtain a list controller. This controller can be modified and these modifications will be reflected in this list and will be
 * propagated to subscribers.
 *
 * @param <E> The type of element in the list
 */
public class DefaultObservableList<E> extends AbstractList<E> implements ObservableList<E>, RandomAccess {
	private ArrayList<E> theValues;
	private ArrayList<ObservableElement<E>> theElements;

	private ReentrantReadWriteLock theLock;
	private AtomicBoolean hasIssuedController = new AtomicBoolean(false);
	private DefaultObservable<Observable<E>> theBackingObservable;
	private Observer<Observable<E>> theBackingController;

	/** Creates the list */
	public DefaultObservableList() {
		theValues = new ArrayList<>();
		theElements = new ArrayList<>();

		theLock = new ReentrantReadWriteLock();
		theBackingObservable = new DefaultObservable<>();
		theBackingController = theBackingObservable.control(subscriber -> {
			doLocked(() -> {
				for(ObservableElement<E> el : theElements)
					subscriber.onNext(el);
			}, false);
		});
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
	public List<E> control() {
		if(hasIssuedController.getAndSet(true))
			throw new IllegalStateException("This observable set is already controlled");
		return new ObservableListController();
	}

	@Override
	public Subscription<Observable<E>> subscribe(Observer<? super Observable<E>> observer) {
		return theBackingObservable.subscribe(observer);
	}

	@Override
	public E get(int index) {
		Object [] ret = new Object[1];
		doLocked(() -> {
			ret[0] = theValues.get(index);
		}, false);
		return (E) ret[0];
	}

	@Override
	public int size() {
		return theValues.size();
	}

	@Override
	public int indexOf(Observable<E> obsEl) {
		return theElements.indexOf(obsEl);
	}

	@Override
	protected DefaultObservableList<E> clone() throws CloneNotSupportedException {
		DefaultObservableList<E> ret = (DefaultObservableList<E>) super.clone();
		ret.theValues = (ArrayList<E>) theValues.clone();
		ret.theElements = new ArrayList<>();
		for(E el : ret.theValues)
			ret.theElements.add(new ObservableElement<>(el));
		ret.theLock = new ReentrantReadWriteLock();
		ret.hasIssuedController = new AtomicBoolean(false);
		ret.theBackingObservable = new DefaultObservable<>();
		ret.theBackingController = ret.theBackingObservable.control(subscriber -> {
			doLocked(() -> {
				for(ObservableElement<E> el : ret.theElements)
					subscriber.onNext(el);
			}, false);
		});
		return ret;
	}

	private class ObservableListController extends AbstractList<E> {
		@Override
		public int size() {
			return DefaultObservableList.this.size();
		}

		@Override
		public boolean contains(Object o) {
			boolean [] ret = new boolean[1];
			doLocked(() -> {
				ret[0] = theValues.contains(o);
			}, false);
			return ret[0];
		}

		@Override
		public Object [] toArray() {
			Object [][] ret = new Object[1][];
			doLocked(() -> {
				ret[0] = theValues.toArray();
			}, false);
			return ret[0];
		}

		@Override
		public <T> T [] toArray(T [] a) {
			Object [][] ret = new Object[1][];
			doLocked(() -> {
				ret[0] = theValues.toArray(a);
			}, false);
			return (T []) ret[0];
		}

		@Override
		public E get(int index) {
			Object [] ret = new Object[1];
			doLocked(() -> {
				ret[0] = theValues.get(index);
			}, false);
			return (E) ret[0];
		}

		@Override
		public int indexOf(Object o) {
			int [] ret = new int[1];
			doLocked(() -> {
				ret[0] = theValues.indexOf(o);
			}, false);
			return ret[0];
		}

		@Override
		public int lastIndexOf(Object o) {
			int [] ret = new int[1];
			doLocked(() -> {
				ret[0] = theValues.lastIndexOf(o);
			}, false);
			return ret[0];
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			boolean [] ret = new boolean[1];
			doLocked(() -> {
				ret[0] = theValues.containsAll(c);
			}, false);
			return ret[0];
		}

		@Override
		public boolean add(E e) {
			doLocked(() -> {
				theValues.add(e);
				ObservableElement<E> add = new ObservableElement<>(e);
				theElements.add(add);
				theBackingController.onNext(add);
			}, true);
			return true;
		}

		@Override
		public boolean remove(Object o) {
			boolean [] ret = new boolean[1];
			doLocked(() -> {
				int idx = theValues.indexOf(o);
				if(idx < 0) {
					ret[0] = false;
					return;
				}
				ret[0] = true;
				theValues.remove(idx);
				theElements.remove(idx).remove();
			}, true);
			return ret[0];
		}

		@Override
		public boolean addAll(Collection<? extends E> c) {
			if(c.isEmpty())
				return false;
			doLocked(() -> {
				for(E e : c) {
					theValues.add(e);
					ObservableElement<E> newWrapper = new ObservableElement<>(e);
					theElements.add(newWrapper);
					theBackingController.onNext(newWrapper);
				}
			}, true);
			return true;
		}

		@Override
		public boolean addAll(int index, Collection<? extends E> c) {
			if(c.isEmpty())
				return false;
			doLocked(() -> {
				for(int i = theValues.size() - 1; i >= index; i--)
					theElements.remove(i).remove();
				int idx = index;
				for(E e : c) {
					theValues.add(idx, e);
					ObservableElement<E> newWrapper = new ObservableElement<>(e);
					theElements.add(newWrapper);
					theBackingController.onNext(newWrapper);
					idx++;
				}
				for(int i = idx; i < theValues.size(); i++) {
					ObservableElement<E> wrapper = new ObservableElement<>(theValues.get(i));
					theElements.add(wrapper);
					theBackingController.onNext(wrapper);
				}
			}, true);
			return true;
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			if(c.isEmpty())
				return false;
			boolean [] ret = new boolean[] {false};
			doLocked(() -> {
				for(Object o : c) {
					int idx = theValues.indexOf(o);
					if(idx >= 0) {
						if(!ret[0]) {
							ret[0] = true;
						}
						theValues.remove(idx);
						theElements.remove(idx).remove();
					}
				}
			}, true);
			return ret[0];
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			if(c.isEmpty()) {
				boolean ret = !isEmpty();
				clear();
				return ret;
			}
			boolean [] ret = new boolean[1];
			doLocked(() -> {
				BitSet keep = new BitSet();
				for(Object o : c) {
					int idx = theValues.indexOf(o);
					if(idx >= 0)
						keep.set(idx);
				}
				ret[0] = keep.nextClearBit(0) < theValues.size();
				if(ret[0]) {
					for(int i = theValues.size() - 1; i >= 0; i--)
						if(!keep.get(i)) {
							theValues.remove(i);
							theElements.remove(i).remove();
						}
				}
			}, true);
			return ret[0];
		}

		@Override
		public void clear() {
			doLocked(() -> {
				theValues.clear();
				ArrayList<ObservableElement<E>> remove = new ArrayList<>();
				remove.addAll(theElements);
				theElements.clear();
				for(int i = remove.size() - 1; i >= 0; i--)
					remove.get(i).remove();
			}, true);
		}

		@Override
		public E set(int index, E element) {
			Object [] ret = new Object[1];
			doLocked(() -> {
				ret[0] = theValues.set(index, element);
				theElements.get(index).set(element);
			}, true);
			return (E) ret[0];
		}

		@Override
		public void add(int index, E element) {
			doLocked(() -> {
				for(int i = theValues.size() - 1; i >= index; i--)
					theElements.remove(i).remove();
				int idx = index;
				theValues.add(idx, element);
				ObservableElement<E> newWrapper = new ObservableElement<>(element);
				theElements.add(newWrapper);
				theBackingController.onNext(newWrapper);
				idx++;
				for(int i = idx; i < theValues.size(); i++) {
					ObservableElement<E> wrapper = new ObservableElement<>(theValues.get(i));
					theElements.add(wrapper);
					theBackingController.onNext(wrapper);
				}
			}, true);
		}

		@Override
		public E remove(int index) {
			Object [] ret = new Object[1];
			doLocked(() -> {
				ret[0] = theValues.remove(index);
				theElements.remove(index).remove();
			}, true);
			return (E) ret[0];
		}
	}
}
