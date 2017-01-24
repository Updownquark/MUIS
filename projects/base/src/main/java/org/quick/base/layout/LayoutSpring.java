package org.quick.base.layout;

import org.qommons.FloatList;
import org.quick.core.layout.LayoutUtils;
import org.quick.core.layout.SizeGuide;

public interface LayoutSpring {
	public static float OUTER_PREF_TENSION = 1000;
	public static float NOT_PREF_TENSION_THRESH = 100000;
	public static float OUTER_SIZE_TENSION = 1000000;
	public static float MAX_TENSION = 1E9f;
	public static final FloatList TENSIONS = new FloatList(new float[] { //
		MAX_TENSION, OUTER_SIZE_TENSION, NOT_PREF_TENSION_THRESH, OUTER_PREF_TENSION, 0, -OUTER_PREF_TENSION, -NOT_PREF_TENSION_THRESH,
		-OUTER_SIZE_TENSION, -MAX_TENSION });

	int getMin(int crossSize);

	int getMinPreferred(int crossSize);

	int getPreferred(int crossSize);

	int getMaxPreferred(int crossSize);

	int getMax(int crossSize);

	default int getSize(int level, int crossSize) {
		switch (level) {
		case 0:
			return 0;
		case 1:
			return getMin(crossSize);
		case 2:
			return getMinPreferred(crossSize);
		case 3:
			return getPreferred(crossSize);
		case 4:
			return getMaxPreferred(crossSize);
		case 5:
			return getMax(crossSize);
		case 6:
			return getMax(crossSize) > Integer.MAX_VALUE / 10 ? Integer.MAX_VALUE : getMax(crossSize) * 10;
		default:
			throw new IllegalStateException("Invalid size level: " + level);
		}
	}

	default int getSize(float tension, int crossSize) {
		int level = (TENSIONS.size() + 1) / 2;
		while (level > 0 && tension < TENSIONS.get(level))
			level--;
		while (level < TENSIONS.size() && tension > TENSIONS.get(level))
			level++;
		if (tension <= TENSIONS.get(level))
			return getSize(level, crossSize);
		else
			return interpolateInt(getSize(level - 1, crossSize), getSize(level, crossSize), TENSIONS.get(level - 1), TENSIONS.get(level),
				tension);
	}

	default float getTension(int size, int crossSize) {
		int level = (TENSIONS.size() + 1) / 2;
		while (level > 0 && size < getSize(level, crossSize))
			level--;
		while (level < TENSIONS.size() && size > getSize(level, crossSize))
			level++;
		if (size <= getSize(level, crossSize))
			return getSize(level, crossSize);
		else
			return interpolateFloat(getSize(level - 1, crossSize), getSize(level, crossSize), TENSIONS.get(level - 1), TENSIONS.get(level),
				size);
	}

	default SizeGuide asGuide() {}

	class SimpleLayoutSpring implements LayoutSpring{
		private final SizeGuide theGuide;

		private int theCachedCrossSize;
		private int theCachedMin;
		private int theCachedMinPref;
		private int theCachedPref;
		private int theCachedMaxPref;
		private int theCachedMax;

		public SimpleLayoutSpring(SizeGuide guide) {
			theGuide = guide;

			isCachedCrossSize(-1);
		}

		private boolean isCachedCrossSize(int crossSize) {
			if (theCachedCrossSize != crossSize) {
				theCachedCrossSize = -1;
				theCachedMin = -1;
				theCachedMinPref = -1;
				theCachedPref = -1;
				theCachedMaxPref = -1;
				theCachedMax = -1;
				return false;
			}
			return true;
		}

		@Override
		public int getMin(int crossSize) {
			if (!isCachedCrossSize(crossSize) || theCachedMin < 0)
				theCachedMin = theGuide.getMin(crossSize, false);
			return theCachedMin;
		}

		@Override
		public int getMinPreferred(int crossSize) {
			if (!isCachedCrossSize(crossSize) || theCachedMinPref < 0)
				theCachedMinPref = theGuide.getMinPreferred(crossSize, false);
			return theCachedMinPref;
		}

		@Override
		public int getPreferred(int crossSize) {
			if (!isCachedCrossSize(crossSize) || theCachedPref < 0)
				theCachedPref = theGuide.getPreferred(crossSize, false);
			return theCachedPref;
		}

		@Override
		public int getMaxPreferred(int crossSize) {
			if (!isCachedCrossSize(crossSize) || theCachedMaxPref < 0)
				theCachedMaxPref = theGuide.getMaxPreferred(crossSize, false);
			return theCachedMaxPref;
		}

