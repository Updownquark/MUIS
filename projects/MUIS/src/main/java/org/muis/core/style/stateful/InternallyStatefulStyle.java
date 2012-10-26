package org.muis.core.style.stateful;

import org.muis.core.style.MuisStyle;

/**
 * Represents a {@link StatefulStyle} that also contains an internal state, meaning it can provide {@link MuisStyle} functionality by
 * evaluating its style expressions against its own state.
 */
public interface InternallyStatefulStyle extends StatefulStyle, MuisStyle {
}
