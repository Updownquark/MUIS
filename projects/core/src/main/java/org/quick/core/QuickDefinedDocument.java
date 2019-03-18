package org.quick.core;

public interface QuickDefinedDocument<W extends QuickDefinedWidget<?, ?>> {
	void init(QuickDocument document, QuickWidgetSet<?, W> widgetSet) throws QuickException;

	QuickDocument getQuickDoc();

	QuickWidgetSet<?, W> getWidgetSet();

	QuickDefinedWidget getRoot();
}