		@Override
		public int getMax(int crossSize) {
			if (!isCachedCrossSize(crossSize) || theCachedMax < 0)
				theCachedMax = theGuide.getMax(crossSize, false);
			return theCachedMax;
		}
	}

	class ConstSpring implements LayoutSpring {
		private final int theSize;

		public ConstSpring(int size) {
			theSize = size;
		}

		@Override
		public int getMin(int crossSize) {
			return 0;
		}

		@Override
		public int getMinPreferred(int crossSize) {
			return theSize;
		}

		@Override
		public int getPreferred(int crossSize) {
			return theSize;
		}

		@Override
		public int getMaxPreferred(int crossSize) {
			return theSize;
		}

		@Override
		public int getMax(int crossSize) {
			return theSize;
		}
	}

	class SeriesSpring implements LayoutSpring {
		private final LayoutSpring[] theComponents;
		private boolean isFreeEnded;

		public SeriesSpring(LayoutSpring... components) {
			theComponents = components;
			isFreeEnded = true;
		}

		public SeriesSpring setFreeEnded(boolean freeEnded) {
			isFreeEnded = freeEnded;
			return this;
		}

		@Override
		public int getMin(int crossSize) {
			int size = 0;
			for (LayoutSpring component : theComponents)
				size = LayoutUtils.add(size, component.getMin(crossSize));
			return size;
		}

		@Override
		public int getMinPreferred(int crossSize) {
			int size = 0;
			for (LayoutSpring component : theComponents)
				size = LayoutUtils.add(size, component.getMinPreferred(crossSize));
			return size;
		}

		@Override
		public int getPreferred(int crossSize) {
			int size = 0;
			for (LayoutSpring component : theComponents)
				size = LayoutUtils.add(size, component.getPreferred(crossSize));
			return size;
		}

		@Override
		public int getMaxPreferred(int crossSize) {
			int size = 0;
			for (LayoutSpring component : theComponents)
				size = LayoutUtils.add(size, component.getMaxPreferred(crossSize));
			return size;
		}

		@Override
		public int getMax(int crossSize) {
			if (isFreeEnded)
				return Integer.MAX_VALUE;
			int size = 0;
			for (LayoutSpring component : theComponents)
				size = LayoutUtils.add(size, component.getMax(crossSize));
			return size;
		}
	}

	class ParallelSpring implements LayoutSpring {
		private final LayoutSpring[] theComponents;

		public ParallelSpring(LayoutSpring... components) {
			theComponents = components;
		}

		@Override
		public int getMin(int crossSize) {
			int maxMin = 0;
			for (LayoutSpring component : theComponents) {
				int compMin = component.getMin();
				if (compMin > maxMin)
					maxMin = compMin;
			}
			return maxMin;
		}

		@Override
		public int getMinPreferred(int crossSize) {
			int maxMin = 0;
			for (LayoutSpring component : theComponents) {
				int compMin = component.getMinPreferred();
				if (compMin > maxMin)
					maxMin = compMin;
			}
			return maxMin;
		}

		@Override
		public int getPreferred(int crossSize) {
			int maxMin = 0;
			int minMax = Integer.MAX_VALUE;
			long sumPref = 0;
			int count = 0;
			for (LayoutSpring component : theComponents) {
				int compMin = component.getMinPreferred();
				int compMax = component.getMaxPreferred();
				if (compMin > maxMin)
					maxMin = compMin;
				if (compMax < minMax)
					minMax = compMax;
				sumPref += component.getPreferred();
				count++;
			}
			int pref = (int) (sumPref / count);
			if (pref > minMax)
				pref = minMax;
			if (pref < maxMin)
				pref = maxMin;
			return pref;
		}

		@Override
		public int getMaxPreferred(int crossSize) {
			int minMax = Integer.MAX_VALUE;
			for (LayoutSpring component : theComponents) {
				int compMax = component.getMaxPreferred();
				if (compMax < minMax)
					minMax = compMax;
			}
			int maxMin = getMinPreferred();
			return Math.max(minMax, maxMin);
		}

		@Override
		public int getMax(int crossSize) {
			int minMax = Integer.MAX_VALUE;
			for (LayoutSpring component : theComponents) {
				int compMax = component.getMax();
				if (compMax < minMax)
					minMax = compMax;
			}
			int maxMin = getMin();
			return Math.max(minMax, maxMin);
		}
	}

	public static float interpolateFloat(int lowI, int highI, float lowF, float highF, int iValue) {
		return lowF + (highF - lowF) * (iValue - lowI) / (highI - lowI);
	}

	public static int interpolateInt(int lowI, int highI, float lowF, float highF, float fValue) {
		return lowI + (int) ((highI - lowI) * (fValue - lowF) / (highF - lowF));
	}
}
