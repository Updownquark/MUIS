package org.quick.base.layout;

import java.util.*;

import org.quick.core.layout.LayoutGuideType;
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
	public static float MAX_TENSION = 1E30f;
//	public static final FloatList TENSIONS = createTensions();
//
//	static FloatList createTensions() {
//		FloatList tensions = new FloatList(new float[] { //
//			MAX_TENSION, //
//			OUTER_SIZE_JUMP, //
//			OUTER_SIZE_TENSION, //
//			OUTER_PREF_JUMP, //
//			OUTER_PREF_TENSION, //
//			0, //
//			-OUTER_PREF_TENSION, //
//			-OUTER_PREF_JUMP, //
//			-OUTER_SIZE_TENSION, //
//			-OUTER_SIZE_JUMP, //
//			-MAX_TENSION//
//		});
//		tensions.seal();
//		return tensions;
//	}

	SizeTickIterator ticks();

//	int get(LayoutGuideType type);
//
//	default int getSize(float tension) {
//		// TODO WRONG!
//		int index = TENSIONS.indexFor(tension);
//		if (index == 0)
//			return 0;
//		if (index >= TENSIONS.size())
//			return Integer.MAX_VALUE;
//		float preTension = TENSIONS.get(index - 1);
//		float postTension = TENSIONS.get(index);
//		if (tension == preTension)
//			return get(LayoutGuideType.values()[index - 1]);
//		return interpolateInt(get(LayoutGuideType.values()[index - 2]), get(LayoutGuideType.values()[index - 1]), //
//			preTension, postTension, tension);
//	}
//
//	default float getTension(int size) {
//		// TODO WRONG!
//		int pref = getPreferred();
//		if (size == pref)
//			return 0;
//		else if (size < pref) {
//			int minPref = getMinPreferred();
//			if (size == minPref)
//				return OUTER_PREF_TENSION;
//			else if (size < minPref) {
//				int min = getMin();
//				if (size == min)
//					return OUTER_SIZE_TENSION;
//				else if (size < min)
//					return interpolateFloat(0, min, OUTER_SIZE_TENSION, MAX_TENSION, size);
//				else
//					return interpolateFloat(min, minPref, OUTER_PREF_TENSION, OUTER_SIZE_TENSION, size);
//			} else
//				return interpolateFloat(minPref, pref, 0, OUTER_PREF_TENSION, size);
//		} else {
//			int maxPref = getMaxPreferred();
//			if (size == maxPref)
//				return -OUTER_PREF_TENSION;
//			else if (size > maxPref) {
//				int max = getMax();
//				if (size == max)
//					return -OUTER_SIZE_TENSION;
//				else if (size > max)
//					return -interpolateFloat(max, Integer.MAX_VALUE, OUTER_SIZE_TENSION, MAX_TENSION, size);
//				else
//					return -interpolateFloat(maxPref, max, OUTER_PREF_TENSION, OUTER_SIZE_TENSION, size);
//			} else
//				return -interpolateFloat(pref, maxPref, 0, OUTER_PREF_TENSION, size);
//		}
//	}
//
//	default int getMin() {
//		return get(LayoutGuideType.min);
//	}
//
//	default int getMinPreferred() {
//		return get(LayoutGuideType.minPref);
//	}
//
//	default int getPreferred() {
//		return get(LayoutGuideType.pref);
//	}
//
//	default int getMaxPreferred() {
//		return get(LayoutGuideType.maxPref);
//	}
//
//	default int getMax() {
//		return get(LayoutGuideType.max);
//	}

	class SizeGuideSpring implements LayoutSpring {
		private final SizeGuide theGuide;
		private final int[] theCache;

		private int theLayoutLength;
		private int theCrossSize;
		private boolean isCrossSizeAvailable;

		public SizeGuideSpring(SizeGuide main) {
			theGuide = main;
			theCache = new int[LayoutGuideType.values().length];
			clearCache();
		}

		protected void clearCache() {
			for (int i = 0; i < theCache.length; i++)
				theCache[0] = -1;
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

		public int get(LayoutGuideType type) {
			if (theCache[type.ordinal()] < 0)
				theCache[type.ordinal()] = calculate(type);
			return theCache[type.ordinal()];
		}

		protected int calculate(LayoutGuideType sizeType) {
			return theGuide.get(sizeType, theLayoutLength, theCrossSize, isCrossSizeAvailable);
		}

		@Override
		public SizeTickIterator ticks() {
			return new SizeTickIterator() {
				private int theIndex = 0;

				@Override
				public int getSize() {
					return get(LayoutGuideType.values()[theIndex]);
				}

				@Override
				public float getTensionBelow() {
					LayoutGuideType sizeType = LayoutGuideType.values()[theIndex];
					switch (sizeType) {
					case min:
						return OUTER_SIZE_JUMP;
					case minPref:
						return OUTER_PREF_JUMP;
					case pref:
						return 0;
					case maxPref:
						return -OUTER_PREF_TENSION;
					case max:
						return -OUTER_SIZE_TENSION;
					}
					throw new IllegalStateException("Unrecognized size type: " + sizeType);
				}

				@Override
				public float getTensionAbove() {
					LayoutGuideType sizeType = LayoutGuideType.values()[theIndex];
					switch (sizeType) {
					case min:
						return OUTER_SIZE_TENSION;
					case minPref:
						return OUTER_PREF_TENSION;
					case pref:
						return 0;
					case maxPref:
						return -OUTER_PREF_JUMP;
					case max:
						return -OUTER_SIZE_JUMP;
					}
					throw new IllegalStateException("Unrecognized size type: " + sizeType);
				}

				@Override
				public boolean next() {
					if (theIndex < LayoutGuideType.values().length - 1) {
						theIndex++;
						return true;
					} else
						return false;
				}

				@Override
				public boolean previous() {
					if (theIndex > 0) {
						theIndex--;
						return true;
					} else
						return false;
				}
			};
		}
	}

	static class Tick {
		public final int size;
		public final float tensionBelow;
		public final float tensionAbove;

		public Tick(int size, float tensionBelow, float tensionAbove) {
			this.size = size;
			this.tensionBelow = tensionBelow;
			this.tensionAbove = tensionAbove;
		}

		public static Tick stateOf(SizeTickIterator iter) {
			return new Tick(iter.getSize(), iter.getTensionBelow(), iter.getTensionAbove());
		}
	}

	class SimpleSpring implements LayoutSpring {
		private final Tick[] theTicks;

		private SimpleSpring(Tick[] ticks) {
			theTicks = ticks;
		}

		@Override
		public SizeTickIterator ticks() {
			return new SizeTickIterator() {
				private int theIndex;

				@Override
				public int getSize() {
					return theTicks[theIndex].size;
				}

				@Override
				public float getTensionBelow() {
					return theTicks[theIndex].tensionBelow;
				}

				@Override
				public float getTensionAbove() {
					return theTicks[theIndex].tensionAbove;
				}

				@Override
				public boolean next() {
					if (theIndex < theTicks.length - 1) {
						theIndex++;
						return true;
					} else
						return false;
				}

				@Override
				public boolean previous() {
					if (theIndex > 0) {
						theIndex--;
						return true;
					} else
						return false;
				}
			};
		}

		public static Builder build(int firstSize, float tensionBelow, float tensionAbove) {
			return new Builder().with(firstSize, tensionBelow, tensionAbove);
		}

		public static class Builder {
			private final TreeMap<Integer, Tick> theTicks = new TreeMap<>();

			public Builder with(int size, float tensionBelow, float tensionAbove) {
				if (size < 0)
					throw new IllegalArgumentException("size must be >=0");
				if (Float.isNaN(tensionBelow) || Float.isNaN(tensionAbove) || Float.isInfinite(tensionBelow)
					|| Float.isInfinite(tensionAbove))
					throw new IllegalArgumentException("tensions may not be NaN or infinite: " + tensionBelow + ", " + tensionAbove);
				if (tensionBelow < tensionAbove)
					throw new IllegalArgumentException("tensionBelow must be >= tensionAbove (tension increases with decreasing size)");
				Integer sz = Integer.valueOf(size);
				Map.Entry<Integer, Tick> entry = theTicks.floorEntry(sz);
				if (entry != null) {
					if (entry.getKey() == size)
						throw new IllegalArgumentException("Two tension definitions for size " + size);
					if (entry.getValue().tensionAbove <= tensionBelow)
						throw new IllegalArgumentException("Tension (" + entry.getValue().tensionAbove + ") above size " + entry.getKey()
							+ " must be > tension (" + tensionBelow + ") below size " + size + ": tension increases with decreasing size");
				}
				entry = theTicks.ceilingEntry(sz);
				if (entry != null) {
					if (entry.getKey() == size)
						throw new IllegalArgumentException("Two tension definitions for size " + size);
					if (tensionAbove <= entry.getValue().tensionBelow)
						throw new IllegalArgumentException(
							"Tension (" + tensionAbove + ") above size " + size + " must be > tension (" + entry.getValue().tensionBelow
								+ ") below size " + entry.getKey() + ": tension increases with decreasing size");
				}
				theTicks.put(sz, new Tick(size, tensionBelow, tensionAbove));
				return this;
			}

			public SimpleSpring build() {
				return new SimpleSpring(theTicks.values().toArray(new Tick[theTicks.size()]));
			}
		}
	}

	abstract class CompositeSpring implements LayoutSpring {
		private final List<? extends LayoutSpring> theComponents;

		public CompositeSpring(List<? extends LayoutSpring> components) {
			if (components.getClass().getSimpleName().contains("Unmodifiable")
				&& !components.getClass().getSimpleName().contains("Immutable"))
				components = Collections.unmodifiableList(components);
			theComponents = components;
		}

		public List<? extends LayoutSpring> getComponents() {
			return theComponents;
		}

		protected abstract class CompositeIterator implements SizeTickIterator {
			protected final SizeTickIterator[] theComponentIters;

			protected float theTension;
			protected int theSize;

			{
				LayoutSpring[] comps = getComponents().toArray(new LayoutSpring[0]);
				theComponentIters = new SizeTickIterator[comps.length];

				theSize = 0;
				for (int i = 0; i < comps.length; i++)
					theComponentIters[i] = comps[i].ticks();
			}

			@Override
			public int getSize() {
				return theSize;
			}
		}
	}

	class SeriesSpring extends CompositeSpring {
		public SeriesSpring(List<? extends LayoutSpring> components) {
			super(components);
		}

		@Override
		public SizeTickIterator ticks() {
			// Series springs have equal tensions, differing sizes which are summed
			return new CompositeIterator() {
				private float theTensionBelow;
				private float theTensionAbove;

				{
					theTension = MAX_TENSION;
					theTensionBelow = MAX_TENSION;
					theTensionAbove = -MAX_TENSION;
					for (SizeTickIterator iter : theComponentIters)
						theSize += getSize(iter);
				}

				private int getSize(SizeTickIterator iter) {
					int size = iter.getSize(theTension);
					if (iter.getTensionBelow() >= theTension && iter.getTensionAbove() <= theTension) {
						theTensionBelow = Math.min(theTensionBelow, iter.getTensionBelow());
						theTensionAbove = Math.max(theTensionAbove, iter.getTensionAbove());
					} else{
						theTensionBelow=theTension;
						theTensionAbove=theTension;
					}
					return size;
				}

				@Override
				public float getTensionBelow() {
					return theTensionBelow;
				}

				@Override
				public float getTensionAbove() {
					return theTensionAbove;
				}

				@Override
				public boolean next() {
					float maxNext = -MAX_TENSION;
					boolean hasNext = false;
					for (SizeTickIterator iter : theComponentIters) {
						if (iter.getTensionBelow() < theTension) {
							// Iterator is positioned beyond current size
							hasNext = true;
							maxNext = Math.max(maxNext, iter.getTension());
						} else if (iter.getTensionAbove() < theTension && iter.next()) {
							// Iterator moved to next position beyond current size
							hasNext = true;
							maxNext = Math.max(maxNext, iter.getTension());
						} // else Iterator is at its end, nothing to do
					}
					if (hasNext) {
						theTensionBelow = theTension;
						theTension = maxNext;
						theTensionAbove = -MAX_TENSION;
						theSize = 0;
						for (SizeTickIterator iter : theComponentIters)
							theSize += iter.getSize();
					}
					return hasNext;
				}

				@Override
				public boolean previous() {
					float minPrev = MAX_TENSION;
					boolean hasPref = false;
					for (SizeTickIterator iter : theComponentIters) {
						if (iter.getTensionAbove() > theTension) {
							// Iterator is positioned below current size
							hasPref = true;
							minPrev = Math.min(minPrev, iter.getTension());
						} else if (iter.getTensionBelow() > theTension && iter.previous()) {
							// Iterator moved to next position below current size
							hasPref = true;
							minPrev = Math.min(minPrev, iter.getTension());
						} // else Iterator is at its beginning, nothing to do
					}
					if (hasPref) {
						theTensionAbove = theTension;
						theTension = minPrev;
						theTensionBelow = MAX_TENSION;
						theSize = 0;
						for (SizeTickIterator iter : theComponentIters)
							theSize += iter.getSize();
					}
					return hasPref;
				}
			};
		}
	}

	class ParallelSpring extends CompositeSpring {
		public ParallelSpring(List<? extends LayoutSpring> components) {
			super(components);
		}

		@Override
		public SizeTickIterator ticks() {
			// Parallel springs have equal sizes, differing tensions which are summed
			return new CompositeIterator() {
				private float theTensionDiffBelow;
				private float theTensionDiffAbove;

				{
					theTension = 0;
					theTensionDiffBelow = MAX_TENSION;
					theTensionDiffAbove = MAX_TENSION;
					boolean first = true;
					for (SizeTickIterator iter : theComponentIters) {
						int size = iter.getSize();
						if (first || size < theSize)
							theSize = size;
						first = false;
					}
					for (SizeTickIterator iter : theComponentIters) {
						while (iter.getSize() < theSize && iter.next())
							;
						calcTension(iter);
					}
				}

				private void calcTension(SizeTickIterator iter) {
					float tension = iter.getTension(theSize);
					theTension += tension;
					if (tension <= iter.getTensionBelow() && tension >= iter.getTensionAbove()) {
						theTensionDiffBelow = Math.min(theTensionDiffBelow, iter.getTensionBelow() - tension);
						theTensionDiffAbove = Math.min(theTensionDiffBelow, tension = iter.getTensionBelow());
					} else {
						theTensionDiffBelow = 0;
						theTensionDiffAbove = 0;
					}
				}

				@Override
				public float getTensionBelow() {
					return theTension + theTensionDiffBelow;
				}

				@Override
				public float getTensionAbove() {
					return theTension - theTensionDiffAbove;
				}

				@Override
				public boolean next() {
					int nextSize = theSize;
					boolean hasNext = false;
					for (SizeTickIterator iter : theComponentIters) {
						if (iter.getSize() > theSize || iter.next()) {
							int size = iter.getSize();
							if (!hasNext || size < nextSize)
								nextSize = size;
							hasNext = true;
						}
					}
					if (hasNext) {
						theSize = nextSize;
						theTension = 0;
						theTensionDiffBelow = MAX_TENSION;
						theTensionDiffAbove = MAX_TENSION;
						for (SizeTickIterator iter : theComponentIters)
							calcTension(iter);
					}
					return hasNext;
				}

				@Override
				public boolean previous() {
					int prevSize = theSize;
					boolean hasPrev = false;
					for (SizeTickIterator iter : theComponentIters) {
						if (iter.getSize() < theSize || iter.previous()) {
							int size = iter.getSize();
							if (!hasPrev || size > prevSize)
								prevSize = size;
							hasPrev = true;
						}
					}
					if (hasPrev) {
						theSize = prevSize;
						theTension = 0;
						theTensionDiffBelow = MAX_TENSION;
						theTensionDiffAbove = MAX_TENSION;
						for (SizeTickIterator iter : theComponentIters)
							calcTension(iter);
					}
					return hasPrev;
				}
			};
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
}
