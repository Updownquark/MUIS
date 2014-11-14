package org.muis.core.rx;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import prisms.lang.Type;

/**
 * A list whose content can be observed. This list is immutable in that none of its methods, including {@link List} methods, can modify its
 * content (List modification methods will throw {@link UnsupportedOperationException}). To modify the list content, use
 * {@link #control(org.muis.core.rx.DefaultObservable.OnSubscribe)} to obtain a list controller. This controller can be modified and these
 * modifications will be reflected in this list and will be propagated to subscribers.
 *
 * @param <E> The type of element in the list
 */
public class DefaultObservableList<E> extends AbstractList<E> implements ObservableList<E>, RandomAccess {
	private final Type theType;
	private ArrayList<E> theValues;
	private ArrayList<ObservableElementImpl<E>> theElements;

	private ReentrantReadWriteLock theLock;
	private AtomicBoolean hasIssuedController = new AtomicBoolean(false);
	private org.muis.core.rx.DefaultObservable.OnSubscribe<ObservableElement<E>> theOnSubscribe;
	private java.util.concurrent.ConcurrentHashMap<Observer<? super ObservableElement<E>>, ConcurrentLinkedQueue<Runnable>> theObservers;
	private volatile ObservableElementImpl<E> theRemovedElement;
	private volatile int theRemovedElementIndex;

	/**
	 * Creates the list
	 *
	 * @param type The type of elements for this list
	 */
	public DefaultObservableList(Type type) {
		theType = type;
		theValues = new ArrayList<>();
		theElements = new ArrayList<>();

		theObservers = new java.util.concurrent.ConcurrentHashMap<>();
		theLock = new ReentrantReadWriteLock();
	}

