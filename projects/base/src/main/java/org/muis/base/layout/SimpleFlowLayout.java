package org.muis.base.layout;

import org.muis.base.layout.AbstractFlowLayout.BreakPolicy;
import org.muis.core.MuisElement;
import org.muis.core.annotations.MuisActionType;
import org.muis.core.annotations.MuisAttrConsumer;
import org.muis.core.annotations.MuisAttrType;
import org.muis.core.annotations.NeededAttr;
import org.muis.core.layout.SizePolicy;

@MuisAttrConsumer(
	attrs = {@NeededAttr(name = "direction", type = MuisAttrType.ENUM, valueType = Direction.class),
			@NeededAttr(name = "flow-break", type = MuisAttrType.ENUM, valueType = AbstractFlowLayout.BreakPolicy.class)},
	childAttrs = {@NeededAttr(name = "left", type = MuisAttrType.POSITION), @NeededAttr(name = "right", type = MuisAttrType.POSITION),
			@NeededAttr(name = "top", type = MuisAttrType.POSITION), @NeededAttr(name = "bottom", type = MuisAttrType.POSITION),
			@NeededAttr(name = "width", type = MuisAttrType.SIZE), @NeededAttr(name = "height", type = MuisAttrType.SIZE)},
	action = MuisActionType.layout)
public class SimpleFlowLayout implements org.muis.core.MuisLayout
{
	private Direction theDirection;

	private BreakPolicy theBreakPolicy;

	private boolean isShapeSet;

	public SimpleFlowLayout()
	{
		theDirection = Direction.RIGHT;
		theBreakPolicy = BreakPolicy.NEEDED;
	}

	protected void checkLayoutAttributes(MuisElement parent)
	{
		isShapeSet = true;
		theDirection = parent.getAttribute(LayoutConstants.direction);
		if(theDirection == null)
			theDirection = Direction.RIGHT;
		theBreakPolicy = parent.getAttribute(FlowLayout.FLOW_BREAK);
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
	public SizePolicy getHSizer(MuisElement parent, MuisElement [] children, int parentWidth)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SizePolicy getWSizer(MuisElement parent, MuisElement [] children, int parentHeight)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void layout(MuisElement parent, MuisElement [] children)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(MuisElement parent)
	{
	}
}
