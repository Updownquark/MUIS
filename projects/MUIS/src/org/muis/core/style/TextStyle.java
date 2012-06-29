package org.muis.core.style;

import org.muis.core.MuisElement;

/** A specialized style for text elements */
public class TextStyle extends ElementStyle
{
	private ElementStyle theParentStyle;

	/** @param element The text element that this style is for */
	public TextStyle(org.muis.core.MuisTextElement element)
	{
		super(element);
		if(element.getParent() != null)
		{
			theParentStyle = element.getParent().getStyle();
			theParentStyle.addDependent(this);
		}
		element.addListener(MuisElement.ELEMENT_MOVED, new org.muis.core.event.MuisEventListener<MuisElement>() {
			@Override
			public boolean isLocal()
			{
				return true;
			}

			@Override
			public void eventOccurred(org.muis.core.event.MuisEvent<MuisElement> event, MuisElement el)
			{
				if(theParentStyle != null)
					theParentStyle.removeDependent(TextStyle.this);
				if(event.getValue() != null)
				{
					theParentStyle = event.getValue().getStyle();
					theParentStyle.addDependent(TextStyle.this);
				}
				else
					theParentStyle = null;
			}
		});
	}
}
