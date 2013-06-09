package org.muis.core.style;

import org.muis.core.MuisProperty;

/**
 * Contains style attributes pertaining to the background of a widget. These styles are supported by {@link org.muis.core.MuisElement} and
 * all of its subclasses unless the {@link org.muis.core.MuisElement#paintSelf(java.awt.Graphics2D, java.awt.Rectangle)} method is
 * overridden in a way that ignores them.
 */
public class BackgroundStyle implements StyleDomain {
	private StyleAttribute<?> [] theAttributes;

	private BackgroundStyle() {
		theAttributes = new StyleAttribute[0];
	}

	private void register(StyleAttribute<?> attr) {
		theAttributes = prisms.util.ArrayUtils.add(theAttributes, attr);
	}

	private static final BackgroundStyle instance;

	/** The texture to render an element's background with */
	public static final StyleAttribute<? extends Texture> texture;

	/** The color of a widget's background */
	public static final StyleAttribute<java.awt.Color> color;

	/** The transparency of a widget's background. Does not apply to the entire widget. */
	public static final StyleAttribute<Double> transparency;

	/** The radius of widget corners */
	public static final StyleAttribute<Size> cornerRadius;

	static {
		instance = new BackgroundStyle();
		texture = new StyleAttribute<>(instance, "texture", new MuisProperty.MuisTypeInstanceProperty<>(Texture.class), new BaseTexture());
		instance.register(texture);
		color = new StyleAttribute<>(instance, "color", MuisProperty.colorAttr, new java.awt.Color(255, 255, 255));
		instance.register(color);
		transparency = new StyleAttribute<>(instance, "transparency", MuisProperty.amountAttr, 0d, new MuisProperty.ComparableValidator<>(
			0d, 1d));
		instance.register(transparency);
		cornerRadius = new StyleAttribute<>(instance, "corner-radius", SizePropertyType.instance, new Size(),
			new MuisProperty.ComparableValidator<>(new Size(), null));
		instance.register(cornerRadius);
	}

	/** @return The style domain for all background styles */
	public static BackgroundStyle getDomainInstance() {
		return instance;
	}

	@Override
	public String getName() {
		return "bg";
	}

	@Override
	public java.util.Iterator<StyleAttribute<?>> iterator() {
		return prisms.util.ArrayUtils.iterator(theAttributes, true);
	}
}