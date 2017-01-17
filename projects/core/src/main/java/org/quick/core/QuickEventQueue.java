package org.quick.core;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import org.qommons.AbstractCausable;
import org.qommons.ArrayUtils;
import org.qommons.Causable;
import org.qommons.ProgramTracker;
import org.quick.util.QuickUtils;

/** The event queue in Quick which makes sure elements's states stay up-to-date */
public class QuickEventQueue {
	private static long DEBUG_TRACKING = 0;

	/** Represents an event that may be queued for handling by the QuickEventQueue */
	public static interface Event extends Causable {
		/** @return The time at which this event was created */
		long getTime();

		/**
		 * Checks whether the event should be handled now or stay in the queue and be handled later
		 *
		 * @param time The time to use in determining whether this event should be handled now
		 * @return Whether the event should be handled now or stay in the queue and be handled later
		 */
		boolean shouldHandle(long time);

		/** Executes this event's action */
		void handle();

		/**
		 * Called when the event queue discards this event because it has been {@link #isSupersededBy(Event) superseded}
		 *
		 * @return Whether this event was discarded. A false value indicates that this event is already being or has been handled
		 */
		boolean discard();

		/** @return Whether this event's {@link #handle()} method is currently executing */
		boolean isHandling();

		/** @return Whether this event's {@link #handle()} method has finished executing or its {@link #discard()} method was called */
		boolean isFinished();

		/** @return Whether this event's {@link #discard()} method was called */
		boolean isDiscarded();

		/**
		 * Compares this event's functionality with that of another event. If this method returns true, it signifies that this event's
		 * action is completely contained by the functionality of the given event such that this event may be discarded and not handled.
		 * This method must obey a contract similar to {@link Comparable#compareTo(Object)}, in that if A supersedes B and B supersedes C,
		 * then A supersedes C.
		 *
		 * @param evt The event to compare with
		 * @return Whether this event's functionality encompasses the functionality of the given event such that the given event need not be
		 *         acted on
		 */
		boolean isSupersededBy(Event evt);

		/**
		 * @param evt The event to supersede
		 * @return Whether the event was superseded by this event. This may be false even if {@link #isSupersededBy(Event)} returns true if
		 *         this event is already being or has been handled.
		 */
		boolean supersede(Event evt);

		/** @return This event's priority. This value must not change for the event. */
		int getPriority();

		/**
		 * Compares the priority of two events whose {@link #getPriority()} methods return the same value.
		 *
		 * @param evt The event to compare with
		 * @return The relative priority of this event compared to <code>o2</code>
		 */
		int comparePriority(Event evt);

		/** @param err The error that was thrown by this event */
		void handleError(Throwable err);
	}

	/** Implements the trivial parts of {@link Event} */
	public static abstract class AbstractEvent extends AbstractCausable implements Event {
		private final long theTime;

		private final int thePriority;

		private final Runnable [] thePostActions;

		private volatile boolean isHandling;

		private volatile boolean isHandled;

		private volatile boolean isDiscarded;

		/**
		 * @param priority The priority of this event
		 * @param postActions The actions to be performed after the event is handled successfully
		 */
		public AbstractEvent(int priority, Runnable... postActions) {
			super(null);
			theTime = System.currentTimeMillis();
			thePriority = priority;
			thePostActions = postActions;
		}

		@Override
		public long getTime() {
			return theTime;
		}

		@Override
		public boolean shouldHandle(long time) {
			return true;
		}

		@Override
		public final void handle() {
			isHandling = true;
			try {
				doHandleAction();
			} finally {
				isHandled = true;
				isHandling = false;
			}
			for(Runnable action : thePostActions)
				action.run();
		}

		/** Executes this event's action */
		protected abstract void doHandleAction();

		@Override
		public boolean discard() {
			if (isHandling || isHandled || isDiscarded)
				return false;
			isDiscarded = true;
			return true;
		}

		@Override
		public boolean isHandling() {
			return isHandling;
		}

		@Override
		public boolean isFinished() {
			return isHandled || isDiscarded;
		}

