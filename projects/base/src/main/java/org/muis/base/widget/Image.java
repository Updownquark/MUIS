package org.muis.base.widget;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisProperty;

/** An extension of GenericImage that allows users to specify its content and behavior with attributes */
public class Image extends GenericImage {
	/** The attribute to use to specify the image resource that an image is to render */
	public static final MuisAttribute<java.net.URL> src = new MuisAttribute<>("src", MuisProperty.resourceAttr);

	/**
	 * The attribute to use to specify both resize policies at once for an image. Will be overridden if either the horizontal or vertical
	 * resize policies are set.
	 */
	public static final MuisAttribute<ImageResizePolicy> resize = new MuisAttribute<>("resize", new MuisProperty.MuisEnumProperty<>(
		ImageResizePolicy.class));

	/** The attribute to use to specify the horizontal resize policy for an image */
	public static final MuisAttribute<ImageResizePolicy> hResize = new MuisAttribute<>("h-resize", new MuisProperty.MuisEnumProperty<>(
		ImageResizePolicy.class));

	/** The attribute to use to specify the vertical resize policy for an image */
	public static final MuisAttribute<ImageResizePolicy> vResize = new MuisAttribute<>("v-resize", new MuisProperty.MuisEnumProperty<>(
		ImageResizePolicy.class));

	/** The attribute to use to specify the {@link #isProportionLocked() proportion-locked} attribute of an image */
	public static final MuisAttribute<Boolean> propLocked = new MuisAttribute<>("prop-locked", MuisProperty.boolAttr);

	/** Creates an image element */
	public Image() {
		atts().require(this, src);
		atts().accept(this, resize);
		atts().accept(this, hResize);
		atts().accept(this, vResize);
		atts().accept(this, propLocked);
		addListener(org.muis.core.MuisConstants.Events.ATTRIBUTE_SET, new org.muis.core.event.MuisEventListener<MuisAttribute<?>>() {
			@Override
			public void eventOccurred(org.muis.core.event.MuisEvent<MuisAttribute<?>> event, org.muis.core.MuisElement element) {
				if(event.getValue() == src)
					setImageLocation(element.atts().get(src));
				else if(event.getValue() == resize) {
					ImageResizePolicy policy = element.atts().get(resize);
					if(element.atts().get(hResize) != null || element.atts().get(vResize) != null) {
						if(policy != null)
							msg()
								.warn(
									hResize.getName() + " and " + vResize.getName() + " attributes override the effects of "
										+ resize.getName());
						if(policy == null)
							policy = ImageResizePolicy.lockIfEmpty;
						if(element.atts().get(hResize) == null)
							setHorizontalResizePolicy(policy);
						else if(element.atts().get(vResize) == null)
							setVerticalResizePolicy(policy);
					} else {
						if(policy == null)
							policy = ImageResizePolicy.lockIfEmpty;
						setHorizontalResizePolicy(policy);
						setVerticalResizePolicy(policy);
					}
				} else if(event.getValue() == hResize) {
					ImageResizePolicy policy = element.atts().get(hResize);
					if(policy == null)
						policy = element.atts().get(resize);
					if(policy == null)
						policy = ImageResizePolicy.lockIfEmpty;
					setHorizontalResizePolicy(policy);
				} else if(event.getValue() == vResize) {
					ImageResizePolicy policy = element.atts().get(vResize);
					if(policy == null)
						policy = element.atts().get(resize);
					if(policy == null)
						policy = ImageResizePolicy.lockIfEmpty;
					setVerticalResizePolicy(policy);
				} else if(event.getValue() == propLocked) {
					Boolean locked = element.atts().get(propLocked);
					setProportionLocked(locked == null ? false : locked.booleanValue());
				}
			}
		});
	}
}
