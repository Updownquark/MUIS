/* Created Feb 23, 2009 by Andrew Butler */
package org.muis.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.muis.core.event.FocusEvent;
import org.muis.core.event.KeyBoardEvent;
import org.muis.core.event.MouseEvent;
import org.muis.core.event.ScrollEvent;
import org.muis.core.mgr.MuisLocker;
import org.muis.core.mgr.MuisMessageCenter;
import org.muis.core.style.attach.DocumentStyleSheet;
import org.muis.core.style.attach.NamedStyleGroup;

import prisms.util.ArrayUtils;

/** Contains all data pertaining to a MUIS application */
public class MuisDocument {
	/** The different policies this document can take with regards to scrolling events */
	public static enum ScrollPolicy {
		/**
		 * With this policy, scroll events will fire as if the event came from the mouse pointer. The position fields will represent the
		 * position of the pointer when the scroll event was fired.
		 */
		MOUSE,
		/**
		 * With this policy, scroll events will fire as if the event came from the upper-left corner of the currently focused widget. The
		 * position fields will represent the (0, 0) position relative to that widget.
		 */
		FOCUS,
		/**
		 * With this policy, scroll events generated from a mouse wheel will fire the same as {@link #MOUSE}, while scroll events generated
		 * from the keyboard will fire the same as {@link #FOCUS}.
		 */
		MIXED;
	}

	/** Allows a MUIS document to retrieve graphics to draw itself on demand */
	public interface GraphicsGetter {
		/** @return The graphics object that this document should use at the moment */
		java.awt.Graphics2D getGraphics();

		/** @param cursor The cursor to set over the document */
		void setCursor(java.awt.Cursor cursor);
	}

	/** A listener to be notified when the rendering changes for a MUIS document */
	public interface RenderListener {
		/** @param doc The MUIS document whose rendering was changed */
		void renderUpdate(MuisDocument doc);
	}

	private final MuisEnvironment theEnvironment;

	private final java.net.URL theLocation;

	private final org.muis.core.parser.MuisParser theParser;

	private MuisClassView theClassView;

	private java.awt.Toolkit theAwtToolkit;

	private MuisHeadSection theHead;

	private BodyElement theRoot;

	private MuisMessageCenter theMessageCenter;

	private DocumentStyleSheet theDocumentStyle;

	private NamedStyleGroup [] theDocumentGroups;

	private ScrollPolicy theScrollPolicy;

	private MuisElement theFocus;

	private boolean hasMouse;

	private int theMouseX;

	private int theMouseY;

	private List<MouseEvent.ButtonType> thePressedButtons;

	private List<KeyBoardEvent.KeyCode> thePressedKeys;

	private final Object theButtonsLock;

	private final Object theKeysLock;

	private final MuisLocker theLocker;

	private volatile MuisRendering theRendering;

	private GraphicsGetter theGraphics;

	private GraphicsGetter theDebugGraphics;

	private java.util.Collection<RenderListener> theRenderListeners;

	/**
	 * Creates a document
	 *
	 * @param env The environment for the document
	 * @param parser The parser that created this document
	 * @param location The location of the file that this document was generated from
	 * @param head The head section for this document
	 */
	public MuisDocument(MuisEnvironment env, org.muis.core.parser.MuisParser parser, java.net.URL location, MuisHeadSection head) {
		theEnvironment = env;
		theParser = parser;
		theLocation = location;
		theHead = head;
		theAwtToolkit = java.awt.Toolkit.getDefaultToolkit();
		theMessageCenter = new MuisMessageCenter(env, this, null);
		theDocumentStyle = new DocumentStyleSheet(this);
		theDocumentGroups = new NamedStyleGroup[] {new NamedStyleGroup(this, "")};
		theScrollPolicy = ScrollPolicy.MOUSE;
		thePressedButtons = new java.util.concurrent.CopyOnWriteArrayList<>();
		thePressedKeys = new java.util.concurrent.CopyOnWriteArrayList<>();
		theButtonsLock = new Object();
		theKeysLock = new Object();
		theRoot = new BodyElement();
		theLocker = new MuisLocker();
		theRenderListeners = new java.util.concurrent.ConcurrentLinkedQueue<>();

		applyHead();
	}

