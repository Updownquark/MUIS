package org.wam.core;

/**
 * Metadata for a WAM document
 */
public class WamHeadSection
{
	private String theTitle;

	/**
	 * Creates a head section
	 */
	public WamHeadSection()
	{
	}

	/**
	 * @return The title for the WAM document
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
