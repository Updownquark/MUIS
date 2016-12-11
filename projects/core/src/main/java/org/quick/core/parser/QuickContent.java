package org.quick.core.parser;

import java.util.Map;

import org.qommons.Sealable;

/** Represents Quick content as defined in XML */
public abstract class QuickContent implements Sealable {
	private final WidgetStructure theParent;

	private Map<String, String> theAttributes;

	private boolean isSealed;

	/** @param parent The parent structure of this content */
	public QuickContent(WidgetStructure parent) {
		theParent = parent;
		theAttributes = new java.util.LinkedHashMap<>();
	}

	@Override
	public boolean isSealed() {
		return isSealed;
	}

	@Override
	public void seal() {
		isSealed = true;
		theAttributes = java.util.Collections.unmodifiableMap(theAttributes);
	}

	/** @return The parent structure of this content */
	public WidgetStructure getParent() {
		return theParent;
	}

	/** @return The attribute map for this widget */
	public Map<String, String> getAttributes() {
		return theAttributes;
	}

	/**
	 * @param attName The name of the attribute to set in this widget
	 * @param attValue The value to set for the attribute
	 */
	public void addAttribute(String attName, String attValue) {
		if (isSealed)
			throw new SealedException(this);
		theAttributes.put(attName, attValue);
	}

	/** @return A string representation of this content's attributes */
	public String attrString() {
		StringBuilder ret = new StringBuilder();
		for (Map.Entry<String, String> attr : theAttributes.entrySet())
			ret.append(' ').append(attr.getKey()).append("=\"").append(attr.getValue()).append('"');
		return ret.toString();
	}
}
