/* Created Mar 8, 2009 by Andrew */
package org.quick.core.style;

import java.awt.Color;
import java.awt.font.TextAttribute;
import java.util.Map;

import org.observe.ObservableValue;
import org.qommons.IterableUtils;
import org.quick.core.prop.QuickProperty;
import org.quick.core.prop.QuickPropertyType;

import com.google.common.reflect.TypeToken;

/** Style attribute that affect the display of text rendered from {@link org.quick.core.QuickTextElement}s */
public class FontStyle implements StyleDomain {
	/** Styles of underlining available */
	public enum Underline {
		/** No underline */
		none,
		/** A single-pixel solid underline */
		on,
		/** A double-pixel solid underline */
		@SuppressWarnings("hiding")
		heavy,
		/** A dashed-underline */
		dashed,
		/** A dotted underline */
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

	// Font weights
	/** The {@link #weight} of normal text */
	public static final double normalWeight = 1;
	/** The {@link #weight} of extra-light text (the lightest named weight, lighter than {@link #light}) */
	public static final double extraLight = TextAttribute.WEIGHT_EXTRA_LIGHT;
	/** The {@link #weight} of light text (heavier than {@link #extraLight}, lighter than {@link #demiLight}) */
	public static final double light = TextAttribute.WEIGHT_LIGHT;
	/** The {@link #weight} of demi-light text (heavier than {@link #light}, lighter than {@link #semiBold}) */
	public static final double demiLight = TextAttribute.WEIGHT_DEMILIGHT;
	/** The {@link #weight} of semi-bold text (heavier than {@link #demiLight}, lighter than {@link #medium}) */
	public static final double semiBold = TextAttribute.WEIGHT_SEMIBOLD;
	/** The {@link #weight} of medium text (heavier than {@link #semiBold}, lighter than {@link #demiBold}) */
	public static final double medium = TextAttribute.WEIGHT_MEDIUM;
	/** The {@link #weight} of demi-bold text (heavier than {@link #medium}, lighter than {@link #bold}) */
	public static final double demiBold = TextAttribute.WEIGHT_DEMIBOLD;
	/** The {@link #weight} of bold text (heavier than {@link #demiBold}, lighter than {@link #heavy}) */
	public static final double bold = TextAttribute.WEIGHT_BOLD;
	/** The {@link #weight} of heavy text (heavier than {@link #bold}, lighter than {@link #extraBold}) */
	public static final double heavy = TextAttribute.WEIGHT_HEAVY;
	/** The {@link #weight} of extra bold text (heavier than {@link #heavy}, lighter than {@link #ultraBold}) */
	public static final double extraBold = TextAttribute.WEIGHT_EXTRABOLD;
	/** The {@link #weight} of ultra bold text (the heaviest named weight, heavier than {@link #extraBold}) */
	public static final double ultraBold = TextAttribute.WEIGHT_ULTRABOLD;

	// Font slants
	/** The {@link #slant} of normal text */
	public static final double normalSlant = TextAttribute.POSTURE_REGULAR;
	/** The {@link #slant} of italic text */
	public static final double italic = TextAttribute.POSTURE_OBLIQUE;

	static {
		instance = new FontStyle();
		QuickPropertyType.Builder<String> familyPTBuilder = QuickPropertyType.build("family", TypeToken.of(String.class));
		Map<String, String> families = new java.util.TreeMap<>();
		for (String familyName : java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
			families.put(familyName.replaceAll(" ", "-"), familyName);
		familyPTBuilder.buildContext(ctx -> {
			for (Map.Entry<String, String> entry : families.entrySet())
				ctx.withValue(entry.getKey(), ObservableValue.constant(TypeToken.of(String.class), entry.getValue()));
		});
		family = StyleAttribute.build(instance, "family", familyPTBuilder.build(), "Default").inherited().build();
		instance.register(family);
		color = StyleAttribute.build(instance, "color", QuickPropertyType.color, Color.black).inherited().build();
		instance.register(color);
		transparency = StyleAttribute.build(instance, "transparency", QuickPropertyType.floating, 0d).inherited()
			.validate(new QuickProperty.ComparableValidator<>(0d, 1d)).build();
		instance.register(transparency);
		Map<String, Double> weights = new java.util.HashMap<>();
		weights.put("normal", normalWeight);
		weights.put("extra-light", extraLight);
		weights.put("light", light);
		weights.put("demi-light", demiLight);
		weights.put("semi-bold", semiBold);
		weights.put("medium", medium);
		weights.put("demi-bold", demiBold);
		weights.put("bold", bold);
		weights.put("heavy", heavy);
		weights.put("extrabold", extraBold);
		weights.put("ultrabold", ultraBold);
		weight = StyleAttribute
			.build(instance, "weight",
				QuickPropertyType.build("weight", TypeToken.of(Double.class)).buildContext(ctx -> {
					for (Map.Entry<String, Double> entry : weights.entrySet())
						ctx.withValue(entry.getKey(), ObservableValue.constant(TypeToken.of(Double.TYPE), entry.getValue()));
				}).build(), 1d)
			.inherited()
			.validate(new QuickProperty.ComparableValidator<>(0.25d, 3d)).build();
		instance.register(weight);
		slant = StyleAttribute.build(instance, "slant", QuickPropertyType.build("slant", TypeToken.of(Double.class))//
			.buildContext(ctx -> {
				ctx.withValue("normal", ObservableValue.constant(TypeToken.of(Double.TYPE), normalSlant));
				ctx.withValue("italic", ObservableValue.constant(TypeToken.of(Double.TYPE), italic));
			}).build(), 0d).validate(new QuickProperty.ComparableValidator<>(0d, 1d)).inherited().build();
		instance.register(slant);
		underline = StyleAttribute.build(instance, "underline", QuickPropertyType.forEnum(Underline.class), Underline.none).inherited()
			.build();
		instance.register(underline);
		strike = StyleAttribute.build(instance, "strike", QuickPropertyType.boole, false).inherited().build();
		instance.register(strike);
		size = StyleAttribute.build(instance, "size", QuickPropertyType.floating, 12d)
			.validate(new QuickProperty.ComparableValidator<>(0.1d, 256d)).inherited().build();
		instance.register(size);
		kerning = StyleAttribute.build(instance, "kerning", QuickPropertyType.boole, true).inherited().build();
		instance.register(kerning);
		ligatures = StyleAttribute.build(instance, "ligatures", QuickPropertyType.boole, true).inherited().build();
		instance.register(ligatures);
		antiAlias = StyleAttribute.build(instance, "anti-alias", QuickPropertyType.boole, true).inherited().build();
		instance.register(antiAlias);
		wordWrap = StyleAttribute.build(instance, "word-wrap", QuickPropertyType.boole, true).inherited().build();
		instance.register(wordWrap);
		stretch = StyleAttribute.build(instance, "stretch", QuickPropertyType.floating, 1d)
			.validate(new QuickProperty.ComparableValidator<>(0.05d, 100d)).inherited().build();
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
