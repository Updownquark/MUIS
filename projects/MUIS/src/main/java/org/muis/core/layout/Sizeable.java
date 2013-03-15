package org.muis.core.layout;

/** Represents an item that may report constraints and preferences for its size */
public interface Sizeable
{
	/** @return The size policy for this item's width */
	SizeGuide getWSizer();

	/** @return The size policy for this item's height */
	SizeGuide getHSizer();
}
