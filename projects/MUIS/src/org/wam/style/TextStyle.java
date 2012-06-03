package org.wam.style;

import org.wam.core.WamElement;

/**
 * A specialized style for text elements
 */
public class TextStyle extends ElementStyle
{
	private ElementStyle theParentStyle;

	/**
	 * @param element The text element that this style is for
	 */
	public TextStyle(org.wam.core.WamTextElement element)
	{
		super(element);
		if(element.getParent() != null)
		{
			theParentStyle = element.getParent().getStyle();
			theParentStyle.addDependent(this);
		}
		element.addListener(WamElement.ELEMENT_MOVED,
			new org.wam.core.event.WamEventListener<WamElement>()
			{
				@Override
				public boolean isLocal()
				{
					return true;
				}

				@Override
				public void eventOccurred(org.wam.core.event.WamEvent<? extends WamElement> event,
					WamElement el)
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

	@Override
	public WamStyle getParent()
	{
		return theParentStyle;
	}
}
