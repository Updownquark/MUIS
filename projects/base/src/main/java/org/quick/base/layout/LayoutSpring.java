package org.quick.base.layout;

import java.util.Collections;
import java.util.List;

import org.qommons.FloatList;
import org.quick.core.layout.LayoutGuideType;
import org.quick.core.layout.LayoutUtils;
import org.quick.core.layout.SizeGuide;

public interface LayoutSpring {
	/**
	 * The absolute value of the tension of a spring at its {@link LayoutGuideType#minPref min-preferred} or {@link LayoutGuideType#maxPref
	 * max-preferred} size
	 */
	public static float OUTER_PREF_TENSION = 1000;
	/**
	 * The tension a spring must have before it moves from its {@link LayoutGuideType#minPref min-preferred} or
	 * {@link LayoutGuideType#maxPref max-preferred} size toward its {@link LayoutGuideType#min min} or {@link LayoutGuideType#max max} size
	 */
	public static float OUTER_PREF_JUMP = 10000;
	/** The absolute value of the tension of a spring at its {@link LayoutGuideType#min min} or {@link LayoutGuideType#max max} size */
	public static float OUTER_SIZE_TENSION = 1000000;
	/**
	 * The tension a spring must have before it moves from its {@link LayoutGuideType#min min} or {@link LayoutGuideType#max max} size
	 * toward zero or infinity
	 */
	public static float OUTER_SIZE_JUMP = 10000000;
	/** The maximum tension a spring can have */
	public static float MAX_TENSION = 1E9f;
	public static final FloatList TENSIONS = createTensions();

	static FloatList createTensions() {
		FloatList tensions = new FloatList(new float[] { //
			MAX_TENSION, //
			OUTER_SIZE_JUMP, //
			OUTER_SIZE_TENSION, //
			OUTER_PREF_JUMP, //
			OUTER_PREF_TENSION, //
			0, //
			-OUTER_PREF_TENSION, //
			-OUTER_PREF_JUMP, //
			-OUTER_SIZE_TENSION, //
			-OUTER_SIZE_JUMP, //
			-MAX_TENSION//
		});
		tensions.seal();
		return tensions;
	}

	int get(LayoutGuideType type);

	default int getSize(float tension) {
		// TODO WRONG!
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
		// TODO WRONG!
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

		public boolean recalculate(int layoutLength, int crossSize, boolean csAvailable) {
			if (theLayoutLength != layoutLength || theCrossSize != crossSize || isCrossSizeAvailable != csAvailable) {
				theLayoutLength = layoutLength;
				theCrossSize = crossSize;
				isCrossSizeAvailable = csAvailable;
				clearCache();
				return true;
			} else
				return false;
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

	abstract class ComponentizedSpring extends AbstractCachingSpring {
		private final List<? extends LayoutSpring> theComponents;

		public ComponentizedSpring(List<? extends LayoutSpring> components) {
			if (components.getClass().getSimpleName().contains("Unmodifiable")
				&& !components.getClass().getSimpleName().contains("Immutable"))
				components = Collections.unmodifiableList(components);
			theComponents = components;
		}

		public List<? extends LayoutSpring> getComponents() {
			return theComponents;
		}
	}

	class SeriesSpring extends ComponentizedSpring {
		public SeriesSpring(List<? extends LayoutSpring> components) {
			super(components);
		}

		@Override
		protected int calculate(LayoutGuideType sizeType) {
			int size = 0;
			for (LayoutSpring component : getComponents())
				size = LayoutUtils.add(size, component.get(sizeType));
			return size;
		}
	}

	class ParallelSpring extends ComponentizedSpring {
		public ParallelSpring(List<? extends LayoutSpring> components) {
			super(components);
		}
		@Override
		protected int calculate(LayoutGuideType sizeType) {
			int size = -1;
			switch (sizeType) {
			case min:
				for (LayoutSpring component : getComponents()) {
					int compSize = component.get(sizeType);
					if (size < 0 || compSize > size)
						size = compSize;
				}
				if (size < 0)
					size = 0;
				break;
			case max:
				for (LayoutSpring component : getComponents()) {
					int compSize = component.get(sizeType);
					if (size < 0 || compSize < size)
						size = compSize;
				}
				int min = getMin();
				if (size < min)
					size = min;
				break;
			case minPref:
				for (LayoutSpring component : getComponents()) {
					int compSize = component.get(sizeType);
					if (size < 0 || compSize > size)
						size = compSize;
				}
				min = getMin();
				if (size < min)
					size = min;
				else {
					int max = getMax();
					if (size > max)
						size = max;
				}
				break;
			case maxPref:
				for (LayoutSpring component : getComponents()) {
					int compSize = component.get(sizeType);
					if (size < 0 || compSize < size)
						size = compSize;
				}
				min = getMin();
				if (size < min)
					size = min;
				else {
					int max = getMax();
					if (size > max)
						size = max;
				}
				break;
			case pref:
				long sizeL = 0;
				for (LayoutSpring component : getComponents())
					sizeL += component.get(sizeType);
				size = (int) (sizeL / getComponents().size());
				min = getMinPreferred();
				if (size < min)
					size = min;
				else {
					int max = getMaxPreferred();
					if (size > max)
						size = max;
				}
				break;
			}
			if (size < 0)
				size = 0;
			return size;
		}
	}

	static float getTensionFor(LayoutGuideType sizeType) {
		switch (sizeType) {
		case min:
			return -OUTER_SIZE_TENSION;
		case minPref:
			return -OUTER_PREF_TENSION;
		case pref:
			return 0;
		case maxPref:
			return OUTER_PREF_TENSION;
		case max:
			return OUTER_SIZE_TENSION;
		}
		throw new IllegalStateException("Unrecognized size type: " + sizeType);
	}

	public static float interpolateFloat(int lowI, int highI, float lowF, float highF, int iValue) {
		return lowF + (highF - lowF) * (iValue - lowI) / (highI - lowI);
	}

	public static int interpolateInt(int lowI, int highI, float lowF, float highF, float fValue) {
		return lowI + (int) ((highI - lowI) * (fValue - lowF) / (highF - lowF));
	}
}
