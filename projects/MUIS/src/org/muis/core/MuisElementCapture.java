package org.muis.core;

import java.util.Iterator;

public class MuisElementCapture implements prisms.util.Sealable, Iterable<MuisElementCapture>
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

	public MuisElementCapture getChild(int index)
	{
		return theChildren[index];
	}

	public Iterable<MuisElementCapture> getChildren()
	{
		return prisms.util.ArrayUtils.iterable(theChildren);
	}

	public Iterable<MuisElementCapture> getTargets()
	{
		if(theChildren.length == 0)
			return new Iterable<MuisElementCapture>() {
				@Override
				public Iterator<MuisElementCapture> iterator()
				{
					return new SelfIterator();
				}
			};
		else
			return new Iterable<MuisElementCapture>() {
				@Override
				public Iterator<MuisElementCapture> iterator()
				{
					return new MuisCaptureIterator(false, true);
				}
			};
	}

	/**
	 * Performs a depth-first iteration of this capture structure
	 *
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<MuisElementCapture> iterator()
	{
		return new MuisCaptureIterator(true, true);
	}

	public Iterable<MuisElementCapture> iterate(final boolean depthFirst)
	{
		return new Iterable<MuisElementCapture>() {
			@Override
			public Iterator<MuisElementCapture> iterator()
			{
				return new MuisCaptureIterator(true, depthFirst);
			}
		};
	}

	public MuisElementCapture find(MuisElement el)
	{
		MuisElement [] path = MuisUtils.path(el);
		MuisElementCapture ret = this;
		int pathIdx;
		for(pathIdx = 1; pathIdx < path.length; pathIdx++)
		{
			boolean found = false;
			for(MuisElementCapture child : ret.theChildren)
				if(child.element == path[pathIdx])
				{
					found = true;
					ret = child;
					break;
				}
			if(!found)
				return null;
		}
		return ret;
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

	private class SelfIterator implements Iterator<MuisElementCapture>
	{
		private boolean hasReturned;

		SelfIterator()
		{
		}

		@Override
		public boolean hasNext()
		{
			return !hasReturned;
		}

		@Override
		public MuisElementCapture next()
		{
			return MuisElementCapture.this;
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}

	private class MuisCaptureIterator implements Iterator<MuisElementCapture>
	{
		private int theIndex;

		private Iterator<MuisElementCapture> theChildIter;

		private boolean isReturningSelf;

		private boolean isDepthFirst;

		private boolean hasReturnedSelf;

		MuisCaptureIterator(boolean returnSelf, boolean depthFirst)
		{
			isReturningSelf = returnSelf;
			isDepthFirst = depthFirst;
		}

		@Override
		public boolean hasNext()
		{
			if(isReturningSelf && !isDepthFirst && !hasReturnedSelf)
				return true;
			while(theIndex < getChildCount())
			{
				if(theChildIter == null)
					theChildIter = getChild(theIndex).getTargets().iterator();
				if(theChildIter.hasNext())
					return true;
				else
					theChildIter = null;
				theIndex++;
			}
			return isReturningSelf && !hasReturnedSelf;
		}

		@Override
		public MuisElementCapture next()
		{
			if(isReturningSelf && !isDepthFirst && !hasReturnedSelf)
			{
				hasReturnedSelf = true;
				return MuisElementCapture.this;
			}
			if(theChildIter != null)
				return theChildIter.next();
			if(isReturningSelf && isDepthFirst && !hasReturnedSelf)
			{
				hasReturnedSelf = true;
				return MuisElementCapture.this;
			}
			return null;
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}
}
