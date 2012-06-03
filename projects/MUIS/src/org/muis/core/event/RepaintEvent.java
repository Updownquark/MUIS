package org.muis.core.event;

/**
 * An event that represents the need of an element to be repainted
 */
public class RepaintEvent implements DocumentEvent
{
	private final org.muis.core.MuisElement theElement;

	/**
	 * Creates a repaint event
	 * 
	 * @param element The element that needs to be repainted
	 */
	public RepaintEvent(org.muis.core.MuisElement element)
	{
		theElement = element;
	}

	@Override
	public boolean contains(DocumentEvent evt)
	{
		return evt instanceof RepaintEvent
			&& theElement.isAncestor(((RepaintEvent) evt).theElement);
	}

	@Override
	public void doAction()
	{
		theElement.repaint(true);
	}
}
