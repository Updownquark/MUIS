package org.quick.util;

import java.util.*;

import org.quick.core.QuickElement;
import org.quick.core.event.ChildEvent;
import org.quick.core.mgr.ElementList;

/**
 * Wraps a backing list and reflects only certain elements based on a filter
 *
 * @param <E> The element type of the list
 */
public abstract class FilteredElementList<E extends QuickElement> extends org.quick.core.mgr.AbstractElementList<E> {
	private final ElementList<? extends E> theBacking;

	/**
	 * @param parent The parent for this list
	 * @param backing The backing list to filter
	 */
	public FilteredElementList(QuickElement parent, ElementList<? extends E> backing) {
		super(parent);
		theBacking = backing;
		theBacking.act(new org.observe.Action<ChildEvent>() {
			@Override
			public void act(ChildEvent evt) {
				int index = -1;
				int movedFromIndex = -1;
				switch (evt.getType()) {
				case ADD:
					index = 0;
					for(int i = 0; i < evt.getPostChangeContent().size() && evt.getPostChangeContent().get(i) != evt.getChild(); i++)
						if(filter((E) evt.getPostChangeContent().get(i)))
							index++;
					movedFromIndex = index;
					break;
				case REMOVE:
					index = 0;
					for(int i = 0; i < evt.getPreChangeContent().size() && evt.getPreChangeContent().get(i) != evt.getChild(); i++)
						if(filter((E) evt.getPreChangeContent().get(i)))
							index++;
					movedFromIndex = index;
					break;
				case MOVE:
					index = 0;
					for(int i = 0; i < evt.getPostChangeContent().size() && evt.getPostChangeContent().get(i) != evt.getChild(); i++)
						if(filter((E) evt.getPostChangeContent().get(i)))
							index++;
					movedFromIndex = 0;
					for(int i = 0; i < evt.getPreChangeContent().size() && evt.getPreChangeContent().get(i) != evt.getChild(); i++)
						if(filter((E) evt.getPreChangeContent().get(i)))
							movedFromIndex++;
					break;
				}
				fireEvent(new ChildEvent(getParent(), evt.getChild(), index, movedFromIndex, evt.getType(), filterList(evt
					.getPreChangeContent()), filterList(evt.getPostChangeContent())));
			}

			org.quick.core.mgr.ImmutableChildList<QuickElement> filterList(org.quick.core.mgr.ImmutableChildList<QuickElement> list) {
				return new org.quick.core.mgr.ImmutableChildList<>(new FilteredElementList<QuickElement>(getParent(), list) {
					@Override
					protected boolean filter(QuickElement element) {
						return baseFilter((E) element);
					}

					@Override
					protected void assertMutable() throws UnsupportedOperationException {
					}

					@Override
					protected void assertFits(QuickElement e) throws IllegalArgumentException {
					}

					@Override
					protected QuickElement [] newArray(int size) {
						return baseNewArray(size);
					}
				});
			}

			boolean baseFilter(E element) {
				return filter(element);
			}

			E [] baseNewArray(int size) {
				return newArray(size);
			}
		});
	}

	/**
	 * @param element The element from the backing array to test
	 * @return Whether the given element should be reflected in this list
	 */
	protected abstract boolean filter(E element);

	/** @throws UnsupportedOperationException If this list is not mutable */
	protected abstract void assertMutable() throws UnsupportedOperationException;

	/**
	 * @param e The element to test
	 * @throws IllegalArgumentException If the given element may not be added to this list
	 */
	protected abstract void assertFits(QuickElement e) throws IllegalArgumentException;

	/**
	 * @param size The size of the array to create
	 * @return A new array of this list's element type with the given size
	 */
	protected abstract E [] newArray(int size);

	@Override
	public int size() {
		int ret = 0;
		for(E child : theBacking)
			if(filter(child))
				ret++;
		return ret;
	}

	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			private final Iterator<? extends E> theBackingIter = theBacking.iterator();

			private E theNext;

			private boolean isRemovable;

			@Override
			public boolean hasNext() {
				isRemovable = false;
				while(theNext == null && theBackingIter.hasNext()) {
					E next = theBackingIter.next();
					if(filter(next))
						theNext = next;
				}
				return theNext != null;
			}

			@Override
			public E next() {
				if(theNext == null && !hasNext())
					throw new java.util.NoSuchElementException();
				E ret = theNext;
				theNext = null;
				isRemovable = true;
				return ret;
			}

