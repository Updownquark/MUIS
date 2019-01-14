package org.quick.core.mgr;

import org.observe.collect.DefaultObservableCollection;
import org.observe.util.TypeTokens;
import org.qommons.tree.BetterTreeList;
import org.quick.core.QuickElement;

/** A list that manages child elements for a parent element */
public class ChildList extends DefaultObservableCollection<QuickElement> implements ElementList<QuickElement> {
	private final QuickElement theParent;

	/** @param parent The parent to manage the children of */
	public ChildList(QuickElement parent) {
		super(TypeTokens.get().of(QuickElement.class), new BetterTreeList<>(parent.getContentLocker()));
		theParent = parent;
	}

	@Override
	public QuickElement getParent() {
		return theParent;
	}
}
