package org.quick.base.layout;

import static org.quick.core.layout.LayoutAttributes.*;
import static org.quick.core.layout.LayoutUtils.add;
import static org.quick.core.layout.Orientation.horizontal;
import static org.quick.core.layout.Orientation.vertical;

import org.observe.Observable;
import org.quick.core.QuickElement;
import org.quick.core.Rectangle;
import org.quick.core.layout.*;
import org.quick.core.style.LayoutStyle;
import org.quick.core.style.Size;
import org.quick.util.CompoundListener;

/**
 * Lays components out by {@link Region regions}. Containers with this layout may have any number of components in any region except center,
 * which may have zero or one component in it.
 */
public class BorderLayout implements org.quick.core.QuickLayout {
	private final CompoundListener theListener;

	/** Creates a border layout */
	public BorderLayout() {
		theListener = CompoundListener.build()//
			.watchAll(LayoutStyle.margin, LayoutStyle.padding).onEvent(CompoundListener.sizeNeedsChanged)//
			.child(builder -> {
				builder.accept(region).onEvent(CompoundListener.sizeNeedsChanged);
				builder.when(el -> el.getAttribute(region) == Region.left, builder2 -> {
					builder2.acceptAll(width, minWidth, maxWidth, right, minRight, maxRight).onEvent(CompoundListener.sizeNeedsChanged);
				});
				builder.when(el -> el.getAttribute(region) == Region.right, builder2 -> {
					builder2.acceptAll(width, minWidth, maxWidth, left, minLeft, maxLeft).onEvent(CompoundListener.sizeNeedsChanged);
				});
				builder.when(el -> el.getAttribute(region) == Region.top, builder2 -> {
					builder2.acceptAll(height, minHeight, maxHeight, bottom, minBottom, maxBottom)
						.onEvent(CompoundListener.sizeNeedsChanged);
				});
				builder.when(el -> el.getAttribute(region) == Region.bottom, builder2 -> {
					builder2.acceptAll(height, minHeight, maxHeight, top, minTop, maxTop).onEvent(CompoundListener.sizeNeedsChanged);
				});
			})//
			.build();
	}

	@Override
	public void install(QuickElement parent, Observable<?> until) {
		theListener.listen(parent, parent, until);
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
				Size margin = parent.getStyle().get(LayoutStyle.margin).get();
				Size padding = parent.getStyle().get(LayoutStyle.padding).get();
				if (children.length == 0) {
					if (type == LayoutGuideType.min)
						return 0;
					return margin.evaluate(0) * 2;
				}
				LayoutSize ret = get(type, crossSize, csMax, 0, padding, null);
				ret.add(margin);
				ret.add(margin);
				return ret.getTotal();
			}

