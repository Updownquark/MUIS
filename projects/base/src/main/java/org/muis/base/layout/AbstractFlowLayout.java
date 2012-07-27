package org.muis.base.layout;

import static org.muis.base.layout.LayoutConstants.*;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisElement;
import org.muis.core.annotations.MuisActionType;
import org.muis.core.annotations.MuisAttrConsumer;
import org.muis.core.annotations.MuisAttrType;
import org.muis.core.annotations.NeededAttr;
import org.muis.core.layout.SizePolicy;
import org.muis.core.style.Position;
import org.muis.core.style.Size;

/**
 * A flow layout is a layout that lays contents out one after another in one direction, perhaps using multiple rows, and obeys constraints
 * set on the contents.
 */
@MuisAttrConsumer(attrs = {@NeededAttr(name = "direction", type = MuisAttrType.ENUM, valueType = Direction.class),
		@NeededAttr(name = "flow-break", type = MuisAttrType.ENUM, valueType = AbstractFlowLayout.BreakPolicy.class)}, childAttrs = {
		@NeededAttr(name = "left", type = MuisAttrType.POSITION), @NeededAttr(name = "min-left", type = MuisAttrType.POSITION),
		@NeededAttr(name = "max-left", type = MuisAttrType.POSITION), @NeededAttr(name = "right", type = MuisAttrType.POSITION),
		@NeededAttr(name = "min-right", type = MuisAttrType.POSITION), @NeededAttr(name = "max-right", type = MuisAttrType.POSITION),
		@NeededAttr(name = "top", type = MuisAttrType.POSITION), @NeededAttr(name = "min-top", type = MuisAttrType.POSITION),
		@NeededAttr(name = "max-top", type = MuisAttrType.POSITION), @NeededAttr(name = "bottom", type = MuisAttrType.POSITION),
		@NeededAttr(name = "min-bottom", type = MuisAttrType.POSITION), @NeededAttr(name = "max-bottom", type = MuisAttrType.POSITION),
			@NeededAttr(name = "width", type = MuisAttrType.SIZE), @NeededAttr(name = "min-width", type = MuisAttrType.SIZE),
			@NeededAttr(name = "max-width", type = MuisAttrType.SIZE), @NeededAttr(name = "height", type = MuisAttrType.SIZE),
			@NeededAttr(name = "min-height", type = MuisAttrType.SIZE), @NeededAttr(name = "max-height", type = MuisAttrType.SIZE)},
	action = MuisActionType.layout)
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
