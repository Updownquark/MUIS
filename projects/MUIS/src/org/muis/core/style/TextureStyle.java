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
	public static final StyleAttribute<Texture> texture;

	/** The direction (in degrees clockwise from the top) from which the light source lighting the texture is coming */
	public static final StyleAttribute<Float> lightSource;

	static
	{
		instance = new TextureStyle();
		texture = StyleAttribute.createMuisTypeStyle(instance, "texture", Texture.class, null);
		instance.register(texture);
		lightSource = StyleAttribute.createFloatStyle(instance, "light-source", 0, 360, 315, "top", 0f, "top-right", 45f, "right", 90f,
			"bottom-right", 135f, "bottom", 180f, "bottom-left", 225f, "left", 270f, "top-left", 315f);
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
