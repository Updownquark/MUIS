package org.wam.style;

import org.wam.core.WamDocument;

/**
 * A named style group within WAM
 */
public class NamedStyleGroup extends TypedStyleGroup<org.wam.core.WamElement>
{
	private final WamDocument theDocument;

	private String theName;

	/**
	 * Creates a named style group
	 * 
	 * @param doc The document that this named style group is for
	 * @param name The name for the group
	 */
	public NamedStyleGroup(WamDocument doc, String name)
	{
		super(null, org.wam.core.WamElement.class);
		if(doc == null)
			throw new IllegalArgumentException(
				"Cannot create a named style group without a document");
		theDocument = doc;
		theName = name;
	}

	/**
	 * @return The document that this named style group is for
	 */
	public WamDocument getDocument()
	{
		return theDocument;
	}

	/**
	 * @return The name of this style group
	 */
	public String getName()
	{
		return theName;
	}
}
