package org.muis.base.widget;

import org.muis.core.tags.Template;

/** Implements a button. Buttons can be set to toggle mode or normal mode. Buttons are containers that may have any type of content in them. */
@Template(location = "../../../../button.muis")
public class Button extends org.muis.core.MuisTemplate2 {
	@Override
	public void doLayout() {
		org.muis.core.MuisElement contents = getElement(getTemplate().getAttachPoint("contents"));
		org.muis.core.style.Size radius = getStyle().getSelf().get(org.muis.core.style.BackgroundStyles.cornerRadius);
		int w = getWidth();
		int h = getHeight();
		int lOff = radius.evaluate(w);
		int tOff = radius.evaluate(h);
		contents.setBounds(lOff, tOff, w - lOff - lOff, h - tOff - tOff);
	}
}
