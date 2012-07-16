package org.muis.base.layout;

import static org.muis.base.layout.LayoutConstants.*;

import org.muis.base.layout.AbstractFlowLayout.BreakPolicy;
import org.muis.core.MuisElement;
import org.muis.core.event.MuisEvent;
import org.muis.core.event.MuisEventListener;
import org.muis.core.layout.SizePolicy;

public class SimpleFlowLayout implements org.muis.core.MuisLayout
{
	private java.util.HashMap<MuisElement, MuisEventListener<MuisElement> []> theContainerListeners;

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
		MuisEventListener<MuisElement> addListener;
		addListener = new MuisEventListener<MuisElement>() {
			@Override
			public void eventOccurred(MuisEvent<MuisElement> event, MuisElement element)
			{
				allowChild(element);
			}

			@Override
			public boolean isLocal()
			{
				return true;
			}
		};
		MuisEventListener<MuisElement> removeListener;
		removeListener = new MuisEventListener<MuisElement>() {
			@Override
			public void eventOccurred(MuisEvent<MuisElement> event, MuisElement element)
			{
				removeChild(element);
			}

			@Override
			public boolean isLocal()
			{
				return true;
			}
		};
		parent.addListener(MuisElement.CHILD_ADDED, addListener);
		parent.addListener(MuisElement.CHILD_REMOVED, removeListener);
		theContainerListeners.put(parent, new MuisEventListener[] {addListener, removeListener});
		parent.acceptAttribute(LayoutConstants.direction);
		parent.acceptAttribute(FlowLayout.FLOW_BREAK);
		for(MuisElement child : children)
			allowChild(child);
	}

	@Override
	public void childAdded(MuisElement parent, MuisElement child)
	{
	}

	@Override
	public void childRemoved(MuisElement parent, MuisElement child)
	{
	}

	void allowChild(MuisElement child)
	{
		child.acceptAttribute(left);
		child.acceptAttribute(right);
		child.acceptAttribute(top);
		child.acceptAttribute(bottom);
		child.acceptAttribute(width);
		child.acceptAttribute(height);
	}

	void removeChild(MuisElement child)
	{
		child.rejectAttribute(left);
		child.rejectAttribute(right);
		child.rejectAttribute(top);
		child.rejectAttribute(bottom);
		child.rejectAttribute(width);
		child.rejectAttribute(height);
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
		MuisEventListener<MuisElement> [] listeners = theContainerListeners.get(parent);
		if(listeners == null)
			return;
		parent.removeListener(listeners[0]);
		parent.removeListener(listeners[1]);
		theContainerListeners.remove(parent);
		parent.rejectAttribute(LayoutConstants.direction);
		parent.rejectAttribute(FlowLayout.FLOW_BREAK);
		for(int c = 0; c < parent.getChildCount(); c++)
			removeChild(parent.getChild(c));
	}
}
