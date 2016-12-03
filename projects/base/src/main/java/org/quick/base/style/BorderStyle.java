package org.quick.base.style;

import org.quick.core.prop.QuickProperty;
import org.quick.core.prop.QuickPropertyType;
import org.quick.core.style.StyleAttribute;
import org.quick.core.style.StyleDomain;

/** Styles applying to borders */
public class BorderStyle implements StyleDomain {
	private StyleAttribute<?>[] theAttributes;

	private BorderStyle() {
		theAttributes = new StyleAttribute[0];
	}

	private void register(StyleAttribute<?> attr) {
		theAttributes = org.qommons.ArrayUtils.add(theAttributes, attr);
	}

	private static final BorderStyle instance;

	/** The thickness of the border, in pixels */
	public static final StyleAttribute<Integer> thickness;

	/** The number of pixels to inset the border */
	public static final StyleAttribute<Integer> inset;

	/** The color of a widget's border */
	public static final StyleAttribute<java.awt.Color> color;

	/** The transparency of a widget's border */
	public static final StyleAttribute<Double> transparency;

	/** The length of the dashes in a border */
	public static final StyleAttribute<Double> dashLength;

	/** The distance between dashes in a border */
	public static final StyleAttribute<Double> dashInterim;

	static {
		instance = new BorderStyle();
		thickness = StyleAttribute.build(instance, "thickness", QuickPropertyType.integer, 1)
			.validate(new QuickProperty.ComparableValidator<>(0, 1000)).build();
		instance.register(thickness);
		inset = StyleAttribute.build(instance, "inset", QuickPropertyType.integer, 1)
			.validate(new QuickProperty.ComparableValidator<>(0, 1000)).build();
		instance.register(inset);
		color = StyleAttribute.build(instance, "color", QuickPropertyType.color, new java.awt.Color(0, 0, 0)).build();
		instance.register(color);
		transparency = StyleAttribute.build(instance, "transparency", QuickPropertyType.floating, 0d)
			.validate(new QuickProperty.ComparableValidator<>(0d, 1d)).build();
		instance.register(transparency);
		dashLength = StyleAttribute.build(instance, "dash-length", QuickPropertyType.floating, 3d)
			.validate(new QuickProperty.ComparableValidator<>(1d, 1000d)).build();
		instance.register(dashLength);
		dashInterim = StyleAttribute.build(instance, "dash-interim", QuickPropertyType.floating, 0d)
			.validate(new QuickProperty.ComparableValidator<>(0d, 1000d)).build();
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
		return org.qommons.IterableUtils.iterator(theAttributes, true);
	}
}
