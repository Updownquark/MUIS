package org.quick.core.style.attach;

import org.quick.core.QuickDocument;

/** A named style group within Quick */
public class NamedStyleGroup extends TypedStyleGroup<org.quick.core.QuickElement> {
	private String theName;

	/**
	 * Creates a named style group
	 *
	 * @param doc The document that this named style group is for
	 * @param name The name for the group
	 */
	public NamedStyleGroup(QuickDocument doc, String name) {
		super(doc, null, org.quick.core.QuickElement.class, name);
		if(doc == null)
			throw new IllegalArgumentException("Cannot create a named style group without a document");
		theName = name;
	}

	/** @return The name of this style group */
	public String getName() {
		return theName;
	}
}
