/*
 * Created Mar 18, 2009 by Andrew
 */
package org.muis.base.style;

/**
 * A type of font to render text in
 */
public enum FontType
{
	/** @see java.awt.Font#SANS_SERIF */
	SansSerif(java.awt.Font.SANS_SERIF),
	/** @see java.awt.Font#SERIF */
	Serif(java.awt.Font.SERIF);

	private final String theName;

	private FontType(String name)
	{
		theName = name;
	}

	/**
	 * @return The name of the font (see {@link java.awt.Font#getFontName()}
	 */
	public String getName()
	{
		return theName;
	}
}
