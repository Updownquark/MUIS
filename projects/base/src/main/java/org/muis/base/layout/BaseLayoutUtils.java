package org.muis.base.layout;

import org.muis.base.layout.AbstractFlowLayout.BreakPolicy;
import org.muis.core.MuisElement;
import org.muis.core.layout.LayoutGuideType;
import org.muis.core.layout.LayoutUtils;
import org.muis.core.layout.Orientation;

public class BaseLayoutUtils {
	public static int doLayout(MuisElement [] children, Orientation orientation, LayoutGuideType type, BreakPolicy policy,
		boolean mainAxis, int crossSize) {
		int [] res;
		switch (type) {
		case min:
		case minPref:
			res = getWithMinimumWraps(children, orientation.opposite(), type, policy, crossSize, Integer.MAX_VALUE, mainAxis);
			return res[1];
		case max:
		case maxPref:
			res = getWithMinimumWraps(children, orientation, type, policy, Integer.MAX_VALUE, crossSize, mainAxis);
			return res[0];
		case pref:
			res = getWithMinimumWraps(children, mainAxis ? orientation : orientation.opposite(), type, policy, mainAxis ? Integer.MAX_VALUE
				: crossSize, mainAxis ? crossSize : Integer.MAX_VALUE, true);
			return mainAxis ? res[0] : res[1];
		}
		throw new IllegalStateException("Unrecognized layout guide type: " + type);
	}

	public static void doLayout(MuisElement [] children, Orientation mainAxis, BreakPolicy policy, int parallelSize, int crossSize) {
	}

	public static int [] getWithMinimumWraps(MuisElement [] children, Orientation orientation, LayoutGuideType type, BreakPolicy policy,
		int parallelSize, int crossSize, boolean mainAxis) {
		int parSizeMax = 0;
		int parSizeLine = 0;
		int crossSizeSum = 0;
		int crossSizeLine = 0;
		int breaks = 0;
		for(MuisElement child : children) {
			int parSizeTemp = LayoutUtils.getSize(child, orientation, type, parallelSize, crossSize);
			int crossSizeTemp = LayoutUtils.getSize(child, orientation.opposite(), type, crossSize, parallelSize);
			int temp = parSizeLine + parSizeTemp;
			if(policy != BreakPolicy.NEVER && (temp < 0 /*Integer overflow*/|| temp > parallelSize)) {
				parSizeMax += parSizeLine;
				parSizeLine = parSizeTemp;
				crossSizeSum += crossSizeLine;
				crossSizeLine = crossSizeTemp;
				breaks++;
			} else {
				parSizeLine += parSizeTemp;
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
