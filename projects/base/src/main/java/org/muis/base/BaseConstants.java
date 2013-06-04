package org.muis.base;

import org.muis.core.mgr.MuisState;

/** Contains constants (inside their own categorical classes) used by MUIS base */
public class BaseConstants {
	/** Contains several {@link org.muis.core.mgr.StateEngine states} that are used by MUIS base */
	public static class States {
		/** The name of the state that is true whenever a button is regarded as pressed */
		public static final String DEPRESSED_NAME = "depressed";

		/** The name of the state that is true whenever a widget is regarded as selected */
		public static final String SELECTED_NAME = "selected";

		/** The name of the state that is true when user interaction with a widget is permitted */
		public static final String ENABLED_NAME = "enabled";

		/** The priority of the depressed state */
		public static final int DEPRESSED_PRIORITY = 200;

		/** The priority of the selected state */
		public static final int SELECTED_PRIORITY = 300;

		/** The priority of the enabled state */
		public static final int ENABLED_PRIORITY = 400;

		/** True whenever a button is pressed down */
		public static final MuisState DEPRESSED = new MuisState(DEPRESSED_NAME, DEPRESSED_PRIORITY);

		/** True whenever a widget is selected */
		public static final MuisState SELECTED = new MuisState(SELECTED_NAME, SELECTED_PRIORITY);

		/** True whenever user interaction with a widget is permitted */
		public static final MuisState ENABLED = new MuisState(ENABLED_NAME, ENABLED_PRIORITY);
	}
}
