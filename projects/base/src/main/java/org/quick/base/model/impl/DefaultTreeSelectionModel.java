package org.quick.base.model.impl;

import java.util.List;

import org.observe.collect.impl.ObservableArrayList;
import org.quick.base.model.TreePath;
import org.quick.base.model.TreeSelectionModel;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

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
	public DefaultTreeSelectionModel(TypeToken<E> type) {
		super(new ObservableArrayList<>(new TypeToken<TreePath<E>>() {}.where(new TypeParameter<E>() {}, type)));
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
