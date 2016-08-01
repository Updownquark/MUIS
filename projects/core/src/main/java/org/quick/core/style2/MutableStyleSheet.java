package org.quick.core.style2;

import java.util.ArrayList;

import org.quick.core.style.StyleAttribute;

public interface MutableStyleSheet extends StyleSheet, ConditionalStyleSetter {
	MutableStyleSheet clear(StyleAttribute<?> attr, StyleCondition condition);

	default <T> MutableStyleSheet clear(StyleAttribute<T> attr) {
		for(StyleConditionValue<T> scv : new ArrayList<>(getStyleExpressions(attr))){
			clear(attr, scv.getCondition());
		}
		return this;
	}
}