	private void applyHead() {
		for(org.muis.core.style.sheet.ParsedStyleSheet styleSheet : theHead.getStyleSheets())
			theDocumentStyle.addStyleSheet(styleSheet);
	}

	/** @param classView The class view for the document */
	public void setClassView(MuisClassView classView) {
		if(theClassView != null)
			throw new IllegalStateException("A document's class view may only be set once");
		theClassView = classView;
	}

	/** @param graphics The getter for graphics to be redrawn when the rendering is updated */
	public void setGraphics(GraphicsGetter graphics) {
		theGraphics = graphics;
	}

	/** @return Graphics to be updated on repaint. May be null. */
	public java.awt.Graphics2D getGraphics() {
		return theGraphics == null ? null : theGraphics.getGraphics();
	}

	/** @param graphics The getter for graphics to be used in debugging (drawn piece-wise instead of in batches) */
	public void setDebugGraphics(GraphicsGetter graphics) {
		theDebugGraphics = graphics;
	}

	/** @return The graphics to be used in debugging */
	public java.awt.Graphics2D getDebugGraphics() {
		return theDebugGraphics == null ? null : theDebugGraphics.getGraphics();
	}

	/** @param listener The listener to be notified when the rendering of this document changes */
	public void addRenderListener(RenderListener listener) {
		if(listener != null)
			theRenderListeners.add(listener);
	}

	/** @param listener The listener to stop notifying */
	public void removeRenderListener(RenderListener listener) {
		theRenderListeners.remove(listener);
	}

	/** @return The environment that this document was created in */
	public MuisEnvironment getEnvironment() {
		return theEnvironment;
	}

	/** @return The parser that created this document */
	public org.muis.core.parser.MuisParser getParser() {
		return theParser;
	}

	/** @return The class map that applies to the whole document */
	public MuisClassView getClassView() {
		return theClassView;
	}

	/** @return The location of the file that this document was generated from */
	public java.net.URL getLocation() {
		return theLocation;
	}

	/** @return The head section of this document */
	public MuisHeadSection getHead() {
		return theHead;
	}

	/** @return The style sheet for this document */
	public DocumentStyleSheet getStyle() {
		return theDocumentStyle;
	}

	/** @return The locker to keep track of element locks */
	public MuisLocker getLocker() {
		return theLocker;
	}

	/** @return The root element of the document */
	public BodyElement getRoot() {
		return theRoot;
	}

	/** @return The number of named groups that exist in this document */
	public int getGroupCount() {
		return theDocumentGroups.length;
	}

	/** @return An Iterable to iterate through this document's groups */
	public Iterable<NamedStyleGroup> groups() {
		return () -> {
			return new GroupIterator(theDocumentGroups);
		};
	}

	/**
	 * @param name The name of the group to determine existence of
	 * @return Whether a group with the given name exists in this document
	 */
	public boolean hasGroup(String name) {
		for(NamedStyleGroup group : theDocumentGroups)
			if(group.getName().equals(name))
				return true;
		return false;
	}

	/**
	 * Gets a group by name, or creates one if no such group exists
	 *
	 * @param name The name of the group to get or create
	 * @return The group in this document with the given name. Will never be null.
	 */
	public NamedStyleGroup getGroup(String name) {
		for(NamedStyleGroup group : theDocumentGroups)
			if(group.getName().equals(name))
				return group;
		NamedStyleGroup ret = new NamedStyleGroup(this, name);
		theDocumentGroups = ArrayUtils.add(theDocumentGroups, ret);
		java.util.Arrays.sort(theDocumentGroups, (NamedStyleGroup g1, NamedStyleGroup g2) -> {
			return g1.getName().compareToIgnoreCase(g2.getName());
		});
		return ret;
	}

