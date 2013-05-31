package org.muis.base.style;

import org.muis.core.MuisProperty;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleDomain;

/** Styles relating specifically to buttons */
public class ButtonStyles implements StyleDomain {
	private StyleAttribute<?> [] theAttributes;

	private ButtonStyles() {
		theAttributes = new StyleAttribute[0];
	}

	private void register(StyleAttribute<?> attr) {
		theAttributes = prisms.util.ArrayUtils.add(theAttributes, attr);
	}

	private static final ButtonStyles instance;

	/** The number of pixels that a "click" can move between its start and end and still be an actionable event */
	public static final StyleAttribute<Double> clickTolerance;

	static {
		instance = new ButtonStyles();
		clickTolerance = new StyleAttribute<>(instance, "transparency", MuisProperty.floatAttr, 5d, new MuisProperty.ComparableValidator<>(
			0d, Double.MAX_VALUE));
		instance.register(clickTolerance);
	}

	/** @return The style domain for all background styles */
	public static ButtonStyles getDomainInstance() {
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
