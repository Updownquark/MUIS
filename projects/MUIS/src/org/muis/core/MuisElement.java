/* Created Feb 23, 2009 by Andrew Butler */
package org.muis.core;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import org.muis.core.event.MuisEvent;
import org.muis.core.event.MuisEventListener;
import org.muis.core.event.MuisEventType;
import org.muis.core.event.MuisPropertyEvent;
import org.muis.core.layout.SimpleSizePolicy;
import org.muis.core.layout.SizePolicy;
import org.muis.core.mgr.*;
import org.muis.core.mgr.MuisLifeCycleManager.Controller;
import org.muis.core.style.*;

import prisms.arch.event.ListenerManager;
import prisms.util.ArrayUtils;

/** The base display element in MUIS. Contains base methods to administer content (children, style, placement, etc.) */
public abstract class MuisElement implements org.muis.core.layout.Sizeable {
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

	// TODO Add code for attach events

	/** The event type representing a mouse event */
	public static final MuisEventType<Void> MOUSE_EVENT = new MuisEventType<Void>("Mouse Event", null);

	/** The event type representing a scrolling action */
	public static final MuisEventType<Void> SCROLL_EVENT = new MuisEventType<Void>("Scroll Event", null);

	/** The event type representing a physical keyboard event */
	public static final MuisEventType<Void> KEYBOARD_EVENT = new MuisEventType<Void>("Keyboard Event", null);

	/**
	 * The event type representing the input of a character. This is distinct from a keyboard event in several situations. For example:
	 * <ul>
	 * <li>The user presses and holds a character key, resulting in a character input, a pause, and then many sequential character inputs
	 * until the user releases the key.</li>
	 * <li>The user copies text and pastes it into a text box. This may result in one or two keyboard events or even mouse events
	 * (menu->Paste) followed by a character event with the pasted text.</li>
	 * </ul>
	 */
	public static final MuisEventType<Void> CHARACTER_INPUT = new MuisEventType<Void>("Character Input", null);

	/** The event type representing focus change on an element */
	public static final MuisEventType<Void> FOCUS_EVENT = new MuisEventType<Void>("Focus Event", null);

	/** The event type representing the relocation of this element within its parent */
	public static final MuisEventType<Rectangle> BOUNDS_CHANGED = new MuisEventType<Rectangle>("Bounds Changed", null);

	/** The event type representing the successful setting of an attribute */
	public static final MuisEventType<MuisAttribute<?>> ATTRIBUTE_SET = new MuisEventType<MuisAttribute<?>>("Attribute Set",
		(Class<MuisAttribute<?>>) (Class<?>) MuisAttribute.class);

	/**
	 * The event type representing the successful setting of an attribute. Different from {@link #ATTRIBUTE_SET} in that the value of the
	 * event is the value of the attribute instead of the attribute itself.
	 */
	public static final MuisEventType<Object> ATTRIBUTE_CHANGED = new MuisEventType<Object>("Attribute Changed", Object.class);

	/** The event type representing the change of an element's stage property */
	public static final MuisEventType<CoreStage> STAGE_CHANGED = new MuisEventType<CoreStage>("Stage Changed", CoreStage.class);

	/**
	 * The event type representing the event when an element is moved from one parent element to another. The event property is the new
	 * parent element. This method is NOT called from {@link #init(MuisDocument, MuisToolkit, MuisClassView, MuisElement, String, String)}
	 */
	public static final MuisEventType<MuisElement> ELEMENT_MOVED = new MuisEventType<MuisElement>("Element Moved", MuisElement.class);

	/**
	 * The event type representing the event when a child is added to an element. The event property is the child that was added. This
	 * method is NOT called from {@link #initChildren(MuisElement[])}.
	 */
	public static final MuisEventType<MuisElement> CHILD_ADDED = new MuisEventType<MuisElement>("Child Added", MuisElement.class);

	/**
	 * The event type representing the event when a child is removed from an element. The event property is the child that was removed. This
	 * method is NOT called from {@link #initChildren(MuisElement[])}.
	 */
	public static final MuisEventType<MuisElement> CHILD_REMOVED = new MuisEventType<MuisElement>("Child Removed", MuisElement.class);

