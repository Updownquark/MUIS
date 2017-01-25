package org.quick.base.layout;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.qommons.FloatList;
import org.quick.core.layout.LayoutUtils;
import org.quick.core.layout.SizeGuide;

public interface LayoutSpring extends SizeGuide {
	public static float OUTER_PREF_TENSION = 1000;
	public static float NOT_PREF_TENSION_THRESH = 100000;
	public static float OUTER_SIZE_TENSION = 1000000;
	public static float MAX_TENSION = 1E9f;
	public static final FloatList TENSIONS = new FloatList(new float[] { //
		MAX_TENSION, OUTER_SIZE_TENSION, NOT_PREF_TENSION_THRESH, OUTER_PREF_TENSION, 0, -OUTER_PREF_TENSION, -NOT_PREF_TENSION_THRESH,
		-OUTER_SIZE_TENSION, -MAX_TENSION });

	static int getSize(SizeGuide guide, int level, int crossSize) {
		switch (level) {
		case 0:
			return 0;
		case 1:
			return guide.getMin(crossSize);
		case 2:
			return guide.getMinPreferred(crossSize);
		case 3:
			return guide.getPreferred(crossSize);
		case 4:
			return guide.getMaxPreferred(crossSize);
		case 5:
			return guide.getMax(crossSize);
		case 6:
			return guide.getMax(crossSize) > Integer.MAX_VALUE / 10 ? Integer.MAX_VALUE : guide.getMax(crossSize) * 10;
		default:
			throw new IllegalStateException("Invalid size level: " + level);
		}
	}

	static int getSize(SizeGuide guide, float tension, int crossSize) {
		int level = (TENSIONS.size() + 1) / 2;
		while (level > 0 && tension < TENSIONS.get(level))
			level--;
		while (level < TENSIONS.size() && tension > TENSIONS.get(level))
			level++;
		if (tension <= TENSIONS.get(level))
			return getSize(guide, level, crossSize);
		else
			return interpolateInt(getSize(guide, level - 1, crossSize), getSize(guide, level, crossSize), TENSIONS.get(level - 1),
				TENSIONS.get(level),
				tension);
	}

	static float getTension(SizeGuide guide, int size, int crossSize) {
		int level = (TENSIONS.size() + 1) / 2;
		while (level > 0 && size < getSize(guide, level, crossSize))
			level--;
		while (level < TENSIONS.size() && size > getSize(guide, level, crossSize))
			level++;
		if (size <= getSize(guide, level, crossSize))
			return getSize(guide, level, crossSize);
		else
			return interpolateFloat(getSize(guide, level - 1, crossSize), getSize(guide, level, crossSize), TENSIONS.get(level - 1),
				TENSIONS.get(level),
				size);
	}

	int getSize();
	LayoutSpring setSize(int size);

	float getTension();
	LayoutSpring setTension(float tension);

	class SimpleLayoutSpring implements LayoutSpring{
		private final SizeGuide theGuide;

		private int theSize = -1;
		private float theTension = Float.NaN;

		public SimpleLayoutSpring(SizeGuide main, SizeGuide cross) {
			theGuide = main;
		}

		@Override
		public int getMin(int crossSize) {
			return theGuide.getMin(crossSize);
		}

		@Override
		public int getMinPreferred(int crossSize) {
			return theGuide.getMinPreferred(crossSize);
		}

		@Override
		public int getPreferred(int crossSize) {
			return theGuide.getPreferred(crossSize);
		}

		@Override
		public int getMaxPreferred(int crossSize) {
			return theGuide.getMaxPreferred(crossSize);
		}

		@Override
		public int getMax(int crossSize) {
			return theGuide.getMax(crossSize);
		}

		@Override
		public int getBaseline(int size) {
			return theGuide.getBaseline(size);
		}

		@Override
		public int getSize() {
			return theSize;
		}

