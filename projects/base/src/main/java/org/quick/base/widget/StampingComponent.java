package org.quick.base.widget;

import org.observe.ObservableValue;
import org.quick.core.QuickElement;

public interface StampingComponent {
	ObservableValue<? extends QuickElement> getRenderer();

	ObservableValue<? extends QuickElement> getEditor();

}
