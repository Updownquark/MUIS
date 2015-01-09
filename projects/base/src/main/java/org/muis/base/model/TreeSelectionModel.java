package org.muis.base.model;

/**
 * A model indicating what nodes in a tree are selected
 *
 * @param <E> The type of elements in the tree
 */
public interface TreeSelectionModel<E> extends TreePathSet<E> {
	/** @return The anchor for the current selection */
	TreePath<E> getAnchor();
}
