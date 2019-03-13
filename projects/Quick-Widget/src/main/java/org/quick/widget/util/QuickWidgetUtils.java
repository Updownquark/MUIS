package org.quick.widget.util;

import java.util.ArrayList;

import org.observe.ObservableValueEvent;
import org.qommons.ArrayUtils;
import org.quick.widget.core.Point;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.Rectangle;
import org.quick.widget.core.event.UserEvent;

public class QuickWidgetUtils {
	/**
	 * @param element The element to get the most distant ancestor of
	 * @return The most distant ancestor of the given element
	 */
	public static QuickWidget getRoot(QuickWidget element) {
		QuickWidget parent = element.getParent().get();
		while (parent != null) {
			element = parent;
			parent = element.getParent().get();
		}
		return element;
	}

	/**
	 * @param element The element to get the path of
	 * @return The path from the root to the given element
	 */
	public static QuickWidget[] path(QuickWidget element) {
		ArrayList<QuickWidget> ret = new ArrayList<>();
		while (element != null) {
			ret.add(element);
			element = element.getParent().get();
		}
		return ArrayUtils.reverse(ret.toArray(new QuickWidget[ret.size()]));
	}

	/**
	 * @param element The element to get the document depth of
	 * @return The number of ancestors the given element has
	 */
	public static int getDepth(QuickWidget element) {
		int ret = 0;
		QuickWidget parent = element.getParent().get();
		while (parent != null) {
			ret++;
			element = parent;
			parent = element.getParent().get();
		}
		return ret;
	}

	/**
	 * @param ancestor The ancestor element to check
	 * @param descendant The descendant element to check
	 * @return Whether ancestor is the same as or an ancestor of descendant
	 */
	public static boolean isAncestor(QuickWidget ancestor, QuickWidget descendant) {
		QuickWidget parent = descendant;
		while (parent != null && parent != ancestor)
			parent = parent.getParent().get();
		return parent == ancestor;
	}

	/**
	 * @param el1 The first element
	 * @param el2 The second element
	 * @return The deepest element that is an ancestor of both arguments, or null if the two elements are not in the same document tree
	 */
	public static QuickWidget commonAncestor(QuickWidget el1, QuickWidget el2) {
		QuickWidget[] path1 = path(el1);
		QuickWidget test = el2;
		while (test != null && !ArrayUtils.contains(path1, test))
			test = test.getParent().get();
		return test;
	}

	/**
	 * Finds the point at which 2 elements branch from a common ancestor
	 *
	 * @param el1 The first element
	 * @param el2 The second element
	 * @return A 3-element array containing:
	 *         <ol>
	 *         <li>the common ancestor
	 *         <li>The ancestor's child that is an ancestor of <code>el1</code> or null if <code>el1</code> is the common ancestor</li>
	 *         <li>The ancestor's child that is an ancestor of <code>el2</code> or null if <code>el2</code> is the common ancestor</li>
	 *         </ol>
	 *         or null if there is no common ancestor between the two elements
	 */
	public static QuickWidget[] getBranchPoint(QuickWidget el1, QuickWidget el2) {
		QuickWidget[] path1 = path(el1);
		QuickWidget[] path2 = path(el2);
		if (path1.length == 0 || path2.length == 0 || path1[0] != path2[0])
			return null;
		int i;
		for (i = 0; i < path1.length && i < path2.length && path1[i] == path2[i]; i++)
			;
		QuickWidget branch1 = i < path1.length ? path1[i] : null;
		QuickWidget branch2 = i < path2.length ? path2[i] : null;
		return new QuickWidget[] { path1[i - 1], branch1, branch2 };
	}

	/**
	 * Translates a rectangle from one element's coordinates into another's
	 *
	 * @param area The area to translate--not modified
	 * @param el1 The element whose coordinates <code>area</code> is in. May be null if the area is in the document root's coordinate system
	 * @param el2 The element whose coordinates to translate <code>area</code> to
	 * @return <code>area</code> translated from <code>el1</code>'s coordinates to <code>el2</code>'s
	 */
	public static Rectangle relative(Rectangle area, QuickWidget el1, QuickWidget el2) {
		Point relP = relative(area.getPosition(), el1, el2);
		return new Rectangle(relP.x, relP.y, area.width, area.height);
	}

	/**
	 * Translates a point from one element's coordinates into another's
	 *
	 * @param point The point to translate--not modified
	 * @param el1 The element whose coordinates <code>point</code> is in. May be null if the point is in the document root's coordinate
	 *        system
	 * @param el2 The element whose coordinates to translate <code>point</code> to
	 * @return <code>point</code> translated from <code>el1</code>'s coordinates to <code>el2</code>'s
	 */
	public static Point relative(Point point, QuickWidget el1, QuickWidget el2) {
		if (el1 == null)
			el1 = getRoot(el2);
		int x = point.x;
		int y = point.y;
		QuickWidget common = commonAncestor(el1, el2);
		if (common == null)
			return null;
		QuickWidget parent = el2;
		while (parent != null && parent != common) {
			x -= parent.bounds().getX();
			y -= parent.bounds().getY();
			parent = parent.getParent().get();
		}
		parent = el1;
		while (parent != null && parent != common) {
			x += parent.bounds().getX();
			y += parent.bounds().getY();
			parent = parent.getParent().get();
		}
		return new Point(x, y);
	}

	/**
	 * @param element The element to get the position of
	 * @return The position of the top-left corner of the element relative to the document root
	 */
	public static Point getDocumentPosition(QuickWidget element) {
		int x = 0;
		int y = 0;
		QuickWidget el = element;
		QuickWidget parent = el.getParent().get();
		while (parent != null) {
			x += el.bounds().getX();
			y += el.bounds().getY();
			el = parent;
			parent = el.getParent().get();
		}
		return new Point(x, y);
	}

	/**
	 * @param event The observable event
	 * @return The user event that ultimately caused the observable event (may be null)
	 */
	public static UserEvent getUserEvent(ObservableValueEvent<?> event) {
		while (event != null) {
			if (event.getCause() instanceof UserEvent)
				return (UserEvent) event.getCause();
			else if (event.getCause() instanceof ObservableValueEvent)
				event = (ObservableValueEvent<?>) event.getCause();
			else
				event = null;
		}
		return null;
	}
}
