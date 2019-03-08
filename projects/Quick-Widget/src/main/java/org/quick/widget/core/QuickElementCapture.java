package org.quick.widget.core;

import java.util.Iterator;

import org.qommons.IterableUtils;
import org.quick.core.Point;
import org.quick.core.QuickElement;
import org.quick.widget.util.QuickWidgetUtils;

/** Represents a capture of an element's bounds and hierarchy at a point in time */
public class QuickElementCapture implements Cloneable, org.qommons.Sealable {
	/** Allows custom conversion between coordinate systems of parent and child */
	public static interface Transformer {
		/**
		 * @param parent The parent capture whose coordinate system <code>pos</code> is in
		 * @param child The child whose coordinate system to convert to
		 * @param pos The point to convert
		 * @return The given point, in the child's coordinate system
		 */
		Point getChildPosition(QuickElementCapture parent, QuickElementCapture child, Point pos);

		/**
		 * @param parent The parent capture whose coordinate system to convert to
		 * @param child The child whose coordinate system <code>pos</code> is in
		 * @param pos The point to convert
		 * @return The given point, in the parent's coordinate system
		 */
		Point getParentPosition(QuickElementCapture parent, QuickElementCapture child, Point pos);
	}

	private QuickElementCapture theParent;

	private final QuickWidget theWidget;

	private final Transformer theTransformer;

	/** The x-coordinate of the screen point relative to this element */
	private final int theX;

	/** The y-coordinate of the screen point relative to this element */
	private final int theY;

	private final int theZ;

	private final int theWidth;

	private final int theHeight;

	private java.util.List<QuickElementCapture> theChildren;

	private boolean isSealed;

	/**
	 * @param p This capture element's parent in the hierarchy
	 * @param widget The element that this structure is a capture of
	 * @param xPos The x-coordinate of the element's upper-left corner
	 * @param yPos The y-coordinate of the element's upper-left corner
	 * @param zIndex The z-index of the element
	 * @param w The width of the element
	 * @param h The height of the element
	 */
	public QuickElementCapture(QuickElementCapture p, QuickWidget widget, int xPos, int yPos, int zIndex, int w, int h) {
		this(p, widget, null, xPos, yPos, zIndex, w, h);
	}

	/**
	 * @param p This capture element's parent in the hierarchy
	 * @param widget The widget that this structure is a capture of
	 * @param transformer The transformer to transform between this capture's coordinates and those of one of its children
	 * @param xPos The x-coordinate of the element's upper-left corner
	 * @param yPos The y-coordinate of the element's upper-left corner
	 * @param zIndex The z-index of the element
	 * @param w The width of the element
	 * @param h The height of the element
	 */
	public QuickElementCapture(QuickElementCapture p, QuickWidget widget, Transformer transformer, int xPos, int yPos, int zIndex, int w,
		int h) {
		theParent = p;
		theWidget = widget;
		theTransformer = transformer;
		theX = xPos;
		theY = yPos;
		theZ = zIndex;
		theWidth = w;
		theHeight = h;
		theChildren = new java.util.ArrayList<>(3);
	}

	/** @return The transformer to transform between this capture's coordinates and those of one of its children */
	public Transformer getTransformer() {
		return theTransformer;
	}

	/**
	 * @param child The child to add to this capture
	 * @throws SealedException If this capture has been sealed
	 */
	public void addChild(QuickElementCapture child) throws SealedException {
		if(isSealed)
			throw new SealedException(this);
		theChildren.add(child);
	}

	/** @return The number of children this capture has--that is, the number of children this capture's element had at the moment of capture */
	public int getChildCount() {
		return theChildren.size();
	}

	/**
	 * @param index The index of the child to get
	 * @return This capture's child at the given index
	 */
	public QuickElementCapture getChild(int index) {
		return theChildren.get(index);
	}

	/** @return An iterator over this capture's immediate children */
	public java.util.List<? extends QuickElementCapture> getChildren() {
		return theChildren;
	}

	/** @return An iterator of each end point (leaf node) in this hierarchy */
	public Iterable<? extends QuickElementCapture> getTargets() {
		if(theChildren.isEmpty())
			return new Iterable<QuickElementCapture>() {
				@Override
				public Iterator<QuickElementCapture> iterator() {
					return new SelfIterator();
				}
			};
		else
			return new Iterable<QuickElementCapture>() {
				@Override
				public Iterator<QuickElementCapture> iterator() {
					return new QuickCaptureIterator(false, true);
				}
			};
	}

