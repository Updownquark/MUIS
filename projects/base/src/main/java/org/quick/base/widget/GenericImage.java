package org.quick.base.widget;

import java.awt.Image;
import java.net.URL;

import org.quick.base.data.ImageData;
import org.quick.base.widget.GenericImage.ImageAnimator;

/** Renders an image */
public abstract class GenericImage extends org.quick.core.LayoutContainer {
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

	/** The cache type to load images from URLs */
	public static final org.quick.core.QuickCache.CacheItemType<URL, ImageData, java.io.IOException> cacheType;

	static {
		cacheType = new org.quick.core.QuickCache.CacheItemType<URL, ImageData, java.io.IOException>() {
			@Override
			public ImageData generate(org.quick.core.QuickEnvironment env, URL key) throws java.io.IOException {
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

	private ImageData thePreDisplayed;

	private Object theLock;

	/** Creates a generic image */
	public GenericImage() {
		theLock = new Object();
		life().runWhen(() -> {
			org.quick.core.QuickEnvironment env = getDocument().getEnvironment();
			String res = getToolkit().getMappedResource("img-load-icon");
			if(res == null)
				msg().error("No configured img-load-icon");
			if(res != null && theLoadingImage == null)
				try {
					env.getCache().get(env, cacheType, org.quick.util.QuickUtils.resolveURL(getToolkit().getURI(), res),
						new org.quick.core.QuickCache.ItemReceiver<URL, ImageData>() {
							@Override
							public void itemGenerated(URL key, ImageData value) {
								if(theLoadingImage == null) {
									theLoadingImage = value;
									imageChanged();
								}
							}

							@Override
							public void errorOccurred(URL key, Throwable exception, boolean firstReport) {
								msg().error("Could not load image loading icon", exception);
							}
						});
				} catch (org.quick.core.QuickException e) {
					msg().error("Could not retrieve image loading icon", e);
				}
			res = getToolkit().getMappedResource("img-load-failed-icon");
			if(res == null)
				msg().error("No configured img-load-failed-icon");
			if(res != null && theErrorImage == null)
				try {
					env.getCache().get(env, cacheType, org.quick.util.QuickUtils.resolveURL(getToolkit().getURI(), res),
						new org.quick.core.QuickCache.ItemReceiver<URL, ImageData>() {
							@Override
							public void itemGenerated(URL key, ImageData value) {
								if(theErrorImage == null) {
									theLoadingImage = value;
									imageChanged();
								}
							}

							@Override
							public void errorOccurred(URL key, Throwable exception, boolean firstReport) {
								msg().error("Could not load image load failed icon", exception);
							}
						});
				} catch (org.quick.core.QuickException e) {
					msg().error("Could not retrieve image load failed icon", e);
				}
		}, org.quick.core.QuickConstants.CoreStage.INIT_SELF.toString(), 1);
		theHResizePolicy = ImageResizePolicy.lockIfEmpty;
		theVResizePolicy = ImageResizePolicy.lockIfEmpty;
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
		getDocument().getEnvironment().getCache().get(getDocument().getEnvironment(), cacheType, location,
			new org.quick.core.QuickCache.ItemReceiver<URL, ImageData>() {
				@Override
				public void itemGenerated(URL key, ImageData value) {
					if (!key.equals(theLocation))
						return;
					theImage = value;
					isLoading = false;
					imageChanged();
				}

				@Override
				public void errorOccurred(URL key, Throwable exception, boolean firstReport) {
					if (!key.equals(theLocation))
						return;
					msg().error("Could not load image from " + key, exception);
					theLoadError = exception;
					isLoading = false;
					imageChanged();
				}
			});
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
				org.quick.motion.AnimationManager.get().start(theAnimator);
			}
			sizeNeedsChanged();
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
		sizeNeedsChanged();
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
		sizeNeedsChanged();
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
		sizeNeedsChanged();
		repaint(null, false);
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

	public int getImageIndex() {
		return theImageIndex;
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
}
