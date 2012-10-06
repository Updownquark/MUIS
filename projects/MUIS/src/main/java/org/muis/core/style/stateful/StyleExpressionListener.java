package org.muis.core.style.stateful;


/** A listener to be notified when a style expression is added, removed, or changed in a {@link StatefulStyle} */
public interface StyleExpressionListener {
	/** @param evt The style expression event representing the change that occurred in the {@link StatefulStyle} */
	public void eventOccurred(StyleExpressionEvent<?> evt);
}
