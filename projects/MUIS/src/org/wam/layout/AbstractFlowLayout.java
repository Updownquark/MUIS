package org.wam.layout;

import static org.wam.layout.LayoutConstants.*;

import org.wam.core.WamAttribute;
import org.wam.core.WamElement;

/**
 * A flow layout is a layout that lays contents out one after another in one direction, perhaps using multiple rows, and
 * obeys constraints set on the contents.
 */
public abstract class AbstractFlowLayout implements org.wam.core.WamLayout
{
	/** Policies for creating multiple rows (or columns) of content */
	public static enum BreakPolicy
	{
		/** Never breaks a line (row or column) of content */
		NEVER,
		/** Breaks content as needed to fit in a container (the default) */
		NEEDED,
		/** Attempts to make content fit squarely in its container, breaking as much as needed */
		SQUARE;
	}

	/**
	 * The attribute in the layout container that specifies the direction in which items should be layed out
	 */
	public static final WamAttribute<Direction> FLOW_DIRECTION = new WamAttribute<Direction>("flow-direction",
		new WamAttribute.WamEnumAttribute<Direction>(Direction.class));

	/** The attribute in the layout container that specifies the break policy for laying out items */
	public static final WamAttribute<BreakPolicy> FLOW_BREAK = new WamAttribute<BreakPolicy>("flow-break",
		new WamAttribute.WamEnumAttribute<BreakPolicy>(BreakPolicy.class));

	private Direction theDirection;

	private BreakPolicy theBreakPolicy;

	private boolean isShapeSet;

	private int theHGap;

	private int theVGap;

	public AbstractFlowLayout()
	{
		theDirection = Direction.RIGHT;
		theBreakPolicy = BreakPolicy.NEEDED;
	}

	public Direction getDirection()
	{
		return theDirection;
	}

	public void setDirection(Direction dir)
	{
		theDirection = dir;
	}

	public BreakPolicy getBreakPolicy()
	{
		return theBreakPolicy;
	}

	public void setBreakPolicy(BreakPolicy policy)
	{
		theBreakPolicy = policy;
	}

	protected void checkLayoutAttributes(WamElement parent)
	{
		isShapeSet = true;
		theDirection = parent.getAttribute(FLOW_DIRECTION);
		if(theDirection == null)
			theDirection = Direction.RIGHT;
		theBreakPolicy = parent.getAttribute(FLOW_BREAK);
		if(theBreakPolicy == null)
			theBreakPolicy = BreakPolicy.NEEDED;
	}

	@Override
	public void initChildren(WamElement parent, WamElement [] children)
	{
		parent.acceptAttribute(FLOW_DIRECTION);
		parent.acceptAttribute(FLOW_BREAK);
		for(WamElement child : children)
		{
			child.acceptAttribute(left);
			child.acceptAttribute(minLeft);
			child.acceptAttribute(maxLeft);
			child.acceptAttribute(right);
			child.acceptAttribute(minRight);
			child.acceptAttribute(maxRight);
			child.acceptAttribute(top);
			child.acceptAttribute(minTop);
			child.acceptAttribute(maxTop);
			child.acceptAttribute(bottom);
			child.acceptAttribute(minBottom);
			child.acceptAttribute(maxBottom);
			child.acceptAttribute(width);
			child.acceptAttribute(minWidth);
			child.acceptAttribute(maxWidth);
			child.acceptAttribute(height);
			child.acceptAttribute(minHeight);
			child.acceptAttribute(maxHeight);
		}
	}

	@Override
	public SizePolicy getHSizer(WamElement parent, WamElement [] children, int parentWidth)
	{
		children = children.clone();
		if(!isShapeSet)
			checkLayoutAttributes(parent);
		switch(theDirection)
		{
		case LEFT:
		case RIGHT:
			return getMajorSize(children, parentWidth);
		case UP:
		case DOWN:
			return getMinorSize(children, parentWidth);
		}
		throw new IllegalStateException("Unrecognized direction: " + theDirection);
	}

	@Override
	public SizePolicy getWSizer(WamElement parent, WamElement [] children, int parentHeight)
	{
		children = children.clone();
		if(!isShapeSet)
			checkLayoutAttributes(parent);
		switch(theDirection)
		{
		case UP:
		case DOWN:
			return getMajorSize(children, parentHeight);
		case LEFT:
		case RIGHT:
			return getMinorSize(children, parentHeight);
		}
		throw new IllegalStateException("Unrecognized direction: " + theDirection);
	}

	@Override
	public void remove(WamElement parent)
	{
	}

	protected abstract SizePolicy getMajorSize(WamElement [] children, int minorSize);

	protected abstract SizePolicy getMinorSize(WamElement [] children, int majorSize);

	protected SizePolicy getChildSizer(WamElement child, boolean major, int oppositeSize)
	{
		boolean horizontal = true;
		switch(theDirection)
		{
		case UP:
		case DOWN:
			horizontal = !major;
			break;
		case LEFT:
		case RIGHT:
			horizontal = major;
			break;
		}
		if(horizontal)
			return child.getHSizer(oppositeSize);
		else
			return child.getVSizer(oppositeSize);
	}

	protected Length getChildConstraint(WamElement child, boolean major, int type, int minMax)
	{
		boolean horizontal = major;
		switch(theDirection)
		{
		case DOWN:
		case RIGHT:
			break;
		case LEFT:
		case UP:
			type = -type;
			break;
		}
		if(type < 0)
		{
			if(horizontal)
			{
				if(minMax < 0)
					return child.getAttribute(minLeft);
				else if(minMax == 0)
					return child.getAttribute(left);
				else
					return child.getAttribute(maxLeft);
			}
			else
			{
				if(minMax < 0)
					return child.getAttribute(minTop);
				else if(minMax == 0)
					return child.getAttribute(top);
				else
					return child.getAttribute(maxTop);
			}
		}
		else if(type == 0)
		{
			if(horizontal)
			{
				if(minMax < 0)
					return child.getAttribute(minWidth);
				else if(minMax == 0)
					return child.getAttribute(width);
				else
					return child.getAttribute(maxWidth);
			}
			else
			{
				if(minMax < 0)
					return child.getAttribute(minHeight);
				else if(minMax == 0)
					return child.getAttribute(height);
				else
					return child.getAttribute(maxHeight);
			}
		}
		else
		{
			if(horizontal)
			{
				if(minMax < 0)
					return child.getAttribute(minRight);
				else if(minMax == 0)
					return child.getAttribute(right);
				else
					return child.getAttribute(maxRight);
			}
			else
			{
				if(minMax < 0)
					return child.getAttribute(minBottom);
				else if(minMax == 0)
					return child.getAttribute(bottom);
				else
					return child.getAttribute(maxBottom);
			}
		}
	}

	public int getMajorGap()
	{
		switch(theDirection)
		{
		case UP:
		case DOWN:
			return theVGap;
		case LEFT:
		case RIGHT:
			return theHGap;
		}
		return 0;
	}

	public int getMinorGap()
	{
		switch(theDirection)
		{
		case UP:
		case DOWN:
			return theHGap;
		case LEFT:
		case RIGHT:
			return theVGap;
		}
		return 0;
	}
}