	/** The event type representing the addition of a message to an element. */
	public static final MuisEventType<MuisMessage> MESSAGE_ADDED = new MuisEventType<MuisMessage>("Message Added", MuisMessage.class);

	/** The event type representing the removal of a message from an element. */
	public static final MuisEventType<MuisMessage> MESSAGE_REMOVED = new MuisEventType<MuisMessage>("Message Removed", MuisMessage.class);

	/**
	 * Used to lock this elements' child sets
	 *
	 * @see MuisLock
	 */
	public static final String CHILDREN_LOCK_TYPE = "Muis Child Lock";

	private final MuisLifeCycleManager theLifeCycleManager;

	private MuisLifeCycleManager.Controller theLifeCycleController;

	private final MuisMessageCenter theMessageCenter;

	private MuisDocument theDocument;

	private MuisToolkit theToolkit;

	private MuisElement theParent;

	private MuisClassView theClassView;

	private String theNamespace;

	private String theTagName;

	private AttributeManager theAttributeManager;

	private ChildList theChildren;

	private final ElementStyle theStyle;

	private final StyleListener theDefaultStyleListener;

	private int theX;

	private int theY;

	private int theZ;

	private int theW;

	private int theH;

	private SizePolicy theHSizer;

	private SizePolicy theVSizer;

	@SuppressWarnings({"rawtypes"})
	private ListenerManager<MuisEventListener> theListeners;

	private boolean isFocusable;

	private Rectangle theCacheBounds;

	private long thePaintDirtyTime;

	private long theLayoutDirtyTime;

	/** Creates a MUIS element */
	public MuisElement() {
		theLifeCycleManager = new MuisLifeCycleManager(this, new MuisLifeCycleManager.ControlAcceptor() {
			@Override
			public void setController(Controller controller) {
				theLifeCycleController = controller;
			}
		});
		theMessageCenter = new MuisMessageCenter(theDocument, this);
		String lastStage = null;
		for(CoreStage stage : CoreStage.values())
			if(stage != CoreStage.OTHER) {
				theLifeCycleManager.addStage(stage.toString(), lastStage);
				lastStage = stage.toString();
			}
		theChildren = new ChildList(this);
		theAttributeManager = new AttributeManager(this);
		theListeners = new ListenerManager<>(MuisEventListener.class);
		theCacheBounds = new Rectangle();
		theStyle = new ElementStyle(this);
		theDefaultStyleListener = new StyleListener(this) {
			@Override
			public void styleChanged(MuisStyle style) {
				repaint(null, false);
			}
		};
		theDefaultStyleListener.addDomain(BackgroundStyles.getDomainInstance());
		theDefaultStyleListener.add();
		MuisEventListener<MuisElement> childListener = new MuisEventListener<MuisElement>() {
			@Override
			public void eventOccurred(MuisEvent<MuisElement> event, MuisElement element) {
				relayout(false);
				if(event.getType() == CHILD_REMOVED) {
					// Need to repaint where the element left even if nothing changes as a result of the layout
					unregisterChild(event.getValue());
					repaint(new Rectangle(element.getX(), element.getY(), element.getWidth(), element.getHeight()), false);
				} else if(event.getType() == CHILD_ADDED) {
					registerChild(event.getValue());
				}
			}

			@Override
			public boolean isLocal() {
				return true;
			}
		};
		addListener(CHILD_ADDED, childListener);
		addListener(CHILD_REMOVED, childListener);
		theChildren.addChildListener(BOUNDS_CHANGED, new MuisEventListener<Rectangle>() {
			@Override
			public void eventOccurred(MuisEvent<Rectangle> event, MuisElement element) {
				Rectangle paintRect = event.getValue().union(((MuisPropertyEvent<Rectangle>) event).getOldValue());
				repaint(paintRect, false);
			}

			@Override
			public boolean isLocal() {
				return true;
			}
		});
		Object styleWanter = new Object();
		theAttributeManager.accept(styleWanter, StyleAttributeType.STYLE_ATTRIBUTE);
		theAttributeManager.accept(styleWanter, StyleAttributeType.STYLE_SELF_ATTRIBUTE);
		theAttributeManager.accept(styleWanter, StyleAttributeType.STYLE_HEIR_ATTRIBUTE);
		addListener(BOUNDS_CHANGED, new MuisEventListener<Rectangle>() {
			@Override
			public void eventOccurred(MuisEvent<Rectangle> event, MuisElement element) {
				Rectangle old = ((MuisPropertyEvent<Rectangle>) event).getOldValue();
				if(event.getValue().width != old.width || event.getValue().height != old.height)
					relayout(false);
			}

			@Override
			public boolean isLocal() {
				return true;
			}
		});
		theLifeCycleManager.addListener(new LifeCycleListener() {
			@Override
			public void preTransition(String fromStage, String toStage) {
			}

			@Override
			public void postTransition(String oldStage, String newStage) {
				if(oldStage.equals(CoreStage.INIT_SELF.toString()))
					repaint(null, false);
			}
		});
		theLifeCycleController.advance(CoreStage.PARSE_SELF.toString());
	}

