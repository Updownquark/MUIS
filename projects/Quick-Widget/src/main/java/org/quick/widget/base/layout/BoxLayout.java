package org.quick.widget.base.layout;

import static org.quick.core.layout.LayoutAttributes.alignment;
import static org.quick.core.layout.LayoutAttributes.crossAlignment;
import static org.quick.core.layout.LayoutAttributes.direction;
import static org.quick.core.layout.LayoutAttributes.height;
import static org.quick.core.layout.LayoutAttributes.maxHeight;
import static org.quick.core.layout.LayoutAttributes.maxWidth;
import static org.quick.core.layout.LayoutAttributes.minHeight;
import static org.quick.core.layout.LayoutAttributes.minWidth;
import static org.quick.core.layout.LayoutAttributes.width;

import java.awt.Rectangle;
import java.util.List;

import org.observe.Observable;
import org.quick.core.layout.Alignment;
import org.quick.core.layout.Direction;
import org.quick.core.layout.End;
import org.quick.core.layout.LayoutAttributes;
import org.quick.core.layout.LayoutGuideType;
import org.quick.core.layout.LayoutSize;
import org.quick.core.layout.Orientation;
import org.quick.core.style.LayoutStyle;
import org.quick.core.style.Size;
import org.quick.util.CompoundListener;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.layout.LayoutUtils;
import org.quick.widget.core.layout.QuickWidgetLayout;
import org.quick.widget.core.layout.SizeGuide;

/**
 * Lays out children one-by-one along a given {@link LayoutAttributes#direction direction} ({@link Direction#down down} by default), with a
 * given {@link LayoutAttributes#alignment alignment} along the opposite axis. {@link LayoutAttributes#width},
 * {@link LayoutAttributes#height}, {@link LayoutAttributes#minWidth}, and {@link LayoutAttributes#minHeight} may be used to help determine
 * the sizes of children.
 */
public class BoxLayout implements QuickWidgetLayout {
	private final CompoundListener<QuickWidget<?>> theListener;

	/** Creates a box layout */
	public BoxLayout() {
		theListener = CompoundListener.<QuickWidget<?>> buildFromQDW()//
			.accept(direction).onEvent(sizeNeedsChanged)//
			.acceptAll(alignment, crossAlignment).onEvent(layout)//
			.child(childBuilder -> {
				childBuilder.acceptAll(width, minWidth, maxWidth, height, minHeight, maxHeight).onEvent(sizeNeedsChanged);
			})//
			.build();
	}

	@Override
	public void install(QuickWidget<?> parent, Observable<?> until) {
		theListener.listen(parent, parent, until);
	}

	@Override
	public SizeGuide getSizer(QuickWidget<?> parent, Iterable<? extends QuickWidget<?>> children, Orientation orientation) {
		Direction dir = parent.getElement().atts().getValue(direction, Direction.right);
		Size margin = parent.getElement().getStyle().get(LayoutStyle.margin).get();
		Size padding = parent.getElement().getStyle().get(LayoutStyle.padding).get();
		if (dir.getOrientation().equals(orientation))
			return getMainSizer(parent, children, dir.getOrientation(), margin, padding);
		else
			return getCrossSizer(parent, children, dir.getOrientation(), margin);
	}

	/**
	 * Gets the size policy in the main direction of the container
	 *
	 * @param parent The parent to get the sizer for
	 *
	 * @param children The children to get the sizer for
	 * @param orient The orientation for the layout
	 * @param margin The margin size for the parent
	 * @param padding The padding size for the parent
	 * @return The size policy for the children
	 */
	protected SizeGuide getMainSizer(final QuickWidget<?> parent, final Iterable<? extends QuickWidget<?>> children,
		final Orientation orient,
		final Size margin, final Size padding) {
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
				Alignment align = parent.getElement().atts().getValue(alignment, null);
				if ((type == LayoutGuideType.max) && !(Alignment.justify.equals(align) || Alignment.center.equals(align)))
					return Integer.MAX_VALUE;
				LayoutSize ret;
				if (type == LayoutGuideType.pref || margin.evaluate(crossSize) == 0) {
					ret = new LayoutSize();
					crossSize -= margin.evaluate(crossSize) * 2;
					ret.setPixels(BaseLayoutUtils.getBoxLayoutSize(children, orient, type, crossSize, csMax, padding, padding));
				} else {
					int withMargin = BaseLayoutUtils.getBoxLayoutSize(children, orient, type, crossSize, csMax, padding, padding);
					crossSize -= margin.evaluate(crossSize) * 2;
					int withoutMargin = BaseLayoutUtils.getBoxLayoutSize(children, orient, type, crossSize, csMax, padding, padding);
					ret = new LayoutSize()
						.setPixels(type.isMin() ? Math.min(withMargin, withoutMargin) : Math.max(withMargin, withoutMargin));
				}
				if (type != LayoutGuideType.min) {
					ret.add(margin);
					ret.add(margin);
				}
				return ret.getTotal();
			}

