package org.quick.widget.base;

import java.awt.Graphics2D;

import org.quick.base.data.ImageData;
import org.quick.base.widget.GenericImage;
import org.quick.core.QuickConstants.CoreStage;
import org.quick.core.layout.LayoutGuideType;
import org.quick.core.layout.Orientation;
import org.quick.widget.base.layout.SimpleLayout;
import org.quick.widget.core.LayoutContainerWidget;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.QuickWidgetDocument;
import org.quick.widget.core.Rectangle;
import org.quick.widget.core.layout.QuickWidgetLayout;
import org.quick.widget.core.layout.SimpleSizeGuide;
import org.quick.widget.core.layout.SizeGuide;

public class GenericImageWidget extends LayoutContainerWidget {
	/** An animator that repaints this GenericImage when the image is animated */
	public class ImageAnimator implements org.quick.motion.Animation {
		private volatile boolean isStopped;

		@Override
		public boolean update(long time) {
			ImageData img = getElement().getDisplayedImage().get();
			if (img == null || isStopped)
				return true;
			int total = 0;
			for (int i = 0; i < img.getImageCount(); i++) {
				if (time < total) {
					setImageIndex(i);
					break;
				}
				total += img.getDelay(i);
			}
			if (time >= total) {
				if (total == 0)
					return true;
				time = time % total;
				total = 0;
				for (int i = 0; i < img.getImageCount(); i++) {
					if (time < total) {
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
			ImageData img = getElement().getDisplayedImage().get();
			if (img == null)
				return 0;
			int ret = 0;
			boolean hetero = false;
			for (int i = 0; i < img.getImageCount(); i++) {
				int delay = img.getDelay(i);
				if (ret == 0)
					ret = delay;
				else if (ret != delay) {
					hetero = true;
					if (delay < ret)
						ret = delay;
				}
			}
			if (hetero)
				ret /= 4;
			return ret;
		}

		/** Called to stop animation for this image */
		public void stop() {
			isStopped = true;
		}
	}

	private ImageAnimator theAnimator;

	private int theImageIndex;

	public GenericImageWidget(QuickWidgetDocument doc, GenericImage element, QuickWidget parent) {
		super(doc, element, parent);

		getElement().life().runWhen(() -> {
			getElement().getDisplayedImage().changes().act(evt -> imageChanged(evt.getNewValue()));
		}, CoreStage.INIT_SELF, 1);
	}

	@Override
	public GenericImage getElement() {
		return (GenericImage) super.getElement();
	}

	@Override
	protected QuickWidgetLayout getDefaultLayout() {
		return new SimpleLayout();
	}

	protected void imageChanged(ImageData image) {
		ImageAnimator anim = theAnimator;
		theAnimator = null;
		if (anim != null)
			anim.stop();
		theImageIndex = 0;
		if (image.getImageCount() > 1) {
			theAnimator = new ImageAnimator();
			org.quick.motion.AnimationManager.get().start(theAnimator);
		}
		sizeNeedsChanged();
		repaint(null, false);
	}

	@Override
	public SizeGuide getSizer(Orientation orientation) {
		ImageData img = getElement().getDisplayedImage().get();
		int w, h;
		if (img != null) {
			w = img.getWidth();
			h = img.getHeight();
		} else
			return super.getSizer(orientation);
		int primary = orientation.isVertical() ? h : w;
		int secondary = orientation.isVertical() ? w : h;
		switch (getElement().getHorizontalResizePolicy()) {
		case none:
			return super.getSizer(orientation);
		case lock:
			return new SimpleSizeGuide(primary, primary, primary, primary, primary);
		case lockIfEmpty:
			if (getChildren().isEmpty())
				return new SimpleSizeGuide(primary, primary, primary, primary, primary);
			else
				return super.getSizer(orientation);
		case repeat:
			return super.getSizer(orientation);
		case resize:
			if (getElement().isProportionLocked())
				return new ProportionalSizeGuide(primary, secondary);
			else
				return new SimpleSizeGuide(0, 0, primary, Integer.MAX_VALUE, Integer.MAX_VALUE);
		}
		return super.getSizer(orientation);
	}

	@Override
	public boolean isTransparent() {
		if (!super.isTransparent())
			return false;
		ImageData img = getElement().getDisplayedImage().get();
		return img == null || img.hasTransparency();
	}

	@Override
	public void paintSelf(Graphics2D graphics, Rectangle area) {
		super.paintSelf(graphics, area);
		ImageData img = getElement().getDisplayedImage().get();
		if (img == null)
			return;
		int imgIdx = getImageIndex();
		imgIdx %= img.getImageCount();
		int h = img.getHeight();
		java.awt.Point off = img.getOffset(imgIdx);
		switch (getElement().getVerticalResizePolicy()) {
		case none:
		case lock:
		case lockIfEmpty:
			drawImage(graphics, img, off.y, off.y + h, 0, h, area, imgIdx);
			break;
		case repeat:
			for (int y = 0; y < bounds().getHeight(); y += h)
				drawImage(graphics, img, y + off.y, y + off.y + h, 0, h, area, imgIdx);
			break;
		case resize:
			if (getElement().isProportionLocked()) {
				int w = img.getWidth();
				if ((off.y + h) * bounds().getWidth() / bounds().getHeight() / (off.x + w) > 0) {
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

	public int getImageIndex() {
		return theImageIndex;
	}

	/** @param index The index of the frame in the animated image to display */
	public void setImageIndex(int index) {
		ImageData img = getElement().getDisplayedImage().get();
		if (img == null || img.getImageCount() < 2)
			index = 0;
		else
			index %= img.getImageCount();
		if (theImageIndex != index) {
			theImageIndex = index;
			repaint(null, true);
		}
	}

	private void drawImage(Graphics2D graphics, ImageData img, int gfxY1, int gfxY2, int imgY1, int imgY2, Rectangle area, int imgIdx) {
		int w = img.getWidth();
		java.awt.Point off = img.getOffset(imgIdx);
		switch (getElement().getHorizontalResizePolicy()) {
		case none:
		case lock:
		case lockIfEmpty:
			drawImage(graphics, img, off.x, gfxY1, off.x + w, gfxY2, 0, imgY1, w, imgY2, area, imgIdx);
			break;
		case repeat:
			for (int x = 0; x < bounds().getWidth(); x += w)
				drawImage(graphics, img, x + off.x, gfxY1, x + off.x + w, gfxY2, 0, imgY1, w, imgY2, area, imgIdx);
			break;
		case resize:
			if (getElement().isProportionLocked()) {
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

	private static void drawImage(Graphics2D graphics, ImageData img, int gfxX1, int gfxY1, int gfxX2, int gfxY2, int imgX1, int imgY1,
		int imgX2, int imgY2, Rectangle area, int imgIdx) {
		if (area != null && (area.x >= gfxX2 || area.x + area.width <= gfxX1 || area.y >= gfxY2 || area.y + area.height <= gfxY1))
			return;
		graphics.drawImage(img.get(imgIdx), gfxX1, gfxY1, gfxX2, gfxY2, imgX1, imgY1, imgX2, imgY2, null);
	}

	private static class ProportionalSizeGuide implements SizeGuide.GenericSizeGuide {
		private final int theMainDim;
		private final int theCrossDim;

		ProportionalSizeGuide(int main, int cross) {
			theMainDim = main;
			theCrossDim = cross;
		}

		@Override
		public int get(LayoutGuideType type, int crossSize, boolean csMax) {
			if (type.isMin() && csMax)
				return 0;
			return theMainDim * crossSize / theCrossDim;
		}

		@Override
		public int getBaseline(int size) {
			return -1;
		}
	}
}
