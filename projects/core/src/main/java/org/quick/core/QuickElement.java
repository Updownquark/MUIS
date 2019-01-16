/* Created Feb 23, 2009 by Andrew Butler */
package org.quick.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.observe.ObservableValue;
import org.observe.SettableValue;
import org.observe.Subscription;
import org.observe.collect.ObservableCollection;
import org.observe.util.TypeTokens;
import org.qommons.Transaction;
import org.qommons.collect.CollectionLockingStrategy;
import org.qommons.collect.StampedLockingStrategy;
import org.qommons.tree.BetterTreeList;
import org.quick.core.QuickConstants.CoreStage;
import org.quick.core.QuickConstants.States;
import org.quick.core.event.BoundsChangedEvent;
import org.quick.core.event.FocusEvent;
import org.quick.core.event.MouseEvent;
import org.quick.core.layout.SimpleSizeGuide;
import org.quick.core.layout.SizeGuide;
import org.quick.core.mgr.AttributeManager2;
import org.quick.core.mgr.ElementBounds;
import org.quick.core.mgr.QuickEventManager;
import org.quick.core.mgr.QuickLifeCycleManager;
import org.quick.core.mgr.QuickLifeCycleManager.Controller;
import org.quick.core.mgr.QuickMessageCenter;
import org.quick.core.mgr.QuickState;
import org.quick.core.mgr.StateEngine;
import org.quick.core.model.QuickAppModel;
import org.quick.core.prop.DefaultExpressionContext;
import org.quick.core.prop.ExpressionContext;
import org.quick.core.style.BackgroundStyle;
import org.quick.core.style.LightedStyle;
import org.quick.core.style.QuickElementStyle;
import org.quick.core.style.StyleAttributes;
import org.quick.core.style.StyleChangeObservable;
import org.quick.core.style.Texture;
import org.quick.core.tags.AcceptAttribute;
import org.quick.core.tags.QuickElementType;
import org.quick.core.tags.QuickTagUtils;
import org.quick.core.tags.State;
import org.quick.util.QuickUtils;

import com.google.common.reflect.TypeToken;

/** The base display element in Quick. Contains base methods to administer content (children, style, placement, etc.) */
@QuickElementType(
	attributes = { @AcceptAttribute(declaringClass = StyleAttributes.class, field = "style"),
		@AcceptAttribute(declaringClass = StyleAttributes.class, field = "group") },
	states={@State(name = States.CLICK_NAME, priority = States.CLICK_PRIORITY),
		@State(name = States.RIGHT_CLICK_NAME, priority = States.RIGHT_CLICK_PRIORITY),
		@State(name = States.MIDDLE_CLICK_NAME, priority = States.MIDDLE_CLICK_PRIORITY),
		@State(name = States.HOVER_NAME, priority = States.HOVER_PRIORITY),
		@State(name = States.FOCUS_NAME, priority = States.FOCUS_PRIORITY),
		@State(name = States.TEXT_SELECTION_NAME, priority = States.TEXT_SELECTION_PRIORITY)})
public abstract class QuickElement implements QuickParseEnv {
	private final QuickLifeCycleManager theLifeCycleManager;

	private QuickLifeCycleManager.Controller theLifeCycleController;

	private final ReentrantReadWriteLock theAttributeLocker;

	private final CollectionLockingStrategy theContentLocker;

	private final StateEngine theStateEngine;

	private final QuickMessageCenter theMessageCenter;

	private final QuickEventManager theEvents;

	private QuickDocument theDocument;

	private QuickToolkit theToolkit;

	private final SettableValue<QuickElement> theParent;

	private QuickClassView theClassView;

	private ExpressionContext theContext;

	private QuickAppModel theSelfModel;

	private String theNamespace;

	private String theTagName;

	private final AttributeManager2 theAttributeManager;

	private final ObservableCollection<QuickElement> theChildren;

	private final ObservableCollection<QuickElement> theExposedChildren;

	private final QuickElementStyle theStyle;

	private final StyleChangeObservable theDefaultStyleListener;

