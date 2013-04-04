package org.muis.base.widget;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.net.URL;

import org.muis.base.data.ImageData;
import org.muis.core.MuisLayout;
import org.muis.core.layout.SimpleSizeGuide;
import org.muis.core.layout.SizeGuide;

/** Renders an image */
public class GenericImage extends org.muis.core.LayoutContainer {
	/** Determines how this image may be resized */
	public enum ImageResizePolicy {
		/** This widget will simply use the size set by the layout */
		none,
		/** This widget's size will be locked to the image size */
		lock,
		/** Functions as {@link #lock} if the image has no content and {@link #none} if it has content */
		lockIfEmpty,
		/** Repeats the image as many times as necessary in the container */
		repeat,
		/** Resizes the image to fit the widget's size */
		resize
	}

	/** An animator that repaints this GenericImage when the image is animated */
	public class ImageAnimator implements org.muis.motion.Animation {
		private volatile boolean isStopped;

		@Override
		public boolean update(long time) {
			ImageData img = getDisplayedImage();
			if(img == null || isStopped)
				return true;
			int total = 0;
			for(int i = 0; i < img.getImageCount(); i++) {
				if(time < total) {
					setImageIndex(i);
					break;
				}
				total += img.getDelay(i);
			}
			if(time >= total) {
				if(total == 0)
					return true;
				time = time % total;
				total = 0;
				for(int i = 0; i < img.getImageCount(); i++) {
					if(time < total) {
						setImageIndex(i);
						break;
					}
					total += img.getDelay(i);
				}
			}
			return isStopped;
		}

		@Override
		public long getMaxFrequency() {
			ImageData img = getDisplayedImage();
			if(img == null)
				return 0;
			int ret = 0;
			boolean hetero = false;
			for(int i = 0; i < img.getImageCount(); i++) {
				int delay = img.getDelay(i);
				if(ret == 0)
					ret = delay;
				else if(ret != delay) {
					hetero = true;
					if(delay < ret)
						ret = delay;
				}
			}
			if(hetero)
				ret /= 4;
			return ret;
		}

		/** Called to stop animation for this image */
		public void stop() {
			isStopped = true;
		}
	}

	/** The cache type to load images from URLs */
	public static final org.muis.core.MuisCache.CacheItemType<URL, ImageData, java.io.IOException> cacheType;

	static {
		cacheType = new org.muis.core.MuisCache.CacheItemType<URL, ImageData, java.io.IOException>() {
			@Override
			public ImageData generate(org.muis.core.MuisEnvironment env, URL key) throws java.io.IOException {
				javax.imageio.stream.ImageInputStream imInput = javax.imageio.ImageIO.createImageInputStream(key.openStream());
				if(imInput == null)
					throw new java.io.IOException("File format not recognized: " + key.getPath());
				java.util.Iterator<javax.imageio.ImageReader> readers = javax.imageio.ImageIO.getImageReaders(imInput);
				if(!readers.hasNext())
					throw new java.io.IOException("File format not recognized: " + key.getPath());
				javax.imageio.ImageReader reader = readers.next();
				synchronized(reader) {
					reader.setInput(imInput);
					return new ImageData(reader);
				}
			}

			@Override
			public int size(ImageData value) {
				int size = value.getSize();
				if(size < 0)
					return 0;
				return size;
			}
		};
	}

	volatile boolean isLoading;

	volatile Throwable theLoadError;

	volatile URL theLocation;

	volatile ImageData theImage;

	volatile ImageData theLoadingImage;

	volatile ImageData theErrorImage;

	private ImageResizePolicy theHResizePolicy;

	private ImageResizePolicy theVResizePolicy;

	boolean isProportionLocked;

	boolean isSizeLocked;

	private int theImageIndex;

	private ImageAnimator theAnimator;

	private ImageData thePreDisplayed;

	private Object theLock;

