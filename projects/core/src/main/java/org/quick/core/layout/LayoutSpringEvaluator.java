package org.quick.core.layout;

public interface LayoutSpringEvaluator {
	int getSize(float tension);

	/**
	 * @param length The difference between the destination position and the source position (in pixels)
	 * @return The tension of this spring. Positive tension means a desire for a larger distance; negative means a desire to shrink the
	 *         distance.
	 */
	LayoutSpringEvaluator.TensionAndGradient getTension(int length);

	public static class TensionAndGradient {
		public final float tension;
		public final float gradient;

		public TensionAndGradient(float tension, float gradient) {
			this.tension = tension;
			this.gradient = gradient;
		}
	}
}