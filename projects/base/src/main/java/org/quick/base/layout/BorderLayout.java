package org.quick.base.layout;

import static org.quick.core.layout.LayoutAttributes.*;
import static org.quick.core.layout.Orientation.horizontal;
import static org.quick.core.layout.Orientation.vertical;

import java.awt.Rectangle;

import org.quick.core.QuickElement;
import org.quick.core.layout.*;
import org.quick.core.style.LayoutStyle;
import org.quick.core.style.Size;
import org.quick.util.CompoundListener;
import org.quick.util.CompoundListener.CompoundElementListener;

/**
 * Lays components out by {@link Region regions}. Containers with this layout may have any number of components in any region except center,
 * which may have zero or one component in it.
 */
public class BorderLayout implements org.quick.core.QuickLayout {
	private final CompoundListener.MultiElementCompoundListener theListener;

	/** Creates a border layout */
	public BorderLayout() {
		theListener = CompoundListener.create(this);
		theListener.child().accept(region).onChange(theListener.individualChecker(false)).onChange(CompoundListener.sizeNeedsChanged);
		theListener.eachChild(new CompoundListener.IndividualElementListener() {
			@Override
			public void individual(QuickElement element, CompoundElementListener listener) {
				listener.chain(Region.left.name()).acceptAll(width, minWidth, maxWidth, right, minRight, maxRight)
					.onChange(CompoundListener.sizeNeedsChanged);
				listener.chain(Region.right.name()).acceptAll(width, minWidth, maxWidth, left, minLeft, maxLeft)
					.onChange(CompoundListener.sizeNeedsChanged);
				listener.chain(Region.top.name()).acceptAll(height, minHeight, maxHeight, bottom, minBottom, maxBottom)
					.onChange(CompoundListener.sizeNeedsChanged);
				listener.chain(Region.bottom.name()).acceptAll(height, minHeight, maxHeight, top, minTop, maxTop)
					.onChange(CompoundListener.sizeNeedsChanged);
				update(element, listener);
			}

			@Override
			public void update(QuickElement element, CompoundElementListener listener) {
				listener.chain(Region.left.name()).setActive(element.atts().get(region) == Region.left);
				listener.chain(Region.right.name()).setActive(element.atts().get(region) == Region.right);
				listener.chain(Region.top.name()).setActive(element.atts().get(region) == Region.top);
				listener.chain(Region.bottom.name()).setActive(element.atts().get(region) == Region.bottom);
			}
		});
	}

	@Override
	public void initChildren(QuickElement parent, QuickElement [] children) {
		theListener.listenerFor(parent);
	}

	@Override
	public void childAdded(QuickElement parent, QuickElement child) {
	}

	@Override
	public void childRemoved(QuickElement parent, QuickElement child) {
	}

	@Override
	public void remove(QuickElement parent) {
		theListener.dropFor(parent);
	}

	@Override
	public SizeGuide getWSizer(QuickElement parent, QuickElement [] children) {
		return getSizer(parent, children, horizontal);
	}

	@Override
	public SizeGuide getHSizer(QuickElement parent, QuickElement [] children) {
		return getSizer(parent, children, vertical);
	}