	/**
	 * @param depthFirst Whether to iterate depth-first or breadth-first
	 * @return An iterable to iterate over every element in this hierarchy
	 */
	public Iterable<? extends QuickElementCapture> iterate(final boolean depthFirst) {
		return new Iterable<QuickElementCapture>() {
			@Override
			public Iterator<QuickElementCapture> iterator() {
				return new QuickCaptureIterator(true, depthFirst);
			}
		};
	}

	/**
	 * @param widget The widget to search for
	 * @return The capture of the given element in this hierarchy, or null if the given element was not located in this capture
	 */
	public QuickElementCapture find(QuickWidget widget) {
		if(theParent != null)
			return getRoot().find(widget);
		QuickWidget[] path = QuickWidgetUtils.path(widget);
		QuickElementCapture ret = this;
		int pathIdx;
		for(pathIdx = 1; pathIdx < path.length; pathIdx++) {
			boolean found = false;
			for(QuickElementCapture child : ret.theChildren)
				if(child.getWidget() == path[pathIdx]) {
					found = true;
					ret = child;
					break;
				}
			if(!found)
				return null;
		}
		return ret;
	}

	/** @return The last end point (leaf node) in this hierarchy */
	public QuickElementCapture getTarget() {
		if(theChildren.isEmpty())
			return this;
		return theChildren.get(theChildren.size() - 1).getTarget();
	}

	@Override
	public boolean isSealed() {
		return isSealed;
	}

	@Override
	public void seal() {
		theChildren = java.util.Collections.unmodifiableList(theChildren);
		isSealed = true;
	}

	@Override
	public final int hashCode() {
		return getWidget().hashCode();
	}

	@Override
	public final boolean equals(Object obj) {
		return obj instanceof QuickElementCapture && ((QuickElementCapture) obj).getWidget().equals(getWidget());
	}

	/** @return The root of this capture */
	public QuickElementCapture getRoot() {
		QuickElementCapture parent = theParent;
		while(parent.getParent() != null)
			parent = parent.getParent();
		return parent;
	}

	/** @return This capture element's parent in the hierarchy */
	public QuickElementCapture getParent() {
		return theParent;
	}

	/** @param parent The parent element's capture */
	public void setParent(QuickElementCapture parent) {
		if(isSealed)
			throw new SealedException(this);
		theParent = parent;
	}

	/** @return The widget that this capture represents */
	public QuickWidget getWidget() {
		return theWidget;
	}

	/** @return The x-coordinate of the element */
	public int getX() {
		return theX;
	}

	/** @return The y-coordinate of the element */
	public int getY() {
		return theY;
	}

	/** @return The z-index of the element */
	public int getZ() {
		return theZ;
	}

	/** @return The width of the element */
	public int getWidth() {
		return theWidth;
	}

	/** @return The height of the element */
	public int getHeight() {
		return theHeight;
	}

	/** @return The location of the top left corner of this element relative to the document's top left corner */
	public Point getDocLocation() {
		Point ret = new Point(theX, theY);
		QuickElementCapture parent = theParent;
		while(parent != null) {
			ret = parent.getParentIntersection(this, ret);
			parent = parent.theParent;
		}
		return ret;
	}

	/**
	 * @param pos The point to check
	 * @return Whether this element's capture is {@link QuickElement#isClickThrough(int, int) click-through} at the given position
	 */
	public boolean isClickThrough(Point pos) {
		if(!getWidget().isClickThrough(pos.x, pos.y))
			return false;
		for(QuickElementCapture child : getChildren()) {
			Point childPos = getChildIntersection(child, pos);
			if(!child.isClickThrough(childPos))
				return false;
		}
		return true;
	}

	/**
	 * Sinks into the element hierarchy by position using the cached bounds of the elements
	 *
	 * @param parent The parent capture for the new capture
	 * @param pos The position of the positioned event within the document
	 * @return The capture of each element in the hierarchy of the document that the event occurred over
	 */
	public QuickEventPositionCapture getPositionCapture(QuickEventPositionCapture parent, Point pos) {
		QuickEventPositionCapture epc = createCapture(parent, pos);
		for(QuickElementCapture child : sortByReverseZ(theChildren)) {
			Point relPos = getChildIntersection(child, pos);
			if(relPos == null)
				continue;
			QuickEventPositionCapture childCapture = child.getPositionCapture(epc, relPos);
			epc.addChild(childCapture);
			if(!childCapture.isClickThrough(relPos))
				break; // Don't add more siblings if one sibling covers the position
		}
		epc.seal();
		return epc;
	}