			@Override
			public void remove() {
				if(!isRemovable)
					throw new IllegalStateException("next() must be called before remove()");
				isRemovable = false;
				assertMutable();
				theBackingIter.remove();
			}
		};
	}

	@Override
	public boolean add(E e) {
		assertMutable();
		assertFits(e);
		ListIterator<E> iter = listIterator();
		while(iter.hasNext())
			iter.next();
		iter.add(e);
		return true;
	}

	@Override
	public boolean remove(Object o) {
		assertMutable();
		Iterator<E> iter = iterator();
		boolean found = false;
		while(iter.hasNext()) {
			if(iter.next() == o) {
				iter.remove();
				found = true;
				break;
			}
		}
		return found;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		Collection<?> copy = new HashSet<>(c);
		for(E el : this) {
			copy.remove(el);
			if(copy.isEmpty())
				break;
		}
		return copy.isEmpty();
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		assertMutable();
		for(E e : c)
			assertFits(e);
		ListIterator<E> iter = listIterator();
		while(iter.hasNext())
			iter.next();
		for(E e : c)
			iter.add(e);
		return true;
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		assertMutable();
		for(E e : c)
			assertFits(e);
		ListIterator<E> iter = listIterator(index);
		for(E e : c)
			iter.add(e);
		return true;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		assertMutable();
		Iterator<E> iter = iterator();
		boolean found = false;
		while(iter.hasNext()) {
			if(c.contains(iter.next())) {
				iter.remove();
				found = true;
			}
		}
		return found;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		assertMutable();
		Iterator<E> iter = iterator();
		boolean found = false;
		while(iter.hasNext()) {
			if(!c.contains(iter.next())) {
				iter.remove();
				found = true;
			}
		}
		return found;
	}

	@Override
	public void clear() {
		assertMutable();
		Iterator<E> iter = iterator();
		while(iter.hasNext()) {
			iter.remove();
		}
	}

	@Override
	public E get(int index) {
		for(E el : this) {
			if(index == 0)
				return el;
			index--;
		}
		throw new IndexOutOfBoundsException();
	}

	@Override
	public E set(int index, E element) {
		assertMutable();
		assertFits(element);
		ListIterator<E> iter = listIterator(index);
		if(!iter.hasNext())
			throw new IndexOutOfBoundsException("" + index);
		E ret = iter.next();
		iter.set(element);
		return ret;
	}

	@Override
	public void add(int index, E element) {
		assertMutable();
		assertFits(element);
		ListIterator<E> iter = listIterator(index);
		iter.add(element);
	}

	@Override
	public E remove(int index) {
		assertMutable();
		Iterator<E> iter = iterator();
		int idx = index;
		E ret = null;
		while(idx >= 0 && iter.hasNext()) {
			ret = iter.next();
		}
		if(idx >= 0)
			throw new IndexOutOfBoundsException(index + " of " + (index - idx));
		iter.remove();
		return ret;
	}

	@Override
	public int indexOf(Object o) {
		int index = 0;
		for(E el : this) {
			if(el == o)
				return index;
			index++;
		}
		return -1;
	}

	@Override
	public ListIterator<E> listIterator() {
		return new ListIterator<E>() {
			private final ListIterator<? extends E> theBackingIter = theBacking.listIterator();

			private E theNext;

			private E thePrev;

			private int theIndex;

			private boolean isRemovable;

			@Override
			public boolean hasNext() {
				isRemovable = false;
				while(theNext == null && theBackingIter.hasNext()) {
					E next = theBackingIter.next();
					if(filter(next))
						theNext = next;
				}
				return theNext != null;
			}

			@Override
			public E next() {
				if(theNext == null && !hasNext())
					throw new java.util.NoSuchElementException();
				E ret = theNext;
				theNext = null;
				isRemovable = true;
				theIndex++;
				return ret;
			}

			@Override
			public boolean hasPrevious() {
				isRemovable = false;
				while(thePrev == null && theBackingIter.hasPrevious()) {
					E prev = theBackingIter.previous();
					if(filter(prev))
						thePrev = prev;
				}
				return thePrev != null;
			}

			@Override
			public E previous() {
				if(thePrev == null && !hasPrevious())
					throw new java.util.NoSuchElementException();
				E ret = thePrev;
				thePrev = null;
				isRemovable = true;
				theIndex--;
				return ret;
			}

			@Override
			public int nextIndex() {
				return theIndex;
			}

			@Override
			public int previousIndex() {
				return theIndex - 1;
			}

			@Override
			public void remove() {
				if(!isRemovable)
					throw new IllegalStateException("next() or previous() must be called before remove()");
				assertMutable();
				isRemovable = false;
				theIndex--;
				theBackingIter.remove();
			}

			@Override
			public void set(E e) {
				if(!isRemovable)
					throw new IllegalStateException("next() or previous() must be called before set()");
				assertMutable();
				assertFits(e);
				((ListIterator<E>) theBackingIter).set(e);
			}

			@Override
			public void add(E e) {
				assertMutable();
				assertFits(e);
				isRemovable = false;
				theIndex++;
				((ListIterator<E>) theBackingIter).add(e);
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
		return new org.quick.core.mgr.SubList<>(this, fromIndex, toIndex);
	}

	@Override
	public E [] toArray() {
		ArrayList<E> ret = new ArrayList<>();
		for(E el : this)
			ret.add(el);
		return ret.toArray(newArray(ret.size()));
	}

	@Override
	public boolean addAll(QuickElement [] children) {
		assertMutable();
		for(QuickElement child : children)
			assertFits(child);
		ListIterator<E> iter = listIterator();
		while(iter.hasNext())
			iter.next();
		for(QuickElement child : children)
			iter.add((E) child);
		return children.length > 0;
	}

	@Override
	public boolean addAll(int index, QuickElement [] children) {
		assertMutable();
		for(QuickElement child : children)
			assertFits(child);
		ListIterator<E> iter = listIterator(index);
		for(QuickElement child : children)
			iter.add((E) child);
		return children.length > 0;
	}
}
