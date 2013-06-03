package org.muis.core;

import java.awt.Rectangle;

import org.muis.core.event.MuisEventType;
import org.muis.core.mgr.AttributeManager;
import org.muis.core.mgr.MuisMessage;
import org.muis.core.mgr.MuisState;

/** Contains constants (inside their own categorical classes) used by the MUIS core */
public final class MuisConstants {
	/** The stages of MUIS element creation recognized by the MUIS core, except for {@link #OTHER} */
	public static enum CoreStage {
		/**
		 * The element is being constructed without any knowledge of its document or other context. During this stage, internal variables
		 * should be created and initialized with default values. Initial listeners may be added to the element at this time as well.
		 */
		CREATION,
		/** The element is being populated with the attributes from its XML. This is a transition time where no work is done by the core. */
		PARSE_SELF,
		/**
		 * The {@link MuisElement#init(MuisDocument, MuisToolkit, MuisClassView, MuisElement, String, String)} method is initializing this
		 * element's context.
		 */
		INIT_SELF,
		/** The children of this element as configured in XML are being parsed. This is a transition time where no work is done by the core. */
		PARSE_CHILDREN,
		/** The {@link MuisElement#initChildren(MuisElement[])} method is populating this element with its contents */
		INIT_CHILDREN,
		/**
		 * This element is fully initialized, but the rest of the document may be loading. This is a transition time where no work is done
		 * by the core.
		 */
		INITIALIZED,
		/**
		 * The {@link MuisElement#postCreate()} method is performing context-sensitive initialization work. The core performs attribute
		 * checks during this time. Before this stage, attributes may be added which are not recognized by the element. During this stage,
		 * all unchecked attributes are checked and errors are logged for any attributes that are unrecognized or malformatted as well as
		 * for any required attributes whose values have not been set. During and after this stage, no attributes may be set in the element
		 * unless they have been {@link AttributeManager#accept(Object, MuisAttribute) accepted} and the type is correct. An element's
		 * children are started up at the tail end of this stage, so note that when an element transitions out of this stage, its contents
		 * will be in the {@link #READY} stage, but its parent will still be in the {@link #STARTUP} stage, though all its attribute work
		 * has completed.
		 */
		STARTUP,
		/** The element has been fully initialized within the full document context and is ready to render and receive events */
		READY,
		/** Represents any stage that the core does not know about */
		OTHER;

		/**
		 * @param name The name of the stage to get the enum value for
		 * @return The enum value of the named stage, unless the stage is not recognized by the MUIS core, in which case {@link #OTHER} is
		 *         returned
		 */
		public static CoreStage get(String name) {
			for(CoreStage stage : values())
				if(stage != OTHER && stage.toString().equals(name))
					return stage;
			return OTHER;
		}
	}

	/** Contains several event types used by the MUIS core */
	public static final class Events {
		/** The event type representing a mouse event */
		public static final MuisEventType<Void> MOUSE = new MuisEventType<>("Mouse Event", null);

		/** The event type representing a scrolling action */
		public static final MuisEventType<Void> SCROLL = new MuisEventType<>("Scroll Event", null);

		/** The event type representing a physical keyboard event */
		public static final MuisEventType<Void> KEYBOARD = new MuisEventType<>("Keyboard Event", null);

		/**
		 * The event type representing the input of a character. This is distinct from a keyboard event in several situations. For example:
		 * <ul>
		 * <li>The user presses and holds a character key, resulting in a character input, a pause, and then many sequential character
		 * inputs until the user releases the key.</li>
		 * <li>The user copies text and pastes it into a text box. This may result in one or two keyboard events or even mouse events
		 * (menu->Paste) followed by a character event with the pasted text.</li>
		 * </ul>
		 */
		public static final MuisEventType<Void> CHARACTER_INPUT = new MuisEventType<>("Character Input", null);

		/** The event type representing focus change on an element */
		public static final MuisEventType<Void> FOCUS = new MuisEventType<>("Focus Event", null);

		/** The event type representing the relocation of this element within its parent */
		public static final MuisEventType<Rectangle> BOUNDS_CHANGED = new MuisEventType<>("Bounds Changed", null);

		/** The event type representing the successful setting of an attribute */
		public static final MuisEventType<MuisAttribute<?>> ATTRIBUTE_SET = new MuisEventType<>("Attribute Set",
			(Class<MuisAttribute<?>>) (Class<?>) MuisAttribute.class);

		/**
		 * The event type representing the successful setting of an attribute. Different from {@link #ATTRIBUTE_SET} in that the value of
		 * the event is the value of the attribute instead of the attribute itself.
		 */
		public static final MuisEventType<Object> ATTRIBUTE_CHANGED = new MuisEventType<>("Attribute Changed", Object.class);

		/** The event type representing the addition or subtraction of an attribute into an element's accepted set */
		public static final MuisEventType<MuisAttribute<?>> ATTRIBUTE_ACCEPTED = new MuisEventType<>("Attribute Accepted",
			(Class<MuisAttribute<?>>) (Class<?>) MuisAttribute.class);

		/** The event type representing the change of an element's stage property */
		public static final MuisEventType<CoreStage> STAGE_CHANGED = new MuisEventType<>("Stage Changed", CoreStage.class);

		/**
		 * The event type representing the event when an element is moved from one parent element to another. The event property is the new
		 * parent element. This method is NOT called from
		 * {@link MuisElement#init(MuisDocument, MuisToolkit, MuisClassView, MuisElement, String, String)}
		 */
		public static final MuisEventType<MuisElement> ELEMENT_MOVED = new MuisEventType<>("Element Moved", MuisElement.class);

		/**
		 * The event type representing the event when a child is added to an element. The event property is the child that was added. This
		 * method is NOT called from {@link MuisElement#initChildren(MuisElement[])}.
		 */
		public static final MuisEventType<MuisElement> CHILD_ADDED = new MuisEventType<>("Child Added", MuisElement.class);

		/**
		 * The event type representing the event when a child is removed from an element. The event property is the child that was removed.
		 * This method is NOT called from {@link MuisElement#initChildren(MuisElement[])}.
		 */
		public static final MuisEventType<MuisElement> CHILD_REMOVED = new MuisEventType<>("Child Removed", MuisElement.class);

		/** The event type representing the addition of a message to an element. */
		public static final MuisEventType<MuisMessage> MESSAGE_ADDED = new MuisEventType<>("Message Added", MuisMessage.class);

		/** The event type representing the removal of a message from an element. */
		public static final MuisEventType<MuisMessage> MESSAGE_REMOVED = new MuisEventType<>("Message Removed", MuisMessage.class);

	}

	/** Contains several {@link org.muis.core.mgr.StateEngine states} that are used by the MUIS core */
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

		/** True whenever the left mouse button is pressed on top of an element */
		public static final MuisState CLICK = new MuisState(CLICK_NAME, CLICK_PRIORITY);

		/** True whenever the right mouse button is pressed on top of an element */
		public static final MuisState RIGHT_CLICK = new MuisState(RIGHT_CLICK_NAME, RIGHT_CLICK_PRIORITY);

		/** True whenever the middle mouse button is pressed on top of an element */
		public static final MuisState MIDDLE_CLICK = new MuisState(MIDDLE_CLICK_NAME, MIDDLE_CLICK_PRIORITY);

		/** True whenever the cursor is over an element */
		public static final MuisState HOVER = new MuisState(HOVER_NAME, HOVER_PRIORITY);

		/** True when an element has the focus */
		public static final MuisState FOCUS = new MuisState(FOCUS_NAME, FOCUS_PRIORITY);
	}
}
