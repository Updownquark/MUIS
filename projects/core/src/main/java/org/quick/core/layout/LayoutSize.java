package org.quick.core.layout;

import org.quick.core.style.Size;

/** A utility to keep track of pixel and percent sizes, either a sum or a maximum */
public class LayoutSize {
	/**
	 * The maximum percentage that can be added to a sum. This is needed because if the percent got too 100, the math for
	 * {@link #getTotal()} would return an infinite size.
	 */
	public static final float PERCENT_THRESHOLD = 95;

	private int thePixels;

	private float thePercent;

	private boolean isMax;

	/** Creates a sum size */
	public LayoutSize() {
		this(false);
	}

	/** @param max Whether to create a max size or a sum size */
	public LayoutSize(boolean max) {
		isMax = max;
	}

	/**
	 * Creates a sum size (regardless of the type of the parameter) with the given initial value
	 *
	 * @param size The initial value for the new size
	 */
	public LayoutSize(LayoutSize size) {
		thePixels = size.thePixels;
		thePercent = size.thePercent;
	}

	/**
	 * @param pixels The pixels to add to this size
	 * @return The pixel value of this size
	 */
	public int add(int pixels) {
		if(isMax) {
			if(pixels > thePixels)
				thePixels = pixels;
			return thePixels;
		} else {
			int total = LayoutUtils.add(thePixels, pixels);
			if(total < 0)
				total = Integer.MAX_VALUE;
			thePixels = total;
			return total;
		}
	}

	/** @param pixels The pixels to subtract from this size */
	public void minus(int pixels) {
		thePixels -= pixels;
	}

	/** @param percent The percent to add to this size */
	public void addPercent(float percent) {
		if(isMax) {
			if(percent > thePercent)
				thePercent = percent;
		} else
			thePercent += percent;
	}

	/**
	 * @param pixels The pixels to set for this size
	 * @return This size
	 */
	public LayoutSize setPixels(int pixels) {
		thePixels = pixels;
		return this;
	}

	/**
	 * @param percent The percent to set for this size
	 * @return This size
	 */
	public LayoutSize setPercent(float percent) {
		thePercent = percent;
		return this;
	}

	/**
	 * @param size The size to add to this value
	 * @return This size
	 */
	public LayoutSize add(Size size) {
		switch (size.getUnit()) {
		case pixels:
		case lexips:
			add((int) size.getValue());
			break;
		case percent:
			addPercent(size.getValue());
		}
		return this;
	}

	/**
	 * @param size The size to subtract from this size
	 * @return This size
	 */
	public LayoutSize minus(Size size) {
		switch (size.getUnit()) {
		case pixels:
		case lexips:
			thePixels -= (int) size.getValue();
			break;
		case percent:
			thePercent -= size.getValue();
		}
		return this;
	}

	/**
	 * @param size The size to add to this value
	 * @return This size
	 */
	public LayoutSize add(LayoutSize size) {
		add(size.thePixels);
		addPercent(size.thePercent);
		return this;
	}

	/**
	 * @param size The size to subtract from this size
	 * @return This size
	 */
	public LayoutSize minus(LayoutSize size) {
		thePixels -= size.thePixels;
		thePercent -= size.thePercent;
		return this;
	}

	/**
	 * Replaces this value with the given value
	 *
	 * @param size The value to set for this size
	 * @return This size
	 */
	public LayoutSize set(LayoutSize size) {
		thePixels = size.thePixels;
		thePercent = size.thePercent;
		return this;
	}

	/** Clears this size to 0 */
	public void clear() {
		thePixels = 0;
		thePercent = 0;
	}

	/** @return This size's pixel value */
	public int getPixels() {
		return thePixels;
	}

	/** @return This size's percent value */
	public float getPercent() {
		return thePercent;
	}

	public boolean isZero() {
		return thePixels == 0 && thePercent == 0;
	}

	public LayoutSize negate(){
		return new LayoutSize().setPixels(-thePixels).setPercent(-thePercent);

	}

	/**
	 * Evaluates this size
	 *
	 * @return The number of pixels evaluated for this size, assuming this size represents the container size
	 */
	public int getTotal() {
		if(isMax)
			return thePixels;
		if(thePixels == 0)
			return 0;
		if(thePercent == 0)
			return thePixels;
		float percents = thePercent;
		if(percents >= PERCENT_THRESHOLD)
			percents = PERCENT_THRESHOLD;
		return Math.round(thePixels / (1 - percents / 100));
	}

	/**
	 * Evaluates this size
	 *
	 * @param size The total size of the container
	 * @return The number of pixels evaluated for this size assuming the given container size
	 */
	public int getTotal(int size) {
		int percentPix = Math.round(thePercent * size / 100);
		if(isMax)
			return percentPix > thePixels ? percentPix : thePixels;
		else
			return LayoutUtils.add(thePixels, percentPix);
	}

	@Override
	public String toString() {
		if(thePercent == 0)
			return thePixels + "px";
		else if(thePixels == 0)
			return thePercent + "%";
		else
			return thePixels + "px" + (isMax ? " or " : "+") + thePercent + "%";
	}
}
