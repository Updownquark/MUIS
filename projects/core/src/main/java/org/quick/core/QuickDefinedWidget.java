package org.quick.core;

import org.observe.ObservableValue;
import org.observe.collect.ObservableCollection;

public interface QuickDefinedWidget {
	QuickDefinedDocument getDocument();

	QuickElement getElement();

	QuickDefinedWidget getChild(QuickElement childElement);

	ObservableValue<? extends QuickDefinedWidget> getParent();

	ObservableCollection<? extends QuickDefinedWidget> getChildren();
}
