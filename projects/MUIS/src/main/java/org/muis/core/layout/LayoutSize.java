package org.muis.core.layout;

import org.muis.core.style.Size;

public class LayoutSize {
	public static final float PERCENT_THRESHOLD = 95;

	private int thePixels;

	private float thePercents;

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
		thePercents += percent;
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

	public int getTotal() {
		if(thePixels == 0)
			return 0;
		if(thePercents == 0)
			return thePixels;
		float percents = thePercents;
		if(percents >= PERCENT_THRESHOLD)
			percents = PERCENT_THRESHOLD;
		return Math.round(thePixels / (1 - percents / 100));
	}
}