		@Override
		public boolean isDiscarded() {
			return isDiscarded;
		}

		@Override
		public boolean isSupersededBy(Event evt) {
			return false;
		}

		@Override
		public boolean supersede(Event evt) {
			return !(isHandling || isHandled || isDiscarded);
		}

		@Override
		public int getPriority() {
			return thePriority;
		}

		@Override
		public int comparePriority(Event evt) {
			return 0;
		}
	}

	/** Represents an element's need to be redrawn */
	public static class PaintEvent extends AbstractEvent {
		/** The priority of paint events */
		public static final int PRIORITY = 0;

		private final QuickElement theElement;

		private final Rectangle theArea;

		private final boolean isNow;

		/**
		 * @param element The element that needs to be repainted
		 * @param area The area in the element that needs to be repainted, or null if the element needs to be repainted entirely
		 * @param now Whether the repaint should happen quickly or be processed in normal time
		 * @param postActions The actions to be performed after the event is handled successfully
		 */
		public PaintEvent(QuickElement element, Rectangle area, boolean now, Runnable... postActions) {
			super(PRIORITY, postActions);
			theElement = element;
			theArea = area;
			isNow = now;
		}

		/** @return The element that needs to be repainted */
		public QuickElement getElement() {
			return theElement;
		}

		/** @return The area in the element that needs to be repainted, or null if the element needs to be repainted entirely */
		public Rectangle getArea() {
			return theArea;
		}

		/** @return Whether this event is meant to happen immediately or in normal time */
		public boolean isNow() {
			return isNow;
		}

		@Override
		public boolean shouldHandle(long time) {
			return isNow || time >= theElement.getPaintDirtyTime() + QuickEventQueue.get().getPaintDirtyTolerance();
		}

		@Override
		protected void doHandleAction() {
			QuickDocument doc = theElement.getDocument();
			QuickElement element = theElement;
			Rectangle area = theArea;
			if(element != doc.getRoot() && element.isTransparent()) {
				Point docPos = QuickUtils.getDocumentPosition(element);
				if(area == null)
					area = new Rectangle(element.getBounds().getSize());
				area.x += docPos.x;
				area.y += docPos.y;
				element = doc.getRoot();
			}
			if(area != null && area.getX() == 0 && area.getY() == 0 && area.getWidth() >= element.bounds().getWidth()
				&& area.getHeight() >= element.bounds().getHeight())
				area = null;
			if(element == doc.getRoot() && area == null) {
				QuickRendering render = new QuickRendering(element.bounds().getWidth(), element.bounds().getHeight());
				Graphics2D graphics = (Graphics2D) render.getImage().getGraphics();
				if(doc.getDebugGraphics() != null)
					graphics = new org.quick.util.AggregateGraphics(graphics, doc.getDebugGraphics());
				render.setRoot(element.paint(graphics, area));
				doc.setRender(render);
				Graphics2D docGraphics = doc.getGraphics();
				if (docGraphics != null) {
					docGraphics.drawImage(render.getImage(), 0, 0, null);
					doc.graphicsUpdated();
				}
				return;
			}
			QuickRendering render = element.getDocument().getRender();
			if(render == null)
				return;
			QuickElementCapture bound = render.getFor(element);
			if(bound == null) {
				// Hierarchy may have been restructured. Need to repaint everything.
				element.getDocument().getRoot().repaint(null, false);
				return;
			}
			QuickRendering newRender = render.clone();
			bound = newRender.getFor(element);
			Point trans = bound.getDocLocation();
			Graphics2D graphics = (Graphics2D) newRender.getImage().getGraphics();
			if(doc.getDebugGraphics() != null)
				graphics = new org.quick.util.AggregateGraphics(graphics, doc.getDebugGraphics());
			QuickElementCapture newBound;
			graphics.translate(trans.x, trans.y);
			try {
				newBound = element.paint(graphics, area);
			} finally {
				graphics.translate(-trans.x, -trans.y);
			}
			Graphics2D docGraphics = doc.getGraphics();
			if(docGraphics != null) {
				int x = trans.x;
				if(x < 0)
					x = 0;
				int y = trans.y;
				if(y < 0)
					y = 0;
				int w = newBound.getWidth();
				if(x + w > newRender.getImage().getWidth())
					w = newRender.getImage().getWidth() - trans.x;
				int h = newBound.getHeight();
				if(y + h > newRender.getImage().getHeight())
					h = newRender.getImage().getHeight() - trans.y;
				docGraphics.drawImage(newRender.getImage().getSubimage(x, y, w, h), x, y, null);
				doc.graphicsUpdated();
			}
			if(bound.getParent() != null)
				((java.util.List<QuickElementCapture>) bound.getParent().getChildren()).set(bound.getParent().getChildren().indexOf(bound),
					newBound);
			else
				newRender.setRoot(newBound);
			element.getDocument().setRender(newRender);
		}

