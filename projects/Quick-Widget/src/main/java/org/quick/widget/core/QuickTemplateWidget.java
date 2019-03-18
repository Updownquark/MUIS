package org.quick.widget.core;

import org.observe.Observable;
import org.quick.core.QuickDefinedWidget;
import org.quick.core.QuickException;
import org.quick.core.QuickTemplate;
import org.quick.core.layout.Orientation;
import org.quick.core.model.QuickBehavior;
import org.quick.widget.core.layout.QuickWidgetLayout;
import org.quick.widget.core.layout.SizeGuide;

public class QuickTemplateWidget<E extends QuickTemplate> extends QuickWidget<E> {
	@Override
	public void init(QuickWidgetDocument document, E element, QuickDefinedWidget<QuickWidgetDocument, ?> parent) throws QuickException {
		super.init(document, element, parent);
		if (getLayout() != null)
			getLayout().install(this, Observable.empty);
		for (QuickBehavior behavior : getElement().getBehaviors()) {
			((QuickWidgetBehavior<QuickTemplateWidget<?>>) behavior).install(this);
		}
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