	/**
	 * Removes a group from a document. This will remove the group from every element that is a member of the group as well.
	 *
	 * @param name The name of the group to remove from this document
	 */
	public void removeGroup(String name) {
		if("".equals(name))
			throw new IllegalArgumentException("Cannot remove the unnamed group from the document");
		for(NamedStyleGroup group : theDocumentGroups)
			if(group.getName().equals(name)) {
				for(MuisElement el : group.members())
					el.getStyle().removeGroup(group);
				theDocumentGroups = ArrayUtils.remove(theDocumentGroups, group);
				break;
			}
	}

	/** Called to initialize the document after all the parsing and linking has been performed */
	public void postCreate() {
		theRoot.postCreate();
	}

	/** @return This document's message center */
	public MuisMessageCenter getMessageCenter() {
		return theMessageCenter;
	}

	/**
	 * Short-hand for {@link #getMessageCenter()}
	 *
	 * @return This document's message center
	 */
	public MuisMessageCenter msg() {
		return getMessageCenter();
	}

	/** @return The policy that this document uses to dispatch scroll events */
	public ScrollPolicy getScrollPolicy() {
		return theScrollPolicy;
	}

	/** @param policy The policy that this document should use to dispatch scroll events */
	public void setScrollPolicy(ScrollPolicy policy) {
		theScrollPolicy = policy;
	}

	/**
	 * Sets the size that this document can render its content in
	 *
	 * @param width The width of the document size
	 * @param height The height of the document size
	 */
	public void setSize(int width, int height) {
		MuisEventQueue.get().scheduleEvent(new MuisEventQueue.ReboundEvent(theRoot, new java.awt.Rectangle(0, 0, width, height)), true);
	}

	/** @return The most recent rendering of this document */
	public MuisRendering getRender() {
		return theRendering;
	}

	void setRender(MuisRendering render) {
		theRendering = render;
		for(RenderListener listener : theRenderListeners)
			listener.renderUpdate(this);
	}

	/**
	 * Renders this MUIS document in a graphics context
	 *
	 * @param graphics The graphics context to render in
	 */
	public void paint(java.awt.Graphics2D graphics) {
		MuisRendering rendering = theRendering;
		if(rendering != null)
			graphics.drawImage(rendering.getImage(), null, 0, 0);
	}

	/** @return Whether the mouse is over this document */
	public boolean hasMouse() {
		return hasMouse;
	}

	/**
	 * @return The x-coordinate of either the mouse's current position relative to the document (if {@link #hasMouse()} is true) or the
	 *         mouse's position where it exited the document (if {@link #hasMouse()} is false).
	 */
	public int getMouseX() {
		return theMouseX;
	}

	/**
	 * @return The y-coordinate of either the mouse's current position relative to the document (if {@link #hasMouse()} is true) or the
	 *         mouse's position where it exited the document (if {@link #hasMouse()} is false).
	 */
	public int getMouseY() {
		return getMouseY();
	}

	/**
	 * @param button The mouse button to check
	 * @return Whether the mouse button is currently pressed
	 */
	public boolean isButtonPressed(MouseEvent.ButtonType button) {
		return ArrayUtils.contains(thePressedButtons, button);
	}

	/** @return All mouse buttons that are currently pressed */
	public List<MouseEvent.ButtonType> getPressedButtons() {
		return java.util.Collections.unmodifiableList(thePressedButtons);
	}

	/**
	 * @param code The key code to check
	 * @return Whether the key with the given code is currently pressed
	 */
	public boolean isKeyPressed(KeyBoardEvent.KeyCode code) {
		return ArrayUtils.contains(thePressedKeys, code);
	}

	/** @return All key codes whose keys are currently pressed */
	public List<KeyBoardEvent.KeyCode> getPressedKeys() {
		return java.util.Collections.unmodifiableList(thePressedKeys);
	}

	/** @return Whether a shift button is currently pressed */
	public boolean isShiftPressed() {
		return isKeyPressed(KeyBoardEvent.KeyCode.SHIFT_LEFT) || isKeyPressed(KeyBoardEvent.KeyCode.SHIFT_RIGHT);
	}

	/** @return Whether a control button is currently pressed */
	public boolean isControlPressed() {
		return isKeyPressed(KeyBoardEvent.KeyCode.CTRL_LEFT) || isKeyPressed(KeyBoardEvent.KeyCode.CTRL_RIGHT);
	}