		@Override
		public boolean isSupersededBy(Event evt) {
			if(!isNow) {
				if(evt instanceof ReboundEvent && QuickUtils.isAncestor(((ReboundEvent) evt).getElement(), theElement))
					return true;
				if(evt instanceof LayoutEvent && QuickUtils.isAncestor(((LayoutEvent) evt).getElement(), theElement))
					return true;
				if (evt instanceof SizeNeedsChangedEvent && QuickUtils.isAncestor(((SizeNeedsChangedEvent) evt).getElement(), theElement))
					return true;
			}
			if(!(evt instanceof PaintEvent))
				return false;
			PaintEvent paint = (PaintEvent) evt;

			if(!QuickUtils.isAncestor(paint.theElement, theElement))
				return false;
			if(paint.theArea == null)
				return true; // Element will be repainted with its ancestor

			Rectangle area = QuickUtils.relative(paint.theArea, paint.theElement, theElement);
			Rectangle area2 = theArea;
			if(area2 == null)
				area2 = theElement.bounds().getBounds();
			return area.contains(area2); // If area contains area 2, the element's area will be repainted with its ancestor
		}

		@Override
		public void handleError(Throwable err) {
			theElement.msg().error("Rendering error", err);
		}

		@Override
		public String toString() {
			return "Paint event for " + theElement;
		}
	}

	/** Represents an element's need to lay out its children */
	public static class LayoutEvent extends AbstractEvent {
		/** The priority of layout events */
		public static final int PRIORITY = 10;

		private final QuickElement theElement;

		private final boolean isNow;

		/**
		 * @param element The element that needs to be layed out
		 * @param now Whether the layout should happen quickly or be processed in normal time
		 * @param postActions The actions to be performed after the event is handled successfully
		 */
		public LayoutEvent(QuickElement element, boolean now, Runnable... postActions) {
			super(PRIORITY, postActions);
			theElement = element;
			isNow = now;
		}

		/** @return The element that needs to be layed out */
		public QuickElement getElement() {
			return theElement;
		}

		/** @return Whether this event is meant to happen immediately or in normal time */
		public boolean isNow() {
			return isNow;
		}

		@Override
		public boolean shouldHandle(long time) {
			return isNow || time >= theElement.getLayoutDirtyTime() + QuickEventQueue.get().getLayoutDirtyTolerance();
		}

		@Override
		protected void doHandleAction() {
			theElement.doLayout();
		}

		@Override
		public boolean isSupersededBy(Event evt) {
			if (evt instanceof ReboundEvent && QuickUtils.isAncestor(((ReboundEvent) evt).getElement(), theElement))
				return true;
			if (evt instanceof LayoutEvent && QuickUtils.isAncestor(((LayoutEvent) evt).getElement(), theElement))
				return true;
			if (evt instanceof SizeNeedsChangedEvent && QuickUtils.isAncestor(((SizeNeedsChangedEvent) evt).getElement(), theElement))
				return true;
			return false;
		}

		@Override
		public int comparePriority(Event evt) {
			if(!(evt instanceof LayoutEvent))
				return super.comparePriority(evt);
			if(QuickUtils.isAncestor(theElement, ((LayoutEvent) evt).theElement))
				return 1;
			if(QuickUtils.isAncestor(((LayoutEvent) evt).theElement, theElement))
				return -1;
			return QuickUtils.getDepth(((LayoutEvent) evt).theElement) - QuickUtils.getDepth(theElement);
		}

