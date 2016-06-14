package org.quick.core.style;

import org.qommons.IterableUtils;
import org.quick.core.layout.LayoutAttributes;

/** Styles that may affect layout */
public class LayoutStyle implements StyleDomain {
	private StyleAttribute<?> [] theAttributes;

	private LayoutStyle() {
		theAttributes = new StyleAttribute[0];
	}

	private void register(StyleAttribute<?> attr) {
		theAttributes = org.qommons.ArrayUtils.add(theAttributes, attr);
	}

	private static final LayoutStyle instance;

	/** The minimum distance between the edge of a container and the nearest contained component */
	public static final StyleAttribute<Size> margin;

	/** The minimum distance between components in a container */
	public static final StyleAttribute<Size> padding;

	static {
		instance = new LayoutStyle();
		margin = StyleAttribute.build(instance, "margin", LayoutAttributes.sizeType, new Size(3, LengthUnit.pixels)).build();
		instance.register(margin);
		padding = StyleAttribute.build(instance, "padding", LayoutAttributes.sizeType, new Size(2, LengthUnit.pixels)).build();
		instance.register(padding);
	}

	/** @return The style domain for all layout styles */
	public static LayoutStyle getDomainInstance() {
		return instance;
	}

	@Override
	public String getName() {
		return "layout";
	}

	@Override
	public java.util.Iterator<StyleAttribute<?>> iterator() {
		return IterableUtils.iterator(theAttributes, true);
	}
}
