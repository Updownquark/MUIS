package org.quick.base.layout;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.qommons.FloatList;
import org.quick.core.layout.LayoutGuideType;
import org.quick.core.layout.LayoutUtils;
import org.quick.core.layout.SizeGuide;

public interface LayoutSpring {
	public static float OUTER_PREF_TENSION = 1000;
	public static float OUTER_SIZE_TENSION = 1000000;
	public static float MAX_TENSION = 1E9f;
	public static final FloatList TENSIONS = createTensions();

	static FloatList createTensions() {
		FloatList tensions = new FloatList(new float[] { //
			MAX_TENSION, // Tension at size 0 (if less than min size)
			OUTER_SIZE_TENSION, // Tension at min size
			OUTER_PREF_TENSION, // Tension at min pref size
			0, // Tension at preferred size
			-OUTER_PREF_TENSION, // Tension at max pref size
			-OUTER_SIZE_TENSION, // Tension at max size
			-MAX_TENSION// Tension at Integer.MAX_VALUE (if greater than max size)
		});
		tensions.seal();
		return tensions;
	}

	int get(LayoutGuideType type);

	default int getSize(float tension) {
		int index = TENSIONS.indexFor(tension);
		if (index == 0)
			return 0;
		if (index >= TENSIONS.size())
			return Integer.MAX_VALUE;
		float preTension = TENSIONS.get(index - 1);
		float postTension = TENSIONS.get(index);
		if (tension == preTension)
			return get(LayoutGuideType.values()[index - 1]);
		return interpolateInt(get(LayoutGuideType.values()[index - 2]), get(LayoutGuideType.values()[index - 1]), //
			preTension, postTension, tension);
	}

	default float getTension(int size) {
		int pref = getPreferred();
		if (size == pref)
			return 0;
		else if (size < pref) {
			int minPref = getMinPreferred();
			if (size == minPref)
				return OUTER_PREF_TENSION;
			else if (size < minPref) {
				int min = getMin();
				if (size == min)
					return OUTER_SIZE_TENSION;
				else if (size < min)
					return interpolateFloat(0, min, OUTER_SIZE_TENSION, MAX_TENSION, size);
				else
					return interpolateFloat(min, minPref, OUTER_PREF_TENSION, OUTER_SIZE_TENSION, size);
			} else
				return interpolateFloat(minPref, pref, 0, OUTER_PREF_TENSION, size);
		} else {
			int maxPref = getMaxPreferred();
			if (size == maxPref)
				return -OUTER_PREF_TENSION;
			else if (size > maxPref) {
				int max = getMax();
				if (size == max)
					return -OUTER_SIZE_TENSION;
				else if (size > max)
					return -interpolateFloat(max, Integer.MAX_VALUE, OUTER_SIZE_TENSION, MAX_TENSION, size);
				else
					return -interpolateFloat(maxPref, max, OUTER_PREF_TENSION, OUTER_SIZE_TENSION, size);
			} else
				return -interpolateFloat(pref, maxPref, 0, OUTER_PREF_TENSION, size);
		}
	}

	default int getMin() {
		return get(LayoutGuideType.min);
	}
	default int getMinPreferred() {
		return get(LayoutGuideType.minPref);
	}
	default int getPreferred() {
		return get(LayoutGuideType.pref);
	}
	default int getMaxPreferred() {
		return get(LayoutGuideType.maxPref);
	}
	default int getMax() {
		return get(LayoutGuideType.max);
	}

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

	abstract class AbstractCachingSpring implements LayoutSpring {
		private final int[] theCache;

		protected AbstractCachingSpring() {
			theCache = new int[LayoutGuideType.values().length];
			clearCache();
		}

		protected void clearCache() {
			for (int i = 0; i < theCache.length; i++)
				theCache[0] = -1;
		}

		@Override
		public int get(LayoutGuideType type) {
			if (theCache[type.ordinal()] < 0)
				theCache[type.ordinal()] = calculate(type);
			return theCache[type.ordinal()];
		}

		protected abstract int calculate(LayoutGuideType sizeType);
	}

	class CachingSpring extends AbstractCachingSpring {
		private final LayoutSpring spring;

		public CachingSpring(LayoutSpring spring) {
			this.spring = spring;
		}

		@Override
		public void clearCache() {
			super.clearCache();
		}

		@Override
		protected int calculate(LayoutGuideType sizeType) {
			return spring.get(sizeType);
		}
	}

	class SizeGuideSpring extends AbstractCachingSpring {
		private final SizeGuide theGuide;
		private int theLayoutLength;
		private int theCrossSize;
		private boolean isCrossSizeAvailable;

		public SizeGuideSpring(SizeGuide main) {
			theGuide = main;
		}

		public void recalculate(int layoutLength, int crossSize, boolean csAvailable) {
			theLayoutLength = layoutLength;
			theCrossSize = crossSize;
			isCrossSizeAvailable = csAvailable;
			clearCache();
		}

		@Override
		protected int calculate(LayoutGuideType sizeType) {
			return theGuide.get(sizeType, theLayoutLength, theCrossSize, isCrossSizeAvailable);
		}
	}

	class ConstSpring implements LayoutSpring {
		private final int theMin;
		private final int theMinPref;
		private final int thePref;
		private final int theMaxPref;
		private final int theMax;

		public ConstSpring(int size) {
			this(size, size, size, size, size);
		}

		public ConstSpring(int min, int minPref, int pref, int maxPref, int max) {
			theMin = min;
			theMinPref = minPref;
			thePref = pref;
			theMaxPref = maxPref;
			theMax = max;
		}

		@Override
		public int get(LayoutGuideType type) {
			switch (type) {
			case min:
				return theMin;
			case minPref:
				return theMinPref;
			case pref:
				return thePref;
			case maxPref:
				return theMaxPref;
			case max:
				return theMax;
			}
			throw new IllegalStateException("Unrecognized layout guide type: " + type);
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