	/** @return The document that this element belongs to */
	public final MuisDocument getDocument() {
		return theDocument;
	}

	/** @return The tool kit that this element belongs to */
	public final MuisToolkit getToolkit() {
		return theToolkit;
	}

	/** @return The MUIS class view that allows for instantiation of child elements */
	public final MuisClassView getClassView() {
		return theClassView;
	}

	/** @return The namespace that this tag was instantiated in */
	public final String getNamespace() {
		return theNamespace;
	}

	/** @return The name of the tag that was used to instantiate this element */
	public final String getTagName() {
		return theTagName;
	}

	/** @return The manager of this element's attributes */
	public AttributeManager getAttributeManager() {
		return theAttributeManager;
	}

	/**
	 * Short-hand for {@link #getAttributeManager()}
	 *
	 * @return The manager of this element's attributes
	 */
	public AttributeManager atts() {
		return getAttributeManager();
	}

	/** @return The style that modifies this element's appearance */
	public final ElementStyle getStyle() {
		return theStyle;
	}

	// Life cycle methods

	/**
	 * Returns a life cycle manager that allows subclasses to customize and hook into the life cycle for this element.
	 *
	 * @return The life cycle manager for this element
	 */
	public MuisLifeCycleManager getLifeCycleManager() {
		return theLifeCycleManager;
	}

	/**
	 * Short-hand for {@link #getLifeCycleManager()}
	 *
	 * @return The life cycle manager for this element
	 */
	public MuisLifeCycleManager life() {
		return getLifeCycleManager();
	}

	/**
	 * Initializes an element's core information
	 *
	 * @param doc The document that this element belongs to
	 * @param toolkit The toolkit that this element belongs to
	 * @param classView The class view for this element
	 * @param parent The parent that this element is under
	 * @param namespace The namespace used to create this element
	 * @param tagName The tag name used to create this element
	 */
	public final void init(MuisDocument doc, MuisToolkit toolkit, MuisClassView classView, MuisElement parent, String namespace,
		String tagName) {
		theLifeCycleController.advance(CoreStage.INIT_SELF.toString());
		if(doc == null)
			throw new IllegalArgumentException("Cannot create an element without a document");
		if(theDocument != null)
			throw new IllegalStateException("An element cannot be initialized twice", null);
		theDocument = doc;
		theToolkit = toolkit;
		theParent = parent;
		theNamespace = namespace;
		theTagName = tagName;
		theClassView = classView;
		theLifeCycleController.advance(CoreStage.PARSE_CHILDREN.toString());
	}

	/**
	 * Initializes an element's descendants
	 *
	 * @param children The child elements specified in the MUIS XML
	 */
	public void initChildren(MuisElement [] children) {
		theLifeCycleController.advance(CoreStage.INIT_CHILDREN.toString());
		try (MuisLock lock = theDocument.getLocker().lock(CHILDREN_LOCK_TYPE, this, true)) {
			theChildren.clear();
			theChildren.addAll(children);
		}
		for(MuisElement child : children)
			registerChild(child);
		if(theW != 0 && theH != 0) // No point laying out if there's nothing to show
			relayout(false);
		theLifeCycleController.advance(CoreStage.INITIALIZED.toString());
	}

