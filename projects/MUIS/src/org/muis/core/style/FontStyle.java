/*
 * Created Mar 8, 2009 by Andrew
 */
package org.muis.core.style;

import java.awt.Color;
import java.awt.font.TextAttribute;

/** Style attribute that affect the display of text rendered from {@link org.muis.core.MuisTextElement}s */
public class FontStyle implements StyleDomain
{
	/** Styles of underlining available */
	public enum Underline
	{
		/** No underline */
		none,
		/** A single-pixel solid underline */
		on,
		/** A double-pixel solid underline */
		heavy,
		/** A dashed-underline */
		dashed,
		/** A dotted underline */
		dotted;
	}

	private StyleAttribute<?> [] theAttributes;

	private FontStyle()
	{
		theAttributes = new StyleAttribute[0];
	}

	private void register(StyleAttribute<?> attr)
	{
		theAttributes = prisms.util.ArrayUtils.add(theAttributes, attr);
	}

	private static final FontStyle instance;

	/** The type of font */
	public static final StyleAttribute<String> family;

	/** The color of the font */
	public static final StyleAttribute<Color> color;

	/** The transparency of the font */
	public static final StyleAttribute<Float> transparency;

	/** The weight of the font's stroke */
	public static final StyleAttribute<Float> weight;

	/** Whether the font is italicized */
	public static final StyleAttribute<Float> slant;

	/** Whether the font is underlined */
	public static final StyleAttribute<Underline> underline;

	/** Whether the font is struck through */
	public static final StyleAttribute<Boolean> strike;

	/** The font's size (in points) */
	public static final StyleAttribute<Float> size;

	/** Whether kerning should be used for the font */
	public static final StyleAttribute<Boolean> kerning;

	/** Whether ligatures should be used for the font */
	public static final StyleAttribute<Boolean> ligatures;

	/** Whether the font should be rendered with anti-aliasing */
	public static final StyleAttribute<Boolean> antiAlias;

	/** Whether text should wrap onto the next line when a line runs out of width in its container */
	public static final StyleAttribute<Boolean> wordWrap;

	/** The vertical stretch factor of the font */
	public static final StyleAttribute<Float> stretch;

	static
	{
		instance = new FontStyle();
		java.util.Map<String, String> families = new java.util.TreeMap<>();
		for(String familyName : java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
			families.put(familyName.replaceAll(" ", "-"), familyName);
		family = StyleAttribute.createArbitraryStyle(String.class, instance, "family", "Default", families);
		instance.register(family);
		color = StyleAttribute.createColorStyle(instance, "color", Color.black);
		instance.register(color);
		transparency = StyleAttribute.createFloatStyle(instance, "transparency", 0, 1, 0);
		instance.register(transparency);
		java.util.Map<String, Float> weights = new java.util.HashMap<>();
		weights.put("normal", 1f);
		weights.put("extra-light", TextAttribute.WEIGHT_EXTRA_LIGHT);
		weights.put("light", TextAttribute.WEIGHT_LIGHT);
		weights.put("demi-light", TextAttribute.WEIGHT_DEMILIGHT);
		weights.put("semi-bold", TextAttribute.WEIGHT_SEMIBOLD);
		weights.put("medium", TextAttribute.WEIGHT_MEDIUM);
		weights.put("demi-bold", TextAttribute.WEIGHT_DEMIBOLD);
		weights.put("bold", TextAttribute.WEIGHT_BOLD);
		weights.put("heavy", TextAttribute.WEIGHT_HEAVY);
		weights.put("extra-bold", TextAttribute.WEIGHT_EXTRABOLD);
		weights.put("ultra-bold", TextAttribute.WEIGHT_ULTRABOLD);
		weight = StyleAttribute.createFloatStyle(instance, "weight", 0.25f, 3, 1, weights);
		instance.register(weight);
		slant = StyleAttribute.createFloatStyle(instance, "slant", -1, 1, 0, "normal", 0f, "italic", TextAttribute.POSTURE_OBLIQUE);
		instance.register(slant);
		underline = StyleAttribute.createEnumStyle(instance, "underline", Underline.class, Underline.none);
		instance.register(underline);
		strike = StyleAttribute.createBooleanStyle(instance, "strike", false);
		instance.register(strike);
		size = StyleAttribute.createFloatStyle(instance, "size", 0.1f, 256, 12);
		instance.register(size);
		kerning = StyleAttribute.createBooleanStyle(instance, "kerning", true);
		instance.register(kerning);
		ligatures = StyleAttribute.createBooleanStyle(instance, "ligatures", true);
		instance.register(ligatures);
		antiAlias = StyleAttribute.createBooleanStyle(instance, "anti-alias", false);
		instance.register(antiAlias);
		wordWrap = StyleAttribute.createBooleanStyle(instance, "word-wrap", true);
		instance.register(wordWrap);
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
