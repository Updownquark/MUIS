package org.quick.core.style.stateful;

import org.observe.collect.ObservableSet;
import org.quick.core.mgr.QuickState;
import org.quick.core.style.QuickStyle;

/**
 * Represents a {@link StatefulStyle} that also contains an internal state, meaning it can provide {@link QuickStyle} functionality by
 * evaluating its style expressions against its own state.
 */
public interface InternallyStatefulStyle extends StatefulStyle, QuickStyle {
	/** @return The internal state of this style */
	ObservableSet<QuickState> getState();
}