	/**
	 * Called when a child is introduced to this parent
	 *
	 * @param child The child that has been added to this parent
	 */
	protected void registerChild(MuisElement child) {
		if(child.getParent() != this)
			child.setParent(this);
	}

	/**
	 * Called when a child is removed to this parent
	 *
	 * @param child The child that has been removed from this parent
	 */
	protected void unregisterChild(MuisElement child) {
		if(child.getParent() == this)
			child.setParent(null);
	}

	/** Called to initialize an element after all the parsing and linking has been performed */
	public final void postCreate() {
		theLifeCycleController.advance(CoreStage.STARTUP.toString());
		for(MuisElement child : theChildren)
			child.postCreate();
		theLifeCycleController.advance(CoreStage.READY.toString());
	}

	// End life cycle methods

	// Hierarchy methods

	/** @return This element's parent in the DOM tree */
	public final MuisElement getParent() {
		return theParent;
	}

	/**
	 * Sets this element's parent after initialization
	 *
	 * @param parent The new parent for this element
	 */
	protected final void setParent(MuisElement parent) {
		if(theParent != null) {
			theParent.theChildren.remove(this);
		}
		theParent = parent;
		fireEvent(new MuisEvent<MuisElement>(ELEMENT_MOVED, theParent), false, false);
	}

	/** @return An augmented {@link java.util.List list} of this element's children */
	public ChildList getChildren() {
		return theChildren;
	}

	/**
	 * Short-hand for {@link #getChildren()}
	 *
	 * @return An augmented {@link java.util.List list} of this element's children
	 */
	public ChildList ch() {
		return getChildren();
	}

	/**
	 * Checks to see if this element is in the subtree rooted at the given element
	 *
	 * @param ancestor The element whose subtree to check
	 * @return Whether this element is in the ancestor's subtree
	 */
	public final boolean isAncestor(MuisElement ancestor) {
		if(ancestor == this)
			return true;
		MuisElement parent = theParent;
		while(parent != null) {
			if(parent == ancestor)
				return true;
			parent = parent.theParent;
		}
		return false;
	}

	/**
	 * @param x The x-coordinate of a point relative to this element's upper left corner
	 * @param y The y-coordinate of a point relative to this element's upper left corner
	 * @return The deepest (and largest-Z) descendant of this element whose bounds contain the given point
	 */
	public final MuisElement deepestChildAt(int x, int y) {
		MuisElement current = this;
		MuisElement [] children = current.theChildren.at(x, y);
		while(children.length > 0) {
			x -= current.theX;
			y -= current.theY;
			current = children[0];
			children = current.theChildren.at(x, y);
		}
		return current;
	}

	// End hierarchy methods

	/**
	 * @return The default style listener to add domains and styles to listen to. When one of the registered styles changes, this element
	 *         repaints itself.
	 */
	public final StyleListener getDefaultStyleListener() {
		return theDefaultStyleListener;
	}

	// Bounds methods

	/** @return The x-coordinate of this element's upper left corner */
	public final int getX() {
		return theX;
	}

	/** @param x The x-coordinate for this element's upper left corner */
	public final void setX(int x) {
		if(theX == x)
			return;
		Rectangle preBounds = new Rectangle(theX, theY, theW, theH);
		theX = x;
		fireEvent(new MuisPropertyEvent<Rectangle>(BOUNDS_CHANGED, preBounds, new Rectangle(theX, theY, theW, theH)), false, false);
	}

	/** @return The y-coordinate of this element's upper left corner */
	public final int getY() {
		return theY;
	}

	/** @param y The y-coordinate for this element's upper left corner */
	public final void setY(int y) {
		if(theY == y)
			return;
		Rectangle preBounds = new Rectangle(theX, theY, theW, theH);
		theY = y;
		fireEvent(new MuisPropertyEvent<Rectangle>(BOUNDS_CHANGED, preBounds, new Rectangle(theX, theY, theW, theH)), false, false);
	}

