package org.muis.core;

import org.muis.core.layout.SimpleSizePolicy;
import org.muis.core.layout.SizePolicy;

/** A very simple layout that sizes children the same as their container */
public class LayerLayout implements org.muis.core.MuisLayout
{
	@Override
	public void initChildren(MuisElement parent, MuisElement [] children)
	{
	}

	@Override
	public SizePolicy getWSizer(MuisElement parent, MuisElement [] children, int parentHeight)
	{
		if(children.length == 0)
			return new SimpleSizePolicy();
		else if(children.length == 1)
			return children[0].getWSizer(parentHeight);
		else
		{
			SimpleSizePolicy ret = new SimpleSizePolicy();
			for(MuisElement child : children)
			{
				SizePolicy cp = child.getWSizer(parentHeight);
				if(ret.getMin() < cp.getMin())
					ret.setMin(cp.getMin());
				if(ret.getMax() > cp.getMax())
					if(cp.getMax() >= ret.getMin())
						ret.setMax(cp.getMax());
				if(ret.getPreferred() < cp.getPreferred())
					if(cp.getPreferred() >= ret.getMin() && cp.getPreferred() <= ret.getMax())
						ret.setPreferred(cp.getPreferred());
			}
			return ret;
		}
	}

	@Override
	public SizePolicy getHSizer(MuisElement parent, MuisElement [] children, int parentWidth)
	{
		if(children.length == 0)
			return new SimpleSizePolicy();
		else if(children.length == 1)
			return children[0].getHSizer(parentWidth);
		else
		{
			SimpleSizePolicy ret = new SimpleSizePolicy();
			for(MuisElement child : children)
			{
				SizePolicy cp = child.getHSizer(parentWidth);
				if(ret.getMin() < cp.getMin())
					ret.setMin(cp.getMin());
				if(ret.getMax() > cp.getMax())
					if(cp.getMax() >= ret.getMin())
						ret.setMax(cp.getMax());
				if(ret.getPreferred() < cp.getPreferred())
					if(cp.getPreferred() >= ret.getMin() && cp.getPreferred() <= ret.getMax())
						ret.setPreferred(cp.getPreferred());
			}
			return ret;
		}
	}

	@Override
	public void layout(MuisElement parent, MuisElement [] children)
	{
		for(MuisElement child : children)
			child.setBounds(0, 0, parent.getWidth(), parent.getHeight());
	}

	@Override
	public void remove(MuisElement parent)
	{
	}
}
