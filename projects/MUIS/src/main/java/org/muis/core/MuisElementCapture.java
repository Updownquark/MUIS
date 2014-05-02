package org.muis.core;

import java.awt.Point;
import java.util.Iterator;

import org.muis.util.MuisUtils;

/** Represents a capture of an element's bounds and hierarchy at a point in time */
public class MuisElementCapture implements Cloneable, prisms.util.Sealable {
	private MuisElementCapture theParent;

	private final MuisElement theElement;

	/** The x-coordinate of the screen point relative to this element */
	private final int theX;

	/** The y-coordinate of the screen point relative to this element */
	private final int theY;

	private final int theZ;

	private final int theWidth;

	private final int theHeight;

	private java.util.List<MuisElementCapture> theChildren;

	private boolean isSealed;

	/**
	 * @param p This capture element's parent in the hierarchy
	 * @param el The MUIS element that this structure is a capture of
	 * @param xPos The x-coordinate of the element's upper-left corner
	 * @param yPos The y-coordinate of the element's upper-left corner
	 * @param zIndex The z-index of the element
	 * @param w The width of the element
	 * @param h The height of the element
	 */
	public MuisElementCapture(MuisElementCapture p, MuisElement el, int xPos, int yPos, int zIndex, int w, int h) {
		theParent = p;
		theElement = el;
		theX = xPos;
		theY = yPos;
		theZ = zIndex;
		theWidth = w;
		theHeight = h;
		theChildren = new java.util.ArrayList<>(3);
	}

