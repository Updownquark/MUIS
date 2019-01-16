package org.quick.core.style;

import java.util.Objects;

import org.observe.ObservableValue;
import org.quick.core.mgr.QuickMessageCenter;

public class StyleConditionValueImpl<T> extends StyleValueImpl<T> implements StyleConditionValue<T> {
	private final StyleCondition theCondition;

	/**
	 * @param attribute The style attribute that this value is for
	 * @param condition The condition under which this value applies
	 * @param value The value for the attribute
	 * @param msg The message center to log values from the observable that are not acceptable for the style attribute
	 */
	public StyleConditionValueImpl(StyleAttribute<T> attribute, StyleCondition condition, ObservableValue<? extends T> value,
		QuickMessageCenter msg) {
		super(attribute, value, msg);
		theCondition = condition;
	}

	/** @return The condition under which this value applies */
	@Override
	public StyleCondition getCondition() {
		return theCondition;
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
		return Objects.equals(theCondition, sev.getCondition());
	}

	@Override
	public String toString() {
		return theCondition + "=" + super.toString();
	}
}
