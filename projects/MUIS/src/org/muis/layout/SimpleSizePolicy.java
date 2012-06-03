package org.muis.layout;

/**
 * A simple implementation of SizePolicy that allows all its parameters to be set
 */
public class SimpleSizePolicy implements SizePolicy
{
	private int theMin;

	private int thePref;

	private int theMax;

	private int theStretch;

	public SimpleSizePolicy()
	{
		theMin = 0;
		theMax = Integer.MAX_VALUE;
		thePref = 0;
		theStretch = 0;
	}

	public int getMin()
	{
		return theMin;
	}

	public int getPreferred()
	{
		return thePref;
	}

	public int getMax()
	{
		return theMax;
	}

	public int getStretch()
	{
		return theStretch;
	}

	/**
	 * @param min The minimum size for the widget
	 * @see #getMin()
	 */
	public void setMin(int min)
	{
		theMin = min;
		if(min > theMax)
			theMax = min;
		if(min > thePref)
			thePref = min;
	}

	/**
	 * @param pref The preferred size for the widget
	 * @see #getPreferred()
	 */
	public void setPreferred(int pref)
	{
		thePref = pref;
		if(pref < theMin)
			theMin = pref;
		if(pref > theMax)
			theMax = pref;
	}

	/**
	 * @param max The maximum size for the widget
	 * @see #getMax()
	 */
	public void setMax(int max)
	{
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
	public void setStretch(int stretch)
	{
		theStretch = stretch;
	}
}
