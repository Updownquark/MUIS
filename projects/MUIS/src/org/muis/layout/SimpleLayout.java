package org.muis.layout;

import java.awt.Rectangle;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisElement;
import org.muis.core.MuisLayout;

public class SimpleLayout implements MuisLayout
{
	@Override
	public void initChildren(MuisElement parent, MuisElement [] children)
	{
		for(MuisElement child : children)
		{
			child.acceptAttribute(LayoutConstants.left);
			child.acceptAttribute(LayoutConstants.right);
			child.acceptAttribute(LayoutConstants.top);
			child.acceptAttribute(LayoutConstants.bottom);
			child.acceptAttribute(LayoutConstants.width);
			child.acceptAttribute(LayoutConstants.height);
		}
	}

	@Override
	public SizePolicy getWSizer(MuisElement parent, MuisElement [] children, int parentHeight)
	{
		return getSizer(children, LayoutConstants.left, LayoutConstants.right, LayoutConstants.width, parent.getWidth(), parentHeight);
	}

	@Override
	public SizePolicy getHSizer(MuisElement parent, MuisElement [] children, int parentWidth)
	{
		return getSizer(children, LayoutConstants.top, LayoutConstants.bottom, LayoutConstants.height, parent.getHeight(), parentWidth);
	}

	public SizePolicy getSizer(MuisElement [] children, MuisAttribute<Length> low, MuisAttribute<Length> high, MuisAttribute<Length> size,
		int length, int breadth)
	{
		SimpleSizePolicy ret = new SimpleSizePolicy();
		ret.setMax(0);
		for(MuisElement child : children)
		{
			SizePolicy childSizer = child.getWSizer(breadth);
			int x;
			if(child.getAttribute(high) != null)
			{
				int r = child.getAttribute(high).evaluate(length);
				if(ret.getMin() < r)
					ret.setMin(r);
				if(ret.getPreferred() < r)
					ret.setPreferred(r);
				if(ret.getMax() < r)
					ret.setMax(r);
			}
			else if(child.getAttribute(size) != null)
			{
				int w = child.getAttribute(size).evaluate(length);
				x = LayoutConstants.getLayoutValue(child, low, length, 0);
				if(ret.getMin() < x + w)
					ret.setMin(x + w);
				if(ret.getPreferred() < x + w)
					ret.setPreferred(x + w);
				if(ret.getMax() < x + w)
					ret.setMax(x + w);
			}
			else
			{
				x = LayoutConstants.getLayoutValue(child, low, length, 0);
				if(ret.getMin() < x + childSizer.getMin())
					ret.setMin(x + childSizer.getMin());
				if(ret.getPreferred() < x + childSizer.getPreferred())
					ret.setPreferred(x + childSizer.getPreferred());
				if(ret.getMax() < x + childSizer.getMax())
					ret.setMax(x + childSizer.getMax());
			}
		}
		return ret;
	}

	@Override
	public void layout(MuisElement parent, MuisElement [] children, Rectangle box)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(MuisElement parent)
	{
		// TODO Auto-generated method stub

	}

}