	private final CoreStateControllers theStateControllers;

	private int theZ;

	private ElementBounds theBounds;

	private SizeGuide theHSizer;

	private SizeGuide theVSizer;

	private boolean isFocusable;

	private long thePaintDirtyTime;

	private long theLayoutDirtyTime;

	/** Creates a Quick element */
	public QuickElement() {
		theParent = new org.observe.SimpleSettableValue<>(TypeToken.of(QuickElement.class), true);
		theParent.changes().act(evt -> {
			if (evt.getOldValue() != null)
				evt.getOldValue().theChildren.remove(QuickElement.this);
		});
		theAttributeLocker = new ReentrantReadWriteLock();
		theContentLocker = new StampedLockingStrategy();
		theMessageCenter = new QuickMessageCenter(null, null, this);
		theLifeCycleManager = new QuickLifeCycleManager(this, (Controller controller) -> {
			theLifeCycleController = controller;
		}, CoreStage.READY.toString());
		theStateEngine = new StateEngine(this);
		theEvents = new QuickEventManager(this);
		theStateControllers = new CoreStateControllers();
		String lastStage = null;
		for(CoreStage stage : CoreStage.values())
			if(stage != CoreStage.OTHER && stage != CoreStage.READY) {
				theLifeCycleManager.addStage(stage.toString(), lastStage);
				lastStage = stage.toString();
			}
		theBounds = new ElementBounds(this);
		theChildren = ObservableCollection.create(TypeTokens.get().of(QuickElement.class), new BetterTreeList<>(theContentLocker));
		theExposedChildren = theChildren.flow().unmodifiable(false).collect();
		theAttributeManager = new AttributeManager2(this, theAttributeLocker);
		theStyle = new QuickElementStyle(this);
		theSelfModel = QuickAppModel.empty("this");
		theDefaultStyleListener = new StyleChangeObservable(theStyle);
		theDefaultStyleListener.watch(BackgroundStyle.getDomainInstance(), LightedStyle.getDomainInstance());
		theDefaultStyleListener.act(evt -> {
			repaint(null, false);
		});
		List<Runnable> childRemoves = new ArrayList<>();
		theChildren.subscribe(evt -> {
			switch (evt.getType()) {
			case add:
				childRemoves.add(evt.getIndex(), registerChild(//
					evt.getNewValue()));
				break;
			case remove:
				Runnable remove = childRemoves.remove(evt.getIndex());
				remove.run();
				break;
			case set:
				if (evt.getOldValue() != evt.getNewValue()) {
					remove = childRemoves.get(evt.getIndex());
					remove.run();
					childRemoves.set(evt.getIndex(), registerChild(//
						evt.getNewValue()));
				}
				break;
			}
		}, true);
		theChildren.simpleChanges().act(cause -> sizeNeedsChanged());
		theChildren.changes().act(event -> {
			Rectangle bounds = null;
			for (QuickElement child : event.getValues()) {
				if (child.bounds().isEmpty())
					continue;
				else if (bounds == null)
					bounds = child.bounds().getBounds();
				else
					bounds = bounds.union(child.bounds().getBounds());
			}
			if (event.getOldValues() != null) {
				for (QuickElement child : event.getOldValues()) {
					if (bounds == null)
						bounds = child.bounds().getBounds();
					else if (!child.bounds().isEmpty())
						bounds = bounds.union(child.bounds().getBounds());
				}
			}
			if (bounds != null)
				repaint(bounds, false);
		});
		bounds().changes().act(event -> {
			Rectangle old = event.getOldValue();
			if (old == null || event.getNewValue().width != old.width || event.getNewValue().height != old.height)
				relayout(false);
		});
		theLifeCycleManager.runWhen(() -> {
			repaint(null, false);
		}, CoreStage.INIT_SELF.toString(), 2);
		addAnnotatedStates();
		addStateListeners();
		theLifeCycleController.advance(CoreStage.PARSE_SELF.toString());
	}