	/** Creates a generic image */
	public GenericImage() {
		theLock = new Object();
		life().runWhen(new Runnable() {
			@Override
			public void run() {
				org.muis.core.MuisEnvironment env = getDocument().getEnvironment();
				org.muis.core.ResourceMapping res = getToolkit().getMappedResource("img-load-icon");
				if(res == null)
					msg().error("No configured img-load-icon");
				if(res != null && theLoadingImage == null)
					try {
						env.getCache().get(env, cacheType, org.muis.core.MuisUtils.resolveURL(getToolkit().getURI(), res.getLocation()),
							new org.muis.core.MuisCache.ItemReceiver<URL, ImageData>() {
								@Override
								public void itemGenerated(URL key, ImageData value) {
									if(theLoadingImage == null) {
										theLoadingImage = value;
										imageChanged();
									}
								}

								@Override
								public void errorOccurred(URL key, Throwable exception) {
									msg().error("Could not load image loading icon", exception);
								}
							});
					} catch(org.muis.core.MuisException | java.io.IOException e) {
						msg().error("Could not retrieve image loading icon", e);
					}
				res = getToolkit().getMappedResource("img-load-failed-icon");
				if(res == null)
					msg().error("No configured img-load-failed-icon");
				if(res != null && theErrorImage == null)
					try {
						env.getCache().get(env, cacheType, org.muis.core.MuisUtils.resolveURL(getToolkit().getURI(), res.getLocation()),
							new org.muis.core.MuisCache.ItemReceiver<URL, ImageData>() {
								@Override
								public void itemGenerated(URL key, ImageData value) {
									if(theErrorImage == null) {
										theLoadingImage = value;
										imageChanged();
									}
								}

								@Override
								public void errorOccurred(URL key, Throwable exception) {
									msg().error("Could not load image load failed icon", exception);
								}
							});
					} catch(org.muis.core.MuisException | java.io.IOException e) {
						msg().error("Could not retrieve image load failed icon", e);
					}
			}
		}, org.muis.core.MuisConstants.CoreStage.INIT_SELF.toString(), 1);
		theHResizePolicy = ImageResizePolicy.lockIfEmpty;
		theVResizePolicy = ImageResizePolicy.lockIfEmpty;
	}

	@Override
	protected MuisLayout getDefaultLayout() {
		return new org.muis.base.layout.SimpleLayout();
	}

	/**
	 * Sets this widget's image via URL
	 *
	 * @param location The URL for the image file
	 */
	public void setImageLocation(URL location) {
		theLocation = location;
		isLoading = true;
		theLoadError = null;
		theImage = null;
		try {
			theImage = getDocument().getEnvironment().getCache()
				.get(getDocument().getEnvironment(), cacheType, location, new org.muis.core.MuisCache.ItemReceiver<URL, ImageData>() {
					@Override
					public void itemGenerated(URL key, ImageData value) {
						if(!key.equals(theLocation))
							return;
						theImage = value;
						isLoading = false;
						imageChanged();
					}

					@Override
					public void errorOccurred(URL key, Throwable exception) {
						if(!key.equals(theLocation))
							return;
						theLoadError = exception;
						isLoading = false;
						imageChanged();
					}
				});
			imageChanged();
		} catch(java.io.IOException e) {
		}
	}

	/** @return The image that this widget should render */
	public ImageData getImage() {
		return theImage;
	}

	/** @param image The image that this widget should render */
	public void setImage(ImageData image) {
		theLocation = null;
		theImage = image;
		imageChanged();
	}

	/** @param image The image that this widget should render */
	public void setImage(Image image) {
		setImage(image == null ? null : new ImageData(image));
	}

	/** @return The error that caused the failure of this widget's last attempted image load */
	public Throwable getLoadingError() {
		return theLoadError;
	}

	/** @param image The image to display while the target image is loading */
	public void setLoadingImage(ImageData image) {
		theLoadingImage = image;
		imageChanged();
	}

	/** @param image The image to display when a target image fails to load */
	public void setErrorImage(ImageData image) {
		theErrorImage = image;
		imageChanged();
	}

	void imageChanged() {
		ImageData img = getDisplayedImage();
		if(img == thePreDisplayed)
			return;
		synchronized(theLock) {
			img = getDisplayedImage();
			if(img == thePreDisplayed)
				return;
			ImageAnimator anim = theAnimator;
			theAnimator = null;
			if(anim != null)
				anim.stop();
			theImageIndex = 0;
			thePreDisplayed = img;
			if(img.getImageCount() > 1) {
				theAnimator = new ImageAnimator();
				org.muis.motion.AnimationManager.get().start(theAnimator);
			}
			if(getParent() != null)
				getParent().relayout(false);
			repaint(null, false);
		}
	}

