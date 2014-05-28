package org.muis.base.widget;

import static org.muis.core.event.AttributeChangedEvent.att;

import java.net.URL;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisProperty;

/** An extension of GenericImage that allows users to specify its content and behavior with attributes */
public class Image extends GenericImage {
	/** The attribute to use to specify the image resource that an image is to render */
	public static final MuisAttribute<URL> src = new MuisAttribute<>("src", MuisProperty.resourceAttr);

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
		events().filterMap(att(src)).act(event -> {
			setImageLocation(event.getElement().atts().get(src));
		});
		events().filterMap(att(resize)).act(
			event -> {
			ImageResizePolicy policy = event.getElement().atts().get(resize);
			if(event.getElement().atts().get(hResize) != null || event.getElement().atts().get(vResize) != null) {
				if(policy != null)
					msg().warn(
						hResize.getName() + " and " + vResize.getName() + " attributes override the effects of " + resize.getName());
				if(policy == null)
					policy = ImageResizePolicy.lockIfEmpty;
				if(event.getElement().atts().get(hResize) == null)
					setHorizontalResizePolicy(policy);
				else if(event.getElement().atts().get(vResize) == null)
					setVerticalResizePolicy(policy);
			} else {
				if(policy == null)
					policy = ImageResizePolicy.lockIfEmpty;
				setHorizontalResizePolicy(policy);
				setVerticalResizePolicy(policy);
			}
			});
		events().filterMap(att(hResize)).act(event -> {
			ImageResizePolicy policy = event.getElement().atts().get(hResize);
			if(policy == null)
				policy = event.getElement().atts().get(resize);
			if(policy == null)
				policy = ImageResizePolicy.lockIfEmpty;
			setHorizontalResizePolicy(policy);
		});
		events().filterMap(att(vResize)).act(event -> {
			ImageResizePolicy policy = event.getElement().atts().get(vResize);
			if(policy == null)
				policy = event.getElement().atts().get(resize);
			if(policy == null)
				policy = ImageResizePolicy.lockIfEmpty;
			setVerticalResizePolicy(policy);
		});
		events().filterMap(att(propLocked)).act(event -> {
			Boolean locked = event.getElement().atts().get(propLocked);
			setProportionLocked(locked == null ? false : locked.booleanValue());
		});
	}
}