	/** @return Whether an alt button is currently pressed */
	public boolean isAltPressed() {
		return isKeyPressed(KeyBoardEvent.KeyCode.ALT_LEFT) || isKeyPressed(KeyBoardEvent.KeyCode.ALT_RIGHT);
	}

	/** @return Whether caps lock is toggled on at the moment */
	public boolean isCapsLocked() {
		return theAwtToolkit.getLockingKeyState(java.awt.event.KeyEvent.VK_CAPS_LOCK);
	}

	/** @return Whether num lock is toggled on at the moment */
	public boolean isNumLocked() {
		return theAwtToolkit.getLockingKeyState(java.awt.event.KeyEvent.VK_NUM_LOCK);
	}

	/** @return Whether scroll lock is toggled on at the moment */
	public boolean isScrollLocked() {
		return theAwtToolkit.getLockingKeyState(java.awt.event.KeyEvent.VK_SCROLL_LOCK);
	}

	/** @return Whether kana lock is toggled on at the moment (for Japanese keyboard layout) */
	public boolean isKanaLocked() {
		return theAwtToolkit.getLockingKeyState(java.awt.event.KeyEvent.VK_KANA_LOCK);
	}

	/**
	 * Called when an element detects that its cursor style may have changed
	 *
	 * @param element The element on which the change was detected
	 */
	public void cursorChanged(MuisElement element) {
		if(theGraphics == null)
			return;
		MuisRendering rendering = theRendering;
		if(rendering == null)
			return;
		MuisEventPositionCapture capture = rendering.capture(theMouseX, theMouseY);
		if(capture.find(element) == null)
			return;
		setCursor(capture.getTarget().getElement());
	}

	private void setCursor(MuisElement element) {
		if(theGraphics == null)
			return;
		theGraphics.setCursor(element.getStyle().getSelf().get(org.muis.core.style.BackgroundStyle.cursor));
	}

	/**
	 * Emulates a mouse event on the document
	 *
	 * @param x The x-coordinate where the event occurred
	 * @param y The y-coordinate where the event occurred
	 * @param type The type of the event
	 * @param buttonType The button that caused the event
	 * @param clickCount The click count for the event
	 */
	public void mouse(int x, int y, MouseEvent.MouseEventType type, MouseEvent.ButtonType buttonType, int clickCount) {
		boolean oldHasMouse = hasMouse;
		int oldX = theMouseX;
		int oldY = theMouseY;
		hasMouse = type != MouseEvent.MouseEventType.exited;
		theMouseX = x;
		theMouseY = y;
		MuisRendering rendering = theRendering;
		if(rendering == null)
			return;
		MuisEventPositionCapture newCapture = rendering.capture(x, y);
		MuisEventPositionCapture oldCapture = rendering.capture(oldX, oldY);
		MouseEvent evt = new MouseEvent(this, newCapture.getTarget().getElement(), type, buttonType, clickCount, thePressedButtons,
			thePressedKeys, newCapture);

		ArrayList<MuisEventQueue.Event> events = new ArrayList<>();

		switch (type) {
		case moved:
			// This means it moved within the document. We have to determine any elements that it might have exited or entered.
			if(oldHasMouse)
				mouseMove(oldCapture, newCapture, events);
			else {
				evt = new MouseEvent(this, newCapture.getTarget().getElement(), MouseEvent.MouseEventType.entered, buttonType, clickCount,
					thePressedButtons, thePressedKeys,
					newCapture);
				events.add(new MuisEventQueue.PositionQueueEvent(theRoot, evt, true));
			}
			break;
		case pressed:
			synchronized(theButtonsLock) {
				if(!thePressedButtons.contains(buttonType))
					thePressedButtons.add(buttonType);
			}
			focusByMouse(newCapture, events);
			events.add(new MuisEventQueue.PositionQueueEvent(theRoot, evt, false));
			break;
		case released:
			synchronized(theButtonsLock) {
				thePressedButtons.remove(buttonType);
			}
			events.add(new MuisEventQueue.PositionQueueEvent(theRoot, evt, false));
			break;
		case clicked:
		case exited:
			events.add(new MuisEventQueue.PositionQueueEvent(theRoot, new MouseEvent(this, oldCapture.getTarget().getElement(), type,
				buttonType, clickCount, thePressedButtons, thePressedKeys, oldCapture), false));
			break;
		case entered:
			events.add(new MuisEventQueue.PositionQueueEvent(theRoot, evt, true));
			break;
		}
		for(MuisEventQueue.Event event : events)
			MuisEventQueue.get().scheduleEvent(event, true);
	}

