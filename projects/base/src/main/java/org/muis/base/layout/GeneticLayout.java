package org.muis.base.layout;

import java.awt.Rectangle;

import org.muis.core.MuisElement;

public class GeneticLayout {
	public static interface LayoutScratchPad extends Iterable<MuisElement> {
		public int size();

		public Rectangle get(MuisElement element);
	}

	public static interface LayoutConstraint {
		boolean isViolated(MuisElement container, LayoutScratchPad layout);

		float getViolation(MuisElement container, LayoutScratchPad layout);
	}

	public GeneticLayout() {
	}

	public void addConstraint(LayoutConstraint constraint, boolean variableCost, float cost) {
	}

	public void layout(int width, int height, MuisElement container, MuisElement... children) {
	}
}
