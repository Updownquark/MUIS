package org.quick.core.mgr;

import org.observe.collect.ObservableList;
import org.observe.collect.ObservableOrderedCollection;
import org.qommons.Transaction;
import org.quick.core.QuickElement;

/**
 * A list of elements (potentially a particular type of element) with a few extra operations available
 *
 * @param <E> The type of element
 */
public interface ElementList<E extends QuickElement> extends ObservableList<E> {
	/** @return The parent whose children this list manages */
	QuickElement getParent();

	/**
	 * Moves an element from one index to another
	 * 
	 * @param element The element to move
	 * @param index The new index for the element
	 * @return The previous index of the element
	 * @throws IllegalArgumentException If the element was not contained in the list
	 */
	default int move(E element, int index) {
		try (Transaction t = lock(true, false)) {
			if (index >= size())
				throw new IndexOutOfBoundsException(index + " of " + size());
			int oldIndex = indexOf(element);
			if (oldIndex < 0)
				throw new IllegalArgumentException("No such element " + element);
			else {
				remove(oldIndex);
				add(index, element);
				return oldIndex;
			}
		}
	}

	/**
	 * @param x The x-coordinate of a point relative to this element's upper left corner
	 * @param y The y-coordinate of a point relative to this element's upper left corner
	 * @return All children of this element whose bounds contain the given point
	 */
	default ObservableList<E> at(int x, int y) {
		return filter(child -> child.bounds().contains(x, y));
	}

	/**
	 * This operation is useful for rendering children in correct sequence and in determining which elements should receive events first.
	 *
	 * @return The children in this list, sorted by z-index
	 */
	default ObservableOrderedCollection<E> sortByZ() {
		return sorted((ch1, ch2) -> ch2.getZ() - ch1.getZ());
	}

	@Override
	default ElementList<E> immutable() {
		return new ImmutableElementList<>(this);
	}

	/**
	 * Implements {@link ElementList#immutable()}
	 *
	 * @param <E> The type of elements in the list
	 */
	class ImmutableElementList<E extends QuickElement> extends ImmutableObservableList<E> implements ElementList<E> {
		protected ImmutableElementList(ObservableList<E> wrap) {
			super(wrap);
		}

		@Override
		protected ElementList<E> getWrapped() {
			return (ElementList<E>) super.getWrapped();
		}

		@Override
		public QuickElement getParent() {
			return getWrapped().getParent();
		}

		@Override
		public ImmutableElementList<E> immutable() {
			return this;
		}
	}
}
