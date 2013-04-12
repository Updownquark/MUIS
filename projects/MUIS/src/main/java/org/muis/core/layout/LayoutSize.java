package org.muis.core.layout;

import org.muis.core.style.Size;

public class LayoutSize {
	public static final float PERCENT_THRESHOLD = 95;

	private int thePixels;

	private float thePercent;

	public int add(int pixels) {
		int total = thePixels + pixels;
		if(total < 0)
			total = Integer.MAX_VALUE;
		thePixels = total;
		return total;
	}

	public void minus(int pixels) {
		thePixels -= pixels;
	}

	public void addPercent(float percent) {
		thePercent += percent;
	}

	public void setPixels(int pixels) {
		thePixels = pixels;
	}

	public void setPercent(float percent) {
		thePercent = percent;
	}

	public void add(Size size) {
		switch (size.getUnit()) {
		case pixels:
		case lexips:
			add((int) size.getValue());
			break;
		case percent:
			addPercent(size.getValue());
		}
	}

	public void add(LayoutSize size) {
		thePixels += size.thePixels;
		thePercent += size.thePercent;
	}

	public void clear() {
		thePixels = 0;
		thePercent = 0;
	}

	public int getPixels() {
		return thePixels;
	}

	public float getPercent() {
		return thePercent;
	}

	public int getTotal() {
		if(thePixels == 0)
			return 0;
		if(thePercent == 0)
			return thePixels;
		float percents = thePercent;
		if(percents >= PERCENT_THRESHOLD)
			percents = PERCENT_THRESHOLD;
		return Math.round(thePixels / (1 - percents / 100));
	}
}
