package org.quick.core;

/** Represents an element that may never have children */
public class QuickLeaf extends QuickElement {
	@Override
	protected void registerChild(QuickElement child) {
		msg().fatal("Elements of type " + getClass().getName() + " may not have children", null, "child", child);
		super.registerChild(child);
	}
}
