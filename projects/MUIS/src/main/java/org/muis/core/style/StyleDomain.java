package org.muis.core.style;

/** A domain for style attributes */
public interface StyleDomain extends Iterable<StyleAttribute<?>>
{
	/** @return The domain's name. This information is only used for messages--it does not affect how the styles are referred to from XML. */
	String getName();
}
