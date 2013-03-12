package org.muis.base.layout;

import org.muis.base.layout.AbstractFlowLayout.BreakPolicy;
import org.muis.core.MuisElement;
import org.muis.core.layout.LayoutGuideType;
import org.muis.core.layout.Orientation;

public class BaseLayoutUtils {
	public static int doLayout(MuisElement [] children, Orientation orientation, LayoutGuideType type, BreakPolicy policy, boolean mainAxis,
		int crossSize) {
		if(type!=null){
			int [] res;
			switch(type){
			case min:
			case minPref:
				res= getWithMinimumWraps(children, orientation.opposite(), type, policy, crossSize);
				return res[1];
			case max:
			case maxPref:
				res=getWithMinimumWraps(children, orientation, type, policy, Integer.MAX_VALUE);
				return res[0];
			case pref:
				//TODO
		}
	}

	public static int [] getWithMinimumWraps(MuisElement [] children, Orientation orientation, LayoutGuideType type, BreakPolicy policy,
		int parallelSize) {
		int parSizeMax = 0;
		int parSizeLine = 0;
		int crossSizeLines = 0;
		int crossSize = 0;
		for(MuisElement child : children) {
			int parSizeTemp = child.bounds().get(orientation).getGuide().get(type, parallelSize);
			int crossSizeTemp = child.bounds().get(orientation.opposite()).getGuide().get(type, Integer.MAX_VALUE);
			int temp = parSizeLine + parSizeTemp;
			if(policy != BreakPolicy.NEVER && (temp < 0 || temp > parallelSize)) { // Integer overflow
				parSizeMax += parSizeLine;
				parSizeLine = parSizeTemp;
				crossSizeLines += crossSize;
				crossSize = crossSizeTemp;
			} else {
				parSizeLine += parSizeTemp;
				if(crossSizeTemp > crossSize)
					crossSize = crossSizeTemp;
			}
		}
		if(parSizeLine > parSizeMax)
			parSizeMax = parSizeLine;
		return new int[] {parSizeMax, crossSizeLines + crossSize};
	}

	public static int [] squareOff(int [][] childSizes) {
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
