package org.quick.base;

import org.quick.core.mgr.QuickState;

/** Contains constants (inside their own categorical classes) used by MUIS base */
public class BaseConstants {
	/** Contains several {@link org.quick.core.mgr.StateEngine states} that are used by MUIS base */
	public static class States {
		/** The name of the state that is true whenever a button is regarded as pressed */
		public static final String DEPRESSED_NAME = "depressed";

		/** The name of the state that is true whenever a widget is regarded as selected */
		public static final String SELECTED_NAME = "selected";

		/** The name of the state that is true when user interaction with a widget is permitted */
		public static final String ENABLED_NAME = "enabled";

		/** The name of the error state */
		public static final String ERROR_NAME = "error";

		/** The priority of the depressed state */
		public static final int DEPRESSED_PRIORITY = 200;

		/** The priority of the selected state */
		public static final int SELECTED_PRIORITY = 300;

		/** The priority of the enabled state */
		public static final int ENABLED_PRIORITY = 400;

		/** The priority of the error state */
		public static final int ERROR_PRIORITY = 500;

		/** True whenever a button is pressed down */
		public static final QuickState DEPRESSED = new QuickState(DEPRESSED_NAME, DEPRESSED_PRIORITY);

		/** True whenever a widget is selected */
		public static final QuickState SELECTED = new QuickState(SELECTED_NAME, SELECTED_PRIORITY);

		/** True whenever user interaction with a widget is permitted */
		public static final QuickState ENABLED = new QuickState(ENABLED_NAME, ENABLED_PRIORITY);

		/** True whenever the information presented from a widget is invalid */
		public static final QuickState ERROR = new QuickState(ERROR_NAME, ERROR_PRIORITY);
	}
}