	/**
	 * @param x The x-coordinate for this element's upper left corner
	 * @param y The y-coordinate for this element's upper left corner
	 */
	public final void setPosition(int x, int y) {
		if(theX == x && theY == y)
			return;
		Rectangle preBounds = new Rectangle(theX, theY, theW, theH);
		theX = x;
		theY = y;
		fireEvent(new MuisPropertyEvent<Rectangle>(BOUNDS_CHANGED, preBounds, new Rectangle(theX, theY, theW, theH)), false, false);
	}

	/** @return The z-index determining the order in which this element is drawn among its siblings */
	public final int getZ() {
		return theZ;
	}

	/** @param z The z-index determining the order in which this element is drawn among its siblings */
	public final void setZ(int z) {
		if(theZ == z)
			return;
		theZ = z;
		if(theParent != null)
			theParent.repaint(new Rectangle(theX, theY, theW, theH), false);
	}

	/** @return The width of this element */
	public final int getWidth() {
		return theW;
	}

	/** @param width The width for this element */
	public final void setWidth(int width) {
		if(theW == width)
			return;
		Rectangle preBounds = new Rectangle(theX, theY, theW, theH);
		theW = width;
		fireEvent(new MuisPropertyEvent<Rectangle>(BOUNDS_CHANGED, preBounds, new Rectangle(theX, theY, theW, theH)), false, false);
	}

	/** @return The height of this element */
	public final int getHeight() {
		return theH;
	}

	/** @param height The height for this element */
	public final void setHeight(int height) {
		if(theH == height)
			return;
		Rectangle preBounds = new Rectangle(theX, theY, theW, theH);
		theH = height;
		fireEvent(new MuisPropertyEvent<Rectangle>(BOUNDS_CHANGED, preBounds, new Rectangle(theX, theY, theW, theH)), false, false);
	}

	/**
	 * @param width The width for this element
	 * @param height The height for this element
	 */
	public final void setSize(int width, int height) {
		if(theW == width && theH == height)
			return;
		Rectangle preBounds = new Rectangle(theX, theY, theW, theH);
		theW = width;
		theH = height;
		fireEvent(new MuisPropertyEvent<Rectangle>(BOUNDS_CHANGED, preBounds, new Rectangle(theX, theY, theW, theH)), false, false);
	}

	/**
	 * @param x The x-coordinate for this element's upper left corner
	 * @param y The y-coordinate for this element's upper left corner
	 * @param width The width for this element
	 * @param height The height for this element
	 */
	public final void setBounds(int x, int y, int width, int height) {
		if(theX == x && theY == y && theW == width && theH == height)
			return;
		Rectangle preBounds = new Rectangle(theX, theY, theW, theH);
		theX = x;
		theY = y;
		theW = width;
		theH = height;
		fireEvent(new MuisPropertyEvent<Rectangle>(BOUNDS_CHANGED, preBounds, new Rectangle(theX, theY, theW, theH)), false, false);
	}

	@Override
	public SizePolicy getWSizer(int height) {
		if(theHSizer == null)
			theHSizer = new SimpleSizePolicy();
		return theHSizer;
	}

	@Override
	public SizePolicy getHSizer(int width) {
		if(theVSizer == null)
			theVSizer = new SimpleSizePolicy();
		return theVSizer;
	}

	/** @return This element's position relative to the document's root */
	public final Point getDocumentPosition() {
		int x = 0;
		int y = 0;
		MuisElement el = this;
		while(el.theParent != null) {
			x += el.theX;
			y += el.theY;
			el = el.theParent;
		}
		return new Point(x, y);
	}

	// End bounds methods

	/**
	 * @return Whether positional events are consumed by this element, or whether they should be propagated to elements under this element.
	 *         By default, this method returns true if and only if the background transparency is one.
	 */
	public boolean isClickThrough() {
		return getStyle().get(BackgroundStyles.transparency) >= 1;
	}

