package org.quick.core.style;

import org.observe.util.TypeTokens;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

/**
 * A conditional style value in a {@link StyleSheet}
 *
 * @param <T> The type of the attribute that this value is for
 */
public interface StyleConditionValue<T> extends StyleValue<T>, Comparable<StyleConditionValue<?>> {
	@SuppressWarnings("rawtypes")
	static TypeToken<StyleConditionValue<?>> TYPE = TypeTokens.get().keyFor(StyleConditionValue.class)
		.enableCompoundTypes(new TypeTokens.CompoundTypeCreator<StyleConditionValue>() {
			@Override
			public <P> TypeToken<? extends StyleConditionValue> createCompoundType(TypeToken<P> param) {
				return new TypeToken<StyleConditionValue<P>>() {}.where(new TypeParameter<P>() {}, param);
			}
		}).parameterized(() -> new TypeToken<StyleConditionValue<?>>() {});

	/** @return The condition under which this value applies */
	StyleCondition getCondition();

	@Override
	default int compareTo(StyleConditionValue<?> o) {
		return getCondition().compareTo(o.getCondition());
	}
}
