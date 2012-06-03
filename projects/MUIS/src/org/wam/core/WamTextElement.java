package org.wam.core;

/**
 * A WAM element that serves as a placeholder for text content which may be interspersed with
 * element children in an element.
 */
public class WamTextElement extends WamElement implements Splittable
{
	private String theText;

	/**
	 * Creates a WAM text element
	 */
	public WamTextElement()
	{
	}

	/**
	 * Creates a WAM text element with text
	 * 
	 * @param text The text for the element
	 */
	public WamTextElement(String text)
	{
		theText = text;
	}

	/**
	 * @param text The text content for this element
	 */
	public void setText(String text)
	{
		theText = text;
	}

	/**
	 * @return This element's text content
	 */
	public String getText()
	{
		return theText;
	}

	public java.awt.Font getFont()
	{
		// TODO
		return java.awt.Font.getFont("Arial");
	}

	@Override
	public WamTextElement split(int width, int height)
	{
	}

	@Override
	public void combine(Splittable split)
	{
		theText += ((WamTextElement) split).theText;
	}
}
