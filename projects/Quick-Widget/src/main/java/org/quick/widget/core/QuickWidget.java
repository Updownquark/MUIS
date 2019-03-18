package org.quick.widget.core;

import java.awt.Graphics2D;
import java.util.*;
import java.util.function.Consumer;

import org.observe.ObservableValue;
import org.observe.SettableValue;
import org.observe.SimpleSettableValue;
import org.observe.Subscription;
import org.observe.collect.CollectionChangeEvent;
import org.observe.collect.ObservableCollection;
import org.qommons.Lockable;
import org.qommons.Transaction;
import org.quick.core.QuickConstants.CoreStage;
import org.quick.core.QuickConstants.States;
import org.quick.core.QuickDefinedWidget;
import org.quick.core.QuickElement;
import org.quick.core.QuickException;
import org.quick.core.layout.Orientation;
import org.quick.core.mgr.StateEngine;
import org.quick.core.style.BackgroundStyle;
import org.quick.core.style.LightedStyle;
import org.quick.core.style.StyleChangeObservable;
import org.quick.widget.core.event.FocusEvent;
import org.quick.widget.core.event.MouseEvent;
import org.quick.widget.core.layout.SimpleSizeGuide;
import org.quick.widget.core.layout.SizeGuide;
import org.quick.widget.core.mgr.ElementBounds;
import org.quick.widget.core.mgr.QuickEventManager;
import org.quick.widget.core.style.BaseTexture;
import org.quick.widget.core.style.QuickWidgetTexture;

import com.google.common.reflect.TypeToken;

public abstract class QuickWidget<E extends QuickElement> implements QuickDefinedWidget<QuickWidgetDocument, E> {
	public static final TypeToken<QuickWidget<?>> WILDCARD = new TypeToken<QuickWidget<?>>() {};

	private QuickWidgetDocument theDocument;

	private E theElement;

	private final SettableValue<QuickWidget<?>> theParent;

	private ObservableCollection<QuickWidget<?>> theChildren;

	private final Map<QuickElement, QuickWidget<?>> theChildMap;

	private final QuickEventManager theEvents;

	private final ElementBounds theBounds;

	private final CoreStateControllers theStateControllers;

	private StyleChangeObservable theDefaultStyleListener;

	private SizeGuide theHSizer;

	private SizeGuide theVSizer;

	private volatile int isHoldingEvents;

	private long thePaintDirtyTime;

	private long theLayoutDirtyTime;

	public QuickWidget() {
		theParent = new SimpleSettableValue<>(WILDCARD, true);
		theEvents = new QuickEventManager(this);
		theBounds = new ElementBounds(this);
		theStateControllers = new CoreStateControllers();
		theChildMap = new HashMap<>();
		bounds().changes().act(event -> {
			if (isHoldingEvents != 0)
				return;
			Rectangle old = event.getOldValue();
			if (old == null || event.getNewValue().width != old.width || event.getNewValue().height != old.height)
				relayout(false);
		});
	}

	@Override
	public void init(QuickWidgetDocument document, E element, QuickDefinedWidget<QuickWidgetDocument, ?> parent) throws QuickException {
		theDocument = document;
		theElement = element;
		theParent.set((QuickWidget<?>) parent, null);
		theChildren = createChildren();
		theElement.life().runWhen(() -> {
			repaint(null, false);
		}, CoreStage.INIT_SELF.toString(), 2);
		theElement.life().runWhen(() -> {
			if (theBounds.getWidth() != 0 && theBounds.getHeight() != 0) // No point laying out if there's nothing to show
				relayout(false);
		}, CoreStage.INITIALIZED, -1);
		theDefaultStyleListener = new StyleChangeObservable(theElement.getStyle());
		theDefaultStyleListener.watch(BackgroundStyle.getDomainInstance(), LightedStyle.getDomainInstance());
		theElement.life().runWhen(() -> {
			theDefaultStyleListener.begin();
		}, CoreStage.READY, -1);
		theChildren.simpleChanges().act(cause -> sizeNeedsChanged());
		List<Consumer<Object>> childRemoves = new ArrayList<>();
		theChildren.subscribe(evt -> {
			switch (evt.getType()) {
			case add:
				theChildMap.put(evt.getNewValue().getElement(), evt.getNewValue());
				childRemoves.add(evt.getIndex(), registerChild(//
					evt.getNewValue(), evt));
				break;
			case remove:
				Consumer<Object> remove = childRemoves.remove(evt.getIndex());
				remove.accept(evt);
				theChildMap.remove(evt.getOldValue().getElement());
				break;
			case set:
				if (evt.getOldValue() != evt.getNewValue()) {
					theChildMap.remove(evt.getOldValue().getElement());
					theChildMap.put(evt.getNewValue().getElement(), evt.getNewValue());
					remove = childRemoves.get(evt.getIndex());
					remove.accept(evt);
					childRemoves.set(evt.getIndex(), registerChild(//
						evt.getNewValue(), evt));
				}
				break;
			}
		}, true);
		theChildren.changes().act(event -> {
			if (isHoldingEvents != 0)
				return;
			Rectangle bounds = null;
			switch (event.type) {
			case add:
			case remove:
				for (QuickWidget<?> child : event.getValues())
					bounds = bounds == null ? child.bounds().get() : bounds.union(child.bounds().get());
					break;
			case set:
				for (CollectionChangeEvent.ElementChange<QuickWidget<?>> el : event.elements) {
					bounds = bounds == null ? el.newValue.bounds().get() : bounds.union(el.newValue.bounds().get());
					if (el.oldValue != null && !el.oldValue.bounds().isEmpty())
						bounds = bounds.union(el.oldValue.bounds().get());
				}
				break;
			}
			if (bounds != null)
				repaint(bounds, false);
		});
		addStateListeners();
		theDefaultStyleListener.act(evt -> {
			if (isHoldingEvents == 0)
				repaint(null, false);
		});
	}

