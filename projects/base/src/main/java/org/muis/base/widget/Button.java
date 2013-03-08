package org.muis.base.widget;

import org.muis.core.layout.SizePolicy;
import org.muis.core.tags.Template;

/** Implements a button. Buttons can be set to toggle mode or normal mode. Buttons are containers that may have any type of content in them. */
@Template(location = "../../../../button.muis")
public class Button extends org.muis.core.MuisTemplate {
	/** Creates a button */
	public Button() {
		setFocusable(true);
	}

	@Override
	public void doLayout() {
		org.muis.core.MuisElement contents = getElement(getTemplate().getAttachPoint("contents"));
		org.muis.core.style.Size radius = getStyle().getSelf().get(org.muis.core.style.BackgroundStyles.cornerRadius);
		int w = getWidth();
		int h = getHeight();
		int lOff = radius.evaluate(w);
		int tOff = radius.evaluate(h);
		contents.setBounds(lOff, tOff, w - lOff - lOff, h - tOff - tOff);
	}

	@Override
	public SizePolicy getWSizer(int height) {
		org.muis.core.MuisElement contents = getElement(getTemplate().getAttachPoint("contents"));
		final org.muis.core.style.Size radius = getStyle().getSelf().get(org.muis.core.style.BackgroundStyles.cornerRadius);
		int tOff = radius.evaluate(height);
		height -= tOff / 2;
		if(height < 0)
			height = 0;
		return new RadiusAddSizePolicy(contents.getWSizer(height), radius);
	}

	@Override
	public SizePolicy getHSizer(int width) {
		org.muis.core.MuisElement contents = getElement(getTemplate().getAttachPoint("contents"));
		final org.muis.core.style.Size radius = getStyle().getSelf().get(org.muis.core.style.BackgroundStyles.cornerRadius);
		int lOff = radius.evaluate(width);
		width -= lOff / 2;
		if(width < 0)
			width = 0;
		return new RadiusAddSizePolicy(contents.getHSizer(width), radius);
	}

	private static class RadiusAddSizePolicy implements SizePolicy {
		private final SizePolicy theWrapped;

		private org.muis.core.style.Size theRadius;

		RadiusAddSizePolicy(SizePolicy wrap, org.muis.core.style.Size rad) {
			theWrapped = wrap;
			theRadius = rad;
		}

		@Override
		public int getMin() {
			return addRadius(theWrapped.getMin());
		}

		@Override
		public int getPreferred() {
			return addRadius(theWrapped.getPreferred());
		}

		@Override
		public int getMax() {
			return addRadius(theWrapped.getMax());
		}

		@Override
		public int getStretch() {
			return theWrapped.getStretch();
		}

		int addRadius(int size) {
			switch (theRadius.getUnit()) {
			case pixels:
			case lexips:
				size += theRadius.getValue() * 2;
				break;
			case percent:
				float radPercent = theRadius.getValue() * 2;
				if(radPercent >= 100)
					radPercent = 90;
				size = Math.round(size * 100 / (100f - radPercent));
				break;
			}
			return size;
		}
	}
}
