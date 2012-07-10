package org.muis.base.widget;

import org.muis.core.MuisLayout;

/** Implements a button. Buttons can be set to toggle mode or normal mode. Buttons are containers that may have any type of content in them. */
public class Button extends org.muis.core.LayoutContainer
{
	@Override
	protected MuisLayout getDefaultLayout()
	{
		return new org.muis.base.layout.SimpleLayout();
	}
}
