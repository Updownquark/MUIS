package org.muis.core.style;

/**
 * A listener to be notified when a style expression is added, removed, or changed in a style
 *
 * @param <S> The type of the style set that the listener will listen to
 * @param <E> The type of expression that the listened style set supports
 */
public interface StyleExpressionListener<S extends ConditionalStyle<S, E>, E extends StyleExpression<E>> {
	/** @param evt The style expression event representing the change that occurred in the style */
	public void eventOccurred(StyleExpressionEvent<S, E, ?> evt);
}