		@Override
		public void handleError(Throwable err) {
			theElement.msg().error("Layout error", err);
		}

		@Override
		public String toString() {
			return "Layout event for " + theElement;
		}
	}

	/**
	 * An event communicating that an element's size needs may have changed and its parent or an ancestor may need to lay its contents out
	 * again
	 */
	public static class SizeNeedsChangedEvent extends AbstractEvent {
		/** The priority of layout events */
		public static final int PRIORITY = 20;

		private final QuickElement theElement;

		/** @param element The element that this event is being fired in */
		public SizeNeedsChangedEvent(QuickElement element) {
			super(PRIORITY);
			theElement = element;
		}

		/** @return The element whose size needs have changed */
		public QuickElement getElement() {
			return theElement;
		}

		@Override
		protected void doHandleAction() {
			QuickElement child = theElement;
			QuickElement parent = theElement.getParent().get();
			while (parent != null) {
				if (!isInPreferred(org.quick.core.layout.Orientation.horizontal, parent, child)
					|| !isInPreferred(org.quick.core.layout.Orientation.vertical, parent, child)) {
					child = parent;
					parent = parent.getParent().get();
				} else
					break;
			}
			if (parent != null)
				parent.doLayout();
			else
				child.doLayout();
		}

		private static boolean isInPreferred(org.quick.core.layout.Orientation orient, QuickElement parent, QuickElement child) {
			org.quick.core.mgr.ElementBounds.ElementBoundsDimension dim = child.bounds().get(orient);
			int size = dim.getSize();
			int cross = child.bounds().get(orient.opposite()).getSize();
			int minPref = org.quick.core.layout.LayoutUtils.getSize(child, orient, org.quick.core.layout.LayoutGuideType.minPref,
				parent.bounds().get(orient).getSize(), cross, false, null);
			if (size < minPref)
				return false;
			int maxPref = org.quick.core.layout.LayoutUtils.getSize(child, orient, org.quick.core.layout.LayoutGuideType.maxPref,
				parent.bounds().get(orient).getSize(), cross, false, null);
			if (size > maxPref)
				return false;
			return true;
		}

		@Override
		public boolean isSupersededBy(Event evt) {
			if (evt instanceof ReboundEvent && QuickUtils.isAncestor(((ReboundEvent) evt).getElement(), theElement))
				return true;
			if (evt instanceof SizeNeedsChangedEvent && QuickUtils.isAncestor(((SizeNeedsChangedEvent) evt).getElement(), theElement))
				return true;
			return false;
		}

		@Override
		public int comparePriority(Event evt) {
			if (!(evt instanceof SizeNeedsChangedEvent))
				return super.comparePriority(evt);
			if (QuickUtils.isAncestor(theElement, ((SizeNeedsChangedEvent) evt).theElement))
				return 1;
			if (QuickUtils.isAncestor(((SizeNeedsChangedEvent) evt).theElement, theElement))
				return -1;
			return QuickUtils.getDepth(((SizeNeedsChangedEvent) evt).theElement) - QuickUtils.getDepth(theElement);
		}

		@Override
		public void handleError(Throwable err) {
			theElement.msg().error("Size change error", err);
		}

		@Override
		public String toString() {
			return "Size needs changed for " + theElement;
		}
	}

	/** Represents a request to set an element's bounds */
	public static class ReboundEvent extends AbstractEvent {
		/** The priority of rebound events */
		public static final int PRIORITY = 20;

		private final QuickElement theElement;

		private final Rectangle theBounds;

		/**
		 * @param element The element to set the bounds of
		 * @param bounds The bounds to set on the element
		 * @param postActions The actions to be performed after the event is handled successfully
		 */
		public ReboundEvent(QuickElement element, Rectangle bounds, Runnable... postActions) {
			super(PRIORITY, postActions);
			theElement = element;
			theBounds = bounds;
		}

