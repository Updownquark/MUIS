package org.muis.core.style.stateful;

import org.muis.core.mgr.MuisState;
import org.muis.core.style.MuisStyle;

/**
 * Represents a {@link StatefulStyle} that also contains an internal state, meaning it can provide {@link MuisStyle} functionality by
 * evaluating its style expressions against its own state.
 */
public interface InternallyStatefulStyle extends StatefulStyle, MuisStyle {
	/** @return The current internal state of this style */
	MuisState [] getState();

	/** @param listener The listener to be notified when this style's state changes */
	void addStateChangeListener(StateChangeListener listener);

	/** @param listener The listener to stop notification for */
	void removeStateChangeListener(StateChangeListener listener);
}
