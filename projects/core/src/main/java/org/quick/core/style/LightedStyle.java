package org.quick.core.style;

import java.awt.Color;

import org.qommons.IterableUtils;
import org.quick.core.prop.QuickProperty;
import org.quick.core.prop.QuickPropertyType;

import com.google.common.reflect.TypeToken;

/** Style attributes that pertain to lighting effects */
public class LightedStyle implements StyleDomain {
	private StyleAttribute<?>[] theAttributes;

	private LightedStyle() {
		theAttributes = new StyleAttribute[0];
	}

	private void register(StyleAttribute<?> attr) {
		theAttributes = org.qommons.ArrayUtils.add(theAttributes, attr);
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
			QuickPropertyType.build("source", TypeToken.of(Double.class)).withValues(str -> {
				switch (str) {
				case "top":
					return 0d;
				case "top-right":
					return 45d;
				case "right":
					return 90d;
				case "bottom-right":
					return 135d;
				case "bottom":
					return 180d;
				case "bottom-left":
					return 225d;
				case "left":
					return 270d;
				case "top-left":
					return 315d;
				default:
					return null;
				}
			}).build(), 315d, new QuickProperty.ComparableValidator<>(0d, 360d));
		instance.register(lightSource);
		lightColor = new StyleAttribute<>(instance, "color", QuickPropertyType.color, Color.white);
		instance.register(lightColor);
		shadowColor = new StyleAttribute<>(instance, "shadow", QuickPropertyType.color, Color.black);
		instance.register(shadowColor);
		maxShadingAmount = new StyleAttribute<>(instance, "max-amount", QuickPropertyType.floating, .5,
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
		return IterableUtils.iterator(theAttributes, true);
	}
}
