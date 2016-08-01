package org.quick.core.style2;

import java.util.Objects;

import org.observe.ObservableValue;
import org.quick.core.mgr.QuickMessageCenter;
import org.quick.core.style.StyleAttribute;

public class StyleConditionValue<T> extends StyleValue<T> implements Comparable<StyleConditionValue<?>> {
	private final StyleCondition theCondition;

	public StyleConditionValue(StyleAttribute<T> attribute, StyleCondition condition, ObservableValue<? extends T> value,
		QuickMessageCenter msg) {
		super(attribute, value, msg);
		theCondition = condition;
	}

	public StyleCondition getCondition() {
		return theCondition;
	}

	@Override
	public int compareTo(StyleConditionValue<?> o) {
		return theCondition.compareTo(o.theCondition);
	}

	@Override
	public int hashCode() {
		int ret = super.hashCode();
		ret *= 13;
		if (theCondition != null)
			ret += theCondition.hashCode();
		return ret;
	}

	@Override
	public boolean equals(Object obj) {
		if (!super.equals(obj))
			return false;
		StyleConditionValue<?> sev = (StyleConditionValue<?>) obj;
		return Objects.equals(theCondition, sev.theCondition);
	}

	@Override
	public String toString() {
		return theCondition + "=" + super.toString();
	}
}
