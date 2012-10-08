package org.muis.core.style.sheet;

/**
 * Represents a style sheet in MUIS that can be populated with style attribute values that are potentially specific to a
 * {@link org.muis.core.style.attach.NamedStyleGroup group}, {@link org.muis.core.style.attach.TypedStyleGroup type}, and/or
 * {@link org.muis.core.mgr.MuisState state}
 */
public interface StyleSheet extends org.muis.core.style.ConditionalStyle<StyleSheet, StateGroupTypeExpression<?>> {
}