	private void addAnnotatedStates() {
		for (QuickState state : QuickTagUtils.getStatesFor(getClass())) {
			try {
				theStateEngine.addState(state);
			} catch(IllegalArgumentException e) {
				msg().warn(e.getMessage(), "state", state);
			}
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
					theStateControllers.clicked.setActive(true, event);
					break;
				case right:
					theStateControllers.rightClicked.setActive(true, event);
					break;
				case middle:
					theStateControllers.middleClicked.setActive(true, event);
					break;
				default:
					break;
				}
				break;
			case released:
				switch (event.getButton()) {
				case left:
					theStateControllers.clicked.setActive(false, event);
					break;
				case right:
					theStateControllers.rightClicked.setActive(false, event);
					break;
				case middle:
					theStateControllers.middleClicked.setActive(false, event);
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
				theStateControllers.hovered.setActive(true, event);
				for(org.quick.core.event.MouseEvent.ButtonType button : theDocument.getPressedButtons()) {
					switch (button) {
					case left:
						theStateControllers.clicked.setActive(true, event);
						break;
					case right:
						theStateControllers.rightClicked.setActive(true, event);
						break;
					case middle:
						theStateControllers.middleClicked.setActive(true, event);
						break;
					default:
						break;
					}
				}
				break;
			case exited:
				theStateControllers.clicked.setActive(false, event);
				theStateControllers.rightClicked.setActive(false, event);
				theStateControllers.middleClicked.setActive(false, event);
				theStateControllers.hovered.setActive(false, event);
				break;
			}
		});
		events().filterMap(FocusEvent.focusEvent).act(event -> {
			theStateControllers.focused.setActive(event.isFocus(), event);
		});
	}

	/** @return A locker controlling threaded access to this element's single-valued attributes */
	public ReentrantReadWriteLock getAttributeLocker() {
		return theAttributeLocker;
	}

	/** @return A locker controlling threaded access to this element's multi-valued attributes (e.g. children) */
	public CollectionLockingStrategy getContentLocker() {
		return theContentLocker;
	}

	/** @return The document that this element belongs to */
	public final QuickDocument getDocument() {
		return theDocument;
	}

	/** @return The document that this element belongs to */
	public final QuickDocument doc() {
		return theDocument;
	}

	/** @return The tool kit that this element belongs to */
	public final QuickToolkit getToolkit() {
		return theToolkit;
	}

	/** @return The Quick class view that allows for instantiation of child elements */
	public final QuickClassView getClassView() {
		return theClassView;
	}

	/** @return The Quick class view that allows for instantiation of child elements */
	@Override
	public final QuickClassView cv() {
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
	public AttributeManager2 getAttributeManager() {
		return theAttributeManager;
	}

	/**
	 * Short-hand for {@link #getAttributeManager()}
	 *
	 * @return The manager of this element's attributes
	 */
	public AttributeManager2 atts() {
		return getAttributeManager();
	}

	/** @return The manager of this element's events */
	public QuickEventManager getEventManager() {
		return theEvents;
	}

	/**
	 * Short-hand for {@link #getEventManager()}
	 *
	 * @return The manager of this element's events
	 */
	public QuickEventManager events() {
		return theEvents;
	}

	/** @return The style that modifies this element's appearance */
	public final QuickElementStyle getStyle() {
		return theStyle;
	}

	// Life cycle methods

	/**
	 * Returns a life cycle manager that allows subclasses to customize and hook into the life cycle for this element.
	 *
	 * @return The life cycle manager for this element
	 */
	public QuickLifeCycleManager getLifeCycleManager() {
		return theLifeCycleManager;
	}

	/**
	 * Short-hand for {@link #getLifeCycleManager()}
	 *
	 * @return The life cycle manager for this element
	 */
	public QuickLifeCycleManager life() {
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
	public final void init(QuickDocument doc, QuickToolkit toolkit, QuickClassView classView, QuickElement parent, String namespace,
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
		QuickAppModel selfModel = getSelfModel();
		if (selfModel == null)
			throw new NullPointerException("Self model is not initialized yet");
		DefaultExpressionContext.Builder ctxBuilder = DefaultExpressionContext.build()//
			.withParent(theDocument.getContext())//
			.withValue("this", ObservableValue.of(selfModel));
		theContext = ctxBuilder.build();
		setParent(parent);
		addAnnotatedAttributes();
		theLifeCycleController.advance(CoreStage.PARSE_CHILDREN.toString());
	}

	private void addAnnotatedAttributes() {
		Object wanter = new Object();
		List<QuickTagUtils.AcceptedAttributeStruct<?>> atts;
		try {
			atts = QuickTagUtils.getAcceptedAttributes(getClass());
		} catch (RuntimeException e) {
			theMessageCenter.fatal("Could not parse attributes on element type " + getClass().getSimpleName() + " from annotation", e);
			return;
		}
		for (QuickTagUtils.AcceptedAttributeStruct<?> att : atts) {
			if (att.annotation.required())
				theAttributeManager.accept(att.attribute, wanter, a -> a.required());
			else
				theAttributeManager.accept(att.attribute, wanter, a -> a.optional());
			if (att.annotation.defaultValue().length() > 0)
				try {
					theAttributeManager.set(att.attribute, att.annotation.defaultValue(), this);
				} catch (QuickException e) {
					theMessageCenter.error("Could not set default value " + att.annotation.defaultValue() + " for attribute "
						+ att.attribute + " from annotation", e);
				}
		}
	}

	/**
	 * Initializes an element's descendants
	 *
	 * @param children The child elements specified in the Quick XML
	 */
	public void initChildren(List<QuickElement> children) {
		theLifeCycleController.advance(CoreStage.INIT_CHILDREN.toString());
		try (Transaction t = theChildren.lock(true, null)) {
			theChildren.clear();
			theChildren.addAll(children);
		}
		if(theBounds.getWidth() != 0 && theBounds.getHeight() != 0) // No point laying out if there's nothing to show
			relayout(false);
		theLifeCycleController.advance(CoreStage.INITIALIZED.toString());
	}

	/**
	 * Called when a child is introduced to this parent
	 *
	 * @param child The child that has been added to this parent
	 * @return A runnable that will be executed when the element is no longer a child of this element
	 */
	protected Runnable registerChild(QuickElement child) {
		if(child.getParent() != this)
			child.setParent(this);

		// Need to catch the child up to where the parent is in the life cycle
		if (child.life().isAfter(CoreStage.INIT_SELF.name()) < 0 && life().isAfter(CoreStage.PARSE_CHILDREN.name()) > 0) {
			QuickToolkit tk;
			if (child.getClass().getClassLoader() instanceof QuickToolkit)
				tk = (QuickToolkit) child.getClass().getClassLoader();
			else
				tk = getDocument().getEnvironment().getCoreToolkit();
			org.quick.core.QuickClassView classView = new org.quick.core.QuickClassView(getDocument().getEnvironment(), theClassView, tk);
			child.init(getDocument(), tk, classView, this, null, null);
		}
		if (child.life().isAfter(CoreStage.INIT_CHILDREN.name()) < 0 && life().isAfter(CoreStage.INITIALIZED.name()) > 0) {
			child.initChildren(Collections.emptyList());
		}
		if (child.life().isAfter(CoreStage.STARTUP.name()) < 0 && life().isAfter(CoreStage.READY.name()) > 0) {
			child.postCreate();
		}
		Subscription eventSub = child.events().filterMap(BoundsChangedEvent.bounds).filter(event -> !isStamp(event.getElement()))
			.act(event -> {
			Rectangle paintRect = event.getNewValue().union(event.getOldValue());
			repaint(paintRect, false);
		});

		return () -> {
			eventSub.unsubscribe();
			if (child.getParent() == this)
				child.setParent(null);
		};
	}

	/**
	 * Called when a child is removed to this parent
	 *
	 * @param child The child that has been removed from this parent
	 */
	protected void unregisterChild(QuickElement child) {
	}

	/** Called to initialize an element after all the parsing and linking has been performed */
	public final void postCreate() {
		theLifeCycleController.advance(CoreStage.STARTUP.toString());
		for(QuickElement child : theChildren)
			child.postCreate();
		theDefaultStyleListener.begin();
		theLifeCycleController.advance(CoreStage.READY.toString());
	}

	// End life cycle methods

	/**
	 * Returns a message center that allows messaging on this element
	 *
	 * @return This element's message center
	 */
	public QuickMessageCenter getMessageCenter() {
		return theMessageCenter;
	}

	/**
	 * Short-hand for {@link #getMessageCenter()}
	 *
	 * @return This element's message center
	 */
	@Override
	public QuickMessageCenter msg() {
		return getMessageCenter();
	}

	@Override
	public ExpressionContext getContext() {
		return theContext;
	}

	/** @return The "this" model for this element */
	public QuickAppModel getSelfModel() {
		return theSelfModel;
	}

	// Hierarchy methods

	/** @return This element's parent in the DOM tree */
	public final ObservableValue<QuickElement> getParent() {
		return theParent.unsettable();
	}

	/**
	 * Sets this element's parent after initialization
	 *
	 * @param parent The new parent for this element
	 */
	protected final void setParent(QuickElement parent) {
		if (theParent.get() == parent)
			return;
		theParent.set(parent, null);
	}

	/** @return An list of the elements immediately contained by this element. By default, this list is immutable. */
	public ObservableCollection<? extends QuickElement> getPhysicalChildren() {
		return theExposedChildren;
	}

	/**
	 * Short-hand for {@link #getPhysicalChildren()}
	 *
	 * @return An unmodifiable list of the elements immediately contained by this element
	 */
	public ObservableCollection<? extends QuickElement> ch() {
		return getPhysicalChildren();
	}

	/**
	 * @return A list of the logical contents of this element. By default, this is the same as its {@link #getPhysicalChildren() physical
	 *         children}.
	 */
	public ObservableCollection<? extends QuickElement> getLogicalChildren() {
		return theExposedChildren;
	}

	/** @return An augmented, modifiable {@link List} of this element's children */
	protected ObservableCollection<QuickElement> getChildManager() {
		return theChildren;
	}

	/**
	 * @param child The child element of this element to check
	 * @return Whether the given child is being used as a stamp for rendering
	 */
	protected boolean isStamp(QuickElement child) {
		return false;
	}

	// End hierarchy methods

	/**
	 * @return The default style listener to add domains and styles to listen to. When one of the registered styles changes, this element
	 *         repaints itself.
	 */
	public final StyleChangeObservable getDefaultStyleListener() {
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
		QuickElement parent = theParent.get();
		if (parent != null)
			parent.repaint(new Rectangle(theBounds.getX(), theBounds.getY(), theBounds.getWidth(), theBounds.getHeight()), false);
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
		if (!theAttributeManager.getAllAttributes().isEmpty())
			str.append(' ').append(theAttributeManager.toString());
		if(!deep || theChildren.isEmpty()) {
			str.append(' ').append('/').append('>');
			return;
		}
		str.append('>');
		if(deep) {
			for(QuickElement child : theChildren) {
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
	 * default this only calls the doLayout() method of its physical children and {@link #repaint(Rectangle, boolean, Runnable...)}.
	 */
	protected void doLayout() {
		if (theBounds.isEmpty())
			return;
		theLayoutDirtyTime = 0;
		for(QuickElement child : getPhysicalChildren())
			child.doLayout();
		repaint(null, false);
	}

	/** Alerts the system that this element's size needs may have changed */
	public final void sizeNeedsChanged() {
		QuickElement parent = getParent().get();
		if (parent != null && parent.bounds().isEmpty())
			return;
		else if (bounds().isEmpty())
			return;
		QuickEventQueue.get().scheduleEvent(new QuickEventQueue.SizeNeedsChangedEvent(this), false);
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
		if (theLayoutDirtyTime == 0)
			theLayoutDirtyTime = System.currentTimeMillis();
		QuickEventQueue.get().scheduleEvent(new QuickEventQueue.LayoutEvent(this, now, postActions), now);
	}

	/** @return The time since which this element has needed a layout operation */
	public final long getLayoutDirtyTime() {
		return theLayoutDirtyTime;
	}

	// End layout methods

	// Paint methods

	/** @return Whether this element is at least partially transparent */
	public boolean isTransparent() {
		return getStyle().get(BackgroundStyle.transparency).get() > 0;
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
	public QuickElementCapture paint(java.awt.Graphics2D graphics, Rectangle area) {
		Rectangle paintBounds = getPaintBounds();
		int cacheX = paintBounds.x + theBounds.getX();
		int cacheY = paintBounds.y + theBounds.getY();
		int cacheZ = theZ;
		Rectangle preClip = Rectangle.fromAwt(graphics.getClipBounds());
		try {
			graphics.setClip(paintBounds.x, paintBounds.y, paintBounds.width, paintBounds.height);
			boolean visible = !((area != null && (area.width <= 0 || area.height <= 0)) || theBounds.getWidth() <= 0 || theBounds
				.getHeight() <= 0);
			if(visible)
				paintSelf(graphics, area);
			QuickElementCapture ret = createCapture(cacheX, cacheY, cacheZ, paintBounds.width, paintBounds.height);
			for(QuickElementCapture childBound : paintChildren(graphics, area)) {
				childBound.setParent(ret);
				childBound.seal();
				ret.addChild(childBound);
			}
			return ret;
		} finally {
			graphics.setClip(preClip.toAwt());
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
	protected QuickElementCapture createCapture(int x, int y, int z, int w, int h) {
		return new QuickElementCapture(null, this, x, y, z, w, h);
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
		if (thePaintDirtyTime == 0)
			thePaintDirtyTime = System.currentTimeMillis();
		QuickEventQueue.get().scheduleEvent(new QuickEventQueue.PaintEvent(this, area, now, postActions), now);
	}

	/**
	 * Renders this element's background or its content, but NOT its children. Children are rendered by
	 * {@link #paintChildren(java.awt.Graphics2D, Rectangle)}. By default, this merely draws the element's background color.
	 *
	 * @param graphics The graphics context to draw in
	 * @param area The area to paint
	 */
	public void paintSelf(java.awt.Graphics2D graphics, Rectangle area) {
		Texture tex = getStyle().get(BackgroundStyle.texture).get();
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
	public QuickElementCapture [] paintChildren(java.awt.Graphics2D graphics, Rectangle area) {
		QuickElement[] children = QuickUtils.sortByZ(ch().toArray());
		QuickElementCapture [] childBounds = new QuickElementCapture[children.length];
		if(children.length == 0)
			return childBounds;
		java.awt.Rectangle awtArea;
		if (area != null)
			awtArea = area.toAwt();
		else
			awtArea = new java.awt.Rectangle(0, 0, theBounds.getWidth(), theBounds.getHeight());
		int translateX = 0;
		int translateY = 0;
		try {
			for(int c = 0; c < children.length; c++) {
				QuickElement child = children[c];
				java.awt.Rectangle childArea = child.theBounds.getBounds().toAwt();
				int childX = childArea.x;
				int childY = childArea.y;
				childArea = childArea.intersection(awtArea);
				childArea.x -= childX;
				childArea.y -= childY;
				translateX += childX;
				translateY += childY;
				graphics.translate(translateX, translateY);
				translateX = -childX;
				translateY = -childY;
				childBounds[c] = child.paint(graphics, Rectangle.fromAwt(childArea));
			}
		} finally {
			if(translateX != 0 || translateY != 0)
				graphics.translate(translateX, translateY);
		}
		return childBounds;
	}

	/** @return The time since which this element has needed a paint operation */
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

	@Override
	protected void finalize() throws Throwable {
		theLifeCycleController.advance(QuickConstants.CoreStage.DISPOSE.name());
		super.finalize();
	}

	private static class CoreStateControllers {
		StateEngine.StateController clicked;

		StateEngine.StateController rightClicked;

		StateEngine.StateController middleClicked;

		StateEngine.StateController hovered;

		StateEngine.StateController focused;
	}
}
