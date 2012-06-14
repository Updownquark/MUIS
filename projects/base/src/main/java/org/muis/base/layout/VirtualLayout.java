package org.muis.base.layout;

import java.awt.Rectangle;

/** A virtual layout that allows layout managers to experiment with widget placement in a cheap and accessible way */
public class VirtualLayout implements Iterable<Rectangle>
{
	private java.util.List<Rectangle> theWidgets;

	private int theContainerWidth;

	private int theContainerHeight;

	/** Creates a virtual layout */
	public VirtualLayout()
	{
		theWidgets = new java.util.ArrayList<Rectangle>();
	}

	/**
	 * @param width The width of the container to lay widgets out in
	 * @param height The height of the container to lay widgets out in
	 */
	public void setContainerSize(int width, int height)
	{
		theContainerWidth = width;
		theContainerHeight = height;
	}

	/** @return The number of constraints set in this virtual layout */
	public int getConstraintCount()
	{
		return theWidgets.size();
	}

	/**
	 * @param index The index of the constraint to get
	 * @return The constraint at the given index in this virtual layout
	 */
	public Rectangle getConstraint(int index)
	{
		return theWidgets.get(index);
	}

	@Override
	public LayoutHoleIterator iterator()
	{
		return new LayoutHoleIterator();
	}

	/** @param block The constraint to add to this virtual layout */
	public void addConstraint(Rectangle block)
	{
		theWidgets.add(block);
	}

	/** @param index The index of the constraint to remove from this virtual layout */
	public void removeConstraint(int index)
	{
		theWidgets.remove(index);
	}

	/** Clears all constraints from this virtual layout */
	public void clearConstraints()
	{
		theWidgets.clear();
	}

	/** @return The largest <code>x + width</code> value of any constraint in this virtual layout */
	public int getUsedWidth()
	{
		int w = 0;
		for(Rectangle block : theWidgets)
		{
			int blockW = block.x + block.width;
			if(blockW > w)
				w = blockW;
		}
		return w;
	}

	/** @return The largest <code>y + height</code> value of any constraint in this virtual layout */
	public int getUsedHeight()
	{
		int h = 0;
		for(Rectangle block : theWidgets)
		{
			int blockH = block.y + block.height;
			if(blockH > h)
				h = blockH;
		}
		return h;
	}

	/** @return The smallest rectangle which will hold all of this layout's constraints */
	public Rectangle getUsedSpace()
	{
		Rectangle ret = new Rectangle();
		for(Rectangle block : theWidgets)
			ret.add(block);
		return ret;
	}

	/**
	 * Iterates over blank spaces around constraints in this layout. WARNING! The rectangle returned from the
	 * {@link #next()} method is reused for every {@link #next()} call. Duplicate the value to use it elsewhere.
	 */
	public class LayoutHoleIterator implements java.util.Iterator<Rectangle>
	{
		private Rectangle theReturn;

		private boolean isVertical;

		private int theLastX;

		private int theLastY;

		private int theNextW;

		private int theNextH;

		private boolean hasNext;

		LayoutHoleIterator()
		{
			theReturn = new Rectangle();
		}

		/** @param vertical Whether this iterator seeks for space along columns or rows first. Default is false. */
		public void setVertical(boolean vertical)
		{
			isVertical = vertical;
		}

		/** Resets this iterator so it can be reused without creating another instance */
		public void reset()
		{
			theLastX = 0;
			theLastY = 0;
			theNextW = 0;
			hasNext = true;
		}