	/** @return The policy that this widget follows when it is resized horizontally */
	public ImageResizePolicy getHorizontalResizePolicy() {
		return theHResizePolicy;
	}

	/** @param policy The policy that this widget should follow when it is resized horizontally */
	public void setHorizontalResizePolicy(ImageResizePolicy policy) {
		if(policy == null)
			throw new NullPointerException();
		if(theHResizePolicy == policy)
			return;
		theHResizePolicy = policy;
		if(getParent() != null)
			getParent().relayout(false);
		repaint(null, false);
	}

	/** @return The policy that this widget follows when it is resized vertically */
	public ImageResizePolicy getVerticalResizePolicy() {
		return theVResizePolicy;
	}

	/** @param policy The policy that this widget should follow when it is resized vertically */
	public void setVerticalResizePolicy(ImageResizePolicy policy) {
		if(policy == null)
			throw new NullPointerException();
		if(theVResizePolicy == policy)
			return;
		theVResizePolicy = policy;
		if(getParent() != null)
			getParent().relayout(false);
		repaint(null, false);
	}

	/**
	 * @return Whether this widget renders the image proportionally. Only works if both resize policies are {@link ImageResizePolicy#resize}
	 */
	public boolean isProportionLocked() {
		return isProportionLocked;
	}

	/**
	 * @param locked Whether this widget renders the image proportionally. Only works if both resize policies are
	 *            {@link ImageResizePolicy#resize}.
	 */
	public void setProportionLocked(boolean locked) {
		if(locked == isProportionLocked)
			return;
		isProportionLocked = locked;
		if(getParent() != null)
			getParent().relayout(false);
		repaint(null, false);
	}

	@Override
	public SizeGuide getWSizer() {
		ImageData img = getDisplayedImage();
		int w, h;
		if(img != null) {
			w = img.getWidth();
			h = img.getHeight();
		} else
			return super.getWSizer();

		switch (theHResizePolicy) {
		case none:
			return super.getWSizer();
		case lock:
			return new SimpleSizeGuide(w, w, w, w, w);
		case lockIfEmpty:
			if(ch().isEmpty())
				return new SimpleSizeGuide(w, w, w, w, w);
			else
				return super.getWSizer();
		case repeat:
			return super.getWSizer();
		case resize:
			if(isProportionLocked)
				return new ProportionalSizeGuide(w, h, false);
			else
				return new SimpleSizeGuide(0, 0, w, Integer.MAX_VALUE, Integer.MAX_VALUE);
		}
		return super.getWSizer();
	}

	@Override
	public SizeGuide getHSizer() {
		ImageData img = getDisplayedImage();
		int w, h;
		if(img != null) {
			w = img.getWidth();
			h = img.getHeight();
		} else
			return super.getHSizer();

		switch (theVResizePolicy) {
		case none:
			return super.getHSizer();
		case lock:
			return new SimpleSizeGuide(h, h, h, h, h);
		case lockIfEmpty:
			if(ch().isEmpty())
				return new SimpleSizeGuide(h, h, h, h, h);
			else
				return super.getHSizer();
		case repeat:
			return super.getHSizer();
		case resize:
			if(isProportionLocked)
				return new ProportionalSizeGuide(h, w, true);
			else
				return new SimpleSizeGuide(0, 0, h, Integer.MAX_VALUE, Integer.MAX_VALUE);
		}
		return super.getHSizer();
	}

	/** @return The image that would be displayed if this widget were painted now (may be the loading or error image) */
	public ImageData getDisplayedImage() {
		if(isLoading)
			return theLoadingImage;
		else if(theLoadError != null)
			return theErrorImage;
		else
			return theImage;
	}

	/** @param index The index of the frame in the animated image to display */
	public void setImageIndex(int index) {
		ImageData img = getDisplayedImage();
		if(img == null || img.getImageCount() < 2)
			index = 0;
		else
			index %= img.getImageCount();
		if(theImageIndex != index) {
			theImageIndex = index;
			repaint(null, true);
		}
	}