		/** @return The element whose bounds need to be set */
		public QuickElement getElement() {
			return theElement;
		}

		/** @return The bounds that will be set on the element */
		public Rectangle getBounds() {
			return theBounds;
		}

		@Override
		protected void doHandleAction() {
			theElement.bounds().setBounds(theBounds.x, theBounds.y, theBounds.width, theBounds.height);
		}

		@Override
		public boolean isSupersededBy(Event evt) {
			return evt instanceof ReboundEvent && evt.getTime() > getTime();
		}

		@Override
		public boolean shouldHandle(long time) {
			return time - getTime() > 50;
		}

		@Override
		public void handleError(Throwable err) {
			theElement.msg().error("Rebound error", err);
		}

		@Override
		public String toString() {
			return "Bound event for " + theElement + " to " + theBounds;
		}
	}

	/** Represents a position event that was captured and needs to be propagated */
	public static class PositionQueueEvent extends AbstractEvent {
		/** The priority of mouse events */
		public static final int PRIORITY = 100;

		private final QuickElement theRoot;

		private final org.quick.core.event.PositionedUserEvent theEvent;

		private final boolean isDownward;

		/**
		 * @param root The root from which this event fires downward or to which it fires upward
		 * @param evt The position event to propagate
		 * @param downward Whether this event fires downward from the root to the deepest level or the reverse
		 * @param postActions The actions to be performed after the event is handled successfully
		 */
		public PositionQueueEvent(QuickElement root, org.quick.core.event.PositionedUserEvent evt, boolean downward, Runnable... postActions) {
			super(PRIORITY, postActions);
			theRoot = root;
			theEvent = evt;
			isDownward = downward;
		}

		/** @return The root from which this event fires downward or to which it fires upward */
		public QuickElement getRoot() {
			return theRoot;
		}

		/** @return The event to be propagated */
		public org.quick.core.event.PositionedUserEvent getEvent() {
			return theEvent;
		}

		/** @return Whether this event fires downward from the root to the deepest level or the reverse */
		public boolean isDownward() {
			return isDownward;
		}

		@Override
		protected void doHandleAction() {
			if(theEvent.getCapture() == null) { // Non-positioned event
				if(isDownward)
					for(QuickElement pathEl : QuickUtils.path(theEvent.getElement()))
						pathEl.events().fire(theEvent.copyFor(pathEl));
				else {
					QuickElement el = theEvent.getElement();
					while(el != null) {
						el.events().fire(theEvent.copyFor(el));
						el = el.getParent().get();
					}
				}
			} else
				for(QuickEventPositionCapture el : theEvent.getCapture().iterate(!isDownward))
					el.getElement().events().fire(theEvent.copyFor(el.getElement()));
		}

		@Override
		public void handleError(Throwable err) {
			theEvent.getElement().msg().error("User event error", err);
		}

		@Override
		public String toString() {
			return "User positioned event: " + theEvent;
		}
	}

	/** Represents a non-positioned user event that needs to be fired */
	public static class UserQueueEvent extends AbstractEvent {
		/** The priority of user events */
		public static int PRIORITY = 100;

		private final org.quick.core.event.UserEvent theEvent;

		private final boolean isDownward;

		/**
		 * @param evt The event to fire
		 * @param downward Whether the event should fire from root to deepest element or the reverse
		 * @param postActions The actions to be performed after the event is handled successfully
		 */
		public UserQueueEvent(org.quick.core.event.UserEvent evt, boolean downward, Runnable... postActions) {
			super(PRIORITY, postActions);
			theEvent = evt;
			isDownward = downward;
		}

		/** @return The event that needs to be fired */
		public org.quick.core.event.UserEvent getEvent() {
			return theEvent;
		}

		/** @return Whether the event will be fired from root to deepest element or the reverse */
		public boolean isDownward() {
			return isDownward;
		}

