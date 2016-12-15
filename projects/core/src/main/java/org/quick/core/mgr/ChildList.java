package org.quick.core.mgr;

import org.observe.collect.impl.ObservableArrayList;
import org.quick.core.QuickElement;

import com.google.common.reflect.TypeToken;

/** A list that manages child elements for a parent element */
public class ChildList extends ObservableArrayList<QuickElement> implements ElementList<QuickElement> {
	private final QuickElement theParent;

	/** @param parent The parent to manage the children of */
	public ChildList(QuickElement parent) {
		super(TypeToken.of(QuickElement.class));
		theParent = parent;
	}

	@Override
	public QuickElement getParent() {
		return theParent;
	}
}
