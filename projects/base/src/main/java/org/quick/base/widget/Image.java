package org.quick.base.widget;

import java.net.URL;

import org.quick.core.QuickConstants;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.prop.QuickProperty;

/** An extension of GenericImage that allows users to specify its content and behavior with attributes */
public class Image extends GenericImage {
	/** The attribute to use to specify the image resource that an image is to render */
	public static final QuickAttribute<URL> src = new QuickAttribute<>("src", QuickProperty.resourceAttr);

	/**
	 * The attribute to use to specify both resize policies at once for an image. Will be overridden if either the horizontal or vertical
	 * resize policies are set.
	 */
	public static final QuickAttribute<ImageResizePolicy> resize = new QuickAttribute<>("resize", new QuickProperty.QuickEnumProperty<>(
		ImageResizePolicy.class));

	/** The attribute to use to specify the horizontal resize policy for an image */
	public static final QuickAttribute<ImageResizePolicy> hResize = new QuickAttribute<>("h-resize", new QuickProperty.QuickEnumProperty<>(
		ImageResizePolicy.class));

	/** The attribute to use to specify the vertical resize policy for an image */
	public static final QuickAttribute<ImageResizePolicy> vResize = new QuickAttribute<>("v-resize", new QuickProperty.QuickEnumProperty<>(
		ImageResizePolicy.class));

	/** The attribute to use to specify the {@link #isProportionLocked() proportion-locked} attribute of an image */
	public static final QuickAttribute<Boolean> propLocked = new QuickAttribute<>("prop-locked", QuickProperty.boolAttr);

	/** Creates an image element */
	public Image() {
		life().runWhen(
			() -> {
				atts().require(this, src).act(event -> {
					setImageLocation(event.getValue());
				});
				atts().accept(this, resize).act(
					event -> {
						ImageResizePolicy policy = event.getValue();
						if(atts().get(hResize) != null || atts().get(vResize) != null) {
							if(policy != null)
								msg().warn(
									hResize.getName() + " and " + vResize.getName() + " attributes override the effects of "
										+ resize.getName());
							if(policy == null)
								policy = ImageResizePolicy.lockIfEmpty;
							if(atts().get(hResize) == null)
								setHorizontalResizePolicy(policy);
							else if(atts().get(vResize) == null)
								setVerticalResizePolicy(policy);
						} else {
							if(policy == null)
								policy = ImageResizePolicy.lockIfEmpty;
							setHorizontalResizePolicy(policy);
							setVerticalResizePolicy(policy);
						}
					});
				atts().accept(this, hResize).act(event -> {
					ImageResizePolicy policy = event.getValue();
					if(policy == null)
						policy = atts().get(resize);
					if(policy == null)
						policy = ImageResizePolicy.lockIfEmpty;
					setHorizontalResizePolicy(policy);
				});
				atts().accept(this, vResize).act(event -> {
					ImageResizePolicy policy = event.getValue();
					if(policy == null)
						policy = atts().get(resize);
					if(policy == null)
						policy = ImageResizePolicy.lockIfEmpty;
					setVerticalResizePolicy(policy);
				});
				atts().accept(this, propLocked).act(event -> {
					Boolean locked = event.getValue();
					setProportionLocked(locked == null ? false : locked.booleanValue());
				});
			}, QuickConstants.CoreStage.INIT_SELF.toString(), 1);
	}
}
