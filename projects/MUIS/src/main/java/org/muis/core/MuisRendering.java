package org.muis.core;

import java.awt.image.BufferedImage;

import org.muis.util.MuisUtils;

/** Represents a rendering of a MUIS document and some state associated with the rendering */
public class MuisRendering implements Cloneable {
	private BufferedImage theImage;

	private MuisElementCapture theRoot;

	/**
	 * @param width The width of the document to render
	 * @param height The height of the document to render
	 */
	public MuisRendering(int width, int height) {
		theImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
	}

	/** @param root A capture of the element structure that was rendered */
	public void setRoot(MuisElementCapture root) {
		theRoot = root;
	}

	/** @return The rendered (or rendering) image */
	public BufferedImage getImage() {
		return theImage;
	}

	/** @return A capture of the element structure that was rendered */
	public MuisElementCapture getRoot() {
		return theRoot;
	}

	/**
	 * @param element The element to get the capture structure for
	 * @return The captured bounds and hierarchy structure for the given element, or null if the element cannot be found at the same place
	 *         in the hierarchy
	 */
	public MuisElementCapture getFor(MuisElement element) {
		MuisElement [] path = MuisUtils.path(element);
		if(path == null || path.length == 0 || path[0] != theRoot.getElement())
			return null;
		MuisElementCapture ret = theRoot;
		boolean found = true;
		for(int p = 1; p < path.length && found; p++) {
			found = false;
			for(MuisElementCapture ch : ret.getChildren()) {
				if(ch.getElement() == path[p]) {
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
	public MuisEventPositionCapture capture(int x, int y) {
		return theRoot.getPositionCapture(null, new java.awt.Point(x, y));
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
		ret.theRoot = theRoot.clone();
		return ret;
	}
}