	/**
	 * @param parent The container to lay the elements out in
	 * @param children The elements to lay out
	 * @param orient The orientation to get the sizer for
	 * @return The sizer for this layout with the given contents and the given orientation
	 */
	public SizeGuide getSizer(final QuickElement parent, final QuickElement [] children, final Orientation orient) {
		return new SizeGuide() {
			@Override
			public int getMin(int crossSize, boolean csMax) {
				return get(LayoutGuideType.min, crossSize, csMax);
			}

			@Override
			public int getMinPreferred(int crossSize, boolean csMax) {
				return get(LayoutGuideType.minPref, crossSize, csMax);
			}

			@Override
			public int getPreferred(int crossSize, boolean csMax) {
				return get(LayoutGuideType.pref, crossSize, csMax);
			}

			@Override
			public int getMaxPreferred(int crossSize, boolean csMax) {
				return get(LayoutGuideType.maxPref, crossSize, csMax);
			}

			@Override
			public int getMax(int crossSize, boolean csMax) {
				return get(LayoutGuideType.max, crossSize, csMax);
			}

			@Override
			public int get(LayoutGuideType type, int crossSize, boolean csMax) {
				Size margin = parent.getStyle().getSelf().get(LayoutStyle.margin).get();
				Size padding = parent.getStyle().getSelf().get(LayoutStyle.padding).get();
				LayoutSize ret = get(type, crossSize, csMax, 0, padding, null);
				ret.add(margin);
				ret.add(margin);
				return ret.getTotal();
			}

			private LayoutSize get(LayoutGuideType type, int crossSize, boolean csMax, int startIndex, Size padding, QuickElement center) {
				Region childRegion = children[startIndex].atts().get(region, Region.center);
				if(childRegion == Region.center) {
					if(center != null) {
						// Error later, in layout()
					} else if(startIndex < children.length - 1)
						center = children[startIndex];
					if(startIndex < children.length - 1)
						return get(type, crossSize, csMax, startIndex + 1, padding, center);
					else {
						LayoutSize ret = new LayoutSize();
						if(center != null)
							LayoutUtils.getSize(center, orient, type, 0, crossSize, csMax, ret);
						return ret;
					}
				} else if(childRegion.getOrientation() == orient) {
					LayoutSize ret = new LayoutSize();
					while(childRegion.getOrientation() == orient) {
						LayoutUtils.getSize(children[startIndex], orient, type, 0, crossSize, csMax, ret);
						ret.add(padding);
						startIndex++;
						if(startIndex == children.length)
							break;
						childRegion = children[startIndex].atts().get(region, Region.center);
					}
					if(startIndex < children.length)
						ret.add(get(type, crossSize, csMax, startIndex, padding, center));
					else if(center != null)
						LayoutUtils.getSize(center, orient, type, 0, crossSize, csMax, ret);
					return ret;
				} else {
					LayoutSize ret = new LayoutSize(true);
					while(childRegion.getOrientation() != orient) {
						LayoutUtils.getSize(children[startIndex], orient, type, 0, crossSize, csMax, ret);
						startIndex++;
						if(startIndex == children.length)
							break;
						childRegion = children[startIndex].atts().get(region, Region.center);
					}
					if(startIndex < children.length)
						ret.add(get(type, crossSize, csMax, startIndex, padding, center));
					else if(center != null) {
						ret = new LayoutSize(ret);
						LayoutUtils.getSize(center, orient, type, 0, crossSize, csMax, ret);
					}
					return ret;
				}
			}

			@Override
			public int getBaseline(int size) {
				return 0;
			}
		};
	}

