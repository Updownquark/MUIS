package org.quick.widget.base.layout;

import static org.quick.core.layout.LayoutAttributes.bottom;
import static org.quick.core.layout.LayoutAttributes.height;
import static org.quick.core.layout.LayoutAttributes.left;
import static org.quick.core.layout.LayoutAttributes.maxBottom;
import static org.quick.core.layout.LayoutAttributes.maxHeight;
import static org.quick.core.layout.LayoutAttributes.maxLeft;
import static org.quick.core.layout.LayoutAttributes.maxRight;
import static org.quick.core.layout.LayoutAttributes.maxTop;
import static org.quick.core.layout.LayoutAttributes.maxWidth;
import static org.quick.core.layout.LayoutAttributes.minBottom;
import static org.quick.core.layout.LayoutAttributes.minHeight;
import static org.quick.core.layout.LayoutAttributes.minLeft;
import static org.quick.core.layout.LayoutAttributes.minRight;
import static org.quick.core.layout.LayoutAttributes.minTop;
import static org.quick.core.layout.LayoutAttributes.minWidth;
import static org.quick.core.layout.LayoutAttributes.region;
import static org.quick.core.layout.LayoutAttributes.right;
import static org.quick.core.layout.LayoutAttributes.top;
import static org.quick.core.layout.LayoutAttributes.width;
import static org.quick.core.layout.Orientation.horizontal;
import static org.quick.core.layout.Orientation.vertical;
import static org.quick.widget.core.layout.LayoutUtils.add;

import java.util.List;

import org.observe.Observable;
import org.quick.core.layout.LayoutGuideType;
import org.quick.core.layout.LayoutSize;
import org.quick.core.layout.Orientation;
import org.quick.core.layout.Region;
import org.quick.core.style.LayoutStyle;
import org.quick.core.style.Size;
import org.quick.util.CompoundListener;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.Rectangle;
import org.quick.widget.core.layout.LayoutUtils;
import org.quick.widget.core.layout.QuickWidgetLayout;
import org.quick.widget.core.layout.SizeGuide;

/**
 * Lays components out by {@link Region regions}. Containers with this layout may have any number of components in any region except center,
 * which may have zero or one component in it.
 */
public class BorderLayout implements QuickWidgetLayout {
	private final CompoundListener<QuickWidget> theListener;

	/** Creates a border layout */
	public BorderLayout() {
		theListener = CompoundListener.<QuickWidget> buildFromQDW()//
			.watchAll(LayoutStyle.margin, LayoutStyle.padding).onEvent(sizeNeedsChanged)//
			.child(builder -> {
				builder.accept(region).onEvent(sizeNeedsChanged);
				builder.when(el -> el.getAttribute(region) == Region.left, builder2 -> {
					builder2.acceptAll(width, minWidth, maxWidth, right, minRight, maxRight).onEvent(sizeNeedsChanged);
				});
				builder.when(el -> el.getAttribute(region) == Region.right, builder2 -> {
					builder2.acceptAll(width, minWidth, maxWidth, left, minLeft, maxLeft).onEvent(sizeNeedsChanged);
				});
				builder.when(el -> el.getAttribute(region) == Region.top, builder2 -> {
					builder2.acceptAll(height, minHeight, maxHeight, bottom, minBottom, maxBottom)
					.onEvent(sizeNeedsChanged);
				});
				builder.when(el -> el.getAttribute(region) == Region.bottom, builder2 -> {
					builder2.acceptAll(height, minHeight, maxHeight, top, minTop, maxTop).onEvent(sizeNeedsChanged);
				});
			})//
			.build();
	}

	@Override
	public void install(QuickWidget parent, Observable<?> until) {
		theListener.listen(parent, parent, until);
	}

