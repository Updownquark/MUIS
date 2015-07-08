package org.muis.base.model.impl;

import java.util.List;

import org.muis.base.model.TreePath;
import org.muis.base.model.TreeSelectionModel;
import org.observe.collect.impl.ObservableArrayList;

import prisms.lang.Type;

/**
 * Default tree selection model implementation
 *
 * @param <E> The type of element in the tree
 */
public class DefaultTreeSelectionModel<E> extends org.observe.util.ObservableListWrapper<TreePath<E>> implements TreeSelectionModel<E> {
	private final List<TreePath<E>> theControl;

	/**
	 * Creates the selection model
	 *
	 * @param type The type of element in the tree
	 */
	public DefaultTreeSelectionModel(Type type) {
		super(new ObservableArrayList<>(new Type(TreePath.class, type)));
		theControl = getWrapped();
	}

	@Override
	public boolean add(TreePath<E> path) {
		if(path.equals(getAnchor()))
			return false;
		theControl.remove(path);
		theControl.add(path);
		return true;
	}

	@Override
	public boolean remove(TreePath<E> path) {
		return theControl.remove(path);
	}

	@Override
	public TreePath<E> getAnchor() {
		return isEmpty() ? null : get(size() - 1);
	}
}
