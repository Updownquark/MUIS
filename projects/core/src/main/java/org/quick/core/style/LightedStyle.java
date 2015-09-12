package org.quick.core.style;

import java.awt.Color;

import org.quick.core.QuickAttribute;
import org.quick.core.QuickProperty;

/** Style attributes that pertain to lighting effects */
public class LightedStyle implements StyleDomain {
	private StyleAttribute<?> [] theAttributes;

	private LightedStyle() {
		theAttributes = new StyleAttribute[0];
	}

	private void register(StyleAttribute<?> attr) {
		theAttributes = prisms.util.ArrayUtils.add(theAttributes, attr);
	}

	private static final LightedStyle instance;

	/** The direction (in degrees clockwise from the top) from which the light source lighting the texture is coming */
	public static final StyleAttribute<Double> lightSource;

	/** The color of the lighting */
	public static final StyleAttribute<Color> lightColor;

	/** The color of the shadowing */
	public static final StyleAttribute<Color> shadowColor;

	/** The maximum amount of shading that should be rendered as a result of lighting */
	public static final StyleAttribute<Double> maxShadingAmount;

	static {
		instance = new LightedStyle();
		lightSource = new StyleAttribute<>(instance, "source",
			new QuickProperty.NamedValuePropertyType<>(QuickProperty.floatAttr, "top", 0d, "top-right", 45d, "right", 90d, "bottom-right",
				135d, "bottom", 180d, "bottom-left", 225d, "left", 270d, "top-left", 315d),
			315d, new QuickProperty.ComparableValidator<>(0d, 360d));
		instance.register(lightSource);
		lightColor = new StyleAttribute<>(instance, "color", QuickAttribute.colorAttr, Color.white);
		instance.register(lightColor);
		shadowColor = new StyleAttribute<>(instance, "shadow", QuickAttribute.colorAttr, Color.black);
		instance.register(shadowColor);
		maxShadingAmount = new StyleAttribute<>(instance, "max-amount", QuickAttribute.floatAttr, .5,
			new QuickProperty.ComparableValidator<>(0d, 1d));
	}

	/** @return The style domain for all lighting styles */
	public static LightedStyle getDomainInstance() {
		return instance;
	}

	@Override
	public String getName() {
		return "light";
	}

	@Override
	public java.util.Iterator<StyleAttribute<?>> iterator() {
		return prisms.util.ArrayUtils.iterator(theAttributes, true);
	}
}
