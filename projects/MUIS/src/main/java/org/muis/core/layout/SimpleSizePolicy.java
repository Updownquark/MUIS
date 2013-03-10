package org.muis.core.layout;

/** A simple implementation of SizePolicy that allows all its parameters to be set directly */
public class SimpleSizePolicy implements SizePolicy {
	private int theMin;

	private int thePref;

	private int theMax;

	private int theMinPref;

	private int theMaxPref;

	private float theStretch;

	/**
	 * Creates a SimpleSizePolicy with zero minimum and preferred sizes, infinite maximum size, and zero stretch benefit
	 */
	public SimpleSizePolicy() {
		theMin = 0;
		theMax = Integer.MAX_VALUE;
		thePref = 0;
		theMinPref = 0;
		theMaxPref = 0;
		theStretch = 0;
	}

	/**
	 * Creates a filled-in SimpleSizePolicy
	 *
	 * @param min The minimum size for the widget
	 * @param minPref The minimum preferred size for the widget
	 * @param pref The preferred size for the widget
	 * @param maxPref The maximum preferred size for the widget
	 * @param max The maximum size for the widget
	 * @param stretch The stretch factor for the widget
	 */
	public SimpleSizePolicy(int min, int minPref, int pref, int maxPref, int max, int stretch) {
		theMin = min;
		theMinPref = minPref;
		thePref = pref;
		theMaxPref = maxPref;
		theMax = max;
		theStretch = stretch;
	}

	@Override
	public int getMinPreferred() {
		return theMinPref;
	}

	@Override
	public int getMaxPreferred() {
		return theMaxPref;
	}

	@Override
	public int getMin(int crossSize) {
		return theMin;
	}

	@Override
	public int getPreferred(int crossSize) {
		return thePref;
	}

	@Override
	public int getMax(int crossSize) {
		return theMax;
	}

	@Override
	public float getStretch() {
		return theStretch;
	}

	/**
	 * @param min The minimum size for the widget
	 * @see #getMin(int)
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
	 * @see #getMinPreferred()
	 */
	public void setMinPreferred(int minPref) {
		theMinPref = minPref;
	}

	/**
	 * @param pref The preferred size for the widget
	 * @see #getPreferred(int)
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
	 * @see #getMaxPreferred()
	 */
	public void setMaxPreferred(int maxPref) {
		theMaxPref = maxPref;
	}

	/**
	 * @param max The maximum size for the widget
	 * @see #getMax(int)
	 */
	public void setMax(int max) {
		theMax = max;
		if(max < theMin)
			theMin = max;
		if(max < thePref)
			thePref = max;
	}

	/**
	 * @param stretch The stretch factor for the widget
	 * @see #getStretch()
	 */
	public void setStretch(float stretch) {
		theStretch = stretch;
	}
}
