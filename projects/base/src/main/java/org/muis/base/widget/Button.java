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
		int w = bounds().getWidth();
		int h = bounds().getHeight();
		int lOff = radius.evaluate(w);
		int tOff = radius.evaluate(h);
		contents.bounds().setBounds(lOff, tOff, w - lOff - lOff, h - tOff - tOff);
	}

	@Override
	public SizePolicy getWSizer() {
		org.muis.core.MuisElement contents = getElement(getTemplate().getAttachPoint("contents"));
		final org.muis.core.style.Size radius = getStyle().getSelf().get(org.muis.core.style.BackgroundStyles.cornerRadius);
		return new RadiusAddSizePolicy(contents.getWSizer(), radius);
	}

	@Override
	public SizePolicy getHSizer() {
		org.muis.core.MuisElement contents = getElement(getTemplate().getAttachPoint("contents"));
		final org.muis.core.style.Size radius = getStyle().getSelf().get(org.muis.core.style.BackgroundStyles.cornerRadius);
		return new RadiusAddSizePolicy(contents.getHSizer(), radius);
	}

	private static class RadiusAddSizePolicy implements SizePolicy {
		private final SizePolicy theWrapped;

		private org.muis.core.style.Size theRadius;

		RadiusAddSizePolicy(SizePolicy wrap, org.muis.core.style.Size rad) {
			theWrapped = wrap;
			theRadius = rad;
		}

		@Override
		public int getMinPreferred() {
			return addRadius(theWrapped.getMinPreferred());
		}

		@Override
		public int getMaxPreferred() {
			return addRadius(theWrapped.getMaxPreferred());
		}

		@Override
		public int getMin(int crossSize) {
			return addRadius(theWrapped.getMin(removeRadius(crossSize)));
		}

		@Override
		public int getPreferred(int crossSize) {
			return addRadius(theWrapped.getPreferred(removeRadius(crossSize)));
		}

		@Override
		public int getMax(int crossSize) {
			return addRadius(theWrapped.getMax(removeRadius(crossSize)));
		}

		@Override
		public float getStretch() {
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
			if(size < 0)
				return Integer.MAX_VALUE;
			return size;
		}

		int removeRadius(int size) {
			return size - theRadius.evaluate(size);
		}
	}
}
