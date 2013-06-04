package org.muis.core.mgr;

import java.util.List;

import org.muis.core.MuisElement;

/**
 * A list of elements (potentially a particular type of element) with a few extra operations available
 *
 * @param <E> The type of element
 */
public interface ElementList<E extends MuisElement> extends List<E> {
	/** @return The parent whose children this list manages */
	MuisElement getParent();

	/**
	 * @return The last child in this list
	 * @throws java.util.NoSuchElementException If this list is empty
	 */
	MuisElement getLast() throws java.util.NoSuchElementException;

	/**
	 * @param x The x-coordinate of a point relative to this element's upper left corner
	 * @param y The y-coordinate of a point relative to this element's upper left corner
	 * @return All children of this element whose bounds contain the given point
	 */
	MuisElement [] at(int x, int y);

	/**
	 * This operation is useful for rendering children in correct sequence and in determining which elements should receive events first.
	 *
	 * @return The children in this list, sorted by z-index
	 */
	MuisElement [] sortByZ();

	/**
	 * Adds a listener for an event type to this element's direct children
	 *
	 * @param <T> The type of the property that the event represents
	 * @param type The event type to listen for
	 * @param listener The listener to notify when an event of the given type occurs
	 */
	<T> void addChildListener(org.muis.core.event.MuisEventType<T> type, org.muis.core.event.MuisEventListener<? super T> listener);

	/** @param listener The listener to remove from this element's children */
	void removeChildListener(org.muis.core.event.MuisEventListener<?> listener);

	@Override
	public E [] toArray();

	/**
	 * @param children The children to add to this list
	 * @return Whether the list was modified
	 */
	boolean addAll(MuisElement [] children);

	/**
	 * @param index The index to insert the new children at
	 * @param children The children to add to this list
	 * @return Whether the list was modified
	 */
	boolean addAll(int index, MuisElement [] children);
}
