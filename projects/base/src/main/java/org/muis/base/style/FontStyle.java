/*
 * Created Mar 8, 2009 by Andrew
 */
package org.muis.base.style;

import java.awt.Color;

import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleDomain;

/** Style attribute that affect the display of text rendered from {@link org.muis.core.MuisTextElement}s */
public class FontStyle implements StyleDomain
{
	private StyleAttribute<?> [] theAttributes;

	private FontStyle()
	{
		theAttributes = new StyleAttribute [0];
	}

	private void register(StyleAttribute<?> attr)
	{
		theAttributes = prisms.util.ArrayUtils.add(theAttributes, attr);
	}

	private static final FontStyle instance;

	/** The type of font */
	public static final StyleAttribute<FontType> type;

	/** The color of the font */
	public static final StyleAttribute<Color> color;

	/** The transparency of the font */
	public static final StyleAttribute<Float> transparency;

	/** The weight of the font's stroke */
	public static final StyleAttribute<Integer> weight;

	/** Whether the font is italicized */
	public static final StyleAttribute<Boolean> italic;

	/** Whether the font is underlined */
	public static final StyleAttribute<Boolean> underline;

	/** The font's size (in points) */
	public static final StyleAttribute<Float> size;

	/** The vertical stretch factor of the font */
	public static final StyleAttribute<Float> stretch;

	static
	{
		instance = new FontStyle();
		type = StyleAttribute.createEnumStyle(instance, "type", FontType.class, FontType.SansSerif);
		instance.register(type);
		color = StyleAttribute.createColorStyle(instance, "color", Color.black);
		instance.register(color);
		transparency = StyleAttribute.createFloatStyle(instance, "transparency", 0, 1, 0);
		instance.register(transparency);
		weight = StyleAttribute.createIntStyle(instance, "weight", 0, 5, 1);
		instance.register(weight);
		italic = StyleAttribute.createBooleanStyle(instance, "italic", false);
		instance.register(italic);
		underline = StyleAttribute.createBooleanStyle(instance, "underline", false);
		instance.register(underline);
		size = StyleAttribute.createFloatStyle(instance, "size", 0.1f, 256, 8);
		instance.register(size);
		stretch = StyleAttribute.createFloatStyle(instance, "stretch", 0.05f, 100, 1);
		instance.register(stretch);
	}

	/** @return The style domain for all font styles */
	public static FontStyle getDomainInstance()
	{
		return instance;
	}

	@Override
	public String getName()
	{
		return "font";
	}

	@Override
	public java.util.Iterator<StyleAttribute<?>> iterator()
	{
		return new DomainAttributeIterator(theAttributes);
	}
}
