package org.quick.core.mgr;

import java.util.List;

import org.observe.Observable;
import org.quick.core.QuickElement;
import org.quick.core.event.ChildEvent;
import org.quick.core.event.QuickEvent;

/**
 * A list of elements (potentially a particular type of element) with a few extra operations available
 *
 * @param <E> The type of element
 */
public interface ElementList<E extends QuickElement> extends List<E>, Observable<ChildEvent> {
	/** @return The parent whose children this list manages */
	QuickElement getParent();

	/**
	 * @return The last child in this list
	 * @throws java.util.NoSuchElementException If this list is empty
	 */
	QuickElement getLast() throws java.util.NoSuchElementException;

	/**
	 * @param x The x-coordinate of a point relative to this element's upper left corner
	 * @param y The y-coordinate of a point relative to this element's upper left corner
	 * @return All children of this element whose bounds contain the given point
	 */
	QuickElement [] at(int x, int y);

	/**
	 * This operation is useful for rendering children in correct sequence and in determining which elements should receive events first.
	 *
	 * @return The children in this list, sorted by z-index
	 */
	QuickElement [] sortByZ();

	/** @return An event listener manager to allow the addition of listeners on every element in this list */
	Observable<QuickEvent> events();

	@Override
	public E [] toArray();

	/**
	 * @param children The children to add to this list
	 * @return Whether the list was modified
	 */
	boolean addAll(QuickElement [] children);

	/**
	 * @param index The index to insert the new children at
	 * @param children The children to add to this list
	 * @return Whether the list was modified
	 */
	boolean addAll(int index, QuickElement [] children);
}
