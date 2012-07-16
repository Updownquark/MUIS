package org.muis.base.layout;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisElement;
import org.muis.core.event.MuisEvent;
import org.muis.core.layout.SimpleSizePolicy;
import org.muis.core.layout.SizePolicy;
import org.muis.core.style.Position;
import org.muis.core.style.Size;

/**
 * Lays components out by {@link Region regions}. Containers with this layout may have any number of components in any region except center,
 * which may have zero or one component in it.
 */
public class BorderLayout implements org.muis.core.MuisLayout
{
	private static class RelayoutListener implements org.muis.core.event.MuisEventListener<MuisAttribute<?>>
	{
		private final MuisElement theParent;

		RelayoutListener(MuisElement parent)
		{
			theParent = parent;
		}

		@Override
		public void eventOccurred(MuisEvent<MuisAttribute<?>> event, MuisElement element)
		{
			MuisAttribute<?> attr = event.getValue();
			if(attr == LayoutConstants.region)
			{
				switch (element.getAttribute(LayoutConstants.region))
				{
				case left:
					element.rejectAttribute(LayoutConstants.left);
					element.rejectAttribute(LayoutConstants.top);
					element.rejectAttribute(LayoutConstants.bottom);
					element.acceptAttribute(LayoutConstants.right);
					break;
				case top:
					element.rejectAttribute(LayoutConstants.left);
					element.rejectAttribute(LayoutConstants.top);
					element.rejectAttribute(LayoutConstants.right);
					element.acceptAttribute(LayoutConstants.bottom);
					break;
				case right:
					element.rejectAttribute(LayoutConstants.right);
					element.rejectAttribute(LayoutConstants.top);
					element.rejectAttribute(LayoutConstants.bottom);
					element.acceptAttribute(LayoutConstants.left);
					break;
				case bottom:
					element.rejectAttribute(LayoutConstants.left);
					element.rejectAttribute(LayoutConstants.bottom);
					element.rejectAttribute(LayoutConstants.right);
					element.acceptAttribute(LayoutConstants.top);
					break;
				case center:
					element.rejectAttribute(LayoutConstants.left);
					element.rejectAttribute(LayoutConstants.right);
					element.rejectAttribute(LayoutConstants.top);
					element.rejectAttribute(LayoutConstants.bottom);
					break;
				}
			}
			if(attr == LayoutConstants.region || attr == LayoutConstants.left || attr == LayoutConstants.right
				|| attr == LayoutConstants.top || attr == LayoutConstants.bottom || attr == LayoutConstants.width
				|| attr == LayoutConstants.height || attr == LayoutConstants.minWidth || attr == LayoutConstants.minHeight)
				theParent.relayout(false);
		}

		@Override
		public boolean isLocal()
		{
			return true;
		}
	}

	@Override
	public void initChildren(MuisElement parent, MuisElement [] children)
	{
		RelayoutListener listener = new RelayoutListener(parent);
		for(MuisElement child : children)
		{
			child.requireAttribute(LayoutConstants.region);
			child.acceptAttribute(LayoutConstants.width);
			child.acceptAttribute(LayoutConstants.height);
			child.acceptAttribute(LayoutConstants.minWidth);
			child.acceptAttribute(LayoutConstants.minHeight);
			child.addListener(MuisElement.ATTRIBUTE_SET, listener);
		}
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
	public SizePolicy getWSizer(MuisElement parent, MuisElement [] children, int parentHeight)
	{

		SimpleSizePolicy ret = new SimpleSizePolicy();
		for(MuisElement child : children)
		{
			Position pos;
			Size size = child.getAttribute(LayoutConstants.width);
			Size minSize = child.getAttribute(LayoutConstants.minWidth);
			SizePolicy sizer;
			switch (child.getAttribute(LayoutConstants.region))
			{
			case left:
				pos = child.getAttribute(LayoutConstants.right);
				if(pos != null && !pos.getUnit().isRelative())
				{
					ret.setMin(ret.getMin() + pos.evaluate(0));
					ret.setPreferred(ret.getPreferred() + pos.evaluate(0));
				}
				else if(size != null && !size.getUnit().isRelative())
				{
					ret.setMin(ret.getMin() + size.evaluate(0));
					ret.setPreferred(ret.getPreferred() + size.evaluate(0));
				}
				else
				{
					sizer = child.getWSizer(parentHeight);
					ret.setMin(ret.getMin() + sizer.getMin());
					ret.setPreferred(ret.getPreferred() + sizer.getPreferred());
				}
				break;
			case right:
				pos = child.getAttribute(LayoutConstants.right);
				if(pos != null && !pos.getUnit().isRelative())
				{
					ret.setMin(ret.getMin() + pos.evaluate(0));
					ret.setPreferred(ret.getPreferred() + pos.evaluate(0));
				}
				else if(size != null && !size.getUnit().isRelative())
				{
					ret.setMin(ret.getMin() + size.evaluate(0));
					ret.setPreferred(ret.getPreferred() + size.evaluate(0));
				}
				else
				{
					sizer = child.getWSizer(parentHeight);
					ret.setMin(ret.getMin() + sizer.getMin());
					ret.setPreferred(ret.getPreferred() + sizer.getPreferred());
				}
				break;
			case top:
				if(size != null && !size.getUnit().isRelative())
				{
					ret.setMin(ret.getMin() + size.evaluate(0));
					ret.setPreferred(ret.getPreferred() + size.evaluate(0));
				}
				else
				{
					sizer = child.getWSizer(parentHeight);
					ret.setMin(ret.getMin() + sizer.getMin());
					ret.setPreferred(ret.getPreferred() + sizer.getPreferred());
				}
				break;
			case bottom:
			case center:
			}
		}
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SizePolicy getHSizer(MuisElement parent, MuisElement [] children, int parentWidth)
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
		for(MuisElement child : parent.getChildren())
		{
			child.removeListener(MuisElement.ATTRIBUTE_SET, RelayoutListener.class);
			child.rejectAttribute(LayoutConstants.region);
			child.rejectAttribute(LayoutConstants.width);
			child.rejectAttribute(LayoutConstants.height);
			child.rejectAttribute(LayoutConstants.minWidth);
			child.rejectAttribute(LayoutConstants.minHeight);
		}
	}
}
