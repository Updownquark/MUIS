package org.muis.base.layout;

import static org.muis.core.layout.LayoutAttributes.*;
import static org.muis.core.layout.LayoutGuideType.*;
import static org.muis.core.layout.Orientation.horizontal;
import static org.muis.core.layout.Orientation.vertical;
import static org.muis.core.style.LayoutStyles.margin;
import static org.muis.core.style.LayoutStyles.padding;

import org.muis.base.layout.AbstractFlowLayout.BreakPolicy;
import org.muis.core.MuisElement;
import org.muis.core.layout.*;
import org.muis.core.style.Size;
import org.muis.util.CompoundListener;

/**
 * Arranges components in order along a single axis, wrapping them to the next row or column according to its
 * {@link org.muis.base.layout.AbstractFlowLayout.BreakPolicy BreakPolicy}.
 */
public class FlowLayout implements org.muis.core.MuisLayout {
	private final CompoundListener.MultiElementCompoundListener theListener;

	/** Creates a flow layout */
	public FlowLayout() {
		theListener = CompoundListener.create(this);
		theListener.acceptAll(direction, AbstractFlowLayout.FLOW_BREAK, alignment, crossAlignment).watchAll(margin, padding)
			.onChange(CompoundListener.layout);
		theListener.child().acceptAll(width, minWidth, maxWidth, height, minHeight, maxHeight).onChange(CompoundListener.layout);
	}

	@Override
	public void initChildren(MuisElement parent, MuisElement [] children) {
		theListener.listenerFor(parent);
	}

	@Override
	public void remove(MuisElement parent) {
		theListener.dropFor(parent);
	}

	@Override
	public void childAdded(MuisElement parent, MuisElement child) {
	}

	@Override
	public void childRemoved(MuisElement parent, MuisElement child) {
	}

	@Override
	public SizeGuide getHSizer(MuisElement parent, final MuisElement [] children) {
		if(checkForRelativeSizes(children))
            return new org.muis.core.layout.SimpleSizeGuide();
		Direction dir = parent.atts().get(direction);
		if(dir == null)
			dir = Direction.RIGHT;
		final BreakPolicy bp = parent.atts().get(AbstractFlowLayout.FLOW_BREAK) != null ? parent.atts().get(AbstractFlowLayout.FLOW_BREAK)
			: BreakPolicy.NEEDED;
		final Size marginSz = parent.getStyle().getSelf().get(margin);
		final Size paddingSz = parent.getStyle().getSelf().get(padding);

		final boolean main = dir.getOrientation() == vertical;

		return new SizeGuide() {
			@Override
			public int getMinPreferred(int crossSize) {
				return getLayoutSize(children, vertical, minPref, bp, main, crossSize, marginSz, paddingSz);
			}

			@Override
			public int getMaxPreferred(int crossSize) {
				return getLayoutSize(children, vertical, maxPref, bp, main, crossSize, marginSz, paddingSz);
			}

			@Override
			public int getMin(int crossSize) {
				return getLayoutSize(children, vertical, min, bp, main, crossSize, marginSz, paddingSz);
			}

			@Override
			public int getPreferred(int crossSize) {
				return getLayoutSize(children, vertical, pref, bp, main, crossSize, marginSz, paddingSz);
			}

			@Override
			public int getMax(int crossSize) {
				return getLayoutSize(children, vertical, max, bp, main, crossSize, marginSz, paddingSz);
			}

			@Override
			public int get(LayoutGuideType type, int crossSize) {
				return getLayoutSize(children, vertical, type, bp, main, crossSize, marginSz, paddingSz);
			}
		};
	}

