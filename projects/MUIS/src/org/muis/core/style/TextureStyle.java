package org.muis.core.style;

/** Styles relating to textures */
public class TextureStyle implements StyleDomain
{
	private StyleAttribute<?> [] theAttributes;

	private TextureStyle()
	{
		theAttributes = new StyleAttribute[0];
	}

	private void register(StyleAttribute<?> attr)
	{
		theAttributes = prisms.util.ArrayUtils.add(theAttributes, attr);
	}

	private static final TextureStyle instance;

	/** The radius of the button texture's influence over the border of a widget */
	public static final StyleAttribute<? extends Texture> texture;

	/** The direction (in degrees clockwise from the top) from which the light source lighting the texture is coming */
	public static final StyleAttribute<Double> lightSource;

	static
	{
		instance = new TextureStyle();
		texture = StyleAttribute.createStyle(instance, "texture", new org.muis.core.MuisAttribute.MuisTypeInstanceAttribute<Texture>(
			Texture.class), null);
		instance.register(texture);
		lightSource = StyleAttribute.createBoundedStyle(instance, "light-source", org.muis.core.MuisAttribute.floatAttr, 315d, 0d, 360d,
			"top", 0d, "top-right", 45d, "right", 90d, "bottom-right", 135d, "bottom", 180d, "bottom-left", 225d, "left", 270d, "top-left",
			315d);
		instance.register(lightSource);
	}

	/** @return The style domain for all background styles */
	public static TextureStyle getDomainInstance()
	{
		return instance;
	}

	@Override
	public String getName()
	{
		return "tex";
	}

	@Override
	public java.util.Iterator<StyleAttribute<?>> iterator()
	{
		return prisms.util.ArrayUtils.iterator(theAttributes);
	}
}
