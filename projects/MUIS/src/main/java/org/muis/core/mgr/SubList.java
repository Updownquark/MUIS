package org.muis.core.mgr;

import java.util.*;

/**
 * A sub-list implementation that reflects only a certain range of a list
 *
 * @param <E> The type of elements in the list
 */
public class SubList<E> implements List<E> {
	private final List<E> theBacking;

	private int theStart;

	private int theEnd;

	/**
	 * @param backing The list to back this sub list
	 * @param start The starting index for this list to reflect
	 * @param end The end index for this list to reflect
	 */
	public SubList(List<E> backing, int start, int end) {
		theBacking = backing;
		theStart = start;
		theEnd = end;
		int size = backing.size();
		if(start < 0 || end >= size || end < start)
			throw new IndexOutOfBoundsException(start + " to " + end + " of " + size);
	}

	@Override
	public int size() {
		return theEnd - theStart;
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public boolean contains(Object o) {
		for(E val : this)
			if(Objects.equals(val, o))
				return true;
		return false;
	}

	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			private final Iterator<E> theBackingIter = theBacking.iterator();

			int theIndex;

			{
				for(theIndex = 0; theIndex < theStart; theIndex++) {
					if(theBackingIter.hasNext())
						throw new IndexOutOfBoundsException("Backing list no longer contains range " + theStart + " to " + theEnd);
					theBackingIter.next();
				}
			}

			@Override
			public boolean hasNext() {
				if(theIndex >= theStart)
					return false;
				return theBackingIter.hasNext();
			}

			@Override
			public E next() {
				if(theIndex >= theStart)
					throw new java.util.NoSuchElementException();
				return theBackingIter.next();
			}

			@Override
			public void remove() {
				theBackingIter.remove();
				theEnd--;
			}
		};
	}

	@Override
	public Object [] toArray() {
		ArrayList<Object> ret = new ArrayList<>(size());
		for(E val : this)
			ret.add(val);
		return ret.toArray(new Object[ret.size()]);
	}

	@Override
	public <T> T [] toArray(T [] a) {
		ArrayList<Object> ret = new ArrayList<>(size());
		for(E val : this)
			ret.add(val);
		return ret.toArray(a);
	}

	@Override
	public boolean add(E e) {
		ListIterator<E> iter = listIterator(size());
		iter.add(e);
		theEnd++;
		return true;
	}

	@Override
	public boolean remove(Object o) {
		Iterator<E> iter = iterator();
		while(iter.hasNext()) {
			if(Objects.equals(iter.next(), o)) {
				iter.remove();
				theEnd--;
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		Collection<?> copy = new HashSet<>(c);
		for(E val : this) {
			copy.remove(val);
		}
		return copy.isEmpty();
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		ListIterator<E> iter = theBacking.listIterator(theEnd);
		for(E val : c)
			iter.add(val);
		theEnd += c.size();
		return true;
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		ListIterator<E> iter = theBacking.listIterator(theStart + index);
		for(E val : c)
			iter.add(val);
		theEnd += c.size();
		return true;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		Iterator<E> iter = iterator();
		boolean found = false;
		while(iter.hasNext()) {
			if(c.contains(iter.next())) {
				iter.remove();
				theEnd--;
				found = true;
				return true;
			}
		}
		return found;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		Iterator<E> iter = iterator();
		boolean found = false;
		while(iter.hasNext()) {
			if(!c.contains(iter.next())) {
				iter.remove();
				theEnd--;
				found = true;
				return true;
			}
		}
		return found;
	}

	@Override
	public void clear() {
		Iterator<E> iter = iterator();
		while(iter.hasNext()) {
			iter.remove();
		}
		theEnd = theStart;
	}

	@Override
	public E get(int index) {
		int idx = 0;
		for(E val : this) {
			if(idx == index)
				return val;
			idx++;
		}
		throw new IndexOutOfBoundsException(index + " of " + idx);
	}

	@Override
	public E set(int index, E element) {
		ListIterator<E> iter = listIterator(index);
		if(!iter.hasNext())
			throw new IndexOutOfBoundsException("" + index);
		E ret = iter.next();
		iter.set(element);
		return ret;
	}

	@Override
	public void add(int index, E element) {
		ListIterator<E> iter = listIterator(index);
		iter.add(element);
		theEnd++;
	}

	@Override
	public E remove(int index) {
		int idx = 0;
		Iterator<E> iter = iterator();
		while(iter.hasNext()) {
			E ret = iter.next();
			if(idx == index) {
				iter.remove();
				return ret;
			}
			idx++;
		}
		throw new IndexOutOfBoundsException(index + " of " + idx);
	}

	@Override
	public int indexOf(Object o) {
		int idx = 0;
		for(E val : this) {
			if(Objects.equals(val, o))
				return idx;
			idx++;
		}
		return -1;
	}

	@Override
	public int lastIndexOf(Object o) {
		int idx = 0;
		int lastHit = -1;
		for(E val : this) {
			if(Objects.equals(val, o))
				lastHit = idx;
			idx++;
		}
		return lastHit;
	}

	@Override
	public ListIterator<E> listIterator() {
		return new ListIterator<E>() {
			private final ListIterator<E> theBackingIter = theBacking.listIterator();

			private int theIndex;

			private boolean isRemovable;

			{
				for(theIndex = 0; theIndex < theStart; theIndex++) {
					if(theBackingIter.hasNext())
						throw new IndexOutOfBoundsException("Backing list no longer contains range " + theStart + " to " + theEnd);
					theBackingIter.next();
				}
			}

			@Override
			public boolean hasNext() {
				if(theIndex >= theEnd)
					return false;
				return theBackingIter.hasNext();
			}

			@Override
			public E next() {
				if(theIndex >= theEnd)
					throw new java.util.NoSuchElementException();
				return theBackingIter.next();
			}

			@Override
			public boolean hasPrevious() {
				if(theIndex <= theStart)
					return false;
				return theBackingIter.hasPrevious();
			}

			@Override
			public E previous() {
				if(theIndex <= theStart)
					throw new java.util.NoSuchElementException();
				return theBackingIter.previous();
			}

			@Override
			public int nextIndex() {
				return theBackingIter.nextIndex() - theStart;
			}

			@Override
			public int previousIndex() {
				return theBackingIter.previousIndex() - theStart;
			}

			@Override
			public void remove() {
				if(!isRemovable)
					throw new IllegalStateException("next() or previous() must be called before remove()");
				theBackingIter.remove();
				theEnd--;
			}

			@Override
			public void set(E e) {
				if(!isRemovable)
					throw new IllegalStateException("next() or previous() must be called before set()");
				theBackingIter.set(e);
			}

			@Override
			public void add(E e) {
				theBackingIter.add(e);
				theEnd++;
			}
		};
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		ListIterator<E> ret = listIterator();
		int idx = index;
		while(idx > 0 && ret.hasNext()) {
			idx--;
			ret.next();
		}
		if(idx > 0)
			throw new IndexOutOfBoundsException(index + " of " + (index - idx));
		return ret;
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		if(fromIndex < 0 || toIndex > (theEnd - theStart) || toIndex < fromIndex)
			throw new IndexOutOfBoundsException(fromIndex + " to " + toIndex + " of " + (theEnd - theStart));
		int size = theBacking.size();
		if(size < theStart + toIndex)
			throw new IndexOutOfBoundsException("Backing list no longer contains sufficient range");
		return theBacking.subList(theStart + fromIndex, theStart + toIndex);
	}
}
