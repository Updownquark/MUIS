package org.muis.base.widget;

import org.muis.core.layout.SizeGuide;
import org.muis.core.tags.Template;

/** Wraps an element (or set thereof) in a border */
@Template(location = "../../../../simple-container.muis")
public class BorderPane extends org.muis.core.MuisTemplate {
	/** @return The panel containing the contents of this border */
	public Block getContentPane() {
		return (Block) getElement(getTemplate().getAttachPoint("contents"));
	}

	@Override
	public void doLayout() {
		org.muis.core.style.Size radius = getStyle().getSelf().get(org.muis.core.style.BackgroundStyle.cornerRadius);
		int w = bounds().getWidth();
		int h = bounds().getHeight();
		int lOff = radius.evaluate(w);
		int tOff = radius.evaluate(h);
		getContentPane().bounds().setBounds(lOff, tOff, w - lOff - lOff, h - tOff - tOff);
	}

	@Override
	public SizeGuide getWSizer() {
		final org.muis.core.style.Size radius = getStyle().getSelf().get(org.muis.core.style.BackgroundStyle.cornerRadius);
		final int thickness = getStyle().getSelf().get(org.muis.base.style.BorderStyle.thickness).intValue();
		return new RadiusAddSizePolicy(getContentPane().getWSizer(), radius, thickness);
	}

	@Override
	public SizeGuide getHSizer() {
		final org.muis.core.style.Size radius = getStyle().getSelf().get(org.muis.core.style.BackgroundStyle.cornerRadius);
		final int thickness = getStyle().getSelf().get(org.muis.base.style.BorderStyle.thickness).intValue();
		return new RadiusAddSizePolicy(getContentPane().getHSizer(), radius, thickness);
	}

	private static class RadiusAddSizePolicy extends org.muis.core.layout.AbstractSizeGuide {
		private final SizeGuide theWrapped;

		private org.muis.core.style.Size theRadius;

		private int theBorderThickness;

		RadiusAddSizePolicy(SizeGuide wrap, org.muis.core.style.Size rad, int borderThickness) {
			theWrapped = wrap;
			theRadius = rad;
			theBorderThickness = borderThickness;
		}

		@Override
		public int getMinPreferred(int crossSize, boolean csMax) {
			return addRadius(theWrapped.getMinPreferred(crossSize, csMax));
		}

		@Override
		public int getMaxPreferred(int crossSize, boolean csMax) {
			return addRadius(theWrapped.getMaxPreferred(crossSize, csMax));
		}

		@Override
		public int getMin(int crossSize, boolean csMax) {
			return addRadius(theWrapped.getMin(removeRadius(crossSize), csMax));
		}

		@Override
		public int getPreferred(int crossSize, boolean csMax) {
			return addRadius(theWrapped.getPreferred(removeRadius(crossSize), csMax));
		}

		@Override
		public int getMax(int crossSize, boolean csMax) {
			return addRadius(theWrapped.getMax(removeRadius(crossSize), csMax));
		}

		@Override
		public int getBaseline(int size) {
			int remove = size - removeRadius(size) - theBorderThickness;
			int ret = theWrapped.getBaseline(size - remove * 2);
			return ret + remove;
		}

		int addRadius(int size) {
			size += theBorderThickness * 2;
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
			return size - theRadius.evaluate(size) - theBorderThickness;
		}
	}
}
