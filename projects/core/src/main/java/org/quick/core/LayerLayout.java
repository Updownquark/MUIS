package org.quick.core;

import org.observe.Observable;
import org.quick.core.layout.AbstractSizeGuide;
import org.quick.core.layout.SimpleSizeGuide;
import org.quick.core.layout.SizeGuide;

/** A very simple layout that sizes children the same as their container */
public class LayerLayout implements org.quick.core.QuickLayout {
	@Override
	public void install(QuickElement parent, Observable<?> until) {
	}

	@Override
	public SizeGuide getWSizer(QuickElement parent, final QuickElement [] children) {
		if(children.length == 0)
			return new SimpleSizeGuide();
		else if(children.length == 1)
			return children[0].getWSizer();
		else
			return new AbstractSizeGuide() {
				@Override
				public int getMinPreferred(int crossSize, boolean csMax) {
					int ret = 0;
					for(QuickElement child : children) {
						SizeGuide cp = child.getWSizer();
						int cpRes = cp.getMinPreferred(crossSize, csMax);
						if(cpRes > ret)
							ret = cpRes;
					}
					return ret;
				}

				@Override
				public int getMaxPreferred(int crossSize, boolean csMax) {
					int ret = Integer.MAX_VALUE;
					for(QuickElement child : children) {
						SizeGuide cp = child.getWSizer();
						int cpRes = cp.getMaxPreferred(crossSize, csMax);
						if(cpRes < ret)
							ret = cpRes;
					}
					return ret;
				}

				@Override
				public int getMin(int crossSize, boolean csMax) {
					int ret = 0;
					for(QuickElement child : children) {
						SizeGuide cp = child.getWSizer();
						int cpRes = cp.getMin(crossSize, csMax);
						if(cpRes > ret)
							ret = cpRes;
					}
					return ret;
				}

				@Override
				public int getPreferred(int crossSize, boolean csMax) {
					int minPref = 0;
					int maxPref = Integer.MAX_VALUE;
					int maxOfPreferred = 0;
					for(QuickElement child : children) {
						SizeGuide cp = child.getWSizer();
						int cpRes = cp.getMinPreferred(crossSize, csMax);
						if(cpRes > minPref)
							minPref = cpRes;
						cpRes = cp.getMaxPreferred(crossSize, csMax);
						if(cpRes < maxPref)
							maxPref = cpRes;
						cpRes = cp.getPreferred(crossSize, csMax);
						if(cpRes > maxOfPreferred)
							maxOfPreferred = cpRes;
					}
					if(maxOfPreferred >= minPref && maxOfPreferred <= maxPref)
						return maxOfPreferred;
					else if(maxOfPreferred < minPref)
						return minPref;
					else
						return maxPref;
				}

				@Override
				public int getMax(int crossSize, boolean csMax) {
					int ret = Integer.MAX_VALUE;
					for(QuickElement child : children) {
						SizeGuide cp = child.getWSizer();
						int cpRes = cp.getMax(crossSize, csMax);
						if(cpRes < ret)
							ret = cpRes;
					}
					return ret;
				}

				@Override
				public int getBaseline(int size) {
					if(children.length == 0)
						return 0;
					return children[0].getWSizer().getBaseline(size);
				}
			};
	}

	@Override
	public SizeGuide getHSizer(QuickElement parent, final QuickElement [] children) {
		if(children.length == 0)
			return new SimpleSizeGuide();
		else if(children.length == 1)
			return children[0].getHSizer();
		else
			return new AbstractSizeGuide() {
				@Override
				public int getMinPreferred(int crossSize, boolean csMax) {
					int ret = 0;
					for(QuickElement child : children) {
						SizeGuide cp = child.getHSizer();
						int cpRes = cp.getMinPreferred(crossSize, csMax);
						if(cpRes > ret)
							ret = cpRes;
					}
					return ret;
				}

				@Override
				public int getMaxPreferred(int crossSize, boolean csMax) {
					int ret = Integer.MAX_VALUE;
					for(QuickElement child : children) {
						SizeGuide cp = child.getHSizer();
						int cpRes = cp.getMaxPreferred(crossSize, csMax);
						if(cpRes < ret)
							ret = cpRes;
					}
					return ret;
				}

				@Override
				public int getMin(int crossSize, boolean csMax) {
					int ret = 0;
					for(QuickElement child : children) {
						SizeGuide cp = child.getHSizer();
						int cpRes = cp.getMin(crossSize, csMax);
						if(cpRes > ret)
							ret = cpRes;
					}
					return ret;
				}

				@Override
				public int getPreferred(int crossSize, boolean csMax) {
					int minPref = 0;
					int maxPref = Integer.MAX_VALUE;
					float sumPref = 0;
					for(QuickElement child : children) {
						SizeGuide cp = child.getHSizer();
						int cpRes = cp.getMinPreferred(crossSize, csMax);
						if(cpRes > minPref)
							minPref = cpRes;
						cpRes = cp.getMaxPreferred(crossSize, csMax);
						if(cpRes > maxPref)
							maxPref = cpRes;
						sumPref = cp.getPreferred(crossSize, csMax);
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
				public int getMax(int crossSize, boolean csMax) {
					int ret = Integer.MAX_VALUE;
					for(QuickElement child : children) {
						SizeGuide cp = child.getHSizer();
						int cpRes = cp.getMax(crossSize, csMax);
						if(cpRes < ret)
							ret = cpRes;
					}
					return ret;
				}

				@Override
				public int getBaseline(int size) {
					if(children.length == 0)
						return size;
					return children[0].getHSizer().getBaseline(size);
				}
			};
	}

	@Override
	public void layout(QuickElement parent, QuickElement [] children) {
		for(QuickElement child : children)
			child.bounds().setBounds(0, 0, parent.bounds().getWidth(), parent.bounds().getHeight());
	}

	@Override
	public String toString() {
		return "layer";
	}
}