		@Override
		public boolean hasNext()
		{
			hasNext = false;
			while(!hasNext)
			{
				if(isVertical)
				{
					boolean newColumn = false;
					while(theNextW <= 0 && theLastX < theContainerHeight)
					{
						newColumn = true;
						// Find the first point (x, y) that is not inside a constraint where y==theLastY
						int nextTryX = theLastX;
						if(theLastY == 0)
						{
							for(Rectangle block : theWidgets)
							{
								if(block.x <= theLastX && block.x + block.width > theLastX)
								{
									if(block.y <= theLastY && block.y + block.height > theLastY)
										theLastY = block.y + block.height;
									if(nextTryX == theLastX || block.x + block.width < nextTryX)
										nextTryX = block.x + block.width;
								}
							}
						}
						if(theLastY >= theContainerHeight)
						{ // No such point. Go to the next y and try to find a point there
							theLastX = nextTryX;
							continue;
						}
						/* (theLastX, theLastY) is not inside a constraint. Now find the width of the horizontal segment
						 * from this point to the left boundary of the constraint that is to the right of the point. If
						 * there is no constraint to the point's right, use the boundary of the container. */
						theNextH = theContainerHeight - theLastY;
						for(Rectangle block : theWidgets)
							if(block.x <= theLastX && block.x + block.width > theLastX)
							{
								if(block.y > theLastY && block.y < theLastY + theNextH)
									theNextH = block.y - theLastY;
							}
					}
					if(theLastX >= theContainerWidth)
						break; // No more holes
					// (theLastX, theLastY) is not inside a constraint and theLastW is the width of the next rectangle
					// to return
					theReturn.x = theLastX;
					theReturn.y = theLastY;
					theReturn.height = theNextH;
					Rectangle obstacle = null;
					for(Rectangle block : theWidgets)
					{
						if(block.y < theLastY + theNextH && block.y + block.width < theLastY && block.x > theLastX)
						{
							if(obstacle == null || block.x < obstacle.x)
								obstacle = block;
						}
					}
					int w;
					if(obstacle == null)
					{
						w = theContainerWidth - theLastX;
						theReturn.height = 0; // Need to skip to the next y position
					}
					else
					{
						w = obstacle.x - theLastX;
						theReturn.height = obstacle.y - theLastY;
					}
					if(newColumn || w > theReturn.width)
					{
						/* Advantageous to return this rectangle since it is either in a different location or has a larger
						 * height bigger dimension than the previous one returned */
						hasNext = true;
					}
				}
				else
				{
					boolean newRow = false;
					while(theNextW <= 0 && theLastY < theContainerHeight)
					{
						newRow = true;
						// Find the first point (x, y) that is not inside a constraint where y==theLastY
						int nextTryY = theLastY;
						if(theLastX == 0)
						{
							for(Rectangle block : theWidgets)
							{
								if(block.y <= theLastY && block.y + block.height > theLastY)
								{
									if(block.x <= theLastX && block.x + block.width > theLastX)
										theLastX = block.x + block.width;
									if(nextTryY == theLastY || block.y + block.width < nextTryY)
										nextTryY = block.y + block.height;
								}
							}
						}
						if(theLastX >= theContainerWidth)
						{ // No such point. Go to the next y and try to find a point there
							theLastY = nextTryY;
							continue;
						}
						/* (theLastX, theLastY) is not inside a constraint. Now find the width of the horizontal segment
						 * from this point to the left boundary of the constraint that is to the right of the point. If
						 * there is no constraint to the point's right, use the boundary of the container. */
						theNextW = theContainerWidth - theLastX;
						for(Rectangle block : theWidgets)
							if(block.y <= theLastY && block.y + block.height > theLastY)
							{
								if(block.x > theLastX && block.x < theLastX + theNextW)
									theNextW = block.x - theLastX;
							}
					}
					if(theLastY >= theContainerHeight)
						break; // No more holes
					// (theLastX, theLastY) is not inside a constraint and theLastW is the width of the next rectangle
					// to
					// return
					theReturn.x = theLastX;
					theReturn.y = theLastY;
					theReturn.width = theNextW;
					Rectangle obstacle = null;
					for(Rectangle block : theWidgets)
					{
						if(block.x < theLastX + theNextW && block.x + block.height < theLastX && block.y > theLastY)
						{
							if(obstacle == null || block.y < obstacle.y)
								obstacle = block;
						}
					}
					int h;
					if(obstacle == null)
					{
						h = theContainerHeight - theLastY;
						theReturn.width = 0; // Need to skip to the next y position
					}
					else
					{
						h = obstacle.y - theLastY;
						theReturn.width = obstacle.x - theLastX;
					}
					if(newRow || h > theReturn.height)
					{
						/* Advantageous to return this rectangle since it is either in a different location or has a larger
						 * height bigger dimension than the previous one returned */
						hasNext = true;
					}
				}
			}
			return hasNext;
		}

		@Override
		public Rectangle next()
		{
			if(!hasNext && !hasNext())
				throw new java.util.NoSuchElementException("No more holes left");
			return theReturn;
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}
}
