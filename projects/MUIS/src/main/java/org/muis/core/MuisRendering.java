package org.muis.core;

import java.awt.image.BufferedImage;

public class MuisRendering {
	public static class ElementBound {
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
	}

	private final BufferedImage theImage;

	private ElementBound theRoot;

	public MuisRendering(int width, int height) {
		theImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
	}

	public void setRoot(ElementBound root) {
		if(theRoot != null)
			throw new IllegalStateException("Rendering root already set");
		theRoot = root;
	}

	public BufferedImage getImage() {
		return theImage;
	}

	public ElementBound getRoot() {
		return theRoot;
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
				MuisElementCapture childCapture = capture(root, child, relX, relY);
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
}
