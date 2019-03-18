package org.quick.widget.base;

import org.quick.base.widget.SimpleContainer;
import org.quick.widget.core.QuickTemplateWidget;

public abstract class SimpleContainerWidget<E extends SimpleContainer> extends QuickTemplateWidget<E> {
	public BlockWidget<?> getContentPane() {
		return (BlockWidget<?>) getChild(getElement().getContentPane());
	}
}
