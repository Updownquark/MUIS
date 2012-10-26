package org.muis.base.layout;

import static org.muis.base.layout.LayoutConstants.*;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisElement;
import org.muis.core.layout.SizePolicy;
import org.muis.core.style.Position;
import org.muis.core.style.Size;
import org.muis.util.CompoundListener;

/**
 * A flow layout is a layout that lays contents out one after another in one direction, perhaps using multiple rows, and obeys constraints
 * set on the contents.
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
		new org.muis.core.MuisProperty.MuisEnumProperty<BreakPolicy>(BreakPolicy.class));

	private final CompoundListener.MultiElementCompoundListener theListener;

	private Direction theDirection;

	private BreakPolicy theBreakPolicy;

	private boolean isShapeSet;

	private int theHGap;

	private int theVGap;

	public AbstractFlowLayout()
	{
		theDirection = Direction.RIGHT;
		theBreakPolicy = BreakPolicy.NEEDED;
		theListener = CompoundListener.create(this);
		theListener.acceptAll(direction, FLOW_BREAK).onChange(CompoundListener.layout);
		theListener.child()
			.acceptAll(left, minLeft, maxLeft, right, minRight, maxRight, width, minWidth, maxWidth, height, minHeight, maxHeight)
			.onChange(CompoundListener.layout);
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
		theDirection = parent.atts().get(direction);
		if(theDirection == null)
			theDirection = Direction.RIGHT;
		theBreakPolicy = parent.atts().get(FLOW_BREAK);
		if(theBreakPolicy == null)
			theBreakPolicy = BreakPolicy.NEEDED;
	}

	@Override
	public void initChildren(MuisElement parent, MuisElement [] children)
	{
	}

	@Override
	public void childAdded(MuisElement parent, MuisElement child)
	{
	}

	@Override
	public void childRemoved(MuisElement parent, MuisElement child)
	{
	}

	@Override
	public void layout(MuisElement parent, MuisElement [] children)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public SizePolicy getHSizer(MuisElement parent, MuisElement [] children, int parentWidth)
	{
		children = children.clone();
		if(!isShapeSet)
			checkLayoutAttributes(parent);
		switch (theDirection)
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
		switch (theDirection)
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
		switch (theDirection)
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

	protected Position getChildPositionConstraint(MuisElement child, boolean major, int type, int minMax)
	{
		boolean horizontal = major;
		switch (theDirection)
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
					return child.atts().get(minLeft);
				else if(minMax == 0)
					return child.atts().get(left);
				else
					return child.atts().get(maxLeft);
			}
			else if(minMax < 0)
				return child.atts().get(minTop);
			else if(minMax == 0)
				return child.atts().get(top);
			else
				return child.atts().get(maxTop);
		}
		else if(horizontal)
		{
			if(minMax < 0)
				return child.atts().get(minRight);
			else if(minMax == 0)
				return child.atts().get(right);
			else
				return child.atts().get(maxRight);
		}
		else if(minMax < 0)
			return child.atts().get(minBottom);
		else if(minMax == 0)
			return child.atts().get(bottom);
		else
			return child.atts().get(maxBottom);
	}

	protected Size getChildSizeConstraint(MuisElement child, boolean major, int type, int minMax)
	{
		boolean horizontal = major;
		switch (theDirection)
		{
		case DOWN:
		case RIGHT:
			break;
		case LEFT:
		case UP:
			type = -type;
			break;
		}
		if(horizontal)
		{
			if(minMax < 0)
				return child.atts().get(minWidth);
			else if(minMax == 0)
				return child.atts().get(width);
			else
				return child.atts().get(maxWidth);
		}
		else if(minMax < 0)
			return child.atts().get(minHeight);
		else if(minMax == 0)
			return child.atts().get(height);
		else
			return child.atts().get(maxHeight);
	}

	public int getMajorGap()
	{
		switch (theDirection)
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
		switch (theDirection)
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
