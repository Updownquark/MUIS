package org.quick.core;

import java.util.Iterator;
import java.util.List;

import org.observe.Observable;
import org.quick.core.layout.LayoutGuideType;
import org.quick.core.layout.Orientation;
import org.quick.core.layout.SimpleSizeGuide;
import org.quick.core.layout.SizeGuide;

/** A very simple layout that sizes children the same as their container */
public class LayerLayout implements org.quick.core.QuickLayout {
	@Override
	public void install(QuickElement parent, Observable<?> until) {}

	@Override
	public SizeGuide getSizer(QuickElement parent, Iterable<? extends QuickElement> children, Orientation orientation) {
		if (!children.iterator().hasNext())
			return new SimpleSizeGuide();
		return new SizeGuide.GenericSizeGuide() {
			@Override
			public int get(LayoutGuideType type, int crossSize, boolean csMax) {
				if (type == LayoutGuideType.pref)
					return getPreferred(crossSize, csMax);
				int ret = type.isMin() ? 0 : Integer.MAX_VALUE;
				for (QuickElement child : children) {
					SizeGuide cp = child.getSizer(orientation);
					int cpRes = cp.get(type, crossSize, csMax);
					if (type.isMin()) {
						if (cpRes > ret)
							ret = cpRes;
					} else {
						if (cpRes < ret)
							ret = cpRes;
					}
				}
				return ret;
			}

			@Override
			public int getPreferred(int crossSize, boolean csMax) {
				int minPref = 0;
				int maxPref = Integer.MAX_VALUE;
				int maxOfPreferred = 0;
				for (QuickElement child : children) {
					SizeGuide cp = child.getSizer(orientation);
					int cpRes = cp.getMinPreferred(crossSize, csMax);
					if (cpRes > minPref)
						minPref = cpRes;
					cpRes = cp.getMaxPreferred(crossSize, csMax);
					if (cpRes < maxPref)
						maxPref = cpRes;
					cpRes = cp.getPreferred(crossSize, csMax);
					if (cpRes > maxOfPreferred)
						maxOfPreferred = cpRes;
				}
				if (maxOfPreferred >= minPref && maxOfPreferred <= maxPref)
					return maxOfPreferred;
				else if (maxOfPreferred < minPref)
					return minPref;
				else
					return maxPref;
			}

			@Override
			public int getBaseline(int size) {
				Iterator<? extends QuickElement> iter = children.iterator();
				if (!iter.hasNext())
					return 0;
				return iter.next().getSizer(orientation).getBaseline(size);
			}
		};
	}

	@Override
	public void layout(QuickElement parent, List<? extends QuickElement> children) {
		for (QuickElement child : children)
			child.bounds().setBounds(0, 0, parent.bounds().getWidth(), parent.bounds().getHeight());
	}

	@Override
	public String toString() {
		return "layer";
	}
}
