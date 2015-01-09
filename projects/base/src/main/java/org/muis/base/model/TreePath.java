package org.muis.base.model;

import java.util.AbstractList;

/**
 * A path from the root of a tree, through nested intermediate nodes, down to a target node
 *
 * @param <E> The type of elements in the tree
 */
public class TreePath<E> extends AbstractList<E> {
	private final Object [] theElements;

	public TreePath(E [] elements) {
		theElements = elements;
	}

	public TreePath(java.util.List<E> elements) {
		theElements = elements.toArray();
	}

	@Override
	public E get(int index) {
		return (E) theElements[index];
	}

	@Override
	public int size() {
		return theElements.length;
	}

	/** @return The last element in this tree path */
	public E target() {
		return (E) theElements[theElements.length - 1];
	}

	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(!(o instanceof TreePath))
			return false;
		return prisms.util.ArrayUtils.equals(theElements, ((TreePath<?>) o).theElements);
	}

	@Override
	public int hashCode() {
		return prisms.util.ArrayUtils.hashCode(theElements);
	}
}
