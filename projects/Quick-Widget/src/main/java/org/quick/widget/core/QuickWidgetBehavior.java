package org.quick.widget.core;

import org.quick.core.model.QuickBehavior;

public interface QuickWidgetBehavior<E> extends QuickBehavior {
	/** @param element The widget to install this behavior in */
	void install(E element);

	/** @param element The widget to uninstall this behavior from */
	void uninstall(E element);
}
