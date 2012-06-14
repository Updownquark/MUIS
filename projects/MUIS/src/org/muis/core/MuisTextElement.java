package org.muis.core;

/** A MUIS element that serves as a placeholder for text content which may be interspersed with element children in an element. */
public class MuisTextElement extends MuisElement
{
	private String theText;

	/** Creates a MUIS text element */
	public MuisTextElement()
	{
	}

	/**
	 * Creates a MUIS text element with text
	 *
	 * @param text The text for the element
	 */
	public MuisTextElement(String text)
	{
		theText = text;
	}

	/** @param text The text content for this element */
	public void setText(String text)
	{
		theText = text;
	}

	/** @return This element's text content */
	public String getText()
	{
		return theText;
	}
}
