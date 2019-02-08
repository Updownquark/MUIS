package org.quick.core.layout;

public interface LayoutSpringEvaluator {
	int getSize(float tension);

	/**
	 * @param length The difference between the destination position and the source position (in pixels)
	 * @return The tension of this spring. Positive tension means a desire for a larger distance; negative means a desire to shrink the
	 *         distance.
	 */
	LayoutSpringEvaluator.TensionAndSnap getTension(int length);

	public static class TensionAndSnap {
		public static final TensionAndSnap ZERO = new TensionAndSnap(0, 0);
		public final float tension;
		public final int snap;

		public TensionAndSnap(float tension, int snap) {
			if (Float.isNaN(tension))
				throw new IllegalArgumentException("NaN");
			this.tension = tension;
			this.snap = snap;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof TensionAndSnap && tension == ((TensionAndSnap) o).tension && snap == ((TensionAndSnap) o).snap;
		}

		@Override
		public String toString() {
			return tension + "->" + snap;
		}
	}
}