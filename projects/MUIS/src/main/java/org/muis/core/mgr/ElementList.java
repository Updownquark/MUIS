package org.muis.core.mgr;

import java.util.List;

import org.muis.core.MuisElement;
import org.muis.core.event.ChildEvent;
import org.muis.core.event.MuisEventListener;

/**
 * A list of elements (potentially a particular type of element) with a few extra operations available
 *
 * @param <E> The type of element
 */
public interface ElementList<E extends MuisElement> extends List<E> {
	/** @param listener The listener to be notified when this child list's content changes */
	void addListener(MuisEventListener<ChildEvent> listener);

	/** @param listener The listener to stop notifying */
	void removeListener(MuisEventListener<ChildEvent> listener);

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

	/** @return An event listener manager to allow the addition of listeners on every element in this list */
	EventListenerManager events();

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