		@Override
		public LayoutSpring setSize(int size) {
			theSize = size;
			return this;
		}

		@Override
		public float getTension() {
			return theTension;
		}

		@Override
		public LayoutSpring setTension(float tension) {
			theTension = tension;
			return this;
		}
	}

	class ConstSpring implements LayoutSpring {
		private final int theSize;
		private final ConstSpring theCross;

		public ConstSpring(int size) {
			theSize = size;
			theCross = new ConstSpring(0, this);
		}

		private ConstSpring(int size, ConstSpring cross) {
			theSize = size;
			theCross = cross;
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

		@Override
		public int getBaseline(int size) {
			return 0;
		}

		@Override
		public ConstSpring getOpposite() {
			return theCross;
		}
	}

	class SeriesSpring implements LayoutSpring {
		private final List<LayoutSpring> theComponents;
		private final ParallelSpring theCross;
		private boolean isFreeEnded;

		public SeriesSpring(LayoutSpring... components) {
			theComponents = Collections.unmodifiableList(Arrays.asList(components));
			theCross = new ParallelSpring(theComponents, this);
			isFreeEnded = true;
		}

		private SeriesSpring(List<LayoutSpring> components, ParallelSpring cross) {
			theComponents = components;
			theCross = cross;
			isFreeEnded = true;
		}

		public List<LayoutSpring> getComponents() {
			return theComponents;
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

		@Override
		public int getBaseline(int size) {
			// TODO Shouldn't use the size directly, needs to be proprotional to the size of the first component
			return theComponents.size() == 0 ? 0 : theComponents.get(0).getBaseline(size);
		}

		@Override
		public ParallelSpring getOpposite() {
			return theCross;
		}
	}

	class ParallelSpring implements LayoutSpring {
		private final List<LayoutSpring> theComponents;
		private final SeriesSpring theCross;

		public ParallelSpring(LayoutSpring... components) {
			theComponents = Collections.unmodifiableList(Arrays.asList(components));
			;
			theCross = new SeriesSpring(theComponents, this);
		}

		private ParallelSpring(List<LayoutSpring> components, SeriesSpring cross) {
			theComponents = components;
			theCross = cross;
		}

		public List<LayoutSpring> getComponents() {
			return theComponents;
		}

		@Override
		public int getMin(int crossSize) {
			int firstTryCrossSize = crossSize / theComponents.size();
			int maxMin = 0;
			for (LayoutSpring spring : theComponents) {
				int size = spring.getMin(firstTryCrossSize);
				if (size > maxMin)
					maxMin = size;
			}
			int tempMin = -1;
			float crossTension;
			final int maxTries = 1;
			for (int tri = 0; tri < maxTries && tempMin != maxMin; tri++) {
				tempMin = maxMin;
				maxMin = 0;
				crossTension = theCross.getTension(crossSize, tempMin);
				for (LayoutSpring spring : theComponents) {
					int size = spring.getMin(spring.getSize(crossTension, tempMin));
					if (size > maxMin)
						maxMin = size;
				}
			}
			return maxMin;
		}

		@Override
		public int getMinPreferred(int crossSize) {
			int firstTryCrossSize = crossSize / theComponents.size();
			int maxMin = 0;
			for (LayoutSpring spring : theComponents) {
				int size = spring.getMinPreferred(firstTryCrossSize);
				if (size > maxMin)
					maxMin = size;
			}
			int tempMin = -1;
			float crossTension;
			final int maxTries = 1;
			for (int tri = 0; tri < maxTries && tempMin != maxMin; tri++) {
				tempMin = maxMin;
				maxMin = 0;
				crossTension = theCross.getTension(crossSize, tempMin);
				for (LayoutSpring spring : theComponents) {
					int size = spring.getMinPreferred(spring.getSize(crossTension, tempMin));
					if (size > maxMin)
						maxMin = size;
				}
			}
			return maxMin;
		}

