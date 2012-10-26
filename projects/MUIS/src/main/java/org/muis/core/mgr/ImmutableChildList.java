package org.muis.core.mgr;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.muis.core.MuisElement;
import org.muis.core.event.MuisEventListener;
import org.muis.core.event.MuisEventType;

/** An augmented, immutable list of an element's children */
public class ImmutableChildList implements ElementList<MuisElement> {
	private final ChildList theContents;

	/** @param contents The ChildList that governs an element's children */
	public ImmutableChildList(ChildList contents) {
		theContents = contents;
	}

	@Override
	public MuisElement getParent() {
		return theContents.getParent();
	}

	@Override
	public MuisElement getLast() {
		return theContents.getLast();
	}

	@Override
	public final MuisElement [] at(int x, int y) {
		return theContents.at(x, y);
	}

	@Override
	public MuisElement [] sortByZ() {
		return theContents.sortByZ();
	}

	@Override
	public final <T> void addChildListener(MuisEventType<T> type, MuisEventListener<? super T> listener) {
		theContents.addChildListener(type, listener);
	}

	@Override
	public final void removeChildListener(MuisEventListener<?> listener) {
		theContents.removeChildListener(listener);
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
	public Iterator<MuisElement> iterator() {
		return new Iterator<MuisElement>() {
			private Iterator<MuisElement> theWrapped = theContents.iterator();

			@Override
			public boolean hasNext() {
				return theWrapped.hasNext();
			}

			@Override
			public MuisElement next() {
				return theWrapped.next();
			}

			@Override
			public void remove() {
				throwUnsupported();
			}
		};
	}

	@Override
	public MuisElement [] toArray() {
		return theContents.toArray();
	}

	@Override
	public <T> T [] toArray(T [] a) {
		return theContents.toArray(a);
	}

	@Override
	public boolean add(MuisElement e) {
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
	public boolean addAll(Collection<? extends MuisElement> c) {
		throwUnsupported();
		return false;
	}

	@Override
	public boolean addAll(int index, Collection<? extends MuisElement> c) {
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
	public MuisElement get(int index) {
		return theContents.get(index);
	}

	@Override
	public MuisElement set(int index, MuisElement element) {
		throwUnsupported();
		return null;
	}

	@Override
	public void add(int index, MuisElement element) {
		throwUnsupported();
	}

	@Override
	public MuisElement remove(int index) {
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
	public ListIterator<MuisElement> listIterator() {
		return new ImmutableChildListIterator(theContents.listIterator());
	}

	@Override
	public ListIterator<MuisElement> listIterator(int index) {
		return new ImmutableChildListIterator(theContents.listIterator(index));
	}

	@Override
	public List<MuisElement> subList(int fromIndex, int toIndex) {
		return java.util.Collections.unmodifiableList(theContents.subList(fromIndex, toIndex));
	}

	static void throwUnsupported() {
		throw new UnsupportedOperationException("A " + MuisElement.class
			+ "'s set of children may not be modified through the getChildren() or ch() list");
	}

	private static class ImmutableChildListIterator implements ListIterator<MuisElement> {
		private final ListIterator<MuisElement> theWrapped;

		ImmutableChildListIterator(ListIterator<MuisElement> wrap) {
			theWrapped = wrap;
		}

		@Override
		public boolean hasNext() {
			return theWrapped.hasNext();
		}

		@Override
		public MuisElement next() {
			return theWrapped.next();
		}

		@Override
		public boolean hasPrevious() {
			return theWrapped.hasPrevious();
		}

		@Override
		public MuisElement previous() {
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
		public void set(MuisElement e) {
			throwUnsupported();
		}

		@Override
		public void add(MuisElement e) {
			throwUnsupported();
		}
	}
}