	/**
	 * @param child The child to add to this capture
	 * @throws SealedException If this capture has been sealed
	 */
	public void addChild(MuisElementCapture child) throws SealedException {
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
	public MuisElementCapture getChild(int index) {
		return theChildren.get(index);
	}

	/** @return An iterator over this capture's immediate children */
	public java.util.List<? extends MuisElementCapture> getChildren() {
		return theChildren;
	}

	/** @return An iterator of each end point (leaf node) in this hierarchy */
	public Iterable<? extends MuisElementCapture> getTargets() {
		if(theChildren.isEmpty())
			return new Iterable<MuisElementCapture>() {
				@Override
				public Iterator<MuisElementCapture> iterator() {
					return new SelfIterator();
				}
			};
		else
			return new Iterable<MuisElementCapture>() {
				@Override
				public Iterator<MuisElementCapture> iterator() {
					return new MuisCaptureIterator(false, true);
				}
			};
	}

	/**
	 * @param depthFirst Whether to iterate depth-first or breadth-first
	 * @return An iterable to iterate over every element in this hierarchy
	 */
	public Iterable<? extends MuisElementCapture> iterate(final boolean depthFirst) {
		return new Iterable<MuisElementCapture>() {
			@Override
			public Iterator<MuisElementCapture> iterator() {
				return new MuisCaptureIterator(true, depthFirst);
			}
		};
	}

	/**
	 * @param el The element to search for
	 * @return The capture of the given element in this hierarchy, or null if the given element was not located in this capture
	 */
	public MuisElementCapture find(MuisElement el) {
		if(theParent != null)
			return getRoot().find(el);
		MuisElement [] path = MuisUtils.path(el);
		MuisElementCapture ret = this;
		int pathIdx;
		for(pathIdx = 1; pathIdx < path.length; pathIdx++) {
			boolean found = false;
			for(MuisElementCapture child : ret.theChildren)
				if(child.getElement() == path[pathIdx]) {
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
	public MuisElementCapture getTarget() {
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
		return getElement().hashCode();
	}

	@Override
	public final boolean equals(Object obj) {
		return obj instanceof MuisElementCapture && ((MuisElementCapture) obj).getElement().equals(getElement());
	}

	/** @return The root of this capture */
	public MuisElementCapture getRoot() {
		MuisElementCapture parent = theParent;
		while(parent.getParent() != null)
			parent = parent.getParent();
		return parent;
	}

	/** @return This capture element's parent in the hierarchy */
	public MuisElementCapture getParent() {
		return theParent;
	}

	/** @param parent The parent element's capture */
	public void setParent(MuisElementCapture parent) {
		if(isSealed)
			throw new SealedException(this);
		theParent = parent;
	}

	/** @return The element that this capture represents */
	public MuisElement getElement() {
		return theElement;
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
	public java.awt.Point getDocLocation() {
		java.awt.Point ret = new java.awt.Point(theX, theY);
		MuisElementCapture parent = theParent;
		while(parent != null) {
			ret.x += parent.theX;
			ret.y += parent.theY;
			parent = parent.theParent;
		}
		return ret;
	}

	/**
	 * Sinks into the element hierarchy by position using the cached bounds of the elements
	 *
	 * @param pos The position of the positioned event within the document
	 * @return The capture of each element in the hierarchy of the document that the event occurred over
	 */
	public MuisEventPositionCapture getPositionCapture(MuisEventPositionCapture parent, Point pos) {
		MuisEventPositionCapture epc = createCapture(parent, pos);
		for(MuisElementCapture child : sortByReverseZ(theChildren)) {
			Point relPos = getChildIntersection(child, pos);
			if(relPos == null)
				continue;
			MuisEventPositionCapture childCapture = child.getPositionCapture(epc, relPos);
			epc.addChild(childCapture);
			if(!childCapture.isClickThrough(relPos))
				break; // Don't add more siblings if one sibling covers the position
		}
		epc.seal();
		return epc;
	}

	public Iterable<? extends MuisElementCapture> getChildrenAt(final Point pos) {
		return new Iterable<MuisElementCapture>() {
			@Override
			public Iterator<MuisElementCapture> iterator() {
				return prisms.util.ArrayUtils.conditionalIterator(sortByReverseZ(getChildren()).iterator(),
					new prisms.util.ArrayUtils.Accepter<MuisElementCapture, MuisElementCapture>() {
						@Override
						public MuisElementCapture accept(MuisElementCapture value) {
							return getChildIntersection(value, pos) != null ? value : null;
						}
					}, false);
			}
		};
	}

	protected Point getChildIntersection(MuisElementCapture child, Point pos) {
		int relX = pos.x - child.getX();
		int relY = pos.y - child.getY();
		if(relX >= 0 && relY >= 0 && relX < child.getWidth() && relY < child.getHeight())
			return new Point(relX, relY);
		return null;
	}

	protected MuisEventPositionCapture createCapture(MuisEventPositionCapture parent, Point pos) {
		return new MuisEventPositionCapture(parent, getElement(), theX, theY, theZ, theWidth, theHeight, pos.x, pos.y);
	}

	private static java.util.List<MuisElementCapture> sortByReverseZ(java.util.List<? extends MuisElementCapture> children) {
		java.util.ArrayList<MuisElementCapture> ret = new java.util.ArrayList<>(children);
		java.util.Collections.sort(ret, new java.util.Comparator<MuisElementCapture>() {
			@Override
			public int compare(MuisElementCapture o1, MuisElementCapture o2) {
				return o2.getZ() - o2.getZ();
			}
		});
		return ret;
	}

	@Override
	protected MuisElementCapture clone() {
		MuisElementCapture ret;
		try {
			ret = (MuisElementCapture) super.clone();
		} catch(CloneNotSupportedException e) {
			throw new IllegalStateException(e);
		}
		ret.theChildren = new java.util.ArrayList<>();
		for(MuisElementCapture child : theChildren) {
			MuisElementCapture newChild = child.clone();
			newChild.theParent = ret;
			ret.theChildren.add(newChild);
		}
		ret.isSealed = false;
		return ret;
	}

	@Override
	public String toString() {
		return "Capture for " + theElement;
	}

	private class SelfIterator implements Iterator<MuisElementCapture> {
		private boolean hasReturned;

		SelfIterator() {
		}

		@Override
		public boolean hasNext() {
			return !hasReturned;
		}

		@Override
		public MuisElementCapture next() {
			hasReturned = true;
			return MuisElementCapture.this;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private class MuisCaptureIterator implements Iterator<MuisElementCapture> {
		private int theIndex;

		private Iterator<? extends MuisElementCapture> theChildIter;

		private boolean isReturningSelf;

		private boolean isDepthFirst;

		private boolean hasReturnedSelf;

		MuisCaptureIterator(boolean returnSelf, boolean depthFirst) {
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
		public MuisElementCapture next() {
			if(isReturningSelf && !isDepthFirst && !hasReturnedSelf) {
				hasReturnedSelf = true;
				return MuisElementCapture.this;
			}
			if(theChildIter != null)
				return theChildIter.next();
			if(isReturningSelf && isDepthFirst && !hasReturnedSelf) {
				hasReturnedSelf = true;
				return MuisElementCapture.this;
			}
			return null;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
