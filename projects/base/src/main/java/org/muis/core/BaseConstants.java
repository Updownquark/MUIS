package org.muis.core;

import org.muis.core.mgr.MuisState;

/** Contains constants (inside their own categorical classes) used by MUIS base */
public class BaseConstants {
	/** Contains several {@link org.muis.core.mgr.StateEngine states} that are used by MUIS base */
	public static class States {
		/** The name of the state that is true whenever a button is regarded as pressed */
		public static final String DEPRESSED_NAME = "depressed";

		/** The priority of the depressed state */
		public static final int DEPRESSED_PRIORITY = 200;

		/** True whenever a button is pressed down */
		public static final MuisState DEPRESSED = new MuisState(DEPRESSED_NAME, DEPRESSED_PRIORITY);
	}
}