		@Override
		public int getPreferred(int crossSize) {
			int firstTryCrossSize = crossSize / theComponents.size();
			int maxMin = 0;
			int minMax = Integer.MAX_VALUE;
			long prefSum = 0;
			for (LayoutSpring spring : theComponents) {
				int min = spring.getMinPreferred(firstTryCrossSize);
				int max = spring.getMaxPreferred(firstTryCrossSize);
				if (min > maxMin)
					maxMin = min;
				if (max < minMax)
					minMax = max;
				prefSum += spring.getPreferred(firstTryCrossSize);
			}
			int pref = (int) (prefSum / theComponents.size());
			if (pref > minMax)
				pref = minMax;
			if (pref < maxMin)
				pref = maxMin;
			int tempPref = -1;
			float crossTension;
			final int maxTries = 1;
			for (int tri = 0; tri < maxTries && tempPref != pref; tri++) {
				tempPref = pref;
				prefSum = 0;
				crossTension = theCross.getTension(crossSize, tempPref);
				for (LayoutSpring spring : theComponents) {
					int componentCrossSize = spring.getSize(crossTension, tempPref);
					int min = spring.getMinPreferred(componentCrossSize);
					int max = spring.getMaxPreferred(componentCrossSize);
					if (min > maxMin)
						maxMin = min;
					if (max < minMax)
						minMax = max;
					prefSum += spring.getPreferred(componentCrossSize);
				}
				pref = (int) (prefSum / theComponents.size());
				if (pref > minMax)
					pref = minMax;
				if (pref < maxMin)
					pref = maxMin;
			}
			return pref;
		}

		@Override
		public int getMaxPreferred(int crossSize) {
			int firstTryCrossSize = crossSize / theComponents.size();
			int minMax = Integer.MAX_VALUE;
			for (LayoutSpring spring : theComponents) {
				int size = spring.getMaxPreferred(firstTryCrossSize);
				if (size < minMax)
					minMax = size;
			}
			int tempMax = -1;
			float crossTension;
			final int maxTries = 1;
			for (int tri = 0; tri < maxTries && tempMax != minMax; tri++) {
				tempMax = minMax;
				minMax = Integer.MAX_VALUE;
				crossTension = theCross.getTension(crossSize, tempMax);
				for (LayoutSpring spring : theComponents) {
					int size = spring.getMaxPreferred(spring.getSize(crossTension, tempMax));
					if (size < minMax)
						minMax = size;
				}
			}
			return minMax;
		}

		@Override
		public int getMax(int crossSize) {
			int firstTryCrossSize = crossSize / theComponents.size();
			int minMax = Integer.MAX_VALUE;
			for (LayoutSpring spring : theComponents) {
				int size = spring.getMax(firstTryCrossSize);
				if (size < minMax)
					minMax = size;
			}
			int tempMax = -1;
			float crossTension;
			final int maxTries = 1;
			for (int tri = 0; tri < maxTries && tempMax != minMax; tri++) {
				tempMax = minMax;
				minMax = Integer.MAX_VALUE;
				crossTension = theCross.getTension(crossSize, tempMax);
				for (LayoutSpring spring : theComponents) {
					int size = spring.getMax(spring.getSize(crossTension, tempMax));
					if (size < minMax)
						minMax = size;
				}
			}
			return minMax;
		}

		@Override
		public int getBaseline(int size) {
			return theComponents.size() == 0 ? 0 : theComponents.get(0).getBaseline(size);
		}

		@Override
		public SeriesSpring getOpposite() {
			return theCross;
		}
	}

	public static float interpolateFloat(int lowI, int highI, float lowF, float highF, int iValue) {
		return lowF + (highF - lowF) * (iValue - lowI) / (highI - lowI);
	}

	public static int interpolateInt(int lowI, int highI, float lowF, float highF, float fValue) {
		return lowI + (int) ((highI - lowI) * (fValue - lowF) / (highF - lowF));
	}
}
