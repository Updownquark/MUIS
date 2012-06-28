package org.muis.core;

/** Represents an element that may never have children */
public class MuisLeaf extends MuisElement
{
	@Override
	public final void initChildren(MuisElement [] children)
	{
		if(children.length > 0)
			fatal("Elements of type " + getClass().getName() + " may not have children", null, "children", children);
		super.initChildren(children);
	}

	@Override
	protected final void addChild(MuisElement child, int index)
	{
		fatal("Elements of type " + getClass().getName() + " may not have children", null, "child", child);
		super.addChild(child, index);
	}
}
