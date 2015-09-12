package org.quick.core.mgr;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.observe.Observable;
import org.observe.Observer;
import org.observe.Subscription;
import org.quick.core.QuickElement;
import org.quick.core.event.ChildEvent;
import org.quick.core.event.QuickEvent;

/**
 * An augmented, immutable list of elements
 *
 * @param <E> The type of element in the list
 */
public class ImmutableChildList<E extends QuickElement> implements ElementList<E> {
	private final ElementList<E> theContents;

	/** @param contents The list to wrap */
	public ImmutableChildList(ElementList<E> contents) {
		theContents = contents;
	}

	@Override
	public Subscription subscribe(Observer<? super ChildEvent> observer) {
		return theContents.subscribe(observer);
	}

	@Override
	public QuickElement getParent() {
		return theContents.getParent();
	}

	@Override
	public QuickElement getLast() {
		return theContents.getLast();
	}

	@Override
	public final QuickElement [] at(int x, int y) {
		return theContents.at(x, y);
	}

	@Override
	public QuickElement [] sortByZ() {
		return theContents.sortByZ();
	}

	@Override
	public Observable<QuickEvent> events() {
		return theContents.events();
	}

	@Override
	public boolean addAll(QuickElement [] children) {
		throwUnsupported();
		return false;
	}

	@Override
	public boolean addAll(int index, QuickElement [] children) {
		throwUnsupported();
		return false;
	}

	@Override
	public int size() {
		return theContents.size();
	}

	@Override
	public boolean isEmpty() {
		return theContents.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return theContents.contains(o);
	}

	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			private Iterator<E> theWrapped = theContents.iterator();

			@Override
			public boolean hasNext() {
				return theWrapped.hasNext();
			}

			@Override
			public E next() {
				return theWrapped.next();
			}

			@Override
			public void remove() {
				throwUnsupported();
			}
		};
	}

	@Override
	public E [] toArray() {
		return theContents.toArray();
	}

	@Override
	public <T> T [] toArray(T [] a) {
		return theContents.toArray(a);
	}

	@Override
	public boolean add(QuickElement e) {
		throwUnsupported();
		return false;
	}

	@Override
	public boolean remove(Object o) {
		throwUnsupported();
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return theContents.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		throwUnsupported();
		return false;
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		throwUnsupported();
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throwUnsupported();
		return false;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throwUnsupported();
		return false;
	}

	@Override
	public void clear() {
		throwUnsupported();
	}

	@Override
	public E get(int index) {
		return theContents.get(index);
	}

	@Override
	public QuickElement set(int index, QuickElement element) {
		throwUnsupported();
		return null;
	}

	@Override
	public void add(int index, QuickElement element) {
		throwUnsupported();
	}

	@Override
	public E remove(int index) {
		throwUnsupported();
		return null;
	}

	@Override
	public int indexOf(Object o) {
		return theContents.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return theContents.lastIndexOf(o);
	}

	@Override
	public ListIterator<E> listIterator() {
		return new ImmutableChildListIterator<>(theContents.listIterator());
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return new ImmutableChildListIterator<>(theContents.listIterator(index));
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return java.util.Collections.unmodifiableList(theContents.subList(fromIndex, toIndex));
	}

	static void throwUnsupported() {
		throw new UnsupportedOperationException("A " + QuickElement.class
			+ "'s set of children may not be modified through the getChildren() or ch() list");
	}

	private static class ImmutableChildListIterator<E extends QuickElement> implements ListIterator<E> {
		private final ListIterator<E> theWrapped;

		ImmutableChildListIterator(ListIterator<E> wrap) {
			theWrapped = wrap;
		}

		@Override
		public boolean hasNext() {
			return theWrapped.hasNext();
		}

		@Override
		public E next() {
			return theWrapped.next();
		}

		@Override
		public boolean hasPrevious() {
			return theWrapped.hasPrevious();
		}

		@Override
		public E previous() {
			return theWrapped.previous();
		}

		@Override
		public int nextIndex() {
			return theWrapped.nextIndex();
		}

		@Override
		public int previousIndex() {
			return theWrapped.previousIndex();
		}

		@Override
		public void remove() {
			throwUnsupported();
		}

		@Override
		public void set(QuickElement e) {
			throwUnsupported();
		}

		@Override
		public void add(QuickElement e) {
			throwUnsupported();
		}
	}
}