	protected ObservableCollection<QuickWidget<?>> createChildren() {
		return theElement.ch().flow().map(WILDCARD, //
			this::createChild, opts -> opts.cache(true).reEvalOnUpdate(false))
			.filter(el -> el == null ? "Unable to create child" : null).collect();
	}

	protected <CE extends QuickElement> QuickWidget<CE> createChild(CE childElement) {
		try {
			return (QuickWidget<CE>) getDocument().getWidgetSet().createWidget(theDocument, childElement, this);
		} catch (QuickException e) {
			getElement().msg().error("Could not create child for element", e, "element", childElement);
			return null;
		}
	}

	@Override
	public <CE extends QuickElement> QuickWidget<CE> getChild(CE childElement) {
		return childElement == null ? null : (QuickWidget<CE>) theChildMap.get(childElement);
	}

	private void addStateListeners() {
		theStateControllers.clicked = theElement.state().control(States.CLICK);
		theStateControllers.rightClicked = theElement.state().control(States.RIGHT_CLICK);
		theStateControllers.middleClicked = theElement.state().control(States.MIDDLE_CLICK);
		theStateControllers.hovered = theElement.state().control(States.HOVER);
		theStateControllers.focused = theElement.state().control(States.FOCUS);
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
				for (org.quick.widget.core.event.MouseEvent.ButtonType button : theDocument.getPressedButtons()) {
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

	/**
	 * Called when a child is introduced to this parent
	 *
	 * @param child The child that has been added to this parent
	 * @return A runnable that will be executed when the element is no longer a child of this widget
	 */
	protected Consumer<Object> registerChild(QuickWidget<?> child, Object addCause) {
		if (child.getParent() != this)
			child.setParent(this, addCause);

		Subscription eventSub = theElement.getResourcePool().poolValue(child.bounds()).noInitChanges().act(event -> {
			if (isHoldingEvents == 0) {
				Rectangle paintRect = event.getNewValue().union(event.getOldValue());
				repaint(paintRect, false);
			}
		});

		return removeCause -> {
			eventSub.unsubscribe();
			unregisterChild(child);
			if (child.getParent() == this)
				child.setParent(null, removeCause);
		};
	}

	/**
	 * Called when a child is removed to this parent
	 *
	 * @param child The child that has been removed from this parent
	 */
	protected void unregisterChild(QuickWidget<?> child) {}

	@Override
	public QuickWidgetDocument getDocument() {
		return theDocument;
	}

	@Override
	public E getElement() {
		return theElement;
	}

	@Override
	public ObservableValue<QuickWidget<?>> getParent() {
		return theParent.unsettable();
	}

	protected void setParent(QuickWidget<?> parent, Object cause) {
		theParent.set(parent, cause);
	}

	@Override
	public ObservableCollection<QuickWidget<?>> getChildren() {
		return theChildren;
	}

	/** @return The manager of this element's events */
	public QuickEventManager events() {
		return theEvents;
	}

	protected StyleChangeObservable getDefaultStyleListener() {
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

	/**
	 * Places a temporary hold on this element so that changes to its content or attributes do not cause events
	 *
	 * @param editAttributes Whether attributes are expected to be modified during the hold
	 * @param editContent Whether content is expected to be modified during the hold
	 * @return A transaction to close to release the lock
	 */
	public Transaction holdEvents(boolean editAttributes, boolean editContent) {
		if (!editAttributes && !editContent)
			throw new IllegalArgumentException("Either an attribute or content lock must be held");
		Transaction release = Lockable.lockAll(//
			Lockable.lockable(theElement.getAttributeLocker(), editContent), //
			Lockable.lockable(theElement.getContentLocker(), editContent, editContent, null));
		isHoldingEvents++;
		boolean[] released = new boolean[1];
		return () -> {
			if (!released[0]) {
				released[0] = true;
				isHoldingEvents--;
				release.close();
			}
		};
	}

	/**
	 * @param orientation The orientation direction to get the sizer for
	 * @return The size policy for this item along the given orientation
	 */
	public SizeGuide getSizer(Orientation orientation) {
		SizeGuide sizer;
		if (orientation.isVertical()) {
			if (theVSizer == null)
				theVSizer = new SimpleSizeGuide();
			sizer = theVSizer;
		} else {
			if (theHSizer == null)
				theHSizer = new SimpleSizeGuide();
			sizer = theHSizer;
		}
		return sizer;
	}

	// End bounds methods

	// Layout methods

	/**
	 * Causes this element to adjust the position and size of its children in a way defined in this element type's implementation. By
	 * default this only calls the doLayout() method of its physical children and {@link #repaint(Rectangle, boolean, Runnable...)}.
	 */
	protected void doLayout() {
		if (theBounds.isEmpty())
			return;
		theLayoutDirtyTime = 0;
		for (QuickWidget<?> child : getChildren())
			child.doLayout();
		repaint(null, false);
	}

	/** Alerts the system that this element's size needs may have changed */
	public final void sizeNeedsChanged() {
		if (isHoldingEvents > 0)
			return;
		QuickWidget<?> parent = getParent().get();
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
		if (isHoldingEvents > 0 || theBounds.getWidth() <= 0 || theBounds.getHeight() <= 0)
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

	/** @return Whether this element is at least partially transparent */
	public boolean isTransparent() {
		return theElement.getStyle().get(BackgroundStyle.transparency).get() > 0;
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
		// int cacheZ = theZ;
		int cacheZ = 0;
		Rectangle preClip = Rectangle.fromAwt(graphics.getClipBounds());
		try {
			graphics.setClip(paintBounds.x, paintBounds.y, paintBounds.width, paintBounds.height);
			boolean visible = !((area != null && (area.width <= 0 || area.height <= 0)) || theBounds.getWidth() <= 0
				|| theBounds.getHeight() <= 0);
			if (visible)
				paintSelf(graphics, area);
			QuickElementCapture ret = createCapture(cacheX, cacheY, cacheZ, paintBounds.width, paintBounds.height);
			for (QuickElementCapture childBound : paintChildren(graphics, area)) {
				childBound.setParent(ret);
				childBound.seal();
				ret.addChild(childBound);
			}
			return ret;
		} finally {
			graphics.setClip(preClip == null ? null : preClip.toAwt());
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
	 *        a result of a user operation such as a mouse or keyboard event because this allows all necessary paint events to be performed
	 *        at one time with no duplication after the event is finished. This parameter may be true if this is called from an independent
	 *        thread.
	 * @param postActions The actions to be performed after the event is handled successfully
	 */
	public final void repaint(Rectangle area, boolean now, Runnable... postActions) {
		if (isHoldingEvents > 0 || theBounds.getWidth() <= 0 || theBounds.getHeight() <= 0)
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
		QuickWidgetTexture tex = (QuickWidgetTexture) theElement.getStyle().get(BackgroundStyle.texture).get();
		if (tex == null)
			tex = new BaseTexture();
		tex.render(graphics, this, area);
	}

	/**
	 * Draws this element's children
	 *
	 * @param graphics The graphics context to render in
	 * @param area The area in this element's coordinates to repaint
	 * @return The cached bounds used to draw each of the element's children
	 */
	public QuickElementCapture[] paintChildren(java.awt.Graphics2D graphics, Rectangle area) {
		List<QuickWidget<?>> children = Arrays.asList(getChildren().toArray());
		if (children.isEmpty())
			return new QuickElementCapture[0];
		if (area == null)
			area = new Rectangle(0, 0, bounds().getWidth(), bounds().getHeight());
		return paintChildren(children, graphics, area);
	}

	public static QuickElementCapture[] paintChildren(Iterable<? extends QuickWidget<?>> children, Graphics2D graphics, Rectangle area) {
		java.awt.Rectangle awtArea = area.toAwt();
		List<QuickElementCapture> childBounds = new ArrayList<>();
		int translateX = 0;
		int translateY = 0;
		try {
			for (QuickWidget<?> child : children) {
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
				if (childArea.isEmpty())
					childBounds.add(child.paint(graphics, new Rectangle(childArea.x, childArea.y, 0, 0)));
				else
					childBounds.add(child.paint(graphics, Rectangle.fromAwt(childArea)));
			}
		} finally {
			if (translateX != 0 || translateY != 0)
				graphics.translate(translateX, translateY);
		}
		return childBounds.toArray(new QuickElementCapture[childBounds.size()]);
	}

	/** @return The time since which this element has needed a paint operation */
	public final long getPaintDirtyTime() {
		return thePaintDirtyTime;
	}

	// End paint methods

	@Override
	public int hashCode() {
		return theElement.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}

	@Override
	public String toString() {
		return theElement.toString();
	}

	private static class CoreStateControllers {
		StateEngine.StateController clicked;

		StateEngine.StateController rightClicked;

		StateEngine.StateController middleClicked;

		StateEngine.StateController hovered;

		StateEngine.StateController focused;
	}
}
