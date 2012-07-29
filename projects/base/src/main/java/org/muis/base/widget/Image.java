package org.muis.base.widget;

import org.muis.core.MuisAttribute;

/** An extension of GenericImage that allows users to specify its content and behavior with attributes */
public class Image extends GenericImage
{
	/** The attribute to use to specify the image resource that an image is to render */
	public static final MuisAttribute<java.net.URL> src = new MuisAttribute<java.net.URL>("src", MuisAttribute.resourceAttr);

	/**
	 * The attribute to use to specify both resize policies at once for an image. Will be overridden if either the horizontal or vertical
	 * resize policies are set.
	 */
	public static final MuisAttribute<ImageResizePolicy> resize = new MuisAttribute<ImageResizePolicy>("resize",
		new MuisAttribute.MuisEnumAttribute<>(ImageResizePolicy.class));

	/** The attribute to use to specify the horizontal resize policy for an image */
	public static final MuisAttribute<ImageResizePolicy> hResize = new MuisAttribute<ImageResizePolicy>("h-resize",
		new MuisAttribute.MuisEnumAttribute<>(ImageResizePolicy.class));

	/** The attribute to use to specify the vertical resize policy for an image */
	public static final MuisAttribute<ImageResizePolicy> vResize = new MuisAttribute<ImageResizePolicy>("v-resize",
		new MuisAttribute.MuisEnumAttribute<>(ImageResizePolicy.class));

	/** The attribute to use to specify the {@link #isProportionLocked() proportion-locked} attribute of an image */
	public static final MuisAttribute<Boolean> propLocked = new MuisAttribute<Boolean>("prop-locked", MuisAttribute.boolAttr);

	/** Creates an image element */
	public Image()
	{
		requireAttribute(this, src);
		acceptAttribute(this, resize);
		acceptAttribute(this, hResize);
		acceptAttribute(this, vResize);
		acceptAttribute(this, propLocked);
		addListener(ATTRIBUTE_SET, new org.muis.core.event.MuisEventListener<MuisAttribute<?>>() {
			@Override
			public void eventOccurred(org.muis.core.event.MuisEvent<MuisAttribute<?>> event, org.muis.core.MuisElement element)
			{
				if(event.getValue() == src)
					setImageLocation(element.getAttribute(src));
				else if(event.getValue() == resize)
				{
					ImageResizePolicy policy = element.getAttribute(resize);
					if(element.getAttribute(hResize) != null || element.getAttribute(vResize) != null)
					{
						if(policy != null)
							warn(hResize.name + " and " + vResize.name + " attributes override the effects of " + resize.name);
						if(policy == null)
							policy = ImageResizePolicy.lockIfEmpty;
						if(element.getAttribute(hResize) == null)
							setHorizontalResizePolicy(policy);
						else if(element.getAttribute(vResize) == null)
							setVerticalResizePolicy(policy);
					}
					else
					{
						if(policy == null)
							policy = ImageResizePolicy.lockIfEmpty;
						setHorizontalResizePolicy(policy);
						setVerticalResizePolicy(policy);
					}
				}
				else if(event.getValue() == hResize)
				{
					ImageResizePolicy policy = element.getAttribute(hResize);
					if(policy == null)
						policy = element.getAttribute(resize);
					if(policy == null)
						policy = ImageResizePolicy.lockIfEmpty;
					setHorizontalResizePolicy(policy);
				}
				else if(event.getValue() == vResize)
				{
					ImageResizePolicy policy = element.getAttribute(vResize);
					if(policy == null)
						policy = element.getAttribute(resize);
					if(policy == null)
						policy = ImageResizePolicy.lockIfEmpty;
					setVerticalResizePolicy(policy);
				}
				else if(event.getValue() == propLocked)
				{
					Boolean locked = element.getAttribute(propLocked);
					setProportionLocked(locked == null ? false : locked.booleanValue());
				}
			}

			@Override
			public boolean isLocal()
			{
				return true;
			}
		});
	}
}
