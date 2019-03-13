package org.quick.core;

public interface QuickImplementation<D extends QuickDefinedDocument, W extends QuickDefinedWidget> {
	W createWidget(QuickDefinedDocument doc, QuickElement element) throws QuickException;
}
