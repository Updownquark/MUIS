package org.quick.core;

import org.quick.core.mgr.AttributeManager;
import org.quick.core.mgr.QuickState;

/** Contains constants (inside their own categorical classes) used by the Quick core */
public final class QuickConstants {
	/** The stages of Quick element creation recognized by the Quick core, except for {@link #OTHER} */
	public static enum CoreStage {
		/**
		 * The element is being constructed without any knowledge of its document or other context. During this stage, internal variables
		 * should be created and initialized with default values. Initial listeners may be added to the element at this time as well.
		 */
		CREATION,
		/** The element is being populated with the attributes from its XML. This is a transition time where no work is done by the core. */
		PARSE_SELF,
		/**
		 * The {@link QuickElement#init(QuickDocument, QuickToolkit, QuickClassView, QuickElement, String, String)} method is
		 * initializing this element's context.
		 */
		INIT_SELF,
		/** The children of this element as configured in XML are being parsed. This is a transition time where no work is done by the core. */
		PARSE_CHILDREN,
		/** The {@link QuickElement#initChildren(java.util.List)} method is populating this element with its contents */
		INIT_CHILDREN,
		/**
		 * This element is fully initialized, but the rest of the document may be loading. This is a transition time where no work is done
		 * by the core.
		 */
		INITIALIZED,
		/**
		 * The {@link QuickElement#postCreate()} method is performing context-sensitive initialization work. The core performs attribute
		 * checks during this time. Before this stage, attributes may be added which are not recognized by the element. During this stage,
		 * all unchecked attributes are checked and errors are logged for any attributes that are unrecognized or malformatted as well as
		 * for any required attributes whose values have not been set. During and after this stage, no attributes may be set in the element
		 * unless they have been {@link AttributeManager#accept(Object, org.quick.core.prop.QuickAttribute) accepted} and the type is
		 * correct. An element's children are started up at the tail end of this stage, so note that when an element transitions out of this
		 * stage, its contents will be in the {@link #READY} stage, but its parent will still be in the {@link #STARTUP} stage, though all
		 * its attribute work has completed.
		 */
		STARTUP,
		/** The element has been fully initialized within the full document context and is ready to render and receive events */
		READY,
		/** Represents any stage that the core does not know about */
		OTHER;

		/**
		 * @param name The name of the stage to get the enum value for
		 * @return The enum value of the named stage, unless the stage is not recognized by the Quick core, in which case {@link #OTHER} is
		 *         returned
		 */
		public static CoreStage get(String name) {
			for(CoreStage stage : values())
				if(stage != OTHER && stage.toString().equals(name))
					return stage;
			return OTHER;
		}
	}

	/** Contains several {@link org.quick.core.mgr.StateEngine states} that are used by the Quick core */
	public static final class States {
		/** The name of the state that is true whenever the left mouse button is pressed on top of an element */
		public static final String CLICK_NAME = "click";

		/** The name of the state that is true whenever the right mouse button is pressed on top of an element */
		public static final String RIGHT_CLICK_NAME = "right-click";

		/** The name of the state that is true whenever the middle mouse button is pressed on top of an element */
		public static final String MIDDLE_CLICK_NAME = "middle-click";

		/** The name of the state that is true whenever the cursor is over an element */
		public static final String HOVER_NAME = "hover";

		/** The name of the state that is true when an element has the focus */
		public static final String FOCUS_NAME = "focus";

		/** The name of the state that is true for text when it is selected */
		public static final String TEXT_SELECTION_NAME = "text-select";

		/** The priority of the click state */
		public static final int CLICK_PRIORITY = 100;

		/** The priority of the right-click state */
		public static final int RIGHT_CLICK_PRIORITY = 100;

		/** The priority of the middle-click state */
		public static final int MIDDLE_CLICK_PRIORITY = 100;

		/** The priority of the hover state */
		public static final int HOVER_PRIORITY = 10;

		/** The priority of the focus state */
		public static final int FOCUS_PRIORITY = 2;

		/** The priority of the text selection state */
		public static final int TEXT_SELECTION_PRIORITY = 1000;

		/** True whenever the left mouse button is pressed on top of an element */
		public static final QuickState CLICK = new QuickState(CLICK_NAME, CLICK_PRIORITY);

		/** True whenever the right mouse button is pressed on top of an element */
		public static final QuickState RIGHT_CLICK = new QuickState(RIGHT_CLICK_NAME, RIGHT_CLICK_PRIORITY);

		/** True whenever the middle mouse button is pressed on top of an element */
		public static final QuickState MIDDLE_CLICK = new QuickState(MIDDLE_CLICK_NAME, MIDDLE_CLICK_PRIORITY);

		/** True whenever the cursor is over an element */
		public static final QuickState HOVER = new QuickState(HOVER_NAME, HOVER_PRIORITY);

		/** True when an element has the focus */
		public static final QuickState FOCUS = new QuickState(FOCUS_NAME, FOCUS_PRIORITY);

		/** True for text that is selected */
		public static final QuickState TEXT_SELECTION = new QuickState(TEXT_SELECTION_NAME, TEXT_SELECTION_PRIORITY);
	}
}
