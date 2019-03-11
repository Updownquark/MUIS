package org.quick.widget.base;

import org.quick.base.style.BorderStyle;
import org.quick.base.widget.BorderPane;
import org.quick.core.layout.LayoutGuideType;
import org.quick.core.layout.Orientation;
import org.quick.core.style.BackgroundStyle;
import org.quick.core.style.QuickStyle;
import org.quick.core.style.Size;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.QuickWidgetDocument;
import org.quick.widget.core.layout.LayoutUtils;
import org.quick.widget.core.layout.SizeGuide;

public class BorderPaneWidget extends SimpleContainerWidget {
	public BorderPaneWidget(QuickWidgetDocument doc, BorderPane element, QuickWidget parent) {
		super(doc, element, parent);
		getElement().life().runWhen(() -> {
			QuickStyle selfStyle = getElement().getStyle();
			org.observe.Observable.or(//
				selfStyle.get(BackgroundStyle.cornerRadius).changes().noInit(), //
				selfStyle.get(BorderStyle.thickness).changes().noInit(), //
				selfStyle.get(BorderStyle.inset).changes().noInit()//
			).act(event -> relayout(false));
		}, org.quick.core.QuickConstants.CoreStage.INITIALIZED.toString(), 1);
	}

	@Override
	public BorderPane getElement() {
		return (BorderPane) super.getElement();
	}

	public BlockWidget getContentPane() {
		return (BlockWidget) getChild(getElement().getContentPane());
	}

	@Override
	public void doLayout() {
		QuickStyle selfStyle = getElement().getStyle();
		Size radius = selfStyle.get(BackgroundStyle.cornerRadius).get();
		int thickness = selfStyle.get(BorderStyle.thickness).get().intValue();
		thickness += selfStyle.get(BorderStyle.inset).get().intValue();
		int w = bounds().getWidth();
		int h = bounds().getHeight();
		int contentW = LayoutUtils.removeRadius(w, radius) - thickness * 2;
		int contentH = LayoutUtils.removeRadius(h, radius) - thickness * 2;
		BlockWidget content = getContentPane();
		int lOff = (w - contentW) / 2;
		int tOff = (h - contentH) / 2;
		content.bounds().setBounds(lOff, tOff, w - lOff - lOff, h - tOff - tOff);
		super.doLayout();
	}

	@Override
	public SizeGuide getSizer(Orientation orientation) {
		QuickStyle selfStyle = getElement().getStyle();
		Size radius = selfStyle.get(BackgroundStyle.cornerRadius).get();
		int thickness = selfStyle.get(BorderStyle.thickness).get().intValue();
		thickness += selfStyle.get(BorderStyle.inset).get().intValue();
		return new RadiusAddSizePolicy(getContentPane().getSizer(orientation), radius, thickness);
	}

	private static class RadiusAddSizePolicy implements SizeGuide.GenericSizeGuide {
		private final SizeGuide theWrapped;

		private org.quick.core.style.Size theRadius;

		private int theBorderThickness;

		RadiusAddSizePolicy(SizeGuide wrap, org.quick.core.style.Size rad, int borderThickness) {
			theWrapped = wrap;
			theRadius = rad;
			theBorderThickness = borderThickness;
		}

		@Override
		public int get(LayoutGuideType type, int crossSize, boolean csMax) {
			return _addRadius(theWrapped.get(type, _removeRadius(crossSize), csMax));
		}

		@Override
		public int getBaseline(int size) {
			int remove = size - LayoutUtils.removeRadius(size, theRadius) - theBorderThickness * 2;
			int ret = theWrapped.getBaseline(size - remove);
			return ret + remove;
		}

		int _addRadius(int size) {
			return LayoutUtils.add(LayoutUtils.addRadius(size, theRadius), theBorderThickness * 2);
		}

		int _removeRadius(int size) {
			return LayoutUtils.removeRadius(size, theRadius) - theBorderThickness * 2;
		}
	}
}