	/** @return Whether this element is able to accept the focus for the document */
	public boolean isFocusable() {
		return isFocusable;
	}

	/** @param focusable Whether this element should be focusable */
	protected final void setFocusable(boolean focusable) {
		isFocusable = focusable;
	}

	// Event methods

	/**
	 * Adds a listener for an event type to this element
	 *
	 * @param <T> The type of the property that the event type represents
	 * @param type The event type to listen for
	 * @param listener The listener to notify when an event of the given type occurs
	 */
	public final <T> void addListener(MuisEventType<T> type, MuisEventListener<? super T> listener) {
		theListeners.addListener(type, listener);
	}

	/** @param listener The listener to remove from this element */
	public final void removeListener(MuisEventListener<?> listener) {
		theListeners.removeListener(listener);
	}

	/**
	 * Removes all listeners for the given event type whose class is exactly equal (not an extension of) the given listener type
	 *
	 * @param <T> The type of the property that the event type represents
	 * @param type The type of event to stop listening for
	 * @param listenerType The listener type to remove
	 */
	public final <T> void removeListener(MuisEventType<T> type, Class<? extends MuisEventListener<? super T>> listenerType) {
		for(MuisEventListener<?> listener : theListeners.getListeners(type))
			if(listener.getClass() == listenerType)
				theListeners.removeListener(listener);
	}

	/**
	 * Fires an event on this element
	 *
	 * @param <T> The type of the event's property
	 * @param event The event to fire
	 * @param fromDescendant Whether the event was fired on one of this element's descendants or on this element specifically
	 * @param toAncestors Whether the event should be fired on this element's ancestors as well
	 */
	public final <T> void fireEvent(MuisEvent<T> event, boolean fromDescendant, boolean toAncestors) {
		MuisEventListener<T> [] listeners = theListeners.getListeners(event.getType());
		for(MuisEventListener<T> listener : listeners)
			if(!fromDescendant || !listener.isLocal())
				listener.eventOccurred(event, this);
		if(toAncestors && theParent != null)
			theParent.fireEvent(event, true, true);
	}

	/**
	 * Fires a user-generated event on this element, propagating up toward the document root unless canceled
	 *
	 * @param event The event to fire
	 */
	public final void fireUserEvent(org.muis.core.event.UserEvent event) {
		MuisEventListener<Void> [] listeners = theListeners.getListeners(event.getType());
		for(MuisEventListener<Void> listener : listeners)
			if(event.getElement() == this || !listener.isLocal())
				listener.eventOccurred(event, this);
		if(!event.isCanceled() && theParent != null)
			theParent.fireEvent(event, true, true);
	}

	/**
	 * Fires appropriate listeners on this element's subtree for a positioned event which occurred within this element's bounds
	 *
	 * @param event The event that occurred
	 * @param x The x-coordinate of the position at which the event occurred, relative to this element's upper-left corner
	 * @param y The y-coordinate of the position at which the event occurred, relative to this element's upper-left corner
	 */
	protected final void firePositionEvent(org.muis.core.event.PositionedUserEvent event, int x, int y) {
		MuisElement [] children = theChildren.at(x, y);
		for(int c = children.length - 1; c >= 0; c--) {
			if(event.isCanceled())
				continue;
			MuisElement child = children[c];
			int relX = x - child.theX;
			int relY = y - child.theY;
			child.firePositionEvent(event, relX, relY);
		}
		if(!event.isCanceled())
			fireEvent(event, event.getElement() == this, false);
	}

	// End event methods

	// Messaging methods

	/**
	 * Returns a message center that allows messaging on this element
	 *
	 * @return This element's message center
	 */
	public MuisMessageCenter getMessageCenter() {
		return theMessageCenter;
	}

	/**
	 * Short-hand for {@link #getMessageCenter()}
	 *
	 * @return This element's message center
	 */
	public MuisMessageCenter msg() {
		return getMessageCenter();
	}

