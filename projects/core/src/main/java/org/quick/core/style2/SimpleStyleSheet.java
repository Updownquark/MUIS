package org.quick.core.style2;

import org.observe.collect.ObservableSet;
import org.observe.collect.ObservableSortedSet;
import org.quick.core.style.StyleAttribute;

public class SimpleStyleSheet implements StyleSheet {
	private final ObservableMap<StyleAttribute<?>, ObservableSortedSet<StyleConditionValue<?>>> theValues;

	private SimpleStyleSheet

	@Override
	public ObservableSet<StyleAttribute<?>> attributes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> ObservableSortedSet<StyleConditionValue<T>> getStyleExpressions(StyleAttribute<T> attr) {
		// TODO Auto-generated method stub
		return null;
	}

}
