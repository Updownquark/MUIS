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

	int getMin();
	int getMinPreferred();
	int getPreferred();
	int getMaxPreferred();
	int getMax();

	default int getSize(int level) {
		switch (level) {
		case 0:
			return 0;
		case 1:
			return getMin();
		case 2:
			return getMinPreferred();
		case 3:
			return getPreferred();
		case 4:
			return getMaxPreferred();
		case 5:
			return getMax();
		case 6:
			return getMax() > Integer.MAX_VALUE / 10 ? Integer.MAX_VALUE : getMax() * 10;
		default:
			throw new IllegalStateException("Invalid size level: " + level);
		}
	}

	default int getSize(float tension) {
		int level = (TENSIONS.size() + 1) / 2;
		while (level > 0 && tension < TENSIONS.get(level))
			level--;
		while (level < TENSIONS.size() && tension > TENSIONS.get(level))
			level++;
		if (tension <= TENSIONS.get(level))
			return getSize(level);
		else
			return interpolateInt(getSize(level - 1), getSize(level), TENSIONS.get(level - 1), TENSIONS.get(level), tension);
	}

	default float getTension(int size) {
		int level = (TENSIONS.size() + 1) / 2;
		while (level > 0 && size < getSize(level))
			level--;
		while (level < TENSIONS.size() && size > getSize(level))
			level++;
		if (size <= getSize(level))
			return getSize(level);
		else
			return interpolateFloat(getSize(level - 1), getSize(level), TENSIONS.get(level - 1), TENSIONS.get(level), size);
	}

	class SimpleLayoutSpring implements LayoutSpring{
		private final SizeGuide theGuide;
		private final int theCrossSize;
		private final boolean isCSMax;

		private int theCachedMin;
		private int theCachedMinPref;
		private int theCachedPref;
		private int theCachedMaxPref;
		private int theCachedMax;

		public SimpleLayoutSpring(SizeGuide guide, int crossSize, boolean csMax) {
			theGuide = guide;
			theCrossSize = crossSize;
			isCSMax = csMax;

			theCachedMin = -1;
			theCachedMinPref = -1;
			theCachedPref = -1;
			theCachedMaxPref = -1;
			theCachedMax = -1;
		}

		@Override
		public int getMin() {
			if (theCachedMin < 0)
				theCachedMin = theGuide.getMin(theCrossSize, isCSMax);
			return theCachedMin;
		}

		@Override
		public int getMinPreferred() {
			if (theCachedMinPref < 0)
				theCachedMinPref = theGuide.getMinPreferred(theCrossSize, isCSMax);
			return theCachedMinPref;
		}

		@Override
		public int getPreferred() {
			if (theCachedPref < 0)
				theCachedPref = theGuide.getPreferred(theCrossSize, isCSMax);
			return theCachedPref;
		}

		@Override
		public int getMaxPreferred() {
			if (theCachedMaxPref < 0)
				theCachedMaxPref = theGuide.getMaxPreferred(theCrossSize, isCSMax);
			return theCachedMaxPref;
		}

		@Override
		public int getMax() {
			if (theCachedMax < 0)
				theCachedMax = theGuide.getMax(theCrossSize, isCSMax);
			return theCachedMax;
		}
	}

	class ConstSpring implements LayoutSpring {
		private final int theSize;

		public ConstSpring(int size) {
			theSize = size;
		}

		@Override
		public int getMin() {
			return 0;
		}

		@Override
		public int getMinPreferred() {
			return theSize;
		}

		@Override
		public int getPreferred() {
			return theSize;
		}

		@Override
		public int getMaxPreferred() {
			return theSize;
		}

		@Override
		public int getMax() {
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
		public int getMin() {
			int size = 0;
			for (LayoutSpring component : theComponents)
				size = LayoutUtils.add(size, component.getMin());
			return size;
		}

		@Override
		public int getMinPreferred() {
			int size = 0;
			for (LayoutSpring component : theComponents)
				size = LayoutUtils.add(size, component.getMinPreferred());
			return size;
		}

		@Override
		public int getPreferred() {
			int size = 0;
			for (LayoutSpring component : theComponents)
				size = LayoutUtils.add(size, component.getPreferred());
			return size;
		}

		@Override
		public int getMaxPreferred() {
			int size = 0;
			for (LayoutSpring component : theComponents)
				size = LayoutUtils.add(size, component.getMaxPreferred());
			return size;
		}

		@Override
		public int getMax() {
			if (isFreeEnded)
				return Integer.MAX_VALUE;
			int size = 0;
			for (LayoutSpring component : theComponents)
				size = LayoutUtils.add(size, component.getMax());
			return size;
		}

		@Override
		public float getTension(int size) {
			// TODO Auto-generated method stub
		}
	}

	public static float interpolateFloat(int lowI, int highI, float lowF, float highF, int iValue) {
		return lowF + (highF - lowF) * (iValue - lowI) / (highI - lowI);
	}

	public static int interpolateInt(int lowI, int highI, float lowF, float highF, float fValue) {
		return lowI + (int) ((highI - lowI) * (fValue - lowF) / (highF - lowF));
	}
}
