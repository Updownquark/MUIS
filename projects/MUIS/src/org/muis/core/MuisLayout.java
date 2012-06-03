package org.muis.core;

import org.muis.layout.SizePolicy;

/**
 * Manages the position and size of children in a container
 */
public interface MuisLayout
{
	/**
	 * Allows the layout to see the children it may be laying out before they are initialized. This
	 * allows the layout to do things such as install allowed and required attribute templates that
	 * may be needed for instructions on where each widget goes.
	 * 
	 * @param parent The parent that the children will be layed out within
	 * @param children The children that may be layed out
	 */
	void initChildren(MuisElement parent, MuisElement [] children);

	/**
	 * @param parent The parent that the children will be layed out within
	 * @param children The children to get the width policy for
	 * @param parentHeight The height that this layout has determined for the parent, or <=0 if this
	 *        information has not been decided
	 * @return The size policy that determines how a container using this layout should be sized
	 *         horizontally
	 */
	SizePolicy getWSizer(MuisElement parent, MuisElement [] children, int parentHeight);

	/**
	 * @param parent The parent that the children will be layed out within
	 * @param children The children to get the height policy for
	 * @param parentWidth The width that this layout has determined for the parent, or <=0 if this
	 *        information has not been decided
	 * @return The size policy that determines how a container using this layout should be sized
	 *         vertically
	 */
	SizePolicy getHSizer(MuisElement parent, MuisElement [] children, int parentWidth);

	/**
	 * Adjusts the position and size of the container's children according to this layout's scheme
	 * 
	 * @param parent The container of the children to layout. The value is not guaranteed to be an
	 *        implementation of {@link MuisContainer}.
	 * @param children The children to adjust within the container
	 * @param box The rectangle within the parent to layout the children within
	 */
	void layout(MuisElement parent, MuisElement [] children, java.awt.Rectangle box);

	/**
	 * Called when this layout is removed from the container
	 * 
	 * @param parent The layout container that this layout formerly governed
	 */
	void remove(MuisElement parent);
}