	@Override
	public SizeGuide getSizer(QuickWidget parent, Iterable<? extends QuickWidget> children, Orientation orient) {
		return new SizeGuide.GenericSizeGuide() {
			@Override
			public int get(LayoutGuideType type, int crossSize, boolean csMax) {
				Size margin = parent.getElement().getStyle().get(LayoutStyle.margin).get();
				Size padding = parent.getElement().getStyle().get(LayoutStyle.padding).get();
				if (!children.iterator().hasNext()) {
					if (type == LayoutGuideType.min)
						return 0;
					return margin.evaluate(0) * 2;
				}
				int origCrossSize = crossSize;
				if (type != LayoutGuideType.min)
					crossSize -= margin.evaluate(crossSize) * 2;
				if (crossSize < 0)
					crossSize = 0;
				LayoutSize ret=new LayoutSize();
				LayoutSize cross=new LayoutSize(true);
				LayoutSize temp = new LayoutSize();
				QuickWidget center = null;
				for (QuickWidget child : children) {
					Region childRegion = child.getElement().atts().getValue(region, Region.center);
					if(childRegion == Region.center) {
						if(center != null) {
							// Error later, in layout()
						} else
							center = child;
					} else if(childRegion.getOrientation() == orient){
						ret.add(cross);
						cross.clear();
						LayoutUtils.getSize(child, orient, type, 0, crossSize, csMax, ret);
						ret.add(padding);
					} else {
						LayoutUtils.getSize(child, orient, type, crossSize, Integer.MAX_VALUE, true, cross);
						if (crossSize > 0) {
							LayoutUtils.getSize(child, orient.opposite(), type, crossSize, Integer.MAX_VALUE, true, temp.clear());
							crossSize -= temp.add(padding).getTotal(origCrossSize);
							if (crossSize < 0)
								crossSize = 0;
						}
					}
				}
				ret.add(cross);
				if (center != null) {
					ret.add(padding);
					ret.add(padding);
					LayoutUtils.getSize(center, orient, type, 0, crossSize, csMax, ret);
				}
				ret.add(margin);
				ret.add(margin);
				return ret.getTotal();
			}

			@Override
			public int getBaseline(int size) {
				return 0;
			}
		};
	}

	@Override
	public void layout(final QuickWidget parent, List<? extends QuickWidget> children) {
		final int parentWidth = parent.bounds().getWidth();
		final int parentHeight = parent.bounds().getHeight();
		final Size margin = parent.getElement().getStyle().get(LayoutStyle.margin).get();
		final Size padding = parent.getElement().getStyle().get(LayoutStyle.padding).get();
		LayoutUtils.LayoutInterpolation<int []> wResult = LayoutUtils.interpolate(new LayoutUtils.LayoutChecker<int []>() {
			@Override
			public int [] getLayoutValue(LayoutGuideType type) {
				int[] ret = new int[children.size()];
				for (int c = 0; c < children.size(); c++)
					ret[c] = LayoutUtils.getSize(children.get(c), horizontal, type, parentWidth, parentHeight, true, null);
				return ret;
			}

			@Override
			public long getSize(int[] layoutValue) {
				return BorderLayout.this.getSize(parentWidth, margin, padding, children, layoutValue);
			}
		}, parent.bounds().getWidth(), LayoutGuideType.min, LayoutGuideType.max);

		final int[] widths = new int[children.size()];
		for (int c = 0; c < widths.length; c++) {
			widths[c] = wResult.lowerValue[c];
			if(wResult.proportion > 0)
				widths[c] = add(widths[c],
					(int) Math.round(wResult.proportion * (wResult.upperValue[c] - wResult.lowerValue[c])));
		}

		LayoutUtils.LayoutInterpolation<int []> hResult = LayoutUtils.interpolate(new LayoutUtils.LayoutChecker<int []>() {
			@Override
			public int [] getLayoutValue(LayoutGuideType type) {
				int[] ret = new int[children.size()];
				for (int c = 0; c < children.size(); c++)
					ret[c] = LayoutUtils.getSize(children.get(c), vertical, type, parentHeight, widths[c], false, null);
				return ret;
			}

			@Override
			public long getSize(int[] layoutValue) {
				return BorderLayout.this.getSize(parentHeight, margin, padding, children, layoutValue);
			}
		}, parent.bounds().getHeight(), LayoutGuideType.min, LayoutGuideType.max);

		int[] heights = new int[children.size()];
		for (int c = 0; c < children.size(); c++) {
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
		Rectangle[] bounds = new Rectangle[children.size()];
		for (int c = 0; c < children.size(); c++) {
			Region childRegion = children.get(c).getElement().atts().getValue(region, Region.center);
			if(childRegion == Region.center) {
				if(centerIndex >= 0)
					parent.getElement().msg().error("Only one element may be in the center region in a border layout."//
						+ "  Only first center will be layed out.", "element", children.get(c));
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

		for (int c = 0; c < children.size(); c++)
			children.get(c).bounds().set(bounds[c], null);
	}

	private static long getSize(int parentLength, Size margin, Size padding, List<? extends QuickWidget> children, int[] layoutValue) {
		long ret = 0;
		ret += margin.evaluate(parentLength) * 2;
		QuickWidget center = null;
		for (int i = 0; i < children.size(); i++) {
			QuickWidget child = children.get(i);
			Region childRegion = child.getElement().atts().getValue(region, Region.center);
			if (childRegion == Region.center) {
				if (center != null) {
					// Error later, in layout()
				} else {
					center = child;
					ret += layoutValue[i] + padding.evaluate(parentLength) * 2;
				}
			} else if (!childRegion.getOrientation().isVertical())
				ret += layoutValue[i] + padding.evaluate(parentLength);
		}
		return ret;
	}

	@Override
	public String toString() {
		return "border-layout";
	}
}
