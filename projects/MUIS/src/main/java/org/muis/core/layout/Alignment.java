package org.muis.core.layout;

/** Alignment options */
public enum Alignment
{
	/** Left- or top-alignment */
	begin,
	/** Right- or bottom-alignment */
	end,
	/** Center-alignment, where the center of the target is positioned at the center of the container in the cross-dimension */
	center,
	/** Justified-alignment, where the target is sized to take up all the space in the cross-dimension of the container */
	justify;
}
