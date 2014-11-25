/* Created Feb 23, 2009 by Andrew Butler */
package org.muis.core;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import org.muis.core.MuisConstants.CoreStage;
import org.muis.core.MuisConstants.States;
import org.muis.core.event.*;
import org.muis.core.layout.SimpleSizeGuide;
import org.muis.core.layout.SizeGuide;
import org.muis.core.mgr.*;
import org.muis.core.mgr.MuisLifeCycleManager.Controller;
import org.muis.core.model.MuisValueReferenceParser;
import org.muis.core.rx.Action;
import org.muis.core.style.BackgroundStyle;
import org.muis.core.style.MuisStyle;
import org.muis.core.style.Texture;
import org.muis.core.style.attach.*;
import org.muis.core.tags.State;
import org.muis.core.tags.StateSupport;

import prisms.util.ArrayUtils;

/** The base display element in MUIS. Contains base methods to administer content (children, style, placement, etc.) */
@StateSupport({@State(name = States.CLICK_NAME, priority = States.CLICK_PRIORITY),
		@State(name = States.RIGHT_CLICK_NAME, priority = States.RIGHT_CLICK_PRIORITY),
		@State(name = States.MIDDLE_CLICK_NAME, priority = States.MIDDLE_CLICK_PRIORITY),
		@State(name = States.HOVER_NAME, priority = States.HOVER_PRIORITY),
		@State(name = States.FOCUS_NAME, priority = States.FOCUS_PRIORITY),
		@State(name = States.TEXT_SELECTION_NAME, priority = States.TEXT_SELECTION_PRIORITY)})
public abstract class MuisElement implements MuisParseEnv {
	/**
	 * Used to lock this elements' child sets
	 *
	 * @see MuisLock
	 */
	public static final String CHILDREN_LOCK_TYPE = "Muis Child Lock";

	private final MuisLifeCycleManager theLifeCycleManager;

	private MuisLifeCycleManager.Controller theLifeCycleController;

	private final StateEngine theStateEngine;

	private final MuisMessageCenter theMessageCenter;

	private final MuisEventManager theEvents;

	private MuisDocument theDocument;

	private MuisToolkit theToolkit;

	private MuisElement theParent;

	private MuisClassView theClassView;

	private MuisValueReferenceParser theModelParser;

	private String theNamespace;

	private String theTagName;

	private final AttributeManager theAttributeManager;

	private final ChildList theChildren;

	private final ImmutableChildList<MuisElement> theExposedChildren;

	private final ElementStyle theStyle;

	private final CompoundStyleListener theDefaultStyleListener;

	private final CoreStateControllers theStateControllers;

	private int theZ;

	private ElementBounds theBounds;

	private SizeGuide theHSizer;

	private SizeGuide theVSizer;

	private boolean isFocusable;

	private long thePaintDirtyTime;

	private long theLayoutDirtyTime;

