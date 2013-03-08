package org.muis.base.layout;

import static org.muis.base.layout.LayoutConstants.*;

import org.muis.base.layout.AbstractFlowLayout.BreakPolicy;
import org.muis.core.MuisElement;
import org.muis.core.layout.SizePolicy;
import org.muis.util.CompoundListener;

public class SimpleFlowLayout implements org.muis.core.MuisLayout
{
	private final CompoundListener.MultiElementCompoundListener theListener;

	private Direction theDirection;

	private BreakPolicy theBreakPolicy;

	private boolean isShapeSet;

	public SimpleFlowLayout()
	{
		theDirection = Direction.RIGHT;
		theBreakPolicy = BreakPolicy.NEEDED;
		theListener = CompoundListener.create(this);
		theListener.acceptAll(direction, AbstractFlowLayout.FLOW_BREAK).onChange(CompoundListener.layout);
		theListener.child().acceptAll(left, right, top, bottom, width, height).onChange(CompoundListener.layout);
	}

	protected void checkLayoutAttributes(MuisElement parent)
	{
		isShapeSet = true;
		theDirection = parent.atts().get(LayoutConstants.direction);
		if(theDirection == null)
			theDirection = Direction.RIGHT;
		theBreakPolicy = parent.atts().get(AbstractFlowLayout.FLOW_BREAK);
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