	/**
	 * @param pos The position within this element's capture
	 * @return All children of this element's capture that overlap the given point
	 */
	public Iterable<? extends QuickElementCapture> getChildrenAt(final Point pos) {
		return new Iterable<QuickElementCapture>() {
			@Override
			public Iterator<QuickElementCapture> iterator() {
				return IterableUtils.conditionalIterator(sortByReverseZ(getChildren()).iterator(),
					new IterableUtils.Accepter<QuickElementCapture, QuickElementCapture>() {
						@Override
						public QuickElementCapture accept(QuickElementCapture value) {
							return getChildIntersection(value, pos) != null ? value : null;
						}
					}, false);
			}
		};
	}

	/**
	 * @param child The child whose coordinates the given point is in
	 * @param pos The point to convert
	 * @return The given point, in this capture's coordinates, never null.
	 */
	protected Point getParentIntersection(QuickElementCapture child, Point pos) {
		if(theTransformer != null)
			return theTransformer.getParentPosition(this, child, pos);
		return new Point(pos.x + theX, pos.y + theY);
	}

	/**
	 * @param child The child to test
	 * @param pos The position in this element capture's coordinates to test
	 * @return The given position in the child's coordinate system, or null if the child does not overlap the given position
	 */
	protected Point getChildIntersection(QuickElementCapture child, Point pos) {
		if(theTransformer != null)
			return theTransformer.getChildPosition(this, child, pos);
		int relX = pos.x - child.getX();
		int relY = pos.y - child.getY();
		if(relX >= 0 && relY >= 0 && relX < child.getWidth() && relY < child.getHeight())
			return new Point(relX, relY);
		return null;
	}

	/**
	 * @param parent The parent capture for the new capture
	 * @param pos The position for the new capture
	 * @return The element position capture
	 */
	protected QuickEventPositionCapture createCapture(QuickEventPositionCapture parent, Point pos) {
		return new QuickEventPositionCapture(parent, getWidget(), theX, theY, theZ, theWidth, theHeight, pos.x, pos.y);
	}

	private static java.util.List<QuickElementCapture> sortByReverseZ(java.util.List<? extends QuickElementCapture> children) {
		java.util.ArrayList<QuickElementCapture> ret = new java.util.ArrayList<>(children);
		java.util.Collections.sort(ret, (QuickElementCapture o1, QuickElementCapture o2) -> {
			return o2.getZ() - o2.getZ();
		});
		return ret;
	}

	@Override
	protected QuickElementCapture clone() {
		QuickElementCapture ret;
		try {
			ret = (QuickElementCapture) super.clone();
		} catch(CloneNotSupportedException e) {
			throw new IllegalStateException(e);
		}
		ret.theChildren = new java.util.ArrayList<>();
		for(QuickElementCapture child : theChildren) {
			QuickElementCapture newChild = child.clone();
			newChild.theParent = ret;
			ret.theChildren.add(newChild);
		}
		ret.isSealed = false;
		return ret;
	}

	@Override
	public String toString() {
		return "Capture for " + theWidget;
	}

	private class SelfIterator implements Iterator<QuickElementCapture> {
		private boolean hasReturned;

		SelfIterator() {
		}

		@Override
		public boolean hasNext() {
			return !hasReturned;
		}

		@Override
		public QuickElementCapture next() {
			hasReturned = true;
			return QuickElementCapture.this;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private class QuickCaptureIterator implements Iterator<QuickElementCapture> {
		private int theIndex;

		private Iterator<? extends QuickElementCapture> theChildIter;

		private boolean isReturningSelf;

		private boolean isDepthFirst;

		private boolean hasReturnedSelf;

		QuickCaptureIterator(boolean returnSelf, boolean depthFirst) {
			isReturningSelf = returnSelf;
			isDepthFirst = depthFirst;
		}

		@Override
		public boolean hasNext() {
			if(isReturningSelf && !isDepthFirst && !hasReturnedSelf)
				return true;
			while(theIndex < getChildCount()) {
				if(theChildIter == null)
					theChildIter = getChild(theIndex).iterate(isDepthFirst).iterator();
				if(theChildIter.hasNext())
					return true;
				else
					theChildIter = null;
				theIndex++;
			}
			return isReturningSelf && !hasReturnedSelf;
		}

		@Override
		public QuickElementCapture next() {
			if(isReturningSelf && !isDepthFirst && !hasReturnedSelf) {
				hasReturnedSelf = true;
				return QuickElementCapture.this;
			}
			if(theChildIter != null)
				return theChildIter.next();
			if(isReturningSelf && isDepthFirst && !hasReturnedSelf) {
				hasReturnedSelf = true;
				return QuickElementCapture.this;
			}
			return null;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
