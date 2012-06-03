package org.wam.core.event;

/**
 * An event that represents the need of an element to be repainted
 */
public class RepaintEvent implements DocumentEvent
{
	private final org.wam.core.WamElement theElement;

	/**
	 * Creates a repaint event
	 * 
	 * @param element The element that needs to be repainted
	 */
	public RepaintEvent(org.wam.core.WamElement element)
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
