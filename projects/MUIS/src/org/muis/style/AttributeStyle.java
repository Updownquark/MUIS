package org.muis.style;

import org.muis.core.MuisElement;

/** Represents a style set as an attribute on an element */
public class AttributeStyle extends MuisStyle
{
	private final MuisElement theElement;

	public AttributeStyle(MuisElement element)
	{
		theElement = element;
	}

	public MuisElement getElement()
	{
		return theElement;
	}

	@Override
	public MuisStyle getParent()
	{
		return null;
	}
}
