package org.quick.base.layout;

import org.qommons.IntList;

public interface SizeTickIterator {
	/** @return The size at the current tick */
	int getSize();

	/**
	 * @return The tension of the spring at the current size. It sign is the same as that of both {@link #getTensionAbove()} and
	 *         {@link #getTensionBelow()} and its magnitude is the minimum of magnitude of the two.
	 */
	default float getTension() {
		float belowTension = getTensionBelow();
		float aboveTension = getTensionAbove();
		if (belowTension < 0)
			return Math.max(belowTension, aboveTension);
		else
			return Math.min(belowTension, aboveTension);
	}

	/**
	 * @return The tension of the spring just below the current size. Used for interpolation for sizes less than this size and greater than
	 *         the previous tick. Must be &gt;={@link #getTensionAbove()} and must have the same sign.
	 */
	float getTensionBelow();

	/**
	 * @return The tension of the spring just above the current size. Used for interpolation for sizes greater than this size and lower than
	 *         the previous tick. Must be &lt;={@link #getTensionBelow()} and must have the same sign.
	 */
	float getTensionAbove();

	/**
	 * Advances this iterator to the next higher tick size if there is one
	 *
	 * @return Whether this iterator had another higher size to move to
	 */
	boolean next();

	/**
	 * Retreats this iterator to the next lower tick size if there is one
	 *
	 * @return Whether this iterator had another lower size to move to
	 */
	boolean previous();

	default float getTension(int size) {
		int now = getSize();
		int lowSz = now;
		int highSz = now;
		float lowTension = getTensionBelow();
		float highTension = getTensionAbove();
		if (size < now) {
			do {
				highSz = lowSz;
				highTension = getTensionBelow();
				if (!previous()) {
					lowSz = 0;
					lowTension = LayoutSpring.MAX_TENSION;
					break;
				}
				lowSz = getSize();
				lowTension = getTensionAbove();
			} while (size < lowSz);
			if (size == lowSz)
				return getTension();
		} else if (size > now) {
			do {
				lowSz = highSz;
				lowTension = getTensionAbove();
				if (!next()) {
					highSz = Integer.MAX_VALUE;
					highTension = -LayoutSpring.MAX_TENSION;
					break;
				}
				highSz = getSize();
				highTension = getTensionBelow();
			} while (size > highSz);
			if (size == highSz)
				return getTension();
		}
		return interpolateFloat(lowSz, lowTension, highSz, highTension, size);
	}

	default int getSize(float tension) {
		int now = getSize();
		int lowSz = now;
		int highSz = now;
		float lowTension = getTensionBelow();
		float midTension;
		float highTension = getTensionAbove();
		if (tension > lowTension) {
			do {
				highSz = lowSz;
				highTension = getTensionBelow();
				if (!previous()) {
					lowSz = 0;
					lowTension = LayoutSpring.MAX_TENSION;
					midTension = lowTension;
					break;
				}
				lowSz = getSize();
				midTension = getTensionAbove();
				lowTension = getTensionBelow();
			} while (tension > lowTension);
			if (tension >= midTension)
				return lowSz;
			else
				lowTension = midTension;
		} else if (tension < highTension) {
			do {
				lowSz = highSz;
				lowTension = getTensionAbove();
				if (!next()) {
					highSz = Integer.MAX_VALUE;
					highTension = -LayoutSpring.MAX_TENSION;
					midTension = highTension;
					break;
				}
				highSz = getSize();
				midTension = getTensionBelow();
				highTension = getTensionAbove();
			} while (tension < highTension);
			if (tension <= midTension)
				return highSz;
			else
				highTension = midTension;
		}
		return interpolateInt(lowSz, lowTension, highSz, highTension, tension);
	}

	default IntList getAllTicks() {
		IntList ticks = new IntList(true, true);
		while (previous()) {
		}
		do {
			ticks.add(getSize());
		} while (next());
		return ticks;
	}

	public static float interpolateFloat(int lowI, float lowF, int highI, float highF, int iValue) {
		return lowF + (highF - lowF) * (iValue - lowI) / (highI - lowI);
	}

	public static int interpolateInt(int lowI, float lowF, int highI, float highF, float fValue) {
		return lowI + (int) ((highI - lowI) * (fValue - lowF) / (highF - lowF));
	}
}
