package org.muis.core.style.attach;

import org.muis.core.MuisDocument;

/** A named style group within MUIS */
public class NamedStyleGroup extends TypedStyleGroup<org.muis.core.MuisElement> {
	private String theName;

	/**
	 * Creates a named style group
	 *
	 * @param doc The document that this named style group is for
	 * @param name The name for the group
	 */
	public NamedStyleGroup(MuisDocument doc, String name) {
		super(doc, null, org.muis.core.MuisElement.class, name);
		if(doc == null)
			throw new IllegalArgumentException("Cannot create a named style group without a document");
		theName = name;
	}

	/** @return The name of this style group */
	public String getName() {
		return theName;
	}
}
