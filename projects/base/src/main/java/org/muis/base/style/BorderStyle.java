package org.muis.base.style;

import org.muis.core.MuisProperty;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleDomain;

/** Styles applying to borders */
public class BorderStyle implements StyleDomain {
	private StyleAttribute<?> [] theAttributes;

	private BorderStyle() {
		theAttributes = new StyleAttribute[0];
	}

	private void register(StyleAttribute<?> attr) {
		theAttributes = prisms.util.ArrayUtils.add(theAttributes, attr);
	}

	private static final BorderStyle instance;

	/** The thickness of the border, in pixels */
	public static final StyleAttribute<Long> thickness;

	/** The number of pixels to inset the border */
	public static final StyleAttribute<Long> inset;

	/** The color of a widget's border */
	public static final StyleAttribute<java.awt.Color> color;

	/** The length of the dashes in a border */
	public static final StyleAttribute<Double> dashLength;

	/** The distance between dashes in a border */
	public static final StyleAttribute<Double> dashInterim;

	static {
		instance = new BorderStyle();
		thickness = new StyleAttribute<>(instance, "thickness", MuisProperty.intAttr, 1L, new MuisProperty.ComparableValidator<>(0L, 1000L));
		instance.register(thickness);
		inset = new StyleAttribute<>(instance, "inset", MuisProperty.intAttr, 1L, new MuisProperty.ComparableValidator<>(0L, 1000L));
		instance.register(inset);
		color = new StyleAttribute<>(instance, "color", MuisProperty.colorAttr, new java.awt.Color(0, 0, 0));
		instance.register(color);
		dashLength = new StyleAttribute<>(instance, "dash-length", MuisProperty.floatAttr, 3d, new MuisProperty.ComparableValidator<>(1d,
			1000d));
		instance.register(dashLength);
		dashInterim = new StyleAttribute<>(instance, "dash-interim", MuisProperty.floatAttr, 0d, new MuisProperty.ComparableValidator<>(0d,
			1000d));
		instance.register(dashInterim);
	}

	/** @return The style domain for all border styles */
	public static BorderStyle getDomainInstance() {
		return instance;
	}

	@Override
	public String getName() {
		return "border-style";
	}

	@Override
	public java.util.Iterator<StyleAttribute<?>> iterator() {
		return prisms.util.ArrayUtils.iterator(theAttributes, true);
	}
}
