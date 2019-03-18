package org.quick.core;

import org.observe.ObservableValue;
import org.observe.collect.ObservableCollection;

public interface QuickDefinedWidget<D extends QuickDefinedDocument<?>, E extends QuickElement> {
	void init(D document, E element, QuickDefinedWidget<D, ?> parent) throws QuickException;

	D getDocument();

	E getElement();

	<CE extends QuickElement> QuickDefinedWidget<D, CE> getChild(CE childElement);

	ObservableValue<? extends QuickDefinedWidget<D, ?>> getParent();

	ObservableCollection<? extends QuickDefinedWidget<D, ?>> getChildren();
}
