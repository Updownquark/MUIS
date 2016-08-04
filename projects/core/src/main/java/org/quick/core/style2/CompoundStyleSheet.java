package org.quick.core.style2;

import org.observe.collect.ObservableCollection;
import org.observe.collect.ObservableList;
import org.observe.collect.ObservableSet;
import org.observe.collect.ObservableSortedSet;
import org.quick.core.QuickElement;
import org.quick.core.style.StyleAttribute;

/** A StyleSheet whose style values come from other StyleSheets */
public class CompoundStyleSheet implements StyleSheet {
	private final ObservableList<StyleSheet> theComponents;

	/** @param components The style sheets that this style sheet will use as sources */
	public CompoundStyleSheet(ObservableList<StyleSheet> components) {
		theComponents = components;
	}

	/** @return The style sheets that this style sheet uses for sources */
	public ObservableList<StyleSheet> getComponents() {
		return theComponents;
	}

	@Override
	public ObservableSet<StyleAttribute<?>> attributes() {
		return ObservableSet.unique(ObservableCollection.flatten(theComponents.map(ss -> ss.attributes())), Object::equals);
	}

	@Override
	public <T> ObservableSortedSet<StyleConditionValue<T>> getStyleExpressions(StyleAttribute<T> attr) {
		return ObservableSortedSet.flatten(theComponents.map(ss -> ss.getStyleExpressions(attr)), StyleConditionValue::compareTo);
	}

	@Override
	public boolean isSet(QuickElement element, StyleAttribute<?> attr) {
		for (StyleSheet ss : theComponents)
			if (ss.isSet(element, attr))
				return true;
		return false;
	}
}
