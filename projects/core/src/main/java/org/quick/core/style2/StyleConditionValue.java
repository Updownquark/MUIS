package org.quick.core.style2;

import java.util.Objects;

import org.observe.ObservableValue;
import org.quick.core.mgr.QuickMessageCenter;
import org.quick.core.style.StyleAttribute;

/**
 * A conditional style value in a {@link StyleSheet}
 * 
 * @param <T> The type of the attribute that this value is for
 */
public class StyleConditionValue<T> extends StyleValue<T> implements Comparable<StyleConditionValue<?>> {
	private final StyleCondition theCondition;

	/**
	 * @param attribute The style attribute that this value is for
	 * @param condition The condition under which this value applies
	 * @param value The value for the attribute
	 * @param msg The message center to log values from the observable that are not acceptable for the style attribute
	 */
	public StyleConditionValue(StyleAttribute<T> attribute, StyleCondition condition, ObservableValue<? extends T> value,
		QuickMessageCenter msg) {
		super(attribute, value, msg);
		theCondition = condition;
	}

	/** @return The condition under which this value applies */
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
