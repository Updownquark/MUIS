package org.muis.base.model;

import org.observe.collect.ObservableCollection;

/**
 * A set of tree paths
 *
 * @param <E> The type of element in the tree
 */
public interface TreePathSet<E> extends ObservableCollection<TreePath<E>> {
	/**
	 * @param path The path to add to this set
	 * @return Whether the path set changed as a result of this call
	 * @see java.util.Collection#add(java.lang.Object)
	 */
	@Override
	public boolean add(TreePath<E> path);

	/**
	 *
	 * @param path The path to remove from this set
	 * @return Whether the path existed in the set
	 */
	public boolean remove(TreePath<E> path);
}
