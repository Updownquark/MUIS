package org.muis.core.style;

import org.muis.core.MuisDocument;

/**
 * A named style group within MUIS
 */
public class NamedStyleGroup extends TypedStyleGroup<org.muis.core.MuisElement>
{
	private final MuisDocument theDocument;

	private String theName;

	/**
	 * Creates a named style group
	 * 
	 * @param doc The document that this named style group is for
	 * @param name The name for the group
	 */
	public NamedStyleGroup(MuisDocument doc, String name)
	{
		super(null, org.muis.core.MuisElement.class);
		if(doc == null)
			throw new IllegalArgumentException(
				"Cannot create a named style group without a document");
		theDocument = doc;
		theName = name;
	}

	/**
	 * @return The document that this named style group is for
	 */
	public MuisDocument getDocument()
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
