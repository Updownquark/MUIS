package org.muis.core.style;

/** A listener to be notified when a style expression is added, removed, or changed in a {@link SimpleStyleSheet} */
public interface StyleGroupTypeExpressionListener {
	/** @param evt The style expression event representing the change that occurred in the {@link SimpleStyleSheet} */
	public void eventOccurred(StyleGroupTypeExpressionEvent<?, ?> evt);
}
