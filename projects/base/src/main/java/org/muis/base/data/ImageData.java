package org.muis.base.data;

import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;

import javax.imageio.metadata.IIOMetadataNode;

/** Represents an image, with easy access to animation parameters */
public class ImageData implements Iterable<Image> {
	private Image [] theImages;

	private int [] theDelays;

	private Point [] theOffsets;

	private int theWidth;

	private int theHeight;

	private int theSize;

	private Boolean hasTransparency;

	/** @param img The image to wrap. If the image is animated, the new ImageData will see it as a single frame. */
	public ImageData(Image img) {
		theImages = new Image[] {img};
		theDelays = new int[] {0};
		theOffsets = new Point[] {new Point(0, 0)};
		theSize = -1;
		theWidth = -1;
		theHeight = -1;
	}

	/**
	 * @param reader The image reader that is reading the image
	 * @throws java.io.IOException If an error occurs reading the image
	 */
	public ImageData(javax.imageio.ImageReader reader) throws java.io.IOException {
		String format = reader.getFormatName();
		int count = reader.getNumImages(false);
		ArrayList<Image> images = null;
		prisms.util.IntList delays = null;
		ArrayList<Point> offsets = null;
		if(count >= 0) {
			theImages = new Image[count];
			theDelays = new int[count];
			theOffsets = new Point[count];
		} else {
			images = new ArrayList<>();
			delays = new prisms.util.IntList();
			offsets = new ArrayList<>();
		}
		int index;
		for(index = 0; count < 0 || index < count; index++) {
			BufferedImage img;
			try {
				img = reader.read(index);
			} catch(IndexOutOfBoundsException e) {
				break;
			}
			if(img.getWidth() > theWidth)
				theWidth = img.getWidth();
			if(img.getHeight() > theHeight)
				theHeight = img.getHeight();
			theSize += img.getWidth() * img.getHeight() * 4;
			int delay = getDelay(reader, format, index);
			Point offset = getOffset(reader, format, index);
			if(theImages != null) {
				theImages[index] = img;
				theDelays[index] = delay;
				theOffsets[index] = offset;
			} else {
				images.add(img);
				delays.add(delay);
				offsets.add(offset);
			}
		}
		if(theImages == null) {
			theImages = images.toArray(new Image[images.size()]);
			theDelays = delays.toArray();
			theOffsets = offsets.toArray(new Point[offsets.size()]);
		}
	}

	/** @return Approximately how many bytes of memory this image holds */
	public int getSize() {
		if(theSize < 0)
			for(Image img : theImages) {
				int w = img.getWidth(null);
				int h = img.getHeight(null);
				if(w < 0 || h < 0) {
					theSize = -1;
					return -1;
				}
				theSize += w * h * 4;
			}
		return theSize;
	}

	/** @return The maximum width of all the frames in this image */
	public int getWidth() {
		if(theWidth < 0)
			for(Image img : theImages) {
				int w = img.getWidth(null);
				if(w < 0) {
					theWidth = -1;
					return -1;
				}
				if(w > theWidth)
					theWidth = w;
			}
		return theWidth;
	}

	/** @return The maximum height of all the frames in this image */
	public int getHeight() {
		if(theHeight < 0)
			for(Image img : theImages) {
				int h = img.getHeight(null);
				if(h < 0) {
					theHeight = -1;
					return -1;
				}
				if(h > theHeight)
					theHeight = h;
			}
		return theHeight;
	}

	/** @return Whether this image has transparency */
	public boolean hasTransparency() {
		if(hasTransparency == null) {
			for(Image img : theImages) {
				if(img instanceof BufferedImage) {
					BufferedImage bImg = (BufferedImage) img;
					if(bImg.getColorModel().hasAlpha()) {
						hasTransparency = Boolean.TRUE;
						break;
					}
					continue;
				} else {
					hasTransparency = Boolean.TRUE;
					break;
				}
			}
		}
		return hasTransparency;
	}

	/** @return The number of frames in this image */
	public int getImageCount() {
		return theImages.length;
	}

	@Override
	public Iterator<Image> iterator() {
		return prisms.util.ArrayUtils.iterator(theImages, true);
	}

	/**
	 * @param index The index of the frame to get
	 * @return The frame in this image at the given index
	 */
	public Image get(int index) {
		return theImages[index];
	}

	/**
	 * @param index The index of the frame to get the delay of
	 * @return The number of milliseconds to delay switching to the next frame after displaying the given frame
	 */
	public int getDelay(int index) {
		return theDelays[index];
	}

	/**
	 * @param index The index of the frame to get the offset of
	 * @return The offset from the top-left corner to place the given frame of this image at
	 */
	public Point getOffset(int index) {
		return theOffsets[index];
	}

	private static int getDelay(javax.imageio.ImageReader reader, String format, int index) throws java.io.IOException {
		if(format.toLowerCase().contains("gif")) {
			org.w3c.dom.Node root = reader.getImageMetadata(index).getAsTree("javax_imageio_gif_image_1.0");
			for(org.w3c.dom.Node c = root.getFirstChild(); c != null; c = c.getNextSibling())
				if(c instanceof IIOMetadataNode) {
					IIOMetadataNode metaNode = (IIOMetadataNode) c;
					if(c.getNodeName().equals("GraphicControlExtension"))
						if(metaNode.getAttribute("delayTime") != null)
							return Integer.parseInt(metaNode.getAttribute("delayTime")) * 10;
						else
							return 0;
				}
			return 0;
		} else
			return 0;
	}

	private static Point getOffset(javax.imageio.ImageReader reader, String format, int index) throws java.io.IOException {
		if(format.toLowerCase().contains("gif")) {
			org.w3c.dom.Node root = reader.getImageMetadata(index).getAsTree("javax_imageio_gif_image_1.0");
			for(org.w3c.dom.Node c = root.getFirstChild(); c != null; c = c.getNextSibling())
				if(c instanceof IIOMetadataNode) {
					IIOMetadataNode metaNode = (IIOMetadataNode) c;
					if(c.getNodeName().equals("ImageDescriptor"))
						if(metaNode.getAttribute("imageLeftPosition") != null && metaNode.getAttribute("imageTopPosition") != null)
							return new Point(Integer.parseInt(metaNode.getAttribute("imageLeftPosition")), Integer.parseInt(metaNode
								.getAttribute("imageTopPosition")));
						else
							return new Point(0, 0);
				}
			return new Point(0, 0);
		} else
			return new Point(0, 0);
	}
}
