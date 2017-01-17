package org.quick.core.style;

import java.util.Map;
import java.util.Set;

import org.quick.core.prop.QuickProperty;

public interface StyleModifier {
	boolean isApplicable(QuickProperty<?> property);

	Set<StyleModifierProperty<?>> getConfigProperties();

	<T> T apply(QuickProperty<T> property, T value, long time, Map<StyleModifierProperty<?>, Object> config);

	boolean isAnimating(long time);
}
