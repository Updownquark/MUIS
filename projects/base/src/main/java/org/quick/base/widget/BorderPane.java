package org.quick.base.widget;

import static org.quick.core.layout.LayoutUtils.addRadius;
import static org.quick.core.layout.LayoutUtils.removeRadius;

import org.quick.base.style.BorderStyle;
import org.quick.core.layout.LayoutUtils;
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
		return (Block) getElement(getTemplate().getAttachPoint("contents")).get();
	}

	@Override
	public void doLayout() {
		Size radius = getStyle().get(BackgroundStyle.cornerRadius).get();
		int thickness = getStyle().get(BorderStyle.thickness).get().intValue();
		thickness += getStyle().get(BorderStyle.inset).get().intValue();
		int w = bounds().getWidth();
		int h = bounds().getHeight();
		int contentW = removeRadius(w, radius) - thickness * 2;
		int contentH = removeRadius(h, radius) - thickness * 2;
		Block content = getContentPane();
		int lOff = (w - contentW) / 2;
		int tOff = (h - contentH) / 2;
		content.bounds().setBounds(lOff, tOff, w - lOff - lOff, h - tOff - tOff);
		super.doLayout();
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

	private static class RadiusAddSizePolicy implements org.quick.core.layout.SizeGuide {
		private final SizeGuide theWrapped;

		private org.quick.core.style.Size theRadius;

		private int theBorderThickness;

		RadiusAddSizePolicy(SizeGuide wrap, org.quick.core.style.Size rad, int borderThickness) {
			theWrapped = wrap;
			theRadius = rad;
			theBorderThickness = borderThickness;
		}

		@Override
		public int getMinPreferred(int crossSize) {
			return _addRadius(theWrapped.getMinPreferred(_removeRadius(crossSize)));
		}

		@Override
		public int getMaxPreferred(int crossSize) {
			return _addRadius(theWrapped.getMaxPreferred(_removeRadius(crossSize)));
		}

		@Override
		public int getMin(int crossSize) {
			return _addRadius(theWrapped.getMin(_removeRadius(crossSize)));
		}

		@Override
		public int getPreferred(int crossSize) {
			return _addRadius(theWrapped.getPreferred(_removeRadius(crossSize)));
		}

		@Override
		public int getMax(int crossSize) {
			return _addRadius(theWrapped.getMax(_removeRadius(crossSize)));
		}

		@Override
		public int getBaseline(int size) {
			int remove = size - removeRadius(size, theRadius) - theBorderThickness * 2;
			int ret = theWrapped.getBaseline(size - remove);
			return ret + remove;
		}

		int _addRadius(int size) {
			return LayoutUtils.add(addRadius(size, theRadius), theBorderThickness * 2);
		}

		int _removeRadius(int size) {
			return removeRadius(size, theRadius) - theBorderThickness * 2;
		}
	}
}
