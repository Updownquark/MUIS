package org.quick.core;

import org.observe.Observable;

/** Represents an element that may never have children */
public class QuickLeaf extends QuickElement {
	@Override
	protected void registerChild(QuickElement child, Observable<?> until) {
		msg().fatal("Elements of type " + getClass().getName() + " may not have children", null, "child", child);
		super.registerChild(child, until);
	}
}