	@Override
	public SizeGuide getWSizer(MuisElement parent, final MuisElement [] children) {
		if(checkForRelativeSizes(children))
            return new org.muis.core.layout.SimpleSizeGuide();
		Direction dir = parent.atts().get(direction);
		if(dir == null)
			dir = Direction.RIGHT;
		final BreakPolicy bp = parent.atts().get(AbstractFlowLayout.FLOW_BREAK) != null ? parent.atts().get(AbstractFlowLayout.FLOW_BREAK)
			: BreakPolicy.NEEDED;
		final Size marginSz = parent.getStyle().getSelf().get(margin);
		final Size paddingSz = parent.getStyle().getSelf().get(padding);

		final boolean main = dir.getOrientation() == Orientation.horizontal;

		return new SizeGuide() {
			@Override
			public int getMinPreferred(int crossSize) {
				return getLayoutSize(children, horizontal, minPref, bp, main, crossSize, marginSz, paddingSz);
			}

			@Override
			public int getMaxPreferred(int crossSize) {
				return getLayoutSize(children, horizontal, maxPref, bp, main, crossSize, marginSz, paddingSz);
			}

			@Override
			public int getMin(int crossSize) {
				return getLayoutSize(children, horizontal, min, bp, main, crossSize, marginSz, paddingSz);
			}

			@Override
			public int getPreferred(int crossSize) {
				return getLayoutSize(children, horizontal, pref, bp, main, crossSize, marginSz, paddingSz);
			}

			@Override
			public int getMax(int crossSize) {
				return getLayoutSize(children, horizontal, max, bp, main, crossSize, marginSz, paddingSz);
			}

			@Override
			public int get(LayoutGuideType type, int crossSize) {
				return getLayoutSize(children, horizontal, type, bp, main, crossSize, marginSz, paddingSz);
			}
		};
	}

	@Override
	public void layout(MuisElement parent, MuisElement [] children) {
		if(checkForRelativeSizes(children))
			return;
		Direction dir = parent.atts().get(direction);
		if(dir == null)
			dir = Direction.RIGHT;
		final BreakPolicy bp = parent.atts().get(AbstractFlowLayout.FLOW_BREAK) != null ? parent.atts().get(AbstractFlowLayout.FLOW_BREAK)
			: BreakPolicy.NEEDED;
		doLayout(children, dir.getOrientation(), dir.getStartEnd(), bp, parent.bounds().get(dir.getOrientation()).getSize(), parent
			.bounds().get(dir.getOrientation().opposite()).getSize(), parent.getStyle().getSelf().get(margin), parent.getStyle().getSelf()
			.get(padding));
	}

	private static boolean checkForRelativeSizes(MuisElement [] children) {
		boolean ret = false;
		for(MuisElement child : children) {
			for(org.muis.core.MuisAttribute<Size> attr : LayoutAttributes.getSizeAttributes()) {
				Size sz = child.atts().get(attr);
				if(sz != null && sz.getUnit().isRelative()) {
					child.msg().error(FlowLayout.class.getSimpleName() + " does not accept relative sizes: " + attr + "=" + sz,
						"attribute", attr, "value", sz);
					ret = true;
				}
			}
		}
		return ret;
	}

	public static int getLayoutSize(MuisElement [] children, Orientation orientation, LayoutGuideType type, BreakPolicy policy,
		boolean mainAxis, int crossSize, Size marginSz, Size paddingSz) {
		int [] res;
		switch (type) {
		case min:
		case minPref:
			res = getWithMinimumWraps(children, orientation.opposite(), type, policy, crossSize, Integer.MAX_VALUE, mainAxis, marginSz,
				paddingSz);
			return res[1];
		case max:
		case maxPref:
			res = getWithMinimumWraps(children, orientation, type, policy, Integer.MAX_VALUE, crossSize, mainAxis, marginSz, paddingSz);
			return res[0];
		case pref:
			res = getWithMinimumWraps(children, mainAxis ? orientation : orientation.opposite(), type, policy, mainAxis ? Integer.MAX_VALUE
				: crossSize, mainAxis ? crossSize : Integer.MAX_VALUE, true, marginSz, paddingSz);
			return mainAxis ? res[0] : res[1];
		}
		throw new IllegalStateException("Unrecognized layout guide type: " + type);
	}