	@Override
	public void paintSelf(Graphics2D graphics, Rectangle area) {
		super.paintSelf(graphics, area);
		ImageData img = getDisplayedImage();
		if(img == null)
			return;
		int imgIdx = theImageIndex;
		imgIdx %= img.getImageCount();
		int h = img.getHeight();
		java.awt.Point off = img.getOffset(imgIdx);
		switch (theVResizePolicy) {
		case none:
		case lock:
		case lockIfEmpty:
			drawImage(graphics, img, off.y, off.y + h, 0, h, area, imgIdx);
			break;
		case repeat:
			for(int y = 0; y < bounds().getHeight(); y += h)
				drawImage(graphics, img, y + off.y, y + off.y + h, 0, h, area, imgIdx);
			break;
		case resize:
			if(isProportionLocked) {
				int w = img.getWidth();
				if((off.y + h) * bounds().getWidth() / bounds().getHeight() / (off.x + w) > 0) {
					int offY = off.y * bounds().getHeight() / (off.y + h);
					drawImage(graphics, img, offY, bounds().getHeight(), 0, h, area, imgIdx);
				} else {
					int offY = off.y * bounds().getWidth() / (off.x + w);
					drawImage(graphics, img, offY, (off.y + h) * bounds().getWidth() / (off.x + w), 0, h, area, imgIdx);
				}
			} else {
				int offY = off.y * bounds().getHeight() / (off.y + h);
				drawImage(graphics, img, offY, bounds().getHeight(), 0, h, area, imgIdx);
			}
			break;
		}
	}

	private void drawImage(Graphics2D graphics, ImageData img, int gfxY1, int gfxY2, int imgY1, int imgY2, Rectangle area, int imgIdx) {
		int w = img.getWidth();
		java.awt.Point off = img.getOffset(imgIdx);
		switch (theHResizePolicy) {
		case none:
		case lock:
		case lockIfEmpty:
			drawImage(graphics, img, off.x, gfxY1, off.x + w, gfxY2, 0, imgY1, w, imgY2, area, imgIdx);
			break;
		case repeat:
			for(int x = 0; x < bounds().getWidth(); x += w)
				drawImage(graphics, img, x + off.x, gfxY1, x + off.x + w, gfxY2, 0, imgY1, w, imgY2, area, imgIdx);
			break;
		case resize:
			if(isProportionLocked) {
				int gfxW = (gfxY2 - gfxY1) * (off.x + w) / (imgY2 - imgY1);
				int offX = off.x * gfxW / (off.x + w);
				drawImage(graphics, img, offX, gfxY1, gfxW, gfxY2, 0, imgY1, w, imgY2, area, imgIdx);
			} else {
				int offX = off.x * bounds().getWidth() / (off.x + w);
				drawImage(graphics, img, offX, gfxY1, bounds().getWidth(), gfxY2, 0, imgY1, w, imgY2, area, imgIdx);
			}
			break;
		}
	}

	private void drawImage(Graphics2D graphics, ImageData img, int gfxX1, int gfxY1, int gfxX2, int gfxY2, int imgX1, int imgY1, int imgX2,
		int imgY2, Rectangle area, int imgIdx) {
		if(area != null && (area.x >= gfxX2 || area.x + area.width <= gfxX1 || area.y >= gfxY2 || area.y + area.height <= gfxY1))
			return;
		graphics.drawImage(img.get(imgIdx), gfxX1, gfxY1, gfxX2, gfxY2, imgX1, imgY1, imgX2, imgY2, null);
	}

	private static class ProportionalSizeGuide extends org.muis.core.layout.AbstractSizeGuide {
		private final int theMainDim;

		private final int theCrossDim;

		private final boolean isVertical;

		ProportionalSizeGuide(int main, int cross, boolean vertical) {
			theMainDim = main;
			theCrossDim = cross;
			isVertical = vertical;
		}

		@Override
		public int getMinPreferred(int crossSize, boolean csMax) {
			if(csMax)
				return 0;
			else
				return theMainDim * crossSize / theCrossDim;
		}

		@Override
		public int getMaxPreferred(int crossSize, boolean csMax) {
			return theMainDim * crossSize / theCrossDim;
		}

		@Override
		public int getMin(int crossSize, boolean csMax) {
			if(csMax)
				return 0;
			else
				return theMainDim * crossSize / theCrossDim;
		}

		@Override
		public int getPreferred(int crossSize, boolean csMax) {
			return theMainDim * crossSize / theCrossDim;
		}

		@Override
		public int getMax(int crossSize, boolean csMax) {
			return theMainDim * crossSize / theCrossDim;
		}

		@Override
		public int getBaseline(int size) {
			return isVertical ? size : 0;
		}
	}
}
