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
	public void childAdded(MuisElement parent, MuisElement child)
	{
	}

	@Override
	public void childRemoved(MuisElement parent, MuisElement child)
	{
	}

	@Override
	public SizePolicy getWSizer(MuisElement parent, final MuisElement [] children)
	{
		if(children.length == 0)
			return new SimpleSizePolicy();
		else if(children.length == 1)
			return children[0].getWSizer();
		else
			return new SizePolicy() {
				@Override
				public int getMinPreferred() {
					int ret = 0;
					for(MuisElement child : children) {
						SizePolicy cp = child.getWSizer();
						int cpRes = cp.getMinPreferred();
						if(cpRes > ret)
							ret = cpRes;
					}
					return ret;
				}

				@Override
				public int getMaxPreferred() {
					int ret = Integer.MAX_VALUE;
					for(MuisElement child : children) {
						SizePolicy cp = child.getWSizer();
						int cpRes = cp.getMaxPreferred();
						if(cpRes < ret)
							ret = cpRes;
					}
					return ret;
				}

				@Override
				public int getMin(int crossSize) {
					int ret = 0;
					for(MuisElement child : children) {
						SizePolicy cp = child.getWSizer();
						int cpRes = cp.getMin(crossSize);
						if(cpRes > ret)
							ret = cpRes;
					}
					return ret;
				}

				@Override
				public int getPreferred(int crossSize) {
					int minPref = 0;
					int maxPref = Integer.MAX_VALUE;
					float sumPref = 0;
					for(MuisElement child : children) {
						SizePolicy cp = child.getWSizer();
						int cpRes = cp.getMinPreferred();
						if(cpRes > minPref)
							minPref = cpRes;
						cpRes = cp.getMaxPreferred();
						if(cpRes > maxPref)
							maxPref = cpRes;
						sumPref = cp.getPreferred(crossSize);
					}
					sumPref /= children.length;
					if(sumPref >= minPref && sumPref <= maxPref)
						return Math.round(sumPref);
					else if(sumPref < minPref)
						return minPref;
					else
						return maxPref;
				}

				@Override
				public int getMax(int crossSize) {
					int ret = Integer.MAX_VALUE;
					for(MuisElement child : children) {
						SizePolicy cp = child.getWSizer();
						int cpRes = cp.getMax(crossSize);
						if(cpRes < ret)
							ret = cpRes;
					}
					return ret;
				}

				@Override
				public float getStretch() {
					float ret = 0;
					for(MuisElement child : children) {
						SizePolicy cp = child.getWSizer();
						float cpRes = cp.getStretch();
						if(cpRes > ret)
							ret = cpRes;
					}
					return ret;
				}
			};
	}

	@Override
	public SizePolicy getHSizer(MuisElement parent, final MuisElement [] children)
	{
		if(children.length == 0)
			return new SimpleSizePolicy();
		else if(children.length == 1)
			return children[0].getHSizer();
		else
			return new SizePolicy() {
				@Override
				public int getMinPreferred() {
					int ret = 0;
					for(MuisElement child : children) {
						SizePolicy cp = child.getHSizer();
						int cpRes = cp.getMinPreferred();
						if(cpRes > ret)
							ret = cpRes;
					}
					return ret;
				}

				@Override
				public int getMaxPreferred() {
					int ret = Integer.MAX_VALUE;
					for(MuisElement child : children) {
						SizePolicy cp = child.getHSizer();
						int cpRes = cp.getMaxPreferred();
						if(cpRes < ret)
							ret = cpRes;
					}
					return ret;
				}

				@Override
				public int getMin(int crossSize) {
					int ret = 0;
					for(MuisElement child : children) {
						SizePolicy cp = child.getHSizer();
						int cpRes = cp.getMin(crossSize);
						if(cpRes > ret)
							ret = cpRes;
					}
					return ret;
				}

				@Override
				public int getPreferred(int crossSize) {
					int minPref = 0;
					int maxPref = Integer.MAX_VALUE;
					float sumPref = 0;
					for(MuisElement child : children) {
						SizePolicy cp = child.getHSizer();
						int cpRes = cp.getMinPreferred();
						if(cpRes > minPref)
							minPref = cpRes;
						cpRes = cp.getMaxPreferred();
						if(cpRes > maxPref)
							maxPref = cpRes;
						sumPref = cp.getPreferred(crossSize);
					}
					sumPref /= children.length;
					if(sumPref >= minPref && sumPref <= maxPref)
						return Math.round(sumPref);
					else if(sumPref < minPref)
						return minPref;
					else
						return maxPref;
				}

				@Override
				public int getMax(int crossSize) {
					int ret = Integer.MAX_VALUE;
					for(MuisElement child : children) {
						SizePolicy cp = child.getHSizer();
						int cpRes = cp.getMax(crossSize);
						if(cpRes < ret)
							ret = cpRes;
					}
					return ret;
				}

				@Override
				public float getStretch() {
					float ret = 0;
					for(MuisElement child : children) {
						SizePolicy cp = child.getHSizer();
						float cpRes = cp.getStretch();
						if(cpRes > ret)
							ret = cpRes;
					}
					return ret;
			}
			};
	}

	@Override
	public void layout(MuisElement parent, MuisElement [] children)
	{
		for(MuisElement child : children)
			child.bounds().setBounds(0, 0, parent.bounds().getWidth(), parent.bounds().getHeight());
	}

	@Override
	public void remove(MuisElement parent)
	{
	}
}