	public static void doLayout(MuisElement [] children, Orientation mainAxis, End end, BreakPolicy policy, int parallelSize,
		int crossSize, Size marginSz, Size paddingSz) {
		// TODO java's flowlayout uses a component's baseline. Not completely sure what this means or if it's worth implementing in MUIS,
		// but it'd be a good thing to check into
	}

	public static int [] getMajorSize(MuisElement [] children, Orientation orientation, LayoutGuideType type, BreakPolicy policy,
		int minorSize, Size margin, Size padding) {
		int majSizeMax = 0;
		int majSizeLine = 0;
		int minSizeSum = 0;
		int minSizeLine = 0;
		for(MuisElement child : children){
			int chMajSz = LayoutUtils.getSize(child, orientation, type, 0, minorSize);
			int chMinSz = LayoutUtils.getSize(child, orientation.opposite(), type, minorSize, Integer.MAX_VALUE);
			if(policy != BreakPolicy.NEVER && type.isMin()) {

			}
			switch (type) {
			case min:
			case minPref:

			}
		}
	}

	public static int [] getWithMinimumWraps(MuisElement [] children, Orientation orientation, LayoutGuideType type, BreakPolicy policy,
		int parallelSize, int crossSize, boolean mainAxis, Size marginSz, Size paddingSz) {
		int parSizeMax = 0;
		int parSizeLine = 0;
		int crossSizeSum = 0;
		int crossSizeLine = 0;
		int breaks = 0;
		for(MuisElement child : children) {
			int parSizeTemp = LayoutUtils.getSize(child, orientation, type, parallelSize, crossSize);
			int crossSizeTemp = LayoutUtils.getSize(child, orientation.opposite(), type, crossSize, parallelSize);
			int temp = parSizeLine + parSizeTemp + paddingSz.evaluate(parallelSize);
			if(policy != BreakPolicy.NEVER && (temp < 0 /*Integer overflow*/|| temp > parallelSize - marginSz.evaluate(parallelSize) * 2)) {
				parSizeMax += parSizeLine;
				parSizeLine = parSizeTemp;
				if(crossSizeSum > 0)
					crossSizeSum += paddingSz.evaluate(crossSize);
				crossSizeSum += crossSizeLine;
				crossSizeLine = crossSizeTemp;
				breaks++;
			} else {
				parSizeLine = temp;
				if(crossSizeTemp > crossSizeLine)
					crossSizeLine = crossSizeTemp;
			}
		}
		if(parSizeLine > parSizeMax)
			parSizeMax = parSizeLine;
		if(type == LayoutGuideType.pref && policy == BreakPolicy.SQUARE) {
			return squareOff(children, orientation, parallelSize, crossSize, mainAxis, breaks);
		}
		return new int[] {parSizeMax, crossSizeSum + crossSizeLine};
	}

	public static int [] squareOff(MuisElement [] children, Orientation orientation, int parallelSize, int crossSize, boolean mainAxis,
		int minBreaks) {
		if(childSizes.length == 0)
			return new int[] {0, 0};
		float ratio;
		{
			int along = 0;
			int cross = 0;
			for(int [] chSize : childSizes) {
				along += chSize[0];
				if(chSize[1] > cross)
					cross = chSize[1];
			}
			ratio = along * 1.0f / cross;
		}
		float bestRatio = ratio;
		int [] bestBreakIndexes = new int[0];
		int [][] bestBreakSizes = new int[0][0];
		for(int breaks = 1; breaks < childSizes.length && ratio > 0; breaks++) {
			int [] breakIndexes = new int[breaks + 1];
			for(int i = 0; i < breakIndexes.length; i++)
				breakIndexes[i] = childSizes.length / i;
			int [][] breakSizes = new int[breaks + 1][2];
			for(int c = 0, index = 0; c < childSizes.length; c++) {
				breakSizes[index][0] += childSizes[c][0];
				if(childSizes[c][1] > breakSizes[index][1])
					breakSizes[index][1] = childSizes[c][1];
			}

		}
	}
}
