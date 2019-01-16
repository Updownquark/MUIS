package org.quick.core.style;

/**
 * A conditional style value in a {@link StyleSheet}
 *
 * @param <T> The type of the attribute that this value is for
 */
public interface StyleConditionValue<T> extends StyleValue<T>, Comparable<StyleConditionValue<?>> {
	/** @return The condition under which this value applies */
	StyleCondition getCondition();

	@Override
	default int compareTo(StyleConditionValue<?> o) {
		return getCondition().compareTo(o.getCondition());
	}
}
