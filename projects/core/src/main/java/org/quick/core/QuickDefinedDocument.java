package org.quick.core;

public interface QuickDefinedDocument {
	QuickDocument getQuickDoc();

	QuickImplementation<? extends QuickDefinedDocument, ? extends QuickDefinedWidget> getWidgetImpl();

	QuickDefinedWidget getRoot();
}
