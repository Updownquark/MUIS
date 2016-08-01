package org.quick.core.style2;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.observe.ObservableValue;
import org.observe.collect.ObservableSet;
import org.quick.core.QuickElement;
import org.quick.core.style.StyleAttribute;

import com.google.common.reflect.TypeToken;

public class ImmutableStyle implements QuickStyle {
	private final QuickElement theElement;
	private final Map<StyleAttribute<?>, Object> theValues;

	private ImmutableStyle(QuickElement element, Map<StyleAttribute<?>, Object> values) {
		theElement=element;
		theValues=Collections.unmodifiableMap(values);
	}

	@Override
	public QuickElement getElement() {
		return theElement;
	}

	@Override
	public boolean isSet(StyleAttribute<?> attr) {
		return theValues.containsKey(attr);
	}

	@Override
	public ObservableSet<StyleAttribute<?>> attributes() {
		return ObservableSet.constant(new TypeToken<StyleAttribute<?>>() {}, theValues.keySet());
	}

	@Override
	public <T> T get(StyleAttribute<T> attr, boolean withDefault) {
		if (!withDefault || theValues.containsKey(attr))
			return (T) theValues.get(attr);
		else
			return attr.getDefault();
	}

	@Override
	public <T> ObservableValue<T> observe(StyleAttribute<T> attr, boolean withDefault) {
		return ObservableValue.constant(attr.getType().getType(), get(attr, withDefault));
	}

	@Override
	public int hashCode() {
		return theValues.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ImmutableStyle && ((ImmutableStyle) obj).theValues.equals(theValues);
	}

	@Override
	public String toString() {
		return theValues.toString();
	}

	public static Builder build(QuickElement element) {
		return new Builder(element);
	}

	public static class Builder {
		private final QuickElement theElement;
		private final Map<StyleAttribute<?>, Object> theValues;

		private Builder(QuickElement element) {
			theElement = element;
			theValues = new LinkedHashMap<>();
		}

		public <T> Builder set(StyleAttribute<T> attr, T value) {
			if (!attr.canAccept(value))
				throw new IllegalArgumentException("Value " + value + " is not valid for attribute " + attr);
			theValues.put(attr, value);
			return this;
		}

		public ImmutableStyle build() {
			return new ImmutableStyle(theElement, theValues);
		}
	}
}
