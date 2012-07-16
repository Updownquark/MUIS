package org.muis.core.annotations;

/** Available actions to take automatically */
public enum MuisActionType
{
	/** No action will be taken */
	none,
	/** The default action will be taken. If this value is set as the default action, it is equivalent to {@link #none}. */
	def,
	/** {@link org.muis.core.MuisElement#relayout(boolean)} will be called with a {@code false} argument */
	layout,
	/** {@link org.muis.core.MuisElement#repaint(java.awt.Rectangle, boolean)} will be called with {@code null, false} arguments */
	paint
}
