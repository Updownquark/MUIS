package org.muis.core.style.stateful;

import java.util.Set;

import org.muis.core.mgr.MuisState;
import org.muis.core.rx.ObservableValue;
import org.muis.core.style.MuisStyle;

/**
 * Represents a {@link StatefulStyle} that also contains an internal state, meaning it can provide {@link MuisStyle} functionality by
 * evaluating its style expressions against its own state.
 */
public interface InternallyStatefulStyle extends StatefulStyle, MuisStyle {
	/** @return The current internal state of this style */
	MuisState [] getState();

	/** @return This style's states */
	ObservableValue<Set<MuisState>> states();
}