	/** Creates a MUIS element */
	public MuisElement() {
		theMessageCenter = new MuisMessageCenter(null, null, this);
		theLifeCycleManager = new MuisLifeCycleManager(this, (Controller controller) -> {
			theLifeCycleController = controller;
		}, CoreStage.READY.toString());
		theStateEngine = new StateEngine(this);
		theEvents = new MuisEventManager(this);
		theStateControllers = new CoreStateControllers();
		String lastStage = null;
		for(CoreStage stage : CoreStage.values())
			if(stage != CoreStage.OTHER && stage != CoreStage.READY) {
				theLifeCycleManager.addStage(stage.toString(), lastStage);
				lastStage = stage.toString();
			}
		theBounds = new ElementBounds(this);
		theChildren = new ChildList(this);
		theExposedChildren = new ImmutableChildList<>(theChildren);
		theAttributeManager = new AttributeManager(this);
		theStyle = new ElementStyle(this);
		theDefaultStyleListener = new CompoundStyleListener(this) {
			@Override
			public void styleChanged(MuisStyle style) {
				repaint(null, false);
			}
		};
		theDefaultStyleListener.addDomain(BackgroundStyle.getDomainInstance());
		theDefaultStyleListener.addDomain(org.muis.core.style.LightedStyle.getDomainInstance());
		theDefaultStyleListener.add();
		events().filterMap(ChildEvent.child).act(event -> {
			events().fire(new SizeNeedsChangedEvent(MuisElement.this, null));
			switch (event.getType()) {
			case ADD:
				registerChild(event.getChild());
				break;
			case REMOVE:
				// Need to repaint where the element left even if nothing changes as a result of the layout
				unregisterChild(event.getChild());
				repaint(event.getChild().theBounds.getBounds(), false);
				break;
			case MOVE:
				// The child has changed indexes which may have affected its z-order, so we need to repaint over its bounds.
			repaint(event.getChild().theBounds.getBounds(), false);
				break;
			}
		});
		theChildren.events().filterMap(BoundsChangedEvent.bounds).act(event -> {
			Rectangle paintRect = event.getValue().union(event.getOldValue());
			repaint(paintRect, false);
		});
		theChildren.events().filterMap(SizeNeedsChangedEvent.sizeNeeds).act(new Action<SizeNeedsChangedEvent>() {
			@Override
			public void act(SizeNeedsChangedEvent event) {
				if(!isInPreferred(org.muis.core.layout.Orientation.horizontal) || !isInPreferred(org.muis.core.layout.Orientation.vertical)) {
					SizeNeedsChangedEvent newEvent = new SizeNeedsChangedEvent(MuisElement.this, event);
					events().fire(newEvent);
					if(!newEvent.isHandled()) {
						newEvent.handle();
						relayout(false);
					}
				} else {
					event.handle();
					relayout(false);
				}
			}

			private boolean isInPreferred(org.muis.core.layout.Orientation orient) {
				MuisElement parent = getParent();
				if(parent == null)
					return true;
				org.muis.core.mgr.ElementBounds.ElementBoundsDimension dim = bounds().get(orient);
				int size = dim.getSize();
				int cross = bounds().get(orient.opposite()).getSize();
				int minPref = org.muis.core.layout.LayoutUtils.getSize(MuisElement.this, orient,
					org.muis.core.layout.LayoutGuideType.minPref, parent.bounds().get(orient).getSize(), cross, false, null);
				if(size < minPref)
					return false;
				int maxPref = org.muis.core.layout.LayoutUtils.getSize(MuisElement.this, orient,
					org.muis.core.layout.LayoutGuideType.maxPref, parent.bounds().get(orient).getSize(), cross, false, null);
				if(size > maxPref)
					return false;
				return true;
			}
		});
		Object styleWanter = new Object();
		theAttributeManager.accept(styleWanter, StyleAttributeType.STYLE_ATTRIBUTE);
		events().filterMap(AttributeChangedEvent.att(StyleAttributeType.STYLE_ATTRIBUTE)).act(
			(StylePathAccepter) StyleAttributeType.STYLE_ATTRIBUTE.getPathAccepter());
		final boolean [] groupCallbackLock = new boolean[1];
		theAttributeManager.accept(styleWanter, GroupPropertyType.attribute).act(event -> {
			if(groupCallbackLock[0])
				return;
			groupCallbackLock[0] = true;
			try {
				setGroups(event.getValue());
			} finally {
				groupCallbackLock[0] = false;
			}
		});
		events().filterMap(GroupMemberEvent.groups).act(event -> {
			if(groupCallbackLock[0])
				return;
			groupCallbackLock[0] = true;
			try {
				ArrayList<String> groupList = new ArrayList<>();
				for(TypedStyleGroup<?> group : getStyle().groups(true))
					groupList.add(group.getRoot().getName());
				String [] groups = groupList.toArray(new String[groupList.size()]);
				if(!ArrayUtils.equals(groups, atts().get(GroupPropertyType.attribute)))
					try {
						atts().set(GroupPropertyType.attribute, groups);
					} catch(MuisException e) {
						msg().warn("Error reconciling " + GroupPropertyType.attribute + " attribute with group membership change", e,
							"group", event.getGroup());
					}
			} finally {
				groupCallbackLock[0] = false;
			}
		});
		bounds().act(event -> {
			Rectangle old = event.getOldValue();
			if(old == null || event.getValue().width != old.width || event.getValue().height != old.height)
				relayout(false);
		});
		theLifeCycleManager.runWhen(() -> {
			setGroups(theAttributeManager.get(GroupPropertyType.attribute));
			repaint(null, false);
		}, CoreStage.INIT_SELF.toString(), 2);
		addAnnotatedStates();
		addStateListeners();
		theLifeCycleController.advance(CoreStage.PARSE_SELF.toString());
	}

