package org.muis.core.mgr;

import org.muis.core.MuisElement;

/**
 * A list of MUIS elements that can be modified, with a few extra modification utilities
 * 
 * @param <E> The type of element
 */
public interface MutableElementList<E extends MuisElement> extends ElementList<E> {
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
