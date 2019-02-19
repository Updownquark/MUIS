package org.quick.core;

import java.util.List;

import org.observe.Observable;
import org.quick.core.layout.Orientation;
import org.quick.core.layout.SizeGuide;

/** Manages the position and size of children in a container */
public interface QuickLayout {
	/**
	 * Installs this layout for an element
	 *
	 * @param parent The element to layout the children of
	 * @param until An observable that will fire when this layout should cease laying out the parent
	 */
	void install(QuickElement parent, Observable<?> until);

	/**
	 * @param parent The parent that the children will be layed out within
	 * @param children The children to get the width policy for
	 * @return The size policy that determines how a container using this layout should be sized
	 */
	SizeGuide getSizer(QuickElement parent, Iterable<? extends QuickElement> children, Orientation orientation);

	/**
	 * Adjusts the position and size of the container's children according to this layout's scheme
	 *
	 * @param parent The container of the children to layout.
	 * @param children The children to adjust within the container
	 */
	void layout(QuickElement parent, List<? extends QuickElement> children);
}
