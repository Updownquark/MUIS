package org.quick.widget.core;

import org.observe.Observable;
import org.quick.core.QuickTemplate;
import org.quick.core.layout.Orientation;
import org.quick.core.model.QuickBehavior;
import org.quick.widget.core.layout.QuickWidgetLayout;
import org.quick.widget.core.layout.SizeGuide;

public class QuickTemplateWidget extends QuickWidget {
	public QuickTemplateWidget(QuickWidgetDocument doc, QuickTemplate element, QuickWidget parent) {
		super(doc, element, parent);

		// TODO Move all this to the place where the children are initialized
		if (getLayout() != null)
			getLayout().install(this, Observable.empty);
		for (QuickBehavior behavior : getElement().getBehaviors()) {
			((QuickWidgetBehavior<QuickTemplateWidget>) behavior).install(this);
		}
	}

	@Override
	public QuickTemplate getElement() {
		return (QuickTemplate) super.getElement();
	}

	protected QuickWidgetLayout getLayout() {
		return (QuickWidgetLayout) getElement().getLayout();
	}

	@Override
	public SizeGuide getSizer(Orientation orientation) {
		if (getLayout() != null)
			return getLayout().getSizer(this, getChildren(), orientation);
		else
			return super.getSizer(orientation);
	}

	@Override
	public void doLayout() {
		if (getLayout() != null)
			getLayout().layout(this, getChildren());
		super.doLayout();
	}
}
