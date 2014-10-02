package org.muis.core.rx;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ObservableSet<E> implements Observable<Observable<E>> {
	private List<E> theValues;
	private List<ObservableElement<E>> theElements;
	private boolean isOrdered;

	private ReentrantReadWriteLock theLock;
	private AtomicBoolean hasIssuedController = new AtomicBoolean(false);
	private DefaultObservable<Observable<E>> theBackingObservable;
	private Observer<Observable<E>> theBackingController;
	private int theModCount;

	public ObservableSet(boolean ordered) {
		theValues = new ArrayList<>();
		theElements = new ArrayList<>();
		isOrdered = ordered;

		theLock = new ReentrantReadWriteLock();
		theBackingObservable = new DefaultObservable<>();
		theBackingController = theBackingObservable.control(subscriber -> {
			doLocked(() -> {
				for(ObservableElement<E> el : theElements)
					subscriber.onNext(el);
			}, false);
		});
	}

	void doLocked(Runnable action, boolean write) {
		Lock lock = write ? theLock.writeLock() : theLock.readLock();
		lock.lock();
		try {
			action.run();
		} finally {
			lock.unlock();
		}
	}

	public List<E> control() {
		if(hasIssuedController.getAndSet(true))
			throw new IllegalStateException("This observable set is already controlled");
		return new ObservableSetController();
	}

	private class ObservableSetController implements List<E> {
		@Override
		public int size() {
			return theValues.size();
		}

		@Override
		public boolean isEmpty() {
			return theValues.isEmpty();
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
		public Iterator<E> iterator() {
			return new Iterator<E>() {
				private final Iterator<E> backing = theValues.iterator();

				private int thePosition;

				private int theModRef=theModCount;

				@Override
				public boolean hasNext() {
					boolean [] ret=new boolean[1];
					doLocked(() -> {
						if(theModRef != theModCount)
							throw new java.util.ConcurrentModificationException();
						ret[0] = backing.hasNext();
					}, false);
					return ret[0];
				}

				@Override
				public E next() {
					Object [] ret=new Object[1];
					doLocked(()->{
						if(theModRef != theModCount)
							throw new java.util.ConcurrentModificationException();
						thePosition++;
						ret[0] = backing.next();
					}, false);
					return (E) ret[0];
				}

				@Override
				public void remove() {
					ObservableElement<E> [] remove = new ObservableElement[1];
					doLocked(() -> {
						if(theModRef != theModCount)
							throw new java.util.ConcurrentModificationException();
						theModCount++;
						theModRef++;
						backing.remove();
						remove[0] = theElements.remove(thePosition);
					}, true);
					remove[0].remove();
				}
			};
		}

		@Override
		public boolean add(E e) {
			ObservableElement<E> [] add = new ObservableElement[1];
			doLocked(() -> {
				theModCount++;
				theValues.add(e);
				add[0] = new ObservableElement<>(e);
				theElements.add(add[0]);
			}, true);
			theBackingController.onNext(add[0]);
			return true;
		}

		@Override
		public boolean remove(Object o) {
			boolean [] ret = new boolean[1];
			ObservableElement<E> [] remove = new ObservableElement[1];
			doLocked(() -> {
				int idx = theValues.indexOf(o);
				if(idx < 0) {
					ret[0] = false;
					return;
				}
				ret[0] = true;
				theModCount++;
				theValues.remove(idx);
				remove[0] = theElements.remove(idx);
			}, true);
			remove[0].remove();
			return ret[0];
		}

		@Override
		public boolean addAll(Collection<? extends E> c) {
			if(c.isEmpty())
				return false;
			ArrayList<ObservableElement<E>> add = new ArrayList<>();
			doLocked(() -> {
				theModCount++;
				for(E e : c) {
					theValues.add(e);
					ObservableElement<E> newWrapper = new ObservableElement<>(e);
					theElements.add(newWrapper);
					add.add(newWrapper);
				}
			}, true);
			for(ObservableElement<E> el : add)
				theBackingController.onNext(el);
			return true;
		}

		@Override
		public boolean addAll(int index, Collection<? extends E> c) {
			if(c.isEmpty())
				return false;
			ArrayList<ObservableElement<E>> remove = new ArrayList<>();
			ArrayList<ObservableElement<E>> add = new ArrayList<>();
			doLocked(() -> {
				theModCount++;
				if(isOrdered)
					for(int i = theValues.size() - 1; i >= index; i--)
						remove.add(theElements.remove(i));
				int idx = index;
				for(E e : c) {
					theValues.add(idx, e);
					ObservableElement<E> newWrapper = new ObservableElement<>(e);
					theElements.add(newWrapper);
					add.add(newWrapper);
					idx++;
				}
				if(isOrdered)
					for(int i = idx; i < theValues.size(); i++) {
						ObservableElement<E> wrapper = new ObservableElement<>(theValues.get(i));
						theElements.add(wrapper);
						add.add(wrapper);
					}
			}, true);
			for(ObservableElement<E> el : remove)
				el.remove();
			for(ObservableElement<E> el : add)
				theBackingController.onNext(el);
			return true;
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			if(c.isEmpty())
				return false;
			boolean [] ret = new boolean[] {false};
			ArrayList<ObservableElement<E>> remove = new ArrayList<>();
			doLocked(() -> {
				for(Object o : c) {
					int idx = theValues.indexOf(o);
					if(idx >= 0) {
						ret[0] = true;
						theModCount++;
						theValues.remove(idx);
						remove.add(theElements.remove(idx));
					}
				}
				if(ret[0])
					theModCount++;
			}, true);
			for(ObservableElement<E> el : remove)
				el.remove();
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
			ArrayList<ObservableElement<E>> remove = new ArrayList<>();
			doLocked(() -> {
				BitSet keep = new BitSet();
				for(Object o : c){
					int idx=theValues.indexOf(o);
					if(idx >= 0)
						keep.set(idx);
				}
				ret[0] = keep.nextClearBit(0) < theValues.size();
				if(ret[0]) {
					for(int i = 0; i < theValues.size(); i++)
						if(!keep.get(i)) {
							theValues.remove(i);
							remove.add(theElements.remove(i));
						}
				}
				if(ret[0])
					theModCount++;
			}, true);
			for(ObservableElement<E> el : remove)
				el.remove();
			return ret[0];
		}

		@Override
		public void clear() {
			ArrayList<ObservableElement<E>> remove = new ArrayList<>();
			doLocked(() -> {
				theValues.clear();
				remove.addAll(theElements);
				theElements.clear();
			}, true);
			for(ObservableElement<E> el : remove)
				el.remove();
		}

		@Override
		public E set(int index, E element) {
			Object [] ret = new Object[1];
			ObservableElement<E> [] set = new ObservableElement[1];
			doLocked(() -> {
				ret[0] = theValues.set(index, element);
				set[0] = theElements.get(index);
			}, true);
			set[0].set(element);
			return (E) ret[0];
		}

		@Override
		public void add(int index, E element) {
			ArrayList<ObservableElement<E>> remove = new ArrayList<>();
			ArrayList<ObservableElement<E>> add = new ArrayList<>();
			doLocked(() -> {
				theModCount++;
				if(isOrdered)
					for(int i = theValues.size() - 1; i >= index; i--)
						remove.add(theElements.remove(i));
				int idx = index;
				theValues.add(idx, element);
				ObservableElement<E> newWrapper = new ObservableElement<>(element);
				theElements.add(newWrapper);
				add.add(newWrapper);
				idx++;
				if(isOrdered)
					for(int i = idx; i < theValues.size(); i++) {
						ObservableElement<E> wrapper = new ObservableElement<>(theValues.get(i));
						theElements.add(wrapper);
						add.add(wrapper);
					}
			}, true);
			for(ObservableElement<E> el : remove)
				el.remove();
			for(ObservableElement<E> el : add)
				theBackingController.onNext(el);
		}

		@Override
		public E remove(int index) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ListIterator<E> listIterator() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ListIterator<E> listIterator(int index) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<E> subList(int fromIndex, int toIndex) {
			// TODO Auto-generated method stub
			return null;
		}
	}

	private static class ObservableElement<T> extends DefaultObservable<T> {
		private Observer<T> theController;
		private T theValue;

		ObservableElement(T value) {
			theValue = value;
			theController = control(subscriber -> {
				subscriber.onNext(theValue);
			});
		}

		void set(T newValue) {
			theValue = newValue;
			theController.onNext(newValue);
		}

		void remove() {
			theController.onCompleted();
		}
	}
}
