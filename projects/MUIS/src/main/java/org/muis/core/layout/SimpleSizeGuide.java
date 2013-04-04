package org.muis.core.layout;

/** A simple implementation of {@link SizeGuide} that allows all its parameters to be set directly */
public class SimpleSizeGuide extends AbstractSizeGuide {
	private int theMin;

	private int thePref;

	private int theMax;

	private int theMinPref;

	private int theMaxPref;

	/** Creates a SimpleSizeGuide with zero minimum and preferred sizes, infinite maximum size */
	public SimpleSizeGuide() {
		theMin = 0;
		theMax = Integer.MAX_VALUE;
		thePref = 0;
		theMinPref = 0;
		theMaxPref = 0;
	}

	/**
	 * Creates a filled-in SimpleSizePolicy
	 *
	 * @param min The minimum size for the widget
	 * @param minPref The minimum preferred size for the widget
	 * @param pref The preferred size for the widget
	 * @param maxPref The maximum preferred size for the widget
	 * @param max The maximum size for the widget
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

	/**
	 * @param min The minimum size for the widget
	 * @see #getMin(int, boolean)
	 */
	public void setMin(int min) {
		theMin = min;
		if(min > theMax)
			theMax = min;
		if(min > thePref)
			thePref = min;
	}

	/**
	 * @param minPref The minimum preferred size for the widget
	 * @see #getMinPreferred(int, boolean)
	 */
	public void setMinPreferred(int minPref) {
		theMinPref = minPref;
	}

	/**
	 * @param pref The preferred size for the widget
	 * @see #getPreferred(int, boolean)
	 */
	public void setPreferred(int pref) {
		thePref = pref;
		if(pref < theMin)
			theMin = pref;
		if(pref > theMax)
			theMax = pref;
	}

	/**
	 * @param maxPref The maximum preferred size for the widget
	 * @see #getMaxPreferred(int, boolean)
	 */
	public void setMaxPreferred(int maxPref) {
		theMaxPref = maxPref;
	}

	/**
	 * @param max The maximum size for the widget
	 * @see #getMax(int, boolean)
	 */
	public void setMax(int max) {
		theMax = max;
		if(max < theMin)
			theMin = max;
		if(max < thePref)
			thePref = max;
	}

	public void set(int min, int minPref, int pref, int maxPref, int max) {
		theMin = min;
		theMinPref = minPref;
		thePref = pref;
		theMaxPref = maxPref;
		theMax = max;
	}
}