	@Override
	public void layout(final QuickElement parent, final QuickElement [] children) {
		final int parentWidth = parent.bounds().getWidth();
		final int parentHeight = parent.bounds().getHeight();
		final Size margin = parent.getStyle().getSelf().get(LayoutStyle.margin).get();
		final Size padding = parent.getStyle().getSelf().get(LayoutStyle.padding).get();
		LayoutUtils.LayoutInterpolation<int []> wResult = LayoutUtils.interpolate(new LayoutUtils.LayoutChecker<int []>() {
			@Override
			public int [] getLayoutValue(LayoutGuideType type) {
				int [] ret = new int[children.length];
				for(int c = 0; c < children.length; c++)
					ret[c] = LayoutUtils.getSize(children[c], horizontal, type, parentWidth, parentHeight, true, null);
				return ret;
			}

			@Override
			public int getSize(int [] layoutValue) {
				int ret = 0;
				ret += margin.evaluate(parentWidth) * 2;
				ret += getSize(layoutValue, 0, -1);
				return ret;
			}

			private int getSize(int [] layoutValue, int startIndex, int centerIndex) {
				Region childRegion = children[startIndex].atts().get(region, Region.center);
				if(childRegion == Region.center) {
					if(centerIndex >= 0) {
						// Error later
					} else
						centerIndex = startIndex;
					if(startIndex < children.length - 1)
						return getSize(layoutValue, startIndex + 1, centerIndex);
					else
						return layoutValue[centerIndex];
				} else if(childRegion.getOrientation() == horizontal) {
					int ret = 0;
					while(childRegion.getOrientation() == horizontal) {
						ret += layoutValue[startIndex];
						ret += padding.evaluate(parentWidth);
						startIndex++;
						if(startIndex == children.length)
							break;
						childRegion = children[startIndex].atts().get(region, Region.center);
					}
					if(startIndex < children.length)
						ret += getSize(layoutValue, startIndex, centerIndex);
					else if(centerIndex >= 0)
						ret += layoutValue[centerIndex];
					return ret;
				} else {
					int ret = 0;
					while(childRegion.getOrientation() == vertical) {
						if(layoutValue[startIndex] > ret)
							ret = layoutValue[startIndex];
						startIndex++;
						if(startIndex == children.length)
							break;
						childRegion = children[startIndex].atts().get(region, Region.center);
					}
					if(startIndex < children.length) {
						int restRet = getSize(layoutValue, startIndex, centerIndex);
						if(restRet > ret)
							ret = restRet;
					} else if(centerIndex >= 0)
						ret += layoutValue[centerIndex];
					return ret;
				}
			}
		}, parent.bounds().getWidth(), LayoutGuideType.min, LayoutGuideType.max);

		final Rectangle [] bounds = new Rectangle[children.length];
		for(int c = 0; c < bounds.length; c++) {
			bounds[c] = new Rectangle();
			bounds[c].width = wResult.lowerValue[c];
			if(wResult.proportion > 0)
				bounds[c].width += Math.round(wResult.proportion * (wResult.upperValue[c] - wResult.lowerValue[c]));
		}

		LayoutUtils.LayoutInterpolation<int []> hResult = LayoutUtils.interpolate(new LayoutUtils.LayoutChecker<int []>() {
			@Override
			public int [] getLayoutValue(LayoutGuideType type) {
				int [] ret = new int[children.length];
				for(int c = 0; c < children.length; c++)
					ret[c] = LayoutUtils.getSize(children[c], vertical, type, parentHeight, bounds[c].width, false, null);
				return ret;
			}

			@Override
			public int getSize(int [] layoutValue) {
				int ret = 0;
				ret += margin.evaluate(parentHeight) * 2;
				ret += getSize(layoutValue, 0, -1);
				return ret;
			}

			private int getSize(int [] layoutValue, int startIndex, int centerIndex) {
				Region childRegion = children[startIndex].atts().get(region, Region.center);
				if(childRegion == Region.center) {
					if(centerIndex >= 0) {
						// Error later
					} else
						centerIndex = startIndex;
					if(startIndex < children.length - 1)
						return getSize(layoutValue, startIndex + 1, centerIndex);
					else
						return layoutValue[centerIndex];
				} else if(childRegion.getOrientation() == vertical) {
					int ret = 0;
					while(childRegion.getOrientation() == vertical) {
						ret += layoutValue[startIndex];
						ret += padding.evaluate(parentHeight);
						startIndex++;
						if(startIndex == children.length)
							break;
						childRegion = children[startIndex].atts().get(region, Region.center);
					}
					if(startIndex < children.length)
						ret += getSize(layoutValue, startIndex, centerIndex);
					else if(centerIndex >= 0)
						ret += layoutValue[centerIndex];
					return ret;
				} else {
					int ret = 0;
					while(childRegion.getOrientation() == horizontal) {
						if(layoutValue[startIndex] > ret)
							ret = layoutValue[startIndex];
						startIndex++;
						if(startIndex == children.length)
							break;
						childRegion = children[startIndex].atts().get(region, Region.center);
					}
					if(startIndex < children.length) {
						int restRet = getSize(layoutValue, startIndex, centerIndex);
						if(restRet > ret)
							ret = restRet;
					} else if(centerIndex >= 0)
						ret += layoutValue[centerIndex];
					return ret;
				}
			}
		}, parent.bounds().getHeight(), LayoutGuideType.min, LayoutGuideType.max);

		for(int c = 0; c < bounds.length; c++) {
			bounds[c].height = hResult.lowerValue[c];
			if(hResult.proportion > 0)
				bounds[c].height += Math.round(hResult.proportion * (hResult.upperValue[c] - hResult.lowerValue[c]));
		}

		// Got the sizes. Now put them in place.
		int leftEdge = margin.evaluate(parentWidth);
		int rightEdge = parentWidth - margin.evaluate(parentWidth);
		int topEdge = margin.evaluate(parentHeight);
		int bottomEdge = parentHeight - margin.evaluate(parentHeight);
		int centerIndex = -1;
		for(int c = 0; c < bounds.length; c++) {
			Region childRegion = children[c].atts().get(region, Region.center);
			if(childRegion == Region.center) {
				centerIndex = c;
				continue; // Lay out center last
			}
			switch (childRegion) {
			case left:
				bounds[c].x = leftEdge;
				bounds[c].y = topEdge;
				bounds[c].height = bottomEdge - topEdge;
				leftEdge += bounds[c].width + padding.evaluate(parentWidth);
				break;
			case right:
				bounds[c].x = rightEdge - bounds[c].width;
				bounds[c].y = topEdge;
				bounds[c].height = bottomEdge - topEdge;
				rightEdge -= bounds[c].width + padding.evaluate(parentWidth);
				break;
			case top:
				bounds[c].x = leftEdge;
				bounds[c].y = topEdge;
				bounds[c].width = rightEdge - leftEdge;
				topEdge += bounds[c].height + padding.evaluate(parentHeight);
				break;
			case bottom:
				bounds[c].x = leftEdge;
				bounds[c].y = bottomEdge - bounds[c].height;
				bounds[c].width = rightEdge - leftEdge;
				bottomEdge -= bounds[c].height + padding.evaluate(parentHeight);
				break;
			case center:
				if(centerIndex >= 0) {
					parent.msg().error(
						"Only one element may be in the center region in a border layout.  Only first center will be layed out.",
						"element", children[c]);
				} else
					centerIndex = c;
			}
		}

		if(centerIndex >= 0) {
			bounds[centerIndex].x = leftEdge;
			bounds[centerIndex].y = topEdge;
			bounds[centerIndex].width = rightEdge - leftEdge;
			bounds[centerIndex].height = bottomEdge - topEdge;
		}

		for(int c = 0; c < children.length; c++)
			children[c].bounds().setBounds(bounds[c].x, bounds[c].y, bounds[c].width, bounds[c].height);
	}
}
