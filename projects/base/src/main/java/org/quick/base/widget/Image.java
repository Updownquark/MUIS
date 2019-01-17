package org.quick.base.widget;

import java.net.URL;

import org.quick.core.QuickConstants;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.prop.QuickPropertyType;
import org.quick.core.tags.AcceptAttribute;
import org.quick.core.tags.QuickElementType;

/** An extension of GenericImage that allows users to specify its content and behavior with attributes */
@QuickElementType(//
	attributes = { //
		@AcceptAttribute(declaringClass = Image.class, field = "src", required = true), //
		@AcceptAttribute(declaringClass = Image.class, field = "resize"), //
		@AcceptAttribute(declaringClass = Image.class, field = "hResize"), //
		@AcceptAttribute(declaringClass = Image.class, field = "vResize"), //
		@AcceptAttribute(declaringClass = Image.class, field = "propLocked"),//
	})
public class Image extends GenericImage {
	/** The attribute to use to specify the image resource that an image is to render */
	public static final QuickAttribute<URL> src = QuickAttribute.build("src", QuickPropertyType.resource).build();

	/**
	 * The attribute to use to specify both resize policies at once for an image. Will be overridden if either the horizontal or vertical
	 * resize policies are set.
	 */
	public static final QuickAttribute<ImageResizePolicy> resize = QuickAttribute
		.build("resize", QuickPropertyType.forEnum(ImageResizePolicy.class)).build();

	/** The attribute to use to specify the horizontal resize policy for an image */
	public static final QuickAttribute<ImageResizePolicy> hResize = QuickAttribute
		.build("h-resize", QuickPropertyType.forEnum(ImageResizePolicy.class)).build();

	/** The attribute to use to specify the vertical resize policy for an image */
	public static final QuickAttribute<ImageResizePolicy> vResize = QuickAttribute
		.build("v-resize", QuickPropertyType.forEnum(ImageResizePolicy.class)).build();

	/** The attribute to use to specify the {@link #isProportionLocked() proportion-locked} attribute of an image */
	public static final QuickAttribute<Boolean> propLocked = QuickAttribute.build("prop-locked", QuickPropertyType.boole).build();

	/** Creates an image element */
	public Image() {
		life().runWhen(() -> {
			atts().get(src).changes().act(event -> {
				setImageLocation(event.getNewValue());
			});
			atts().get(resize).changes().act(event -> {
				ImageResizePolicy policy = event.getNewValue();
				if (atts().get(hResize) != null || atts().get(vResize) != null) {
					if (policy != null)
						msg().warn(
							hResize.getName() + " and " + vResize.getName() + " attributes override the effects of " + resize.getName());
					if (policy == null)
						policy = ImageResizePolicy.lockIfEmpty;
					if (atts().get(hResize) == null)
						setHorizontalResizePolicy(policy);
					else if (atts().get(vResize) == null)
						setVerticalResizePolicy(policy);
				} else {
					if (policy == null)
						policy = ImageResizePolicy.lockIfEmpty;
					setHorizontalResizePolicy(policy);
					setVerticalResizePolicy(policy);
				}
			});
			atts().get(hResize).changes().act(event -> {
				ImageResizePolicy policy = event.getNewValue();
				if (policy == null)
					policy = atts().get(resize).get();
				if (policy == null)
					policy = ImageResizePolicy.lockIfEmpty;
				setHorizontalResizePolicy(policy);
			});
			atts().get(vResize).changes().act(event -> {
				ImageResizePolicy policy = event.getNewValue();
				if (policy == null)
					policy = atts().get(resize).get();
				if (policy == null)
					policy = ImageResizePolicy.lockIfEmpty;
				setVerticalResizePolicy(policy);
			});
			atts().get(propLocked).changes().act(event -> {
				Boolean locked = event.getNewValue();
				setProportionLocked(locked == null ? false : locked.booleanValue());
			});
		}, QuickConstants.CoreStage.INITIALIZED.toString(), 1);
	}
}
