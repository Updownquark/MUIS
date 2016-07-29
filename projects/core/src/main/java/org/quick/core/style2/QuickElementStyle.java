package org.quick.core.style2;

import org.observe.ObservableValue;
import org.observe.collect.ObservableSet;
import org.quick.core.QuickElement;
import org.quick.core.style.StyleAttribute;
import org.quick.core.style.attach.StyleAttributes;

public class QuickElementStyle implements QuickStyle {
	private final QuickElement theElement;

	public QuickElementStyle(QuickElement element) {
		theElement = element;
	}

	@Override
	public QuickElement getElement() {
		return theElement;
	}

	@Override
	public boolean isSetLocal(StyleAttribute<?> attr) {
		QuickStyle localStyle = theElement.get(StyleAttributes.STYLE_ATTRIBUTE);
		return localStyle != null && localStyle.isSet(attr);
	}

	@Override
	public boolean isSet(StyleAttribute<?> attr) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ObservableSet<StyleAttribute<?>> localAttributes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObservableSet<StyleAttribute<?>> attributes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> ObservableValue<T> getLocal(StyleAttribute<T> attr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> ObservableValue<T> get(StyleAttribute<T> attr, boolean withDefault) {
		// TODO Auto-generated method stub
		return null;
	}
}