	@Override
	public Type getType() {
		return theType;
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
			if(write) {
				theRemovedElement = null;
				theRemovedElementIndex = -1;
			}
			lock.unlock();
		}
	}

	/**
	 * Obtains the controller for this list. Only one call can be made to this method. All calls after the first will throw an
	 * {@link IllegalStateException}.
	 *
	 * @param onSubscribe The listener to be notified when new subscriptions to this collection are made
	 * @return The list to control this list's data.
	 */
	public List<E> control(org.muis.core.rx.DefaultObservable.OnSubscribe<ObservableElement<E>> onSubscribe) {
		if(hasIssuedController.getAndSet(true))
			throw new IllegalStateException("This observable list is already controlled");
		theOnSubscribe = onSubscribe;
		return new ObservableListController();
	}

	@Override
	public Runnable internalSubscribe(Observer<? super ObservableElement<E>> observer) {
		ConcurrentLinkedQueue<Runnable> subSubscriptions = new ConcurrentLinkedQueue<>();
		theObservers.put(observer, subSubscriptions);
		doLocked(() -> {
			for(ObservableElementImpl<E> el : theElements)
				observer.onNext(newValue(el, subSubscriptions));
		}, false);
		if(theOnSubscribe != null)
			theOnSubscribe.onsubscribe(observer);
		return () -> {
			ConcurrentLinkedQueue<Runnable> subs = theObservers.remove(observer);
			for(Runnable sub : subs)
				sub.run();
		};
	}

	private ObservableListElement<E> newValue(ObservableElementImpl<E> el, ConcurrentLinkedQueue<Runnable> observers) {
		return new ObservableListElement<E>() {
			private int theRemovedIndex = -1;

			@Override
			public Type getType() {
				return el.getType();
			}

			@Override
			public E get() {
				return el.get();
			}

			@Override
			public Runnable internalSubscribe(Observer<? super ObservableValueEvent<E>> observer) {
				ObservableValue<E> element = this;
				Runnable ret = el.internalSubscribe(new Observer<ObservableValueEvent<E>>() {
					@Override
					public <V extends ObservableValueEvent<E>> void onNext(V event) {
						observer.onNext(new ObservableValueEvent<>(element, event.getOldValue(), event.getValue(), event.getCause()));
					}

					@Override
					public <V extends ObservableValueEvent<E>> void onCompleted(V event) {
						observer.onCompleted(new ObservableValueEvent<>(element, event.getOldValue(), event.getValue(), event.getCause()));
					}

					@Override
					public void onError(Throwable e) {
						observer.onError(e);
					}
				});
				observers.add(ret);
				return ret;
			}

			@Override
			public ObservableValue<E> persistent() {
				return el;
			}

			@Override
			public int getIndex() {
				int ret = theElements.indexOf(el);
				if(ret < 0)
					ret = theRemovedIndex;
				if(ret < 0 && theRemovedElement == el)
					ret = theRemovedIndex = theRemovedElementIndex;
				return ret;
			}

			@Override
			public String toString() {
				return getType() + " list[" + getIndex() + "]";
			}
		};
	}

	private void fireNewElement(ObservableElementImpl<E> el) {
		for(Map.Entry<Observer<? super ObservableElement<E>>, ConcurrentLinkedQueue<Runnable>> observer : theObservers.entrySet()) {
			observer.getKey().onNext(newValue(el, observer.getValue()));
		}
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
	protected DefaultObservableList<E> clone() throws CloneNotSupportedException {
		DefaultObservableList<E> ret = (DefaultObservableList<E>) super.clone();
		ret.theValues = (ArrayList<E>) theValues.clone();
		ret.theElements = new ArrayList<>();
		for(E el : ret.theValues)
			ret.theElements.add(new ObservableElementImpl<>(theType, el));
		ret.theLock = new ReentrantReadWriteLock();
		ret.hasIssuedController = new AtomicBoolean(false);
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
				ObservableElementImpl<E> add = new ObservableElementImpl<>(theType, e);
				theElements.add(add);
				fireNewElement(add);
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
				theRemovedElement = theElements.remove(idx);
				theRemovedElementIndex = idx;
				theRemovedElement.remove();
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
					ObservableElementImpl<E> newWrapper = new ObservableElementImpl<>(theType, e);
					theElements.add(newWrapper);
					fireNewElement(newWrapper);
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
					ObservableElementImpl<E> newWrapper = new ObservableElementImpl<>(theType, e);
					theElements.add(newWrapper);
					fireNewElement(newWrapper);
					idx++;
				}
				for(int i = idx; i < theValues.size(); i++) {
					ObservableElementImpl<E> wrapper = new ObservableElementImpl<>(theType, theValues.get(i));
					theElements.add(wrapper);
					fireNewElement(wrapper);
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
						theRemovedElement = theElements.remove(idx);
						theRemovedElementIndex = idx;
						theRemovedElement.remove();
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
							theRemovedElement = theElements.remove(i);
							theRemovedElementIndex = i;
							theRemovedElement.remove();
						}
				}
			}, true);
			return ret[0];
		}

		@Override
		public void clear() {
			doLocked(() -> {
				theValues.clear();
				ArrayList<ObservableElementImpl<E>> remove = new ArrayList<>();
				remove.addAll(theElements);
				theElements.clear();
				for(int i = remove.size() - 1; i >= 0; i--) {
					theRemovedElement = remove.get(i);
					theRemovedElementIndex = i;
					theRemovedElement.remove();
				}
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
				ObservableElementImpl<E> newWrapper = new ObservableElementImpl<>(theType, element);
				theElements.add(newWrapper);
				fireNewElement(newWrapper);
				idx++;
				for(int i = idx; i < theValues.size(); i++) {
					ObservableElementImpl<E> wrapper = new ObservableElementImpl<>(theType, theValues.get(i));
					theElements.add(wrapper);
					fireNewElement(wrapper);
				}
			}, true);
		}

		@Override
		public E remove(int index) {
			Object [] ret = new Object[1];
			doLocked(() -> {
				ret[0] = theValues.remove(index);
				theRemovedElement = theElements.remove(index);
				theRemovedElementIndex = index;
				theRemovedElement.remove();
			}, true);
			return (E) ret[0];
		}
	}
}
