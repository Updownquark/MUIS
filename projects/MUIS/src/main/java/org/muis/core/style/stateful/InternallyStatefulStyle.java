package org.muis.core.style.stateful;

import org.muis.core.mgr.MuisState;
import org.muis.core.style.MuisStyle;
import org.observe.collect.ObservableSet;

/**
 * Represents a {@link StatefulStyle} that also contains an internal state, meaning it can provide {@link MuisStyle} functionality by
 * evaluating its style expressions against its own state.
 */
public interface InternallyStatefulStyle extends StatefulStyle, MuisStyle {
	/** @return The internal state of this style */
	ObservableSet<MuisState> getState();
}
