package org.quick.base.model.impl;

import java.util.Set;

import org.observe.collect.impl.ObservableHashSet;
import org.observe.util.ObservableSetWrapper;
import org.quick.base.model.TreePath;
import org.quick.base.model.TreePathSet;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

/**
 * A default implementation of TreePathSet
 *
 * @param <E> The type of element in the tree
 */
public class DefaultTreePathSet<E> extends ObservableSetWrapper<TreePath<E>> implements TreePathSet<E> {
	private final Set<TreePath<E>> theControl;

	/**
	 * Creates the selection model
	 *
	 * @param type The type of element in the tree
	 */
	public DefaultTreePathSet(TypeToken<E> type) {
		super(new ObservableHashSet<>(new TypeToken<TreePath<E>>() {}.where(new TypeParameter<E>() {}, type)));
		theControl = getWrapped();
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