		@Override
		protected void doHandleAction() {
			if(isDownward)
				for(QuickElement pathEl : QuickUtils.path(theEvent.getElement()))
					pathEl.events().fire(theEvent.copyFor(pathEl));
			else {
				QuickElement el = theEvent.getElement();
				while(el != null) {
					el.events().fire(theEvent.copyFor(el));
					el = el.getParent().get();
				}
			}
		}

		@Override
		public void handleError(Throwable err) {
			theEvent.getElement().msg().error("User event error", err);
		}

		@Override
		public String toString() {
			return "User event: " + theEvent;
		}
	}

	private static final QuickEventQueue theInstance = new QuickEventQueue();

	/** @return The instance of the queue to use to schedule core events */
	public static QuickEventQueue get() {
		return theInstance;
	}

	/** @return Whether the current thread is the Quick event queue thread */
	public static boolean isEventThread() {
		QuickEventQueue inst = theInstance;
		if(inst == null)
			return false;
		return Thread.currentThread() == inst.theThread;
	}

	private Event [] theEvents;

	private java.util.Comparator<Event> theComparator;

	final Object theLock;

	private volatile Thread theThread;

	private volatile boolean isShuttingDown;

	volatile boolean isInterrupted;

	volatile boolean hasNewEvent;

	private long theFrequency;

	private long thePaintDirtyTolerance;

	private long theLayoutDirtyTolerance;

	private boolean isPrioritized;

	private org.qommons.ProgramTracker theTracker;

	private QuickEventQueue() {
		theEvents = new Event[0];
		theComparator = (Event o1, Event o2) -> {
			int diff = o1.getPriority() - o2.getPriority();
			if(diff != 0)
				return -diff;
			diff = o1.comparePriority(o2);
			if(diff != 0)
				return -diff;
			diff = o2.comparePriority(o1);
			if(diff != 0)
				return diff;
			long timeDiff = o1.getTime() - o2.getTime();
			return timeDiff < 0 ? -1 : (timeDiff > 0 ? 1 : 0);
		};
		theLock = new Object();
		theFrequency = 25;
		thePaintDirtyTolerance = 50;
		theLayoutDirtyTolerance = 50;
		isPrioritized = true;
		theTracker = new org.qommons.ProgramTracker("Quick Events");
	}

	/**
	 * Schedules an event
	 *
	 * @param event The event that Quick needs to take action on
	 * @param now Whether to take action on the event immediately or allow it to execute when the queue gets to it
	 */
	public void scheduleEvent(Event event, boolean now) {
		ProgramTracker.TrackNode schedule = null;
		if(DEBUG_TRACKING > 0 && isEventThread())
			schedule = theTracker.start("scheduleEvent");
		Event [] events = theEvents;
		for(Event evt : events) {
			if(evt.isHandling() || evt.isFinished())
				continue;
			if(event.isSupersededBy(evt)) {
				if (evt.supersede(event))
					event.discard();
				if(schedule != null)
					theTracker.end(schedule);
				return;
			} else if(evt.isSupersededBy(event)) {
				if (evt.discard())
					remove(evt, schedule != null);
			}
		}
		addEvent(event, now);
		if(schedule != null)
			theTracker.end(schedule);
	}

	private void addEvent(Event event, boolean now) {
		ProgramTracker.TrackNode add = null;
		if(DEBUG_TRACKING > 0 && isEventThread())
			add = theTracker.start("addEvent");
		int spot;
		synchronized(theLock) {
			if(isPrioritized) {
				spot = java.util.Arrays.binarySearch(theEvents, event, theComparator);
				if(spot < 0)
					spot = -(spot + 1);
			} else
				spot = theEvents.length;
			theEvents = ArrayUtils.add(theEvents, event, spot);
		}
		hasNewEvent = true;
		start();
		if(now && spot == 0) {
			isInterrupted = true;
		}
		if(theThread != null)
			theThread.interrupt();
		if(add != null)
			theTracker.end(add);
	}

	void remove(Event event, boolean debug) {
		ProgramTracker.TrackNode remove = null;
		if(debug)
			remove = theTracker.start("removeEvent");
		synchronized(theLock) {
			theEvents = ArrayUtils.remove(theEvents, event);
		}
		if(remove != null)
			theTracker.end(remove);
	}

