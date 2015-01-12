package org.muis.base.model;

import java.util.AbstractList;

import org.muis.core.rx.ObservableTree;

/**
 * A path from the root of a tree, through nested intermediate nodes, down to a target node
 *
 * @param <E> The type of elements in the tree
 */
public class TreePath<E> extends AbstractList<ObservableTree<E>> {
	private final ObservableTree<E> [] theElements;

	public TreePath(ObservableTree<E> root) {
		theElements = new ObservableTree[] {root};
	}

	public TreePath(ObservableTree<E> [] elements) {
		theElements = elements;
	}

	public TreePath(java.util.List<ObservableTree<E>> elements) {
		theElements = elements.toArray(new ObservableTree[elements.size()]);
	}

	@Override
	public ObservableTree<E> get(int index) {
		return theElements[index];
	}

	@Override
	public int size() {
		return theElements.length;
	}

	/** @return The last element in this tree path */
	public ObservableTree<E> target() {
		return theElements[theElements.length - 1];
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
