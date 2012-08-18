package org.muis.core;

/**
 * Metadata for a MUIS document
 */
public class MuisHeadSection
{
	private String theTitle;

	/**
	 * Creates a head section
	 */
	public MuisHeadSection()
	{
	}

	/**
	 * @return The title for the MUIS document
	 */
	public String getTitle()
	{
		return theTitle;
	}

	/**
	 * @param title The title for the document
	 */
	public void setTitle(String title)
	{
		theTitle = title;
	}
}
