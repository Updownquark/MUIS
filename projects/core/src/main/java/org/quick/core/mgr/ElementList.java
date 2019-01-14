package org.quick.core.mgr;

import java.util.Collection;
import java.util.function.Consumer;

import org.observe.Subscription;
import org.observe.collect.Equivalence;
import org.observe.collect.ObservableCollection;
import org.observe.collect.ObservableCollectionEvent;
import org.qommons.Transaction;
import org.qommons.collect.BetterCollections;
import org.qommons.collect.ElementId;
import org.qommons.collect.MutableCollectionElement.StdMsg;
import org.quick.core.QuickElement;

import com.google.common.reflect.TypeToken;

/**
 * A list of elements (potentially a particular type of element) with a few extra operations available
 *
 * @param <E> The type of element
 */
public interface ElementList<E extends QuickElement> extends ObservableCollection<E> {
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

	default ElementList<E> immutable() {
		return new ImmutableElementList<>(this);
	}

	/**
	 * Implements {@link ElementList#immutable()}
	 *
	 * @param <E> The type of elements in the list
	 */
	class ImmutableElementList<E extends QuickElement> extends BetterCollections.UnmodifiableBetterList<E> implements ElementList<E> {
		protected ImmutableElementList(ElementList<E> wrapped) {
			super(wrapped);
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

		@Override
		public TypeToken<E> getType() {
			return getWrapped().getType();
		}

		@Override
		public Subscription onChange(Consumer<? super ObservableCollectionEvent<? extends E>> observer) {
			return getWrapped().onChange(observer);
		}

		@Override
		public Equivalence<? super E> equivalence() {
			return getWrapped().equivalence();
		}

		@Override
		public void setValue(Collection<ElementId> elements, E value) {
			if (!elements.isEmpty())
				throw new UnsupportedOperationException(StdMsg.UNSUPPORTED_OPERATION);
		}
	}
}
