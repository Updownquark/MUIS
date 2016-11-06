package org.quick.base.widget;

import org.quick.base.style.BorderStyle;
import org.quick.core.layout.SizeGuide;
import org.quick.core.style.BackgroundStyle;
import org.quick.core.style.QuickStyle;
import org.quick.core.style.Size;

/** Wraps an element (or set thereof) in a border */
public class BorderPane extends SimpleContainer {
	/** Creates a border pane */
	public BorderPane() {
		life().runWhen(
			() -> {
				QuickStyle selfStyle = getStyle();
				org.observe.Observable.or(selfStyle.get(BackgroundStyle.cornerRadius), selfStyle.get(BorderStyle.thickness),
					selfStyle.get(BorderStyle.inset)).act(event -> relayout(false));
			}, org.quick.core.QuickConstants.CoreStage.INITIALIZED.toString(), 1);
	}

	/** @return The panel containing the contents of this border */
	public Block getContentPane() {
		return (Block) getElement(getTemplate().getAttachPoint("contents"));
	}

	@Override
	public void doLayout() {
		QuickStyle selfStyle = getStyle();
		Size radius = selfStyle.get(BackgroundStyle.cornerRadius).get();
		int thickness = selfStyle.get(BorderStyle.thickness).get().intValue();
		thickness += selfStyle.get(BorderStyle.inset).get().intValue();
		int w = bounds().getWidth();
		int h = bounds().getHeight();
		int lOff = radius.evaluate(w) + thickness;
		int tOff = radius.evaluate(h) + thickness;
		getContentPane().bounds().setBounds(lOff, tOff, w - lOff - lOff, h - tOff - tOff);
	}

	@Override
	public SizeGuide getWSizer() {
		QuickStyle selfStyle = getStyle();
		Size radius = selfStyle.get(BackgroundStyle.cornerRadius).get();
		int thickness = selfStyle.get(BorderStyle.thickness).get().intValue();
		thickness += selfStyle.get(BorderStyle.inset).get().intValue();
		return new RadiusAddSizePolicy(getContentPane().getWSizer(), radius, thickness);
	}

	@Override
	public SizeGuide getHSizer() {
		QuickStyle selfStyle = getStyle();
		Size radius = selfStyle.get(BackgroundStyle.cornerRadius).get();
		int thickness = selfStyle.get(BorderStyle.thickness).get().intValue();
		thickness += selfStyle.get(BorderStyle.inset).get().intValue();
		return new RadiusAddSizePolicy(getContentPane().getHSizer(), radius, thickness);
	}

	private static class RadiusAddSizePolicy extends org.quick.core.layout.AbstractSizeGuide {
		private final SizeGuide theWrapped;

		private org.quick.core.style.Size theRadius;

		private int theBorderThickness;

		RadiusAddSizePolicy(SizeGuide wrap, org.quick.core.style.Size rad, int borderThickness) {
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