			private LayoutSize get(LayoutGuideType type, int crossSize, boolean csMax, int startIndex, Size padding, QuickElement center) {
				Region childRegion = children[startIndex].atts().getValue(region, Region.center);
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
						childRegion = children[startIndex].atts().getValue(region, Region.center);
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
						childRegion = children[startIndex].atts().getValue(region, Region.center);
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
		final Size margin = parent.getStyle().get(LayoutStyle.margin).get();
		final Size padding = parent.getStyle().get(LayoutStyle.padding).get();
		LayoutUtils.LayoutInterpolation<int []> wResult = LayoutUtils.interpolate(new LayoutUtils.LayoutChecker<int []>() {
			@Override
			public int [] getLayoutValue(LayoutGuideType type) {
				int [] ret = new int[children.length];
				for(int c = 0; c < children.length; c++)
					ret[c] = LayoutUtils.getSize(children[c], horizontal, type, parentWidth, parentHeight, true, null);
				return ret;
			}

			@Override
			public long getSize(int[] layoutValue) {
				long ret = 0;
				ret += margin.evaluate(parentWidth) * 2;
				ret += getSize(layoutValue, 0, -1);
				return ret;
			}

			private int getSize(int [] layoutValue, int startIndex, int centerIndex) {
				Region childRegion = children[startIndex].atts().getValue(region, Region.center);
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
						ret = add(ret, layoutValue[startIndex]);
						ret = add(ret, padding.evaluate(parentWidth));
						startIndex++;
						if(startIndex == children.length)
							break;
						childRegion = children[startIndex].atts().getValue(region, Region.center);
					}
					if(startIndex < children.length)
						ret = add(ret, getSize(layoutValue, startIndex, centerIndex));
					else if(centerIndex >= 0)
						ret = add(ret, layoutValue[centerIndex]);
					return ret;
				} else {
					int ret = 0;
					while(childRegion.getOrientation() == vertical) {
						if(layoutValue[startIndex] > ret)
							ret = layoutValue[startIndex];
						startIndex++;
						if(startIndex == children.length)
							break;
						childRegion = children[startIndex].atts().getValue(region, Region.center);
					}
					if(startIndex < children.length) {
						int restRet = getSize(layoutValue, startIndex, centerIndex);
						if(restRet > ret)
							ret = restRet;
					} else if(centerIndex >= 0)
						ret = add(ret, layoutValue[centerIndex]);
					return ret;
				}
			}
		}, parent.bounds().getWidth(), LayoutGuideType.min, LayoutGuideType.max);

		final int[] widths = new int[children.length];
		for (int c = 0; c < children.length; c++) {
			widths[c] = wResult.lowerValue[c];
			if(wResult.proportion > 0)
				widths[c] = add(widths[c],
					(int) Math.round(wResult.proportion * (wResult.upperValue[c] - wResult.lowerValue[c])));
		}

		LayoutUtils.LayoutInterpolation<int []> hResult = LayoutUtils.interpolate(new LayoutUtils.LayoutChecker<int []>() {
			@Override
			public int [] getLayoutValue(LayoutGuideType type) {
				int [] ret = new int[children.length];
				for(int c = 0; c < children.length; c++)
					ret[c] = LayoutUtils.getSize(children[c], vertical, type, parentHeight, widths[c], false, null);
				return ret;
			}

			@Override
			public long getSize(int[] layoutValue) {
				long ret = 0;
				ret += margin.evaluate(parentHeight) * 2;
				ret += getSize(layoutValue, 0, -1);
				return ret;
			}

			private int getSize(int [] layoutValue, int startIndex, int centerIndex) {
				Region childRegion = children[startIndex].atts().getValue(region, Region.center);
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
						ret = add(ret, layoutValue[startIndex]);
						ret = add(ret, padding.evaluate(parentHeight));
						startIndex++;
						if(startIndex == children.length)
							break;
						childRegion = children[startIndex].atts().getValue(region, Region.center);
					}
					if(startIndex < children.length)
						ret = add(ret, getSize(layoutValue, startIndex, centerIndex));
					else if(centerIndex >= 0)
						ret = add(ret, layoutValue[centerIndex]);
					return ret;
				} else {
					int ret = 0;
					while(childRegion.getOrientation() == horizontal) {
						if(layoutValue[startIndex] > ret)
							ret = layoutValue[startIndex];
						startIndex++;
						if(startIndex == children.length)
							break;
						childRegion = children[startIndex].atts().getValue(region, Region.center);
					}
					if(startIndex < children.length) {
						int restRet = getSize(layoutValue, startIndex, centerIndex);
						if(restRet > ret)
							ret = restRet;
					} else if(centerIndex >= 0)
						ret = add(ret, layoutValue[centerIndex]);
					return ret;
				}
			}
		}, parent.bounds().getHeight(), LayoutGuideType.min, LayoutGuideType.max);

		int[] heights = new int[children.length];
		for (int c = 0; c < children.length; c++) {
			heights[c] = hResult.lowerValue[c];
			if(hResult.proportion > 0)
				heights[c] = add(heights[c],
					(int) Math.round(hResult.proportion * (hResult.upperValue[c] - hResult.lowerValue[c])));
		}

		// Got the sizes. Now put them in place.
		int leftEdge = margin.evaluate(parentWidth);
		int rightEdge = parentWidth - margin.evaluate(parentWidth);
		int topEdge = margin.evaluate(parentHeight);
		int bottomEdge = parentHeight - margin.evaluate(parentHeight);
		int centerIndex = -1;
		Rectangle[] bounds = new Rectangle[children.length];
		for (int c = 0; c < children.length; c++) {
			Region childRegion = children[c].atts().getValue(region, Region.center);
			if(childRegion == Region.center) {
				if(centerIndex >= 0)
					parent.msg().error("Only one element may be in the center region in a border layout."//
						+ "  Only first center will be layed out.", "element", children[c]);
				else
					centerIndex = c;
				continue; // Lay out center last
			}
			switch (childRegion) {
			case left:
				bounds[c] = new Rectangle(leftEdge, topEdge, //
					Math.max(0, widths[c]), Math.max(0, bottomEdge - topEdge));
				leftEdge = add(leftEdge, add(bounds[c].width, padding.evaluate(parentWidth)));
				break;
			case right:
				bounds[c] = new Rectangle(rightEdge - widths[c], topEdge, //
					Math.max(0, widths[c]), Math.max(0, bottomEdge - topEdge));
				rightEdge -= add(bounds[c].width, padding.evaluate(parentWidth));
				break;
			case top:
				bounds[c] = new Rectangle(leftEdge, topEdge, //
					Math.max(0, rightEdge - leftEdge), Math.max(0, heights[c]));
				topEdge = add(leftEdge, add(bounds[c].height, padding.evaluate(parentHeight)));
				break;
			case bottom:
				bounds[c] = new Rectangle(leftEdge, bottomEdge - heights[c], //
					Math.max(0, rightEdge - leftEdge), Math.max(0, heights[c]));
				bottomEdge -= add(bounds[c].height, padding.evaluate(parentHeight));
				break;
			case center:
				// Already handled
			}
		}

		if (centerIndex >= 0)
			bounds[centerIndex] = new Rectangle(leftEdge, topEdge, //
				Math.max(0, rightEdge - leftEdge), Math.max(0, bottomEdge - topEdge));

		for(int c = 0; c < children.length; c++)
			children[c].bounds().set(bounds[c], null);
	}

	@Override
	public String toString() {
		return "border-layout";
	}
}
