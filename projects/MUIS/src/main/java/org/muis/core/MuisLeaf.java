package org.muis.core;

/** Represents an element that may never have children */
public class MuisLeaf extends MuisElement {
	@Override
	protected void registerChild(MuisElement child) {
		msg().fatal("Elements of type " + getClass().getName() + " may not have children", null, "child", child);
		super.registerChild(child);
	}
}
