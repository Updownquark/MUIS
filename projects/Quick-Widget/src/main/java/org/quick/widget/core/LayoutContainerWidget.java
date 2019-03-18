package org.quick.widget.core;

import org.qommons.Transaction;
import org.quick.core.LayoutContainer;
import org.quick.core.QuickConstants;
import org.quick.core.QuickException;
import org.quick.core.QuickLayout;
import org.quick.core.layout.Orientation;
import org.quick.core.mgr.AttributeManager2.AttributeValue;
import org.quick.widget.core.layout.QuickWidgetLayout;
import org.quick.widget.core.layout.SizeGuide;

public class LayoutContainerWidget<E extends LayoutContainer> extends QuickWidget<E> {
	public void init(QuickWidgetDocument doc, E element, QuickWidget<?> parent) throws QuickException {
		super.init(doc, element, parent);
		QuickWidgetLayout defLayout = getDefaultLayout();
		AttributeValue<QuickLayout> layoutAtt;
		try {
			layoutAtt = getElement().atts().accept(LayoutContainer.LAYOUT_ATTR, this, a -> a.init(defLayout).required());
			getElement().life().runWhen(() -> {
				layoutAtt.value()
				.act(layout -> ((QuickWidgetLayout) layout).install(LayoutContainerWidget.this, layoutAtt.changes().noInit()));
			}, QuickConstants.CoreStage.STARTUP.toString(), -1);
		} catch (IllegalArgumentException e) {
			getElement().msg().error("Could not set default layout", e, "layout", defLayout);
		}
	}

	/**
	 * Allows types to specify their default layout
	 *
	 * @return The default layout for this container. Null by default.
	 */
	protected QuickWidgetLayout getDefaultLayout() {
		return null;
	}

	public QuickWidgetLayout getLayout() {
		return (QuickWidgetLayout) getElement().getLayout();
	}

	@Override
	public SizeGuide getSizer(Orientation orientation) {
		QuickWidgetLayout layout = getLayout();
		if (layout != null) {
			try (Transaction t = getChildren().lock(false, null)) {
				return layout.getSizer(this, getChildren(), orientation);
			}
		} else
			return super.getSizer(orientation);
	}

	@Override
	public void doLayout() {
		if (bounds().isEmpty())
			return;
		QuickWidgetLayout layout = getLayout();
		if (layout != null) {
			try (Transaction t = getChildren().lock(false, null)) {
				layout.layout(this, getChildren());
			}
		}
		super.doLayout();
	}
}
