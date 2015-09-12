package org.quick.core;

import org.quick.core.layout.SizeGuide;

/** Manages the position and size of children in a container */
public interface QuickLayout {
	/**
	 * Allows the layout to see the children it may be laying out before they are initialized. This allows the layout to do things such as
	 * install allowed and required attribute templates that may be needed for instructions on where each widget goes.
	 *
	 * @param parent The parent that the children will be layed out within
	 * @param children The children that may be layed out
	 */
	void initChildren(QuickElement parent, QuickElement [] children);

	/**
	 * @param parent The parent that the child is being added to
	 * @param child The child that is being added to the parent
	 */
	void childAdded(QuickElement parent, QuickElement child);

	/**
	 * @param parent The parent that the child is being removed from
	 * @param child The child that is being removed from the parent
	 */
	void childRemoved(QuickElement parent, QuickElement child);

	/**
	 * Called when this layout is removed from the container
	 *
	 * @param parent The layout container that this layout formerly governed
	 */
	void remove(QuickElement parent);

	/**
	 * @param parent The parent that the children will be layed out within
	 * @param children The children to get the width policy for
	 * @return The size policy that determines how a container using this layout should be sized horizontally
	 */
	SizeGuide getWSizer(QuickElement parent, QuickElement [] children);

	/**
	 * @param parent The parent that the children will be layed out within
	 * @param children The children to get the height policy for
	 * @return The size policy that determines how a container using this layout should be sized vertically
	 */
	SizeGuide getHSizer(QuickElement parent, QuickElement [] children);

	/**
	 * Adjusts the position and size of the container's children according to this layout's scheme
	 *
	 * @param parent The container of the children to layout.
	 * @param children The children to adjust within the container
	 */
	void layout(QuickElement parent, QuickElement [] children);
}
