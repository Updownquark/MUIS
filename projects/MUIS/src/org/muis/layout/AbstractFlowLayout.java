package org.muis.layout;

import static org.muis.layout.LayoutConstants.bottom;
import static org.muis.layout.LayoutConstants.direction;
import static org.muis.layout.LayoutConstants.height;
import static org.muis.layout.LayoutConstants.left;
import static org.muis.layout.LayoutConstants.maxBottom;
import static org.muis.layout.LayoutConstants.maxHeight;
import static org.muis.layout.LayoutConstants.maxLeft;
import static org.muis.layout.LayoutConstants.maxRight;
import static org.muis.layout.LayoutConstants.maxTop;
import static org.muis.layout.LayoutConstants.maxWidth;
import static org.muis.layout.LayoutConstants.minBottom;
import static org.muis.layout.LayoutConstants.minHeight;
import static org.muis.layout.LayoutConstants.minLeft;
import static org.muis.layout.LayoutConstants.minRight;
import static org.muis.layout.LayoutConstants.minTop;
import static org.muis.layout.LayoutConstants.minWidth;
import static org.muis.layout.LayoutConstants.right;
import static org.muis.layout.LayoutConstants.top;
import static org.muis.layout.LayoutConstants.width;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisElement;

/**
 * A flow layout is a layout that lays contents out one after another in one direction, perhaps using multiple rows, and
 * obeys constraints set on the contents.
 */
public abstract class AbstractFlowLayout implements org.muis.core.MuisLayout
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

	/** The attribute in the layout container that specifies the break policy for laying out items */
	public static final MuisAttribute<BreakPolicy> FLOW_BREAK = new MuisAttribute<BreakPolicy>("flow-break",
		new MuisAttribute.MuisEnumAttribute<BreakPolicy>(BreakPolicy.class));

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

	protected void checkLayoutAttributes(MuisElement parent)
	{
		isShapeSet = true;
		theDirection = parent.getAttribute(direction);
		if(theDirection == null)
			theDirection = Direction.RIGHT;
		theBreakPolicy = parent.getAttribute(FLOW_BREAK);
		if(theBreakPolicy == null)
			theBreakPolicy = BreakPolicy.NEEDED;
	}

	@Override
	public void initChildren(MuisElement parent, MuisElement [] children)
	{
		parent.acceptAttribute(direction);
		parent.acceptAttribute(FLOW_BREAK);
		for(MuisElement child : children)
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
	public SizePolicy getHSizer(MuisElement parent, MuisElement [] children, int parentWidth)
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
	public SizePolicy getWSizer(MuisElement parent, MuisElement [] children, int parentHeight)
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
	public void remove(MuisElement parent)
	{
	}

	protected abstract SizePolicy getMajorSize(MuisElement [] children, int minorSize);

	protected abstract SizePolicy getMinorSize(MuisElement [] children, int majorSize);

	protected SizePolicy getChildSizer(MuisElement child, boolean major, int oppositeSize)
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
			return child.getWSizer(oppositeSize);
		else
			return child.getHSizer(oppositeSize);
	}

	protected Length getChildConstraint(MuisElement child, boolean major, int type, int minMax)
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
			else if(minMax < 0)
				return child.getAttribute(minTop);
			else if(minMax == 0)
				return child.getAttribute(top);
			else
				return child.getAttribute(maxTop);
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
			else if(minMax < 0)
				return child.getAttribute(minHeight);
			else if(minMax == 0)
				return child.getAttribute(height);
			else
				return child.getAttribute(maxHeight);
		}
		else if(horizontal)
		{
			if(minMax < 0)
				return child.getAttribute(minRight);
			else if(minMax == 0)
				return child.getAttribute(right);
			else
				return child.getAttribute(maxRight);
		}
		else if(minMax < 0)
			return child.getAttribute(minBottom);
		else if(minMax == 0)
			return child.getAttribute(bottom);
		else
			return child.getAttribute(maxBottom);
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