	Event [] getEvents() {
		return theEvents;
	}

	private void start() {
		if(theThread != null)
			return;
		new EventQueueThread().start();
	}

	/**
	 * Can't think why this should be called, but if it's needed we can change the modifier to public or provide some mechanism to access it
	 */
	@SuppressWarnings("unused")
	private void shutdown() {
		isShuttingDown = true;
	}

	/** @return Whether this event queue is currently running */
	public boolean isRunning() {
		return theThread != null;
	}

	/** @return Whether this event queue is shutting down. Not currently used. */
	public boolean isShuttingDown() {
		return isShuttingDown;
	}

	/** @return The frequency with which this event queue handles its events */
	public long getFrequency() {
		return theFrequency;
	}

	/**
	 * @return The amount of time for which this queue will let paint events rest until
	 *         {@link QuickElement#repaint(Rectangle, boolean, Runnable...)} stops being called repeatedly
	 */
	public long getPaintDirtyTolerance() {
		return thePaintDirtyTolerance;
	}

	/**
	 * @return The amount of time for which this queue will let layout events rest until {@link QuickElement#relayout(boolean, Runnable...)}
	 *         stops being called repeatedly
	 */
	public long getLayoutDirtyTolerance() {
		return theLayoutDirtyTolerance;
	}

	/** @return The program tracker to use for debugging */
	public org.qommons.ProgramTracker track() {
		return theTracker;
	}

	private class EventQueueThread extends Thread {
		private long theTrackMark;

		EventQueueThread() {
			super("Quick Event Queue");
		}

		/**
		 * The queue thread�s action is to go through the events in the queue. When an event is come to, the dirty paint or layout state of
		 * the element is checked. If not dirty, remove the event and do nothing. If the dirty time is <=10ms ago, skip the event so that it
		 * is checked again the next 50ms. Heavy, multiple-op processes on elements from external threads will cause few layout/redraw
		 * actions this way, but after ops finish, layout/redraw will happen within 60ms, average 35ms.
		 */
		@Override
		public void run() {
			synchronized(theLock) {
				if(theThread != null)
					return;
				theThread = this;
			}
			if(DEBUG_TRACKING > 0)
				theTrackMark = System.currentTimeMillis();
			while(!isShuttingDown()) {
				processEvents();
				if(DEBUG_TRACKING > 0) {
					long now = System.currentTimeMillis();
					if(now - theTrackMark >= DEBUG_TRACKING) {
						theTrackMark = now;
						theTracker.printData(5);
						theTracker.clear();
					}
				}
			}
			theThread = null;
			isShuttingDown = false;
		}

		private void processEvents() {
			ProgramTracker.TrackNode processEvents = null;
			if(DEBUG_TRACKING > 0)
				processEvents = theTracker.start("processEvents");
			try {
				isInterrupted = false;
				ProgramTracker.TrackNode getEvents = null;
				if(DEBUG_TRACKING > 0)
					getEvents = theTracker.start("getEvents");
				Event [] events = getEvents();
				if(getEvents != null)
					theTracker.end(getEvents);
				hasNewEvent = false;
				boolean acted = false;
				long now = System.currentTimeMillis();
				for(Event evt : events) {
					if(isInterrupted) {
						break;
					}
					if(evt.isFinished())
						continue;
					if(evt.shouldHandle(now)) {
						acted = true;
						remove(evt, processEvents != null);
						ProgramTracker.TrackNode handle = null;
						if(DEBUG_TRACKING > 0)
							handle = theTracker.start("handleEvent " + evt);
						try{
							evt.handle();
							evt.finish();
						} catch(Throwable e){
							evt.handleError(e);
						}
						if(handle != null)
							theTracker.end(handle);
						now = System.currentTimeMillis(); // Update the time, since the action may have taken some
					}
				}
				if(processEvents != null)
					theTracker.end(processEvents);
				if(!acted && !hasNewEvent && !isInterrupted)
					Thread.sleep(getFrequency());
			} catch(InterruptedException e) {
			}
		}
	}
}
