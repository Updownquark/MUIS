package org.muis.base.model.impl;

import java.util.Set;

import org.muis.base.model.TreePath;
import org.muis.base.model.TreePathSet;
import org.observe.collect.impl.ObservableHashSet;

import prisms.lang.Type;

/**
 * A default implementation of TreePathSet
 *
 * @param <E> The type of element in the tree
 */
public class DefaultTreePathSet<E> extends ObservableHashSet<TreePath<E>> implements TreePathSet<E> {
	private final Set<TreePath<E>> theControl;

	/**
	 * Creates the selection model
	 *
	 * @param type The type of element in the tree
	 */
	public DefaultTreePathSet(Type type) {
		super(new Type(TreePath.class, type));
		theControl = control(null);
	}

	@Override
	public boolean add(TreePath<E> path) {
		return theControl.add(path);
	}

	@Override
	public boolean remove(TreePath<E> path) {
		return theControl.remove(path);
	}
}
