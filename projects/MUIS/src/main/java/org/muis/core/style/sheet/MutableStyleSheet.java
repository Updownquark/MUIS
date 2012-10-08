package org.muis.core.style.sheet;

/** A {@link StyleSheet} that can be modified directly */
public interface MutableStyleSheet extends StyleSheet, org.muis.core.style.MutableConditionalStyle<StyleSheet, StateGroupTypeExpression<?>> {
}
