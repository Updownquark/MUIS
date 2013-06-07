package org.muis.core;

import java.awt.Point;
import java.awt.image.BufferedImage;

public class MuisRendering implements Cloneable {
	public static class ElementBound {
		public ElementBound parent;

		public final MuisElement element;

		public final int x;

		public final int y;

		public final int z;

		public final int width;

		public final int height;

		public final ElementBound [] children;

		/** The list of children sorted by reverse z-index */
		public final ElementBound [] sortedChildren;

		public ElementBound(MuisElement el, int xPos, int yPos, int zVal, int w, int h, ElementBound [] ch) {
			element = el;
			x = xPos;
			y = yPos;
			z = zVal;
			width = w;
			height = h;
			children = ch;
			sortedChildren = sortByReverseZ(ch);
		}

		public Point getDocLocation() {
			Point ret = new Point(x, y);
			ElementBound p = parent;
			while(p != null) {
				ret.x += p.x;
				ret.y += p.y;
			}
			return ret;
		}

		/**
		 * Decided not to override clone because this method does not call super.clone()
		 *
		 * @return A copy of this element bound
		 */
		public ElementBound copy() {
			ElementBound [] ch = new ElementBound[children.length];
			for(int c = 0; c < children.length; c++) {
				ch[c] = children[c].copy();
			}
			ElementBound ret = new ElementBound(element, x, y, z, width, height, ch);
			for(ElementBound child : ch)
				child.parent = ret;
			return ret;
		}
	}

	private BufferedImage theImage;

	private ElementBound theRoot;

	public MuisRendering(int width, int height) {
		theImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
	}

	public void setRoot(ElementBound root) {
		theRoot = root;
	}

	public BufferedImage getImage() {
		return theImage;
	}

	public ElementBound getRoot() {
		return theRoot;
	}

	public ElementBound getFor(MuisElement element) {
		MuisElement [] path = MuisUtils.path(element);
		if(path == null || path.length == 0 || path[0] != theRoot.element)
			return null;
		ElementBound ret = theRoot;
		boolean found = true;
		for(int p = 1; p < path.length && found; p++) {
			found = false;
			for(ElementBound ch : ret.children) {
				if(ch.element == path[p]) {
					ret = ch;
					found = true;
					break;
				}
			}
		}
		if(!found)
			return null;
		return ret;
	}

	/**
	 * Sinks into the element hierarchy by position using the cached bounds of the elements
	 *
	 * @param x The x-position of the positioned event within the document
	 * @param y The y-position of the positioned event within the document
	 * @return The capture of each element in the hierarchy of the document that the event occurred over
	 */
	public MuisElementCapture capture(int x, int y) {
		return capture(new MuisElementCapture(null, theRoot.element, x, y), theRoot, x, y);
	}

	private static MuisElementCapture capture(MuisElementCapture root, ElementBound bound, int x, int y) {
		for(ElementBound child : bound.sortedChildren) {
			int relX = x - child.x;
			int relY = y - child.y;
			if(relX >= 0 && relY >= 0 && relX < child.width && relY < child.height) {
				MuisElementCapture childCapture = capture(new MuisElementCapture(root, child.element, relX, relY), child, relX, relY);
				root.addChild(childCapture);
				boolean isClickThrough = true;
				for(MuisElementCapture mec : childCapture)
					if(!mec.element.isClickThrough(relX, relY)) {
						isClickThrough = false;
						break;
					}
				if(!isClickThrough)
					break;
			}
		}
		return root;
	}

	private static ElementBound [] sortByReverseZ(ElementBound [] children) {
		children = children.clone();
		java.util.Arrays.sort(children, new java.util.Comparator<ElementBound>() {
			@Override
			public int compare(ElementBound o1, ElementBound o2) {
				return o2.z - o2.z;
			}
		});
		return children;
	}

	@Override
	public MuisRendering clone() {
		MuisRendering ret;
		try {
			ret = (MuisRendering) super.clone();
		} catch(CloneNotSupportedException e) {
			throw new IllegalStateException(e);
		}
		ret.theImage = new BufferedImage(theImage.getWidth(), theImage.getHeight(), theImage.getType());
		ret.theImage.getGraphics().drawImage(theImage, 0, 0, null);
		ret.theRoot = theRoot.copy();
		return ret;
	}
}
