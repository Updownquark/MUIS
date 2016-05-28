package org.quick.base.style;

import org.quick.core.prop.QuickProperty;
import org.quick.core.style.StyleAttribute;
import org.quick.core.style.StyleDomain;

/** Styles applying to borders */
public class BorderStyle implements StyleDomain {
	private StyleAttribute<?> [] theAttributes;

	private BorderStyle() {
		theAttributes = new StyleAttribute[0];
	}

	private void register(StyleAttribute<?> attr) {
		theAttributes = org.qommons.ArrayUtils.add(theAttributes, attr);
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
		thickness = new StyleAttribute<>(instance, "thickness", QuickProperty.intAttr, 1L, new QuickProperty.ComparableValidator<>(0L, 1000L));
		instance.register(thickness);
		inset = new StyleAttribute<>(instance, "inset", QuickProperty.intAttr, 1L, new QuickProperty.ComparableValidator<>(0L, 1000L));
		instance.register(inset);
		color = new StyleAttribute<>(instance, "color", QuickProperty.colorAttr, new java.awt.Color(0, 0, 0));
		instance.register(color);
		dashLength = new StyleAttribute<>(instance, "dash-length", QuickProperty.floatAttr, 3d, new QuickProperty.ComparableValidator<>(1d,
			1000d));
		instance.register(dashLength);
		dashInterim = new StyleAttribute<>(instance, "dash-interim", QuickProperty.floatAttr, 0d, new QuickProperty.ComparableValidator<>(0d,
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
		return org.qommons.ArrayUtils.iterator(theAttributes, true);
	}
}