	private void addAnnotatedStates() {
		Class<?> type = getClass();
		while(MuisElement.class.isAssignableFrom(type)) {
			StateSupport states = type.getAnnotation(StateSupport.class);
			if(states != null)
				for(State state : states.value()) {
					try {
						theStateEngine.addState(new MuisState(state.name(), state.priority()));
					} catch(IllegalArgumentException e) {
						msg().warn(e.getMessage(), "state", state);
					}
				}
			type = type.getSuperclass();
		}
	}

	private void addStateListeners() {
		theStateControllers.clicked = theStateEngine.control(States.CLICK);
		theStateControllers.rightClicked = theStateEngine.control(States.RIGHT_CLICK);
		theStateControllers.middleClicked = theStateEngine.control(States.MIDDLE_CLICK);
		theStateControllers.hovered = theStateEngine.control(States.HOVER);
		theStateControllers.focused = theStateEngine.control(States.FOCUS);
		events().filterMap(MouseEvent.mouse).act(event -> {
			switch (event.getType()) {
			case pressed:
				switch (event.getButton()) {
				case left:
					theStateControllers.clicked.set(true, event);
					break;
				case right:
					theStateControllers.rightClicked.set(true, event);
					break;
				case middle:
					theStateControllers.middleClicked.set(true, event);
					break;
				default:
					break;
				}
				break;
			case released:
				switch (event.getButton()) {
				case left:
					theStateControllers.clicked.set(false, event);
					break;
				case right:
					theStateControllers.rightClicked.set(false, event);
					break;
				case middle:
					theStateControllers.middleClicked.set(false, event);
					break;
				default:
					break;
				}
				break;
			case clicked:
				break;
			case moved:
				break;
			case entered:
				theStateControllers.hovered.set(true, event);
				for(org.muis.core.event.MouseEvent.ButtonType button : theDocument.getPressedButtons()) {
					switch (button) {
					case left:
						theStateControllers.clicked.set(true, event);
						break;
					case right:
						theStateControllers.rightClicked.set(true, event);
						break;
					case middle:
						theStateControllers.middleClicked.set(true, event);
						break;
					default:
						break;
					}
				}
				break;
			case exited:
				theStateControllers.clicked.set(false, event);
				theStateControllers.rightClicked.set(false, event);
				theStateControllers.middleClicked.set(false, event);
				theStateControllers.hovered.set(false, event);
				break;
			}
		});
		events().filterMap(FocusEvent.focusEvent).act(event -> {
			theStateControllers.focused.set(event.isFocus(), event);
		});
	}

	private void setGroups(String [] groupNames) {
		if(getDocument() == null)
			return;
		if(groupNames == null)
			groupNames = new String[0];
		ArrayList<NamedStyleGroup> groups = new ArrayList<>();
		for(NamedStyleGroup group : getDocument().groups())
			groups.add(group);
		ArrayUtils.adjust(groups.toArray(new NamedStyleGroup[0]), groupNames, new ArrayUtils.DifferenceListener<NamedStyleGroup, String>() {
			@Override
			public boolean identity(NamedStyleGroup o1, String o2) {
				return o1.getName().equals(o2);
			}

			@Override
			public NamedStyleGroup added(String o, int mIdx, int retIdx) {
				getStyle().addGroup(getDocument().getGroup(o));
				return null;
			}

			@Override
			public NamedStyleGroup removed(NamedStyleGroup o, int oIdx, int incMod, int retIdx) {
				if(o.isMember(MuisElement.this))
					getStyle().removeGroup(o);
				return null;
			}

			@Override
			public NamedStyleGroup set(NamedStyleGroup o1, int idx1, int incMod, String o2, int idx2, int retIdx) {
				if(!o1.isMember(MuisElement.this))
					getStyle().addGroup(o1);
				return null;
			}
		});
	}

	/** @return The document that this element belongs to */
	public final MuisDocument getDocument() {
		return theDocument;
	}

