package org.muis.core;

public class MuisElementCapture implements prisms.util.Sealable
{
	public final MuisElement element;

	public final int x;

	public final int y;

	private MuisElementCapture [] theChildren;

	private boolean isSealed;

	public MuisElementCapture(MuisElement el, int anX, int aY)
	{
		element = el;
		x = anX;
		y = aY;
		theChildren = new MuisElementCapture[0];
	}

	public void addChild(MuisElementCapture child)
	{
		if(isSealed)
			throw new SealedException(this);
		theChildren = prisms.util.ArrayUtils.add(theChildren, child);
	}

	public int getChildCount()
	{
		return theChildren.length;
	}

	public Iterable<MuisElementCapture> getChildren()
	{
		return prisms.util.ArrayUtils.iterable(theChildren);
	}

	public Iterable<MuisElementCapture> getTargets()
	{

	}

	public MuisElementCapture getTarget()
	{
		if(theChildren.length == 0)
			return this;
		return theChildren[theChildren.length - 1].getTarget();
	}

	@Override
	public boolean isSealed()
	{
		return isSealed;
	}

	@Override
	public void seal()
	{
		isSealed = true;
	}
}
