package org.quick.core.style.sheet;

/** A {@link StyleSheet} that can be modified directly */
public interface MutableStyleSheet extends StyleSheet, org.quick.core.style.MutableConditionalStyle<StyleSheet, StateGroupTypeExpression<?>> {
}