	/** @return The document that this element belongs to */
	public final MuisDocument doc() {
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

	/** @return The MUIS class view that allows for instantiation of child elements */
	@Override
	public final MuisClassView cv() {
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

	/** @return The state engine that controls this element's states */
	public StateEngine getStateEngine() {
		return theStateEngine;
	}

	/**
	 * Short-hand for {@link #getStateEngine()}
	 *
	 * @return The state engine that controls this element's states
	 */
	public StateEngine state() {
		return getStateEngine();
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

	/** @return The manager of this element's events */
	public MuisEventManager getEventManager() {
		return theEvents;
	}

	/**
	 * Short-hand for {@link #getEventManager()}
	 *
	 * @return The manager of this element's events
	 */
	public MuisEventManager events() {
		return theEvents;
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
		theNamespace = namespace;
		theTagName = tagName;
		theClassView = classView;
		theModelParser = new org.muis.core.parser.DefaultModelValueReferenceParser(theDocument.getValueParser(), theClassView);
		setParent(parent);
		theLifeCycleController.advance(CoreStage.PARSE_CHILDREN.toString());
	}

	/**
	 * Initializes an element's descendants
	 *
	 * @param children The child elements specified in the MUIS XML
	 * @return The child list that the children are populated into
	 */
	public ElementList<? extends MuisElement> initChildren(MuisElement [] children) {
		theLifeCycleController.advance(CoreStage.INIT_CHILDREN.toString());
		try (MuisLock lock = theDocument.getLocker().lock(CHILDREN_LOCK_TYPE, this, true)) {
			theChildren.clear();
			theChildren.addAll(children);
		}
		for(MuisElement child : children)
			registerChild(child);
		if(theBounds.getWidth() != 0 && theBounds.getHeight() != 0) // No point laying out if there's nothing to show
			relayout(false);
		theLifeCycleController.advance(CoreStage.INITIALIZED.toString());
		return theChildren;
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
	@Override
	public MuisMessageCenter msg() {
		return getMessageCenter();
	}

	@Override
	public final MuisValueReferenceParser getValueParser() {
		return theModelParser;
	}

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
		if(theParent == parent)
			return;
		MuisElement oldParent = theParent;
		if(theParent != null) {
			theParent.theChildren.remove(this);
		}
		theParent = parent;
		events().fire(new org.muis.core.event.ElementMovedEvent(this, oldParent, parent));
	}

	/** @return An unmodifiable list of this element's children */
	public ImmutableChildList<? extends MuisElement> getChildren() {
		return theExposedChildren;
	}

	/**
	 * Short-hand for {@link #getChildren()}
	 *
	 * @return An unmodifiable list of this element's children
	 */
	public ImmutableChildList<? extends MuisElement> ch() {
		return getChildren();
	}

	/** @return An augmented, modifiable {@link List} of this element's children */
	protected ChildList getChildManager() {
		return theChildren;
	}

	// End hierarchy methods

	/**
	 * @return The default style listener to add domains and styles to listen to. When one of the registered styles changes, this element
	 *         repaints itself.
	 */
	public final CompoundStyleListener getDefaultStyleListener() {
		return theDefaultStyleListener;
	}

	// Bounds methods

	/** @return The bounds of this element */
	public final ElementBounds getBounds() {
		return theBounds;
	}

	/** @return The bounds of this element */
	public final ElementBounds bounds() {
		return theBounds;
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
			theParent.repaint(new Rectangle(theBounds.getX(), theBounds.getY(), theBounds.getWidth(), theBounds.getHeight()), false);
	}

	/** @return The size policy for this item's width */
	public SizeGuide getWSizer() {
		if(theHSizer == null)
			theHSizer = new SimpleSizeGuide();
		return theHSizer;
	}

	/** @return The size policy for this item's height */
	public SizeGuide getHSizer() {
		if(theVSizer == null)
			theVSizer = new SimpleSizeGuide();
		return theVSizer;
	}

	// End bounds methods

	/**
	 * @param x The x-position to check for click-through
	 * @param y The y-position to check for click-through
	 * @return Whether positional events are consumed by this element, or whether they should be propagated to elements under this element.
	 *         By default, this method returns true if and only if the background transparency is one.
	 */
	public boolean isClickThrough(int x, int y) {
		return false;
		// return getStyle().getSelf().get(BackgroundStyle.transparency) >= 1;
	}

	/** @return Whether this element is able to accept the focus for the document */
	public boolean isFocusable() {
		return isFocusable;
	}

	/** @param focusable Whether this element should be focusable */
	protected final void setFocusable(boolean focusable) {
		isFocusable = focusable;
	}

	/**
	 * Generates an XML-representation of this element's content
	 *
	 * @param indent The indention string to use for each level away from the margin
	 * @param deep Whether to print this element's children
	 * @return The XML string representing this element
	 */
	public final String asXML(String indent, boolean deep) {
		StringBuilder ret = new StringBuilder();
		appendXML(ret, indent, 0, deep);
		return ret.toString();
	}

	/**
	 * Appends this element's XML-representation to a string builder
	 *
	 * @param str The string builder to append to
	 * @param indent The indention string to use for each level away from the margin
	 * @param level The depth of this element in the structure being printed
	 * @param deep Whether to print this element's children
	 */
	protected final void appendXML(StringBuilder str, String indent, int level, boolean deep) {
		for(int L = 0; L < level; L++)
			str.append(indent);
		str.append('<');
		if(theNamespace != null)
			str.append(theNamespace).append(':');
		str.append(theTagName);
		if(theAttributeManager.holders().iterator().hasNext())
			str.append(' ').append(theAttributeManager.toString());
		if(!deep || theChildren.isEmpty()) {
			str.append(' ').append('/').append('>');
			return;
		}
		str.append('>');
		if(deep) {
			for(MuisElement child : theChildren) {
				str.append('\n');
				child.appendXML(str, indent, level + 1, deep);
			}
			str.append('\n');
		}
		for(int L = 0; L < level; L++)
			str.append(indent);
		str.append('<').append('/');
		if(theNamespace != null)
			str.append(theNamespace).append(':');
		str.append(theTagName).append('>');
	}

	@Override
	public String toString() {
		return asXML("", false);
	}

	// Layout methods

	/**
	 * Causes this element to adjust the position and size of its children in a way defined in this element type's implementation. By
	 * default this does nothing.
	 */
	protected void doLayout() {
		theLayoutDirtyTime = 0;
		for(MuisElement child : getChildren())
			child.doLayout();
		repaint(null, false);
	}

	/**
	 * Causes a call to {@link #doLayout()}
	 *
	 * @param now Whether to perform the layout action now or allow it to be performed asynchronously
	 * @param postActions Actions to perform after the layout action completes
	 */
	public final void relayout(boolean now, Runnable... postActions) {
		if(theBounds.getWidth() <= 0 || theBounds.getHeight() <= 0)
			return; // No point laying out if there's nothing to show
		theLayoutDirtyTime = System.currentTimeMillis();
		MuisEventQueue.get().scheduleEvent(new MuisEventQueue.LayoutEvent(this, now, postActions), now);
	}

	/** @return The last time a layout event was scheduled for this element */
	public final long getLayoutDirtyTime() {
		return theLayoutDirtyTime;
	}

	// End layout methods

	// Paint methods

	/** @return Whether this element is at least partially transparent */
	public boolean isTransparent() {
		return getStyle().getSelf().get(BackgroundStyle.transparency).get() > 0;
	}

	/**
	 * @return The bounds within which this element may draw and receive events, relative to the layout x,y position. This may extend
	 *         outside the element's layout bounds (e.g. for a menu, which expands, but does not cause a relayout when it does so).
	 */
	public Rectangle getPaintBounds() {
		return new Rectangle(0, 0, theBounds.getWidth(), theBounds.getHeight());
	}

	/**
	 * Renders this element in a graphics context.
	 *
	 * @param graphics The graphics context to render this element in
	 * @param area The area to draw
	 * @return The cached bounds used to draw the element
	 */
	public MuisElementCapture paint(java.awt.Graphics2D graphics, Rectangle area) {
		Rectangle paintBounds = getPaintBounds();
		int cacheX = paintBounds.x + theBounds.getX();
		int cacheY = paintBounds.y + theBounds.getY();
		int cacheZ = theZ;
		Rectangle preClip = graphics.getClipBounds();
		try {
			graphics.setClip(paintBounds.x, paintBounds.y, paintBounds.width, paintBounds.height);
			boolean visible = !((area != null && (area.width <= 0 || area.height <= 0)) || theBounds.getWidth() <= 0 || theBounds
				.getHeight() <= 0);
			if(visible)
				paintSelf(graphics, area);
			MuisElementCapture ret = createCapture(cacheX, cacheY, cacheZ, paintBounds.width, paintBounds.height);
			for(MuisElementCapture childBound : paintChildren(graphics, area)) {
				childBound.setParent(ret);
				childBound.seal();
				ret.addChild(childBound);
			}
			return ret;
		} finally {
			graphics.setClip(preClip);
		}
	}

	/**
	 * @param x The x-coordinate of the capture
	 * @param y The y-coordinate of the capture
	 * @param z The z-index of the capture
	 * @param w The width of the capture
	 * @param h The height of the capture
	 * @return A capture for this element
	 */
	protected MuisElementCapture createCapture(int x, int y, int z, int w, int h) {
		return new MuisElementCapture(null, this, x, y, z, w, h);
	}

	/**
	 * Causes this element to be repainted.
	 *
	 * @param area The area in this element that needs to be repainted. May be null to specify that the entire element needs to be redrawn.
	 * @param now Whether this element should be repainted immediately or not. This parameter should usually be false when this is called as
	 *            a result of a user operation such as a mouse or keyboard event because this allows all necessary paint events to be
	 *            performed at one time with no duplication after the event is finished. This parameter may be true if this is called from
	 *            an independent thread.
	 * @param postActions The actions to be performed after the event is handled successfully
	 */
	public final void repaint(Rectangle area, boolean now, Runnable... postActions) {
		if(theBounds.getWidth() <= 0 || theBounds.getHeight() <= 0)
			return; // No point painting if there's nothing to show
		thePaintDirtyTime = System.currentTimeMillis();
		MuisEventQueue.get().scheduleEvent(new MuisEventQueue.PaintEvent(this, area, now, postActions), now);
	}

	/**
	 * Renders this element's background or its content, but NOT its children. Children are rendered by
	 * {@link #paintChildren(java.awt.Graphics2D, Rectangle)}. By default, this merely draws the element's background color.
	 *
	 * @param graphics The graphics context to draw in
	 * @param area The area to paint
	 */
	public void paintSelf(java.awt.Graphics2D graphics, Rectangle area) {
		Texture tex = getStyle().getSelf().get(BackgroundStyle.texture).get();
		if(tex != null)
			tex.render(graphics, this, area);
	}

	/**
	 * Draws this element's children
	 *
	 * @param graphics The graphics context to render in
	 * @param area The area in this element's coordinates to repaint
	 * @return The cached bounds used to draw each of the element's children
	 */
	public MuisElementCapture [] paintChildren(java.awt.Graphics2D graphics, Rectangle area) {
		MuisElement [] children = ch().sortByZ();
		MuisElementCapture [] childBounds = new MuisElementCapture[children.length];
		if(children.length == 0)
			return childBounds;
		if(area == null)
			area = new Rectangle(0, 0, theBounds.getWidth(), theBounds.getHeight());
		int translateX = 0;
		int translateY = 0;
		try {
			for(int c = 0; c < children.length; c++) {
				MuisElement child = children[c];
				Rectangle childArea = child.theBounds.getBounds();
				int childX = childArea.x;
				int childY = childArea.y;
				childArea = childArea.intersection(area);
				childArea.x -= childX;
				childArea.y -= childY;
				translateX += childX;
				translateY += childY;
				graphics.translate(translateX, translateY);
				translateX = -childX;
				translateY = -childY;
				childBounds[c] = child.paint(graphics, childArea);
			}
		} finally {
			if(translateX != 0 || translateY != 0)
				graphics.translate(translateX, translateY);
		}
		return childBounds;
	}

	/** @return The last time a paint event was scheduled for this element */
	public final long getPaintDirtyTime() {
		return thePaintDirtyTime;
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

	private static class CoreStateControllers {
		StateEngine.StateController clicked;

		StateEngine.StateController rightClicked;

		StateEngine.StateController middleClicked;

		StateEngine.StateController hovered;

		StateEngine.StateController focused;
	}
}
