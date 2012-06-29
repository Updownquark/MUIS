package org.muis.base.style;

import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleDomain;

/** Styles relating to buttons in particular */
public class ButtonStyles implements StyleDomain
{
	private StyleAttribute<?> [] theAttributes;

	private ButtonStyles()
	{
		theAttributes = new StyleAttribute[0];
	}

	private void register(StyleAttribute<?> attr)
	{
		theAttributes = prisms.util.ArrayUtils.add(theAttributes, attr);
	}

	private static final ButtonStyles instance;

	/** The radius of the button texture's influence over the border of a widget */
	public static final StyleAttribute<Integer> radius;

	static
	{
		instance = new ButtonStyles();
		radius = StyleAttribute.createIntStyle(instance, "radius", 0, 100, 4);
		instance.register(radius);
	}

	/** @return The style domain for all background styles */
	public static ButtonStyles getDomainInstance()
	{
		return instance;
	}

	@Override
	public String getName()
	{
		return "button";
	}

	@Override
	public java.util.Iterator<StyleAttribute<?>> iterator()
	{
		return prisms.util.ArrayUtils.iterator(theAttributes);
	}
}
