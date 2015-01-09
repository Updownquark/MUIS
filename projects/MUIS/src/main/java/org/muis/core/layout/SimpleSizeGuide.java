package org.muis.core.layout;

/** A simple implementation of {@link SizeGuide} that allows all its parameters to be set directly */
public class SimpleSizeGuide extends AbstractSizeGuide {
	private int theMin;

	private int thePref;

	private int theMax;

	private int theMinPref;

	private int theMaxPref;

	private int theBaseline;

	/** Creates a SimpleSizeGuide with zero minimum and preferred sizes, infinite maximum size */
	public SimpleSizeGuide() {
		theMin = 0;
		theMax = Integer.MAX_VALUE;
		thePref = 0;
		theMinPref = 0;
		theMaxPref = Integer.MAX_VALUE;
		theBaseline = 0;
	}

	/**
	 * Creates a filled-in SimpleSizePolicy
	 *
	 * @param min The minimum size for the guide
	 * @param minPref The minimum preferred size for the guide
	 * @param pref The preferred size for the guide
	 * @param maxPref The maximum preferred size for the guide
	 * @param max The maximum size for the guide
	 */
	public SimpleSizeGuide(int min, int minPref, int pref, int maxPref, int max) {
		theMin = min;
		theMinPref = minPref;
		thePref = pref;
		theMaxPref = maxPref;
		theMax = max;
	}

	@Override
	public int getMinPreferred(int crossSize, boolean csMax) {
		return theMinPref;
	}

	@Override
	public int getMaxPreferred(int crossSize, boolean csMax) {
		return theMaxPref;
	}

	@Override
	public int getMin(int crossSize, boolean csMax) {
		return theMin;
	}

	@Override
	public int getPreferred(int crossSize, boolean csMax) {
		return thePref;
	}

	@Override
	public int getMax(int crossSize, boolean csMax) {
		return theMax;
	}

	@Override
	public int getBaseline(int size) {
		return theBaseline;
	}

	/**
	 * @param min The minimum size for the guide
	 * @return This instance, for chaining
	 * @see #getMin(int, boolean)
	 */
	public SimpleSizeGuide setMin(int min) {
		theMin = min;
		if(min > theMax)
			theMax = min;
		if(min > thePref)
			thePref = min;
		return this;
	}

	/**
	 * @param minPref The minimum preferred size for the guide
	 * @return This instance, for chaining
	 * @see #getMinPreferred(int, boolean)
	 */
	public SimpleSizeGuide setMinPreferred(int minPref) {
		theMinPref = minPref;
		return this;
	}

	/**
	 * @param pref The preferred size for the guide
	 * @return This instance, for chaining
	 * @see #getPreferred(int, boolean)
	 */
	public SimpleSizeGuide setPreferred(int pref) {
		thePref = pref;
		if(pref < theMin)
			theMin = pref;
		if(pref > theMax)
			theMax = pref;
		return this;
	}

	/**
	 * @param maxPref The maximum preferred size for the guide
	 * @return This instance, for chaining
	 * @see #getMaxPreferred(int, boolean)
	 */
	public SimpleSizeGuide setMaxPreferred(int maxPref) {
		theMaxPref = maxPref;
		return this;
	}

	/**
	 * @param max The maximum size for the guide
	 * @return This instance, for chaining
	 * @see #getMax(int, boolean)
	 */
	public SimpleSizeGuide setMax(int max) {
		theMax = max;
		if(max < theMin)
			theMin = max;
		if(max < thePref)
			thePref = max;
		return this;
	}

	/**
	 * Sets all size parameters on this guide
	 *
	 * @param min The minimum size for the guide
	 * @param minPref The minimum preferred size for the guide
	 * @param pref The preferred size for the guide
	 * @param maxPref The maximum preferred size for the guide
	 * @param max The maximum size for the guide
	 * @return This instance, for chaining
	 */
	public SimpleSizeGuide set(int min, int minPref, int pref, int maxPref, int max) {
		theMin = min;
		theMinPref = minPref;
		thePref = pref;
		theMaxPref = maxPref;
		theMax = max;
		return this;
	}

	/**
	 * @param baseline The baseline for the guide
	 * @return This instance, for chaining
	 */
	public SimpleSizeGuide setBaseline(int baseline) {
		theBaseline = baseline;
		return this;
	}

	/**
	 * @param min The value to get a max of with this guide's minimum
	 * @param minPref The value to get a max of with this guide's minimum preferred
	 * @param pref The value to get a max of with this guide's preferred
	 * @param maxPref The value to get a max of with this guide's maximum preferred
	 * @param max The value to get a max of with this guide's maximum
	 * @return Whether this guide was changed as a result of the call
	 */
	public boolean max(int min, int minPref, int pref, int maxPref, int max) {
		boolean ret = false;
		if(min > theMin) {
			theMin = min;
			ret = true;
		}
		if(minPref > theMinPref) {
			theMinPref = minPref;
			ret = true;
		}
		if(pref > thePref) {
			thePref = pref;
			ret = true;
		}
		if(maxPref > theMaxPref) {
			theMaxPref = maxPref;
			ret = true;
		}
		if(max > theMax) {
			theMax = max;
			ret = true;
		}
		return ret;
	}

	/**
	 * @param min The value to add to this guide's minimum
	 * @param minPref The value to add to this guide's minimum preferred
	 * @param pref The value to add to this guide's preferred
	 * @param maxPref The value to add to this guide's maximum preferred
	 * @param max The value to add to this guide's maximum
	 */
	public void add(int min, int minPref, int pref, int maxPref, int max) {
		theMin = add(theMin, min);
		theMinPref = add(theMinPref, minPref);
		thePref = add(thePref, pref);
		theMaxPref = add(theMaxPref, maxPref);
		theMax = add(theMax, max);
	}

	private static int add(int i1, int i2) {
		int ret = i1 + i2;
		if(ret < i1)
			ret = Integer.MAX_VALUE;
		return ret;
	}
}
