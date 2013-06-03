package org.muis.base.layout;

import static org.muis.core.layout.LayoutAttributes.*;

import org.muis.core.MuisElement;
import org.muis.core.MuisLayout;
import org.muis.core.layout.*;
import org.muis.core.style.LayoutStyles;
import org.muis.core.style.Size;
import org.muis.util.CompoundListener;

/**
 * Lays out children one-by-one along a given {@link LayoutAttributes#direction direction} ({@link Direction#DOWN DOWN} by default), with a
 * given {@link LayoutAttributes#alignment alignment} along the opposite axis. {@link LayoutAttributes#width},
 * {@link LayoutAttributes#height}, {@link LayoutAttributes#minWidth}, and {@link LayoutAttributes#minHeight} may be used to help determine
 * the sizes of children.
 */
public class BoxLayout implements MuisLayout {
	private final CompoundListener.MultiElementCompoundListener theListener;

	/** Creates a box layout */
	public BoxLayout() {
		theListener = CompoundListener.create(this);
		theListener.acceptAll(direction, alignment).onChange(CompoundListener.layout);
		theListener.child().acceptAll(width, minWidth, maxWidth, height, minHeight, maxHeight).onChange(CompoundListener.layout);
	}

	@Override
	public void initChildren(MuisElement parent, MuisElement [] children) {
	}

	@Override
	public void childAdded(MuisElement parent, MuisElement child) {
	}

	@Override
	public void childRemoved(MuisElement parent, MuisElement child) {
	}

	@Override
	public void remove(MuisElement parent) {
	}

	@Override
	public SizeGuide getWSizer(MuisElement parent, MuisElement [] children) {
		Direction dir = parent.atts().get(LayoutAttributes.direction, Direction.RIGHT);
		Size margin = parent.getStyle().getSelf().get(LayoutStyles.margin);
		Size padding = parent.getStyle().getSelf().get(LayoutStyles.padding);
		switch (dir) {
		case UP:
		case DOWN:
			return getCrossSizer(children, dir.getOrientation(), margin);
		case LEFT:
		case RIGHT:
			return getMainSizer(children, dir.getOrientation(), margin, padding);
		}
		throw new IllegalStateException("Unrecognized layout direction: " + dir);
	}

	@Override
	public SizeGuide getHSizer(MuisElement parent, MuisElement [] children) {
		Direction dir = parent.atts().get(LayoutAttributes.direction, Direction.RIGHT);
		Size margin = parent.getStyle().getSelf().get(LayoutStyles.margin);
		Size padding = parent.getStyle().getSelf().get(LayoutStyles.padding);
		switch (dir) {
		case UP:
		case DOWN:
			return getMainSizer(children, dir.getOrientation(), margin, padding);
		case LEFT:
		case RIGHT:
			return getCrossSizer(children, dir.getOrientation(), margin);
		}
		throw new IllegalStateException("Unrecognized layout direction: " + dir);
	}

	/**
	 * Gets the size policy in the main direction of the container
	 *
	 * @param children The children to get the sizer for
	 * @param orient The orientation for the layout
	 * @param margin The margin size for the parent
	 * @param padding The padding size for the parent
	 * @return The size policy for the children
	 */
	protected SizeGuide getMainSizer(final MuisElement [] children, final Orientation orient, final Size margin, final Size padding) {
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
				return BaseLayoutUtils.getBoxLayoutSize(children, orient, type, crossSize, csMax, margin, margin, padding, padding);
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
	 * @param children The children to get the sizer for
	 * @param orient The orientation for the layout (not the cross direction of the layout)
	 * @param margin The margin size for the parent
	 * @return The size policy for the children
	 */
	protected SizeGuide getCrossSizer(final MuisElement [] children, final Orientation orient, final Size margin) {
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
				LayoutSize ret = new LayoutSize();
				BaseLayoutUtils.getBoxLayoutCrossSize(children, orient, type, crossSize, csMax, ret);
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
	public void layout(MuisElement parent, final MuisElement [] children) {
		java.awt.Rectangle bounds = new java.awt.Rectangle();
		final Direction dir = parent.atts().get(LayoutAttributes.direction, Direction.RIGHT);
		Alignment align = parent.atts().get(LayoutAttributes.alignment, Alignment.begin);

		final int parallelSize = parent.bounds().get(dir.getOrientation()).getSize();
		final int crossSize = parent.bounds().get(dir.getOrientation().opposite()).getSize();
		LayoutUtils.LayoutInterpolation<int []> result = LayoutUtils.interpolate(new LayoutUtils.LayoutChecker<int []>() {
			@Override
			public int [] getLayoutValue(LayoutGuideType type) {
				int [] ret = new int[children.length];
				for(int i = 0; i < ret.length; i++)
					ret[i] = LayoutUtils.getSize(children[i], dir.getOrientation(), type, parallelSize, crossSize, false, null);
				return ret;
			}

			@Override
			public int getSize(int [] layoutValue) {
				int ret = 0;
				for(int lv : layoutValue)
					ret += lv;
				return ret;
			}
		}, parallelSize, true, align == Alignment.justify);

		for(int c = 0; c < children.length; c++) {
			int mainSize = result.lowerValue[c];
			if(result.proportion > 0)
				mainSize += Math.round(result.proportion * (result.upperValue[c] - result.lowerValue[c]));
			// TODO
		}
	}
}