	/** Checks the mouse's current position, firing necessary mouse events if it has moved relative to any elements */
	private void mouseMove(MuisEventPositionCapture oldCapture, MuisEventPositionCapture newCapture,
		java.util.List<MuisEventQueue.Event> events) {
		LinkedHashSet<MuisEventPositionCapture> oldSet = new LinkedHashSet<>();
		LinkedHashSet<MuisEventPositionCapture> newSet = new LinkedHashSet<>();
		LinkedHashSet<MuisEventPositionCapture> common = new LinkedHashSet<>();
		for(MuisEventPositionCapture mec : oldCapture.iterate(true))
			oldSet.add(mec);
		for(MuisEventPositionCapture mec : newCapture.iterate(true))
			if(oldSet.remove(mec))
				common.add(mec);
			else
				newSet.add(mec);
		// Remove child elements
		java.util.Iterator<MuisEventPositionCapture> iter = oldSet.iterator();
		while(iter.hasNext()) {
			MuisEventPositionCapture mec = iter.next();
			if(oldSet.contains(mec.getParent()))
				iter.remove();
		}
		iter = newSet.iterator();
		while(iter.hasNext()) {
			MuisEventPositionCapture mec = iter.next();
			if(newSet.contains(mec.getParent()))
				iter.remove();
		}
		// Fire exit events
		for(MuisEventPositionCapture mec : oldSet) {
			MouseEvent exit = new MouseEvent(this, mec.getTarget().getElement(), MouseEvent.MouseEventType.exited, null, 0,
				thePressedButtons, thePressedKeys, mec);
			events.add(new MuisEventQueue.PositionQueueEvent(exit.getElement(), exit, false));
		}
		for(MuisEventPositionCapture mec : common) {
			MouseEvent move = new MouseEvent(this, mec.getTarget().getElement(), MouseEvent.MouseEventType.moved, null, 0,
				thePressedButtons, thePressedKeys, mec);
			events.add(new MuisEventQueue.PositionQueueEvent(move.getElement(), move, false));
		}
		// Fire enter events
		for(MuisEventPositionCapture mec : newSet) {
			MouseEvent enter = new MouseEvent(this, mec.getTarget().getElement(), MouseEvent.MouseEventType.entered, null, 0,
				thePressedButtons, thePressedKeys, mec);
			MuisEventQueue.get().scheduleEvent(new MuisEventQueue.PositionQueueEvent(enter.getElement(), enter, false), true);
		}
		setCursor(newCapture.getTarget().getElement());
	}

	private void focusByMouse(MuisEventPositionCapture capture, java.util.List<MuisEventQueue.Event> events) {
		for(MuisEventPositionCapture mec : capture.iterate(true))
			if(mec.getElement().isFocusable()) {
				setFocus(mec.getElement());
				return;
			}
	}

	/**
	 * Sets the document's focused element. This method does not invoke {@link MuisElement#isFocusable()}, so this will work on any element.
	 *
	 * @param toFocus The element to give the focus to
	 */
	public void setFocus(MuisElement toFocus) {
		ArrayList<MuisEventQueue.Event> events = new ArrayList<>();
		setFocus(toFocus, events);
		for(MuisEventQueue.Event event : events)
			MuisEventQueue.get().scheduleEvent(event, true);
	}

