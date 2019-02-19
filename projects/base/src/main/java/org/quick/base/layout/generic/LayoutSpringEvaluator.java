package org.quick.base.layout.generic;

import java.util.function.IntSupplier;

import org.quick.core.layout.LayoutSize;
import org.quick.core.layout.SizeGuide;

public interface LayoutSpringEvaluator {
	public static final float SUPER_TENSION = 1_000_000;
	public static final float MAX_TENSION = 1_000;
	public static final float MAX_PREF_TENSION = 1;
	public static final int MAX_LAYOUT_SIZE = 1_000_000;
	/** A placeholder for the tension between the bounds of a layout solver */
	public static LayoutSpringEvaluator BOUNDS_TENSION = new LayoutSpringEvaluator() {
		@Override
		public LayoutSize addSize(LayoutSize size, float tension) {
			throw new IllegalStateException("Placeholder! Should not actually be called");
		}

		@Override
		public TensionAndSnap getTension(int length) {
			throw new IllegalStateException("Placeholder! Should not actually be called");
		}
	};

	LayoutSize addSize(LayoutSize size, float tension);

	/**
	 * @param length The difference between the destination position and the source position (in pixels)
	 * @return The tension of this spring. Positive tension means a desire for a larger distance; negative means a desire to shrink the
	 *         distance.
	 */
	TensionAndSnap getTension(int length);

	public static LayoutSpringEvaluator forSizer(IntSupplier crossSize, SizeGuide sizer) {
		return new LayoutSpringEvaluator() {
			@Override
			public LayoutSize addSize(LayoutSize size, float tension) {
				int cs = crossSize.getAsInt();
				if (tension == 0)
					size.add(cap(sizer.getPreferred(cs, true)));
				else if (tension < 0) {
					tension = -tension;
					if (tension == MAX_PREF_TENSION)
						size.add(cap(sizer.getMinPreferred(cs, true)));
					else if (tension < MAX_PREF_TENSION) {
						int pref = cap(sizer.getPreferred(cs, true));
						int minPref = cap(sizer.getMinPreferred(cs, true));
						size.add(Math.round(minPref + (pref - minPref) * tension / MAX_PREF_TENSION));
					} else if (tension >= MAX_TENSION)
						size.add(cap(sizer.getMin(cs, true)));
					else {
						int minPref = cap(sizer.getMinPreferred(cs, true));
						int min = cap(sizer.getMin(cs, true));
						size.add(Math.round(min + (minPref - min) * (tension - MAX_PREF_TENSION) / (MAX_TENSION - MAX_PREF_TENSION)));
					}
				} else {
					if (tension == MAX_PREF_TENSION)
						size.add(cap(sizer.getMaxPreferred(cs, true)));
					else if (tension < MAX_PREF_TENSION) {
						int pref = cap(sizer.getPreferred(cs, true));
						int maxPref = cap(sizer.getMaxPreferred(cs, true));
						size.add(Math.round(pref + (maxPref - pref) * tension / MAX_PREF_TENSION));
					} else if (tension >= MAX_TENSION)
						size.add(cap(sizer.getMax(cs, true)));
					else {
						int maxPref = cap(sizer.getMaxPreferred(cs, true));
						int max = cap(sizer.getMax(cs, true));
						size.add(Math.round(maxPref + (max - maxPref) * (tension - MAX_PREF_TENSION) / (MAX_TENSION - MAX_PREF_TENSION)));
					}
				}
				return size;
			}

			@Override
			public TensionAndSnap getTension(int length) {
				length = cap(length);
				int cs = crossSize.getAsInt();
				int pref = cap(sizer.getPreferred(cs, true));
				if (length < pref) {
					int minPref = cap(sizer.getMinPreferred(cs, true));
					if (length >= minPref) {
						float tension = MAX_PREF_TENSION * (pref - length) / (pref - minPref);
						return new TensionAndSnap(tension, pref);
					} else {
						int min = cap(sizer.getMin(cs, true));
						if (length == min)
							return new TensionAndSnap(MAX_TENSION, minPref);
						else if (length < min) {
							if (length < 0)
								return new TensionAndSnap(SUPER_TENSION, min);
							else {
								float tension = MAX_TENSION + (SUPER_TENSION - MAX_TENSION) * (min - length) / min;
								return new TensionAndSnap(tension, min);
							}
						} else {
							float tension = MAX_PREF_TENSION + (MAX_TENSION - MAX_PREF_TENSION) * (minPref - length) / (minPref - min);
							return new TensionAndSnap(tension, minPref);
						}
					}
				} else if (length == pref) {
					return new TensionAndSnap(0, pref);
				} else {
					int maxPref = cap(sizer.getMaxPreferred(cs, true));
					if (length <= maxPref) {
						float tension = -MAX_PREF_TENSION * (length - pref) / (maxPref - pref);
						return new TensionAndSnap(tension, pref);
					} else {
						int max = cap(sizer.getMax(cs, true));
						if (length == max)
							return new TensionAndSnap(-MAX_TENSION, maxPref);
						else if (length > max) {
							float tension = -MAX_TENSION - (SUPER_TENSION - MAX_TENSION) * (length - max) / (MAX_LAYOUT_SIZE - max);
							return new TensionAndSnap(tension, max);
						} else {
							float tension = -MAX_PREF_TENSION - (MAX_TENSION - MAX_PREF_TENSION) * (length - maxPref) / (max - maxPref);
							return new TensionAndSnap(tension, maxPref);
						}
					}
				}
			}

			private int cap(int size) {
				if (size > MAX_LAYOUT_SIZE)
					return MAX_LAYOUT_SIZE;
				else
					return size;
			}
		};
	}
}