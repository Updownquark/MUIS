package org.muis.core.style.stateful;

/**
 * An extension of MuisStyle that may have different attribute settings depending on a state. MuisStyle query methods on an implementation
 * of this class return the value for the element's current state, if it has one, or the base state (where no states are active) otherwise.
 */
public interface StatefulStyle extends org.muis.core.style.ConditionalStyle<StatefulStyle, StateExpression> {
}