	private void setFocus(MuisElement focus, java.util.List<MuisEventQueue.Event> events) {
		MuisElement oldFocus = theFocus;
		theFocus = focus;
		if(oldFocus != theFocus) {
			if(oldFocus != null)
				events.add(new MuisEventQueue.UserQueueEvent(new FocusEvent(this, oldFocus, thePressedButtons, thePressedKeys, false),
					false));
			if(theFocus != null)
				events
					.add(new MuisEventQueue.UserQueueEvent(new FocusEvent(this, theFocus, thePressedButtons, thePressedKeys, true), false));
		}
	}

	/** Moves this document's focus to the focusable widget previous to the currently focused widget */
	public void backupFocus() {
		if(theFocus == null)
			return;
		if(searchFocus(theFocus, false))
			return;
		/* If we get here, then there was no previous focusable element. We must wrap around to the last focusable element. */
		MuisElement deepest = getDeepestElement(theRoot, false);
		searchFocus(deepest, false);
	}

	/** Moves this document's focus to the focusable widget after the currently focused widget */
	public void advanceFocus() {
		if(theFocus == null)
			return;
		if(searchFocus(theFocus, true))
			return;
		/* If we get here, then there was no next focusable element. We must wrap around to the first focusable element. */
		MuisElement deepest = getDeepestElement(theRoot, true);
		searchFocus(deepest, true);
	}

	boolean searchFocus(MuisElement el, boolean forward) {
		MuisElement lastChild = theFocus;
		MuisElement parent = theFocus.getParent();
		while(parent != null) {
			MuisElement [] children = parent.getChildren().sortByZ();
			if(!forward) {
				ArrayUtils.reverse(children);
			}
			boolean foundLastChild = false;
			for(int c = 0; c < children.length; c++)
				if(foundLastChild) {
					MuisElement deepest = getDeepestElement(children[c], forward);
					if(deepest != children[c]) {
						// Do searchFocus from this deep element
						parent = deepest;
						break;
					} else if(children[c].isFocusable()) {
						setFocus(children[c]);
						return true;
					}
				} else if(children[c] == lastChild)
					foundLastChild = true;
			if(parent.isFocusable()) {
				setFocus(parent);
				return true;
			}
			lastChild = parent;
			parent = parent.getParent();
		}
		return false;
	}

	static MuisElement getDeepestElement(MuisElement root, boolean first) {
		while(!root.ch().isEmpty())
			if(first)
				root = root.ch().get(0);
			else
				root = root.ch().getLast();
		return root;
	}

	/**
	 * Emulates a scroll event on the document
	 *
	 * @param x The x-coordinate where the event occurred
	 * @param y The y-coordinate where the event occurred
	 * @param amount The amount that the mouse wheel was scrolled
	 */
	public void scroll(int x, int y, int amount) {
		ScrollEvent evt = null;
		MuisElement element = null;
		MuisRendering rendering = theRendering;
		if(rendering == null)
			return;
		switch (theScrollPolicy) {
		case MOUSE:
		case MIXED:
			MuisEventPositionCapture capture = rendering.capture(x, y);
			evt = new ScrollEvent(this, element, ScrollEvent.ScrollType.UNIT, true, amount, null, thePressedButtons, thePressedKeys,
				capture);
			break;
		case FOCUS:
			element = theFocus;
			if(element == null)
				element = theRoot;
			evt = new ScrollEvent(this, element, ScrollEvent.ScrollType.UNIT, true, amount, null, thePressedButtons, thePressedKeys, null);
			break;
		}
		MuisEventQueue.get().scheduleEvent(new MuisEventQueue.PositionQueueEvent(theRoot, evt, false), true);
	}

	/**
	 * Emulates a key event on this document
	 *
	 * @param code The key code of the event
	 * @param pressed Whether the key was pressed or released
	 */
	public void keyed(KeyBoardEvent.KeyCode code, boolean pressed) {
		final KeyBoardEvent evt;
		if(theFocus != null)
			evt = new KeyBoardEvent(this, theFocus, code, thePressedButtons, thePressedKeys, pressed);
		else
			evt = new KeyBoardEvent(this, theRoot, code, thePressedButtons, thePressedKeys, pressed);
		final MuisRendering rendering = theRendering;

		synchronized(theKeysLock) {
			if(pressed) {
				if(!thePressedKeys.contains(code))
					thePressedKeys.add(code);
			} else if(ArrayUtils.contains(thePressedKeys, code))
				thePressedKeys.remove(code);
		}
		if(theFocus != null)
			MuisEventQueue.get().scheduleEvent(new MuisEventQueue.UserQueueEvent(evt, false, () -> {
				if(!evt.isUsed())
					scroll(evt, rendering);
			}), true);
		else
			scroll(evt, rendering);
	}

