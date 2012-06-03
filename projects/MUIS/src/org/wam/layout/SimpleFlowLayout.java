package org.wam.layout;

import static org.wam.layout.LayoutConstants.*;

import java.awt.Rectangle;

import org.wam.core.WamElement;
import org.wam.core.event.WamEvent;
import org.wam.core.event.WamEventListener;
import org.wam.layout.FlowLayout.BreakPolicy;

public class SimpleFlowLayout implements org.wam.core.WamLayout
{
	private java.util.HashMap<WamElement, WamEventListener<WamElement> []> theContainerListeners;

	private Direction theDirection;

	private BreakPolicy theBreakPolicy;

	private boolean isShapeSet;

	public SimpleFlowLayout()
	{
		theDirection = Direction.RIGHT;
		theBreakPolicy = BreakPolicy.NEEDED;
	}

	protected void checkLayoutAttributes(WamElement parent)
	{
		isShapeSet = true;
		theDirection = parent.getAttribute(FlowLayout.FLOW_DIRECTION);
		if(theDirection == null)
			theDirection = Direction.RIGHT;
		theBreakPolicy = parent.getAttribute(FlowLayout.FLOW_BREAK);
		if(theBreakPolicy == null)
			theBreakPolicy = BreakPolicy.NEEDED;
	}

	@Override
	public void initChildren(WamElement parent, WamElement [] children)
	{
		WamEventListener<WamElement> addListener;
		addListener = new WamEventListener<WamElement>()
		{
			@Override
			public void eventOccurred(WamEvent<? extends WamElement> event, WamElement element)
			{
				allowChild(element);
			}

			@Override
			public boolean isLocal()
			{
				return true;
			}
		};
		WamEventListener<WamElement> removeListener;
		removeListener = new WamEventListener<WamElement>()
		{
			@Override
			public void eventOccurred(WamEvent<? extends WamElement> event, WamElement element)
			{
				removeChild(element);
			}

			@Override
			public boolean isLocal()
			{
				return true;
			}
		};
		parent.addListener(WamElement.CHILD_ADDED, addListener);
		parent.addListener(WamElement.CHILD_REMOVED, removeListener);
		theContainerListeners.put(parent, new WamEventListener [] {addListener, removeListener});
		parent.acceptAttribute(FlowLayout.FLOW_DIRECTION);
		parent.acceptAttribute(FlowLayout.FLOW_BREAK);
		for(WamElement child : children)
			allowChild(child);
	}

	void allowChild(WamElement child)
	{
		child.acceptAttribute(left);
		child.acceptAttribute(right);
		child.acceptAttribute(top);
		child.acceptAttribute(bottom);
		child.acceptAttribute(width);
		child.acceptAttribute(height);
		child.acceptAttribute(included);
	}

	void removeChild(WamElement child)
	{
		child.rejectAttribute(left);
		child.rejectAttribute(right);
		child.rejectAttribute(top);
		child.rejectAttribute(bottom);
		child.rejectAttribute(width);
		child.rejectAttribute(height);
		child.rejectAttribute(included);
	}

	@Override
	public SizePolicy getHSizer(WamElement parent, WamElement [] children, int parentWidth)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SizePolicy getWSizer(WamElement parent, WamElement [] children, int parentHeight)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void layout(WamElement parent, WamElement [] children, Rectangle box)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(WamElement parent)
	{
		WamEventListener<WamElement> [] listeners = theContainerListeners.get(parent);
		if(listeners == null)
			return;
		parent.removeListener(listeners[0]);
		parent.removeListener(listeners[1]);
		theContainerListeners.remove(parent);
		parent.rejectAttribute(FlowLayout.FLOW_DIRECTION);
		parent.rejectAttribute(FlowLayout.FLOW_BREAK);
		for(int c = 0; c < parent.getChildCount(); c++)
			removeChild(parent.getChild(c));
	}
}
