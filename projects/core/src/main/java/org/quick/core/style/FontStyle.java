/* Created Mar 8, 2009 by Andrew */
package org.quick.core.style;

import java.awt.Color;
import java.awt.font.TextAttribute;

import org.qommons.IterableUtils;
import org.quick.core.prop.QuickProperty;
import org.quick.core.prop.QuickPropertyType;

import com.google.common.reflect.TypeToken;

/** Style attribute that affect the display of text rendered from {@link org.quick.core.QuickTextElement}s */
public class FontStyle implements StyleDomain {
	/** Styles of underlining available */
	public enum Underline {
		/** No underline */
		none, /** A single-pixel solid underline */
		on, /** A double-pixel solid underline */
		heavy, /** A dashed-underline */
		dashed, /** A dotted underline */
		dotted;
	}

	private StyleAttribute<?>[] theAttributes;

	private FontStyle() {
		theAttributes = new StyleAttribute[0];
	}

	private void register(StyleAttribute<?> attr) {
		theAttributes = org.qommons.ArrayUtils.add(theAttributes, attr);
	}

	private static final FontStyle instance;

	/** The type of font */
	public static final StyleAttribute<String> family;

	/** The color of the font */
	public static final StyleAttribute<Color> color;

	/** The transparency of the font */
	public static final StyleAttribute<Double> transparency;

	/** The weight of the font's stroke */
	public static final StyleAttribute<Double> weight;

	/** Whether the font is italicized */
	public static final StyleAttribute<Double> slant;

	/** Whether the font is underlined */
	public static final StyleAttribute<Underline> underline;

	/** Whether the font is struck through */
	public static final StyleAttribute<Boolean> strike;

	/** The font's size (in points) */
	public static final StyleAttribute<Double> size;

	/** Whether kerning should be used for the font */
	public static final StyleAttribute<Boolean> kerning;

	/** Whether ligatures should be used for the font */
	public static final StyleAttribute<Boolean> ligatures;

	/** Whether the font should be rendered with anti-aliasing */
	public static final StyleAttribute<Boolean> antiAlias;

	/** Whether text should wrap onto the next line when a line runs out of width in its container */
	public static final StyleAttribute<Boolean> wordWrap;

	/** The vertical stretch factor of the font */
	public static final StyleAttribute<Double> stretch;

	static {
		instance = new FontStyle();
		QuickPropertyType.Builder<String> familyPTBuilder = QuickPropertyType.build("family", TypeToken.of(String.class));
		java.util.Map<String, String> families = new java.util.TreeMap<>();
		for (String familyName : java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
			families.put(familyName.replaceAll(" ", "-"), familyName);
		familyPTBuilder.withValues(str -> families.get(str));
		family = new StyleAttribute<>(instance, "family",
			QuickPropertyType.build("family", TypeToken.of(String.class)).withValues(str -> families.get(str)).build(), "Default");
		instance.register(family);
		color = new StyleAttribute<>(instance, "color", QuickPropertyType.color, Color.black);
		instance.register(color);
		transparency = new StyleAttribute<>(instance, "transparency", QuickPropertyType.floating, 0d,
			new QuickProperty.ComparableValidator<>(0d, 1d));
		instance.register(transparency);
		java.util.Map<String, Double> weights = new java.util.HashMap<>();
		weights.put("normal", 1d);
		weights.put("extra-light", (double) TextAttribute.WEIGHT_EXTRA_LIGHT);
		weights.put("light", (double) TextAttribute.WEIGHT_LIGHT);
		weights.put("demi-light", (double) TextAttribute.WEIGHT_DEMILIGHT);
		weights.put("semi-bold", (double) TextAttribute.WEIGHT_SEMIBOLD);
		weights.put("medium", (double) TextAttribute.WEIGHT_MEDIUM);
		weights.put("demi-bold", (double) TextAttribute.WEIGHT_DEMIBOLD);
		weights.put("bold", (double) TextAttribute.WEIGHT_BOLD);
		weights.put("heavy", (double) TextAttribute.WEIGHT_HEAVY);
		weights.put("extra-bold", (double) TextAttribute.WEIGHT_EXTRABOLD);
		weights.put("ultra-bold", (double) TextAttribute.WEIGHT_ULTRABOLD);
		weight = new StyleAttribute<>(instance, "weight",
			QuickPropertyType.build("weight", TypeToken.of(Double.class)).withValues(str -> weights.get(str)).build(), 1d,
			new QuickProperty.ComparableValidator<>(0.25d, 3d));
		instance.register(weight);
		slant = new StyleAttribute<>(instance, "slant", QuickPropertyType.build("slant", TypeToken.of(Double.class)).withValues(str -> {
			switch (str) {
			case "normal":
				return 0d;
			case "italic":
				return (double) TextAttribute.POSTURE_OBLIQUE;
			default:
				return null;
			}
		}).build(), 0d, new QuickProperty.ComparableValidator<>(-1d, 1d));
		instance.register(slant);
		underline = new StyleAttribute<>(instance, "underline", QuickPropertyType.forEnum(Underline.class), Underline.none);
		instance.register(underline);
		strike = new StyleAttribute<>(instance, "strike", QuickPropertyType.boole, false);
		instance.register(strike);
		size = new StyleAttribute<>(instance, "size", QuickPropertyType.floating, 12d, new QuickProperty.ComparableValidator<>(0.1d, 256d));
		instance.register(size);
		kerning = new StyleAttribute<>(instance, "kerning", QuickPropertyType.boole, true);
		instance.register(kerning);
		ligatures = new StyleAttribute<>(instance, "ligatures", QuickPropertyType.boole, true);
		instance.register(ligatures);
		antiAlias = new StyleAttribute<>(instance, "anti-alias", QuickPropertyType.boole, false);
		instance.register(antiAlias);
		wordWrap = new StyleAttribute<>(instance, "word-wrap", QuickPropertyType.boole, true);
		instance.register(wordWrap);
		stretch = new StyleAttribute<>(instance, "stretch", QuickPropertyType.floating, 1d,
			new QuickProperty.ComparableValidator<>(0.05d, 100d));
		instance.register(stretch);
	}

	/** @return The style domain for all font styles */
	public static FontStyle getDomainInstance() {
		return instance;
	}

	@Override
	public String getName() {
		return "font";
	}

	@Override
	public java.util.Iterator<StyleAttribute<?>> iterator() {
		return IterableUtils.iterator(theAttributes, true);
	}
}