	/** @return The worst type of messages in this element and its children */
	public MuisMessage.Type getWorstMessageType() {
		MuisMessage.Type ret = theMessageCenter.getWorstMessageType();
		for(MuisElement child : theChildren) {
			MuisMessage.Type childType = child.theMessageCenter.getWorstMessageType();
			if(ret == null || ret.compareTo(childType) < 0)
				ret = childType;
		}
		return ret;
	}

	/** @return All messages in this element or any of its children */
	public Iterable<MuisMessage> allMessages() {
		java.util.ArrayList<MuisMessageCenter> centers = new java.util.ArrayList<>();
		centers.add(theMessageCenter);
		for(MuisElement child : theChildren)
			centers.add(child.msg());
		return ArrayUtils.iterable(centers.toArray(new MuisMessageCenter[centers.size()]));
	}

	// End messaging methods

	/**
	 * Generates an XML-representation of this element's content
	 *
	 * @param indent The indention string to use for each level away from the margin
	 * @return The XML string representing this element
	 */
	public final String asXML(String indent) {
		StringBuilder ret = new StringBuilder();
		appendXML(ret, indent, 0);
		return ret.toString();
	}

	/**
	 * Appends this element's XML-representation to a string builder
	 *
	 * @param str The string builder to append to
	 * @param indent The indention string to use for each level away from the margin
	 * @param level The depth of this element in the structure being printed
	 */
	protected final void appendXML(StringBuilder str, String indent, int level) {
		for(int L = 0; L < level; L++)
			str.append(indent);
		str.append('<');
		if(theNamespace != null) {
			str.append(theNamespace);
			str.append(':');
		}
		str.append(theTagName);
		for(AttributeManager.AttributeHolder holder : theAttributeManager.holders()) {
			str.append(' ');
			str.append(holder.getAttribute().name);
			str.append('=');
			str.append('"');
			str.append(holder.getValue());
			str.append('"');
		}
		if(theChildren.isEmpty()) {
			str.append('/');
			str.append('>');
			return;
		}
		str.append('>');
		for(MuisElement child : theChildren) {
			str.append('\n');
			child.appendXML(str, indent, level + 1);
		}
		str.append('\n');
		for(int L = 0; L < level; L++)
			str.append(indent);
		str.append('<');
		str.append('/');
		if(theNamespace != null) {
			str.append(theNamespace);
			str.append(':');
		}
		str.append(theTagName);
		str.append('>');
	}

	// Layout methods

	/**
	 * Causes this element to adjust the position and size of its children in a way defined in this element type's implementation. By
	 * default this does nothing.
	 */
	public void doLayout() {
		theLayoutDirtyTime = 0;
		for(MuisElement child : getChildren())
			child.doLayout();
		repaint(null, false);
	}

	/**
	 * Causes a call to {@link #doLayout()}
	 *
	 * @param now Whether to perform the layout action now or allow it to be performed asynchronously
	 */
	public final void relayout(boolean now) {
		if(theW <= 0 || theH <= 0)
			return; // No point layout out if there's nothing to show
		theLayoutDirtyTime = System.currentTimeMillis();
		MuisEventQueue.get().scheduleEvent(new MuisEventQueue.LayoutEvent(this, now), now);
	}

	/** @return The last time a layout event was scheduled for this element */
	public final long getLayoutDirtyTime() {
		return theLayoutDirtyTime;
	}

	// End layout methods

	// Paint methods

	/** @return The graphics object to use to draw this element */
	public Graphics2D getGraphics() {
		int x = 0, y = 0;
		MuisElement el = this;
		while(el.theParent != null) {
			x += getX();
			y += getY();
			el = el.theParent;
		}
		java.awt.Graphics2D graphics = theDocument.getGraphics();
		if(el != theDocument.getRoot()) {
			graphics = (Graphics2D) graphics.create(x, y, 0, 0);
			return graphics;
		}
		graphics = (Graphics2D) graphics.create(x, y, theW, theH);
		return graphics;
	}

	/**
	 * @return The bounds within which this element may draw and receive events, relative to the layout x,y position. This may extend
	 *         outside the element's layout bounds (e.g. for a menu, which expands, but does not cause a relayout when it does so).
	 */
	public Rectangle getPaintBounds() {
		return new Rectangle(0, 0, theW, theH);
	}