	private void scroll(KeyBoardEvent evt, MuisRendering rendering) {
		MuisEventPositionCapture capture = null;
		if(!evt.isUsed()) {
			MuisElement scrollElement = null;
			switch (theScrollPolicy) {
			case MOUSE:
				if(hasMouse && rendering != null) {
					capture = rendering.capture(theMouseX, theMouseY);
					scrollElement = capture.getTarget().getElement();
				} else
					scrollElement = null;
				break;
			case FOCUS:
			case MIXED:
				if(theFocus != null)
					scrollElement = theFocus;
				else
					scrollElement = theRoot;
				break;
			}
			if(scrollElement != null) {
				ScrollEvent.ScrollType scrollType = null;
				boolean vertical = true, downOrRight = true;
				switch (evt.getKeyCode()) {
				case LEFT_ARROW:
					scrollType = ScrollEvent.ScrollType.UNIT;
					vertical = false;
					downOrRight = false;
					break;
				case RIGHT_ARROW:
					scrollType = ScrollEvent.ScrollType.UNIT;
					vertical = false;
					downOrRight = true;
					break;
				case UP_ARROW:
					scrollType = ScrollEvent.ScrollType.UNIT;
					vertical = true;
					downOrRight = false;
					break;
				case DOWN_ARROW:
					scrollType = ScrollEvent.ScrollType.UNIT;
					vertical = true;
					downOrRight = true;
					break;
				case PAGE_UP:
					scrollType = ScrollEvent.ScrollType.BLOCK;
					vertical = true;
					downOrRight = false;
					break;
				case PAGE_DOWN:
					scrollType = ScrollEvent.ScrollType.BLOCK;
					vertical = true;
					downOrRight = true;
					break;
				default:
					scrollType = null;
				}
				if(scrollType != null) {
					ScrollEvent scrollEvt = new ScrollEvent(this, scrollElement, scrollType, vertical, downOrRight ? 1 : -1, evt,
						thePressedButtons, thePressedKeys, capture);
					if(capture != null)
						MuisEventQueue.get().scheduleEvent(new MuisEventQueue.PositionQueueEvent(scrollElement, scrollEvt, false), true);
					else
						MuisEventQueue.get().scheduleEvent(new MuisEventQueue.UserQueueEvent(scrollEvt, false), true);
				}
			}

			if(evt.getKeyCode() == KeyBoardEvent.KeyCode.TAB)
				if(isShiftPressed())
					backupFocus();
				else
					advanceFocus();
		}
	}

	/**
	 * Emulates textual input to the document
	 *
	 * @param c The character that was input
	 */
	public void character(char c) {
		org.muis.core.event.CharInputEvent evt = null;
		if(theFocus != null)
			evt = new org.muis.core.event.CharInputEvent(this, theFocus, thePressedButtons, thePressedKeys, c);
		else
			evt = new org.muis.core.event.CharInputEvent(this, theRoot, thePressedButtons, thePressedKeys, c);
		MuisEventQueue.get().scheduleEvent(new MuisEventQueue.UserQueueEvent(evt, false), true);
	}

	private static class GroupIterator implements java.util.Iterator<NamedStyleGroup> {
		private final NamedStyleGroup [] theGroups;

		private int theIndex;

		GroupIterator(NamedStyleGroup [] groups) {
			theGroups = groups;
		}

		@Override
		public boolean hasNext() {
			return theIndex < theGroups.length;
		}

		@Override
		public NamedStyleGroup next() {
			NamedStyleGroup ret = theGroups[theIndex];
			theIndex++;
			return ret;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Document's group iterator does not support modification");
		}
	}
}