			@Override
			public int getBaseline(int size) {
				return 0;
			}
		};
	}

	/**
	 * Gets the size policy in the non-main direction of the container
	 *
	 * @param parent The parent to get the sizer for
	 * @param children The children to get the sizer for
	 * @param orient The orientation for the layout (not the cross direction of the layout)
	 * @param margin The margin size for the parent
	 * @return The size policy for the children
	 */
	protected SizeGuide getCrossSizer(final QuickWidget<?> parent, Iterable<? extends QuickWidget<?>> children, final Orientation orient,
		final Size margin) {
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
				if (!Alignment.justify.equals(parent.getElement().atts().get(crossAlignment).get()))
					return Integer.MAX_VALUE;
				return get(LayoutGuideType.maxPref, crossSize, csMax);
			}

			@Override
			public int getMax(int crossSize, boolean csMax) {
				if (!Alignment.justify.equals(parent.getElement().atts().get(crossAlignment).get()))
					return Integer.MAX_VALUE;
				return get(LayoutGuideType.max, crossSize, csMax);
			}

			@Override
			public int get(LayoutGuideType type, int crossSize, boolean csMax) {
				Alignment align = parent.getElement().atts().getValue(alignment, null);
				if ((type == LayoutGuideType.max) && !(Alignment.justify.equals(align) || Alignment.center.equals(align)))
					return Integer.MAX_VALUE;
				LayoutSize ret;
				if (type == LayoutGuideType.pref || margin.evaluate(crossSize) == 0) {
					ret = new LayoutSize();
					crossSize -= margin.evaluate(crossSize) * 2;
					BaseLayoutUtils.getBoxLayoutCrossSize(children, orient, type, crossSize, csMax, ret);
				} else {
					int withMargin = BaseLayoutUtils.getBoxLayoutCrossSize(children, orient, type, crossSize, csMax, null);
					crossSize -= margin.evaluate(crossSize) * 2;
					int withoutMargin = BaseLayoutUtils.getBoxLayoutCrossSize(children, orient, type, crossSize, csMax, null);
					ret = new LayoutSize()
						.setPixels(type.isMin() ? Math.min(withMargin, withoutMargin) : Math.max(withMargin, withoutMargin));
				}
				if (type != LayoutGuideType.min) {
					ret.add(margin);
					ret.add(margin);
				}
				return ret.getTotal();
			}

			@Override
			public int getBaseline(int size) {
				return 0;
			}
		};
	}

	@Override
	public void layout(QuickWidget<?> parent, List<? extends QuickWidget<?>> children) {
		final Direction dir = parent.getElement().atts().getValue(direction, Direction.right);
		Alignment align = parent.getElement().atts().getValue(alignment, Alignment.begin);
		Alignment crossAlign = parent.getElement().atts().getValue(crossAlignment, Alignment.begin);
		final Size margin = parent.getElement().getStyle().get(LayoutStyle.margin).get();
		final Size padding = parent.getElement().getStyle().get(LayoutStyle.padding).get();

		final int parallelSize = parent.bounds().get(dir.getOrientation()).getSize();
		final int crossSize = parent.bounds().get(dir.getOrientation().opposite()).getSize();
		final int crossSizeWOMargin = crossSize - margin.evaluate(crossSize) * 2;
		LayoutUtils.LayoutInterpolation<LayoutSize []> result = LayoutUtils.interpolate(new LayoutUtils.LayoutChecker<LayoutSize []>() {
			@Override
			public LayoutSize [] getLayoutValue(LayoutGuideType type) {
				LayoutSize[] ret = new LayoutSize[children.size()];
				for(int i = 0; i < ret.length; i++) {
					ret[i] = new LayoutSize();
					LayoutUtils.getSize(children.get(i), dir.getOrientation(), type, parallelSize, crossSizeWOMargin, false, ret[i]);
				}
				return ret;
			}

			@Override
			public long getSize(LayoutSize[] layoutValue) {
				LayoutSize space = new LayoutSize();
				space.add(margin);
				space.add(margin);
				long size = 0;
				for(int i = 0; i < layoutValue.length; i++) {
					if(i > 0)
						space.add(padding);
					size += layoutValue[i].getTotal(parallelSize);
				}
				size += space.getTotal(parallelSize);
				return size;
			}
		}, parallelSize, LayoutGuideType.min, align == Alignment.justify ? LayoutGuideType.max : LayoutGuideType.pref);

		Rectangle[] bounds = new Rectangle[children.size()];
		for (int c = 0; c < children.size(); c++) {
			bounds[c] = new Rectangle();
			int mainSize = result.lowerValue[c].getTotal(parallelSize);
			if(result.proportion > 0)
				mainSize += Math.round(result.proportion
					* (result.upperValue[c].getTotal(parallelSize) - result.lowerValue[c].getTotal(parallelSize)));
			LayoutUtils.setSize(bounds[c], dir.getOrientation(), mainSize);

			int prefCrossSize = LayoutUtils.getSize(children.get(c), dir.getOrientation().opposite(), LayoutGuideType.pref,
				crossSizeWOMargin,
				mainSize, false, null);
			int oppSize;
			if (crossSizeWOMargin < prefCrossSize) {
				int minCrossSize = LayoutUtils.getSize(children.get(c), dir.getOrientation().opposite(), LayoutGuideType.min,
					crossSizeWOMargin,
					mainSize, false, null);
				if(crossSize < minCrossSize)
					oppSize = minCrossSize;
				else
					oppSize = crossSizeWOMargin;
			} else if(align == Alignment.justify) {
				int maxCrossSize = LayoutUtils.getSize(children.get(c), dir.getOrientation().opposite(), LayoutGuideType.max,
					crossSizeWOMargin,
					mainSize, false, null);
				if (crossSizeWOMargin < maxCrossSize)
					oppSize = crossSizeWOMargin;
				else
					oppSize = maxCrossSize;
			} else
				oppSize = prefCrossSize;
			LayoutUtils.setSize(bounds[c], dir.getOrientation().opposite(), oppSize);
		}

		// Main alignment
		// Lower the margin and padding if needed
		int marginSz = margin.evaluate(parallelSize);
		int paddingSz = padding.evaluate(parallelSize);
		if (result.lowerType == LayoutGuideType.min && (marginSz != 0 || paddingSz != 0)) {
			if (result.proportion == 0) {
				marginSz = 0;
				paddingSz = 0;
			} else {
				marginSz = (int) Math.round(marginSz * result.proportion);
				paddingSz = (int) Math.round(paddingSz * result.proportion);
			}
		}
		switch (align) {
		case begin:
		case end:
			int pos = 0;
			for (int c = 0; c < children.size(); c++) {
				int childPos = pos;
				if(c == 0)
					childPos += marginSz;
				else
					childPos += paddingSz;
				LayoutUtils.setPos(bounds[c], dir.getOrientation(), align == Alignment.begin ? childPos : parallelSize - childPos
					- LayoutUtils.getSize(bounds[c], dir.getOrientation()));
				pos = childPos + LayoutUtils.getSize(bounds[c], dir.getOrientation());
			}
			break;
		case center:
		case justify:
			int freeSpace = parallelSize;
			freeSpace -= marginSz * 2;
			for(int c = 0; c < bounds.length; c++) {
				if(c > 0)
					freeSpace -= paddingSz;
				freeSpace -= LayoutUtils.getSize(bounds[c], dir.getOrientation());
			}

			pos = 0;
			int usedSpace = 0;
			for (int c = 0; c < children.size(); c++) {
				int extraSpace = (freeSpace - usedSpace) / (children.size() + 1 - c);
				int childPos = pos + extraSpace;
				if(c == 0)
					childPos += marginSz;
				else
					childPos += paddingSz;
				LayoutUtils.setPos(bounds[c], dir.getOrientation(), dir.getStartEnd() == End.leading ? childPos
					: parallelSize - childPos - LayoutUtils.getSize(bounds[c], dir.getOrientation()));
				pos = childPos + LayoutUtils.getSize(bounds[c], dir.getOrientation());
				usedSpace += extraSpace;
			}
		}

		// Cross alignment
		int crossMargin = margin.evaluate(crossSize);
		int newCrossSizeWOMargin = crossSizeWOMargin;
		if (result.lowerType == LayoutGuideType.min && crossMargin != 0) {
			if (result.proportion == 0)
				crossMargin = 0;
			else
				crossMargin = (int) Math.round(crossMargin * result.proportion);
			newCrossSizeWOMargin = crossSize - crossMargin * 2;
		}
		switch (crossAlign) {
		case begin:
			for(Rectangle bound : bounds)
				LayoutUtils.setPos(bound, dir.getOrientation().opposite(), crossMargin);
			break;
		case end:
			for(Rectangle bound : bounds)
				LayoutUtils.setPos(bound, dir.getOrientation().opposite(),
					crossSize - LayoutUtils.getSize(bound, dir.getOrientation().opposite()) - crossMargin);
			break;
		case center:
		case justify:
			for(Rectangle bound : bounds)
				LayoutUtils.setPos(bound, dir.getOrientation().opposite(),
					crossMargin + (newCrossSizeWOMargin - LayoutUtils.getSize(bound, dir.getOrientation().opposite())) / 2);
		}

		for (int c = 0; c < children.size(); c++)
			children.get(c).bounds().setBounds(bounds[c].x, bounds[c].y, bounds[c].width, bounds[c].height);
	}

	@Override
	public String toString() {
		return "box";
	}
}