	/**
	 * Renders this element in a graphics context.
	 *
	 * @param graphics The graphics context to render this element in
	 * @param area The area to draw
	 */
	public final void paint(java.awt.Graphics2D graphics, Rectangle area) {
		if((area != null && (area.width == 0 || area.height == 0)) || theW == 0 || theH == 0)
			return;
		Rectangle paintBounds = getPaintBounds();
		theCacheBounds.setBounds(paintBounds);
		theCacheBounds.x += theX;
		theCacheBounds.y += theY;
		Rectangle preClip = graphics.getClipBounds();
		try {
			graphics.setClip(paintBounds.x, paintBounds.y, paintBounds.width, paintBounds.height);
			paintSelf(graphics, area);
			paintChildren(graphics, area);
		} finally {
			graphics.setClip(preClip);
		}
	}

	/**
	 * Causes this element to be repainted.
	 *
	 * @param area The area in this element that needs to be repainted. May be null to specify that the entire element needs to be redrawn.
	 * @param now Whether this element should be repainted immediately or not. This parameter should usually be false when this is called as
	 *            a result of a user operation such as a mouse or keyboard event because this allows all necessary paint events to be
	 *            performed at one time with no duplication after the event is finished. This parameter should be true if this is called
	 *            from an independent thread.
	 */
	public final void repaint(Rectangle area, boolean now) {
		if(theW <= 0 || theH <= 0)
			return; // No point painting if there's nothing to show
		thePaintDirtyTime = System.currentTimeMillis();
		MuisEventQueue.get().scheduleEvent(new MuisEventQueue.PaintEvent(this, area, now), now);
	}

	/**
	 * Renders this element's background or its content, but NOT its children. Children are rendered by
	 * {@link #paintChildren(java.awt.Graphics2D, Rectangle)}. By default, this merely draws the element's background color.
	 *
	 * @param graphics The graphics context to draw in
	 * @param area The area to paint
	 */
	public void paintSelf(java.awt.Graphics2D graphics, Rectangle area) {
		Texture tex = getStyle().get(BackgroundStyles.texture);
		if(tex != null)
			tex.render(graphics, this, area);
	}

	/**
	 * Draws this element's children
	 *
	 * @param graphics The graphics context to render in
	 * @param area The area in this element's coordinates to repaint
	 */
	public final void paintChildren(java.awt.Graphics2D graphics, Rectangle area) {
		MuisElement [] children = ch().sortByZ();
		if(children.length == 0)
			return;
		if(area == null)
			area = new Rectangle(theX, theY, theW, theH);
		int translateX = 0;
		int translateY = 0;
		try {
			Rectangle childArea = new Rectangle();
			for(MuisElement child : children) {
				int childX = child.theX;
				int childY = child.theY;
				translateX += childX;
				translateY += childY;
				childArea.x = area.x - translateX;
				childArea.y = area.y - translateY;
				if(childArea.x < 0)
					childArea.x = 0;
				if(childArea.y < 0)
					childArea.y = 0;
				childArea.width = area.width - childArea.x;
				if(childArea.x + childArea.width > child.getWidth())
					childArea.width = child.getWidth() - childArea.x;
				childArea.height = area.height - childArea.y;
				if(childArea.y + childArea.height > child.getHeight())
					childArea.height = child.getHeight() - childArea.y;
				graphics.translate(translateX, translateY);
				child.paint(graphics, childArea);
				translateX = -childX;
				translateY = -childY;
			}
		} finally {
			if(translateX != 0 || translateY != 0)
				graphics.translate(-translateX, -translateY);
		}
	}

	/** @return The last time a paint event was scheduled for this element */
	public final long getPaintDirtyTime() {
		return thePaintDirtyTime;
	}

	/** @return This element's bounds as of the last time it was painted */
	public final Rectangle getCacheBounds() {
		return theCacheBounds;
	}

	// End paint methods

	@Override
	public final boolean equals(Object o) {
		return super.equals(o);
	}

	@Override
	public final int hashCode() {
		return super.hashCode();
	}
}
