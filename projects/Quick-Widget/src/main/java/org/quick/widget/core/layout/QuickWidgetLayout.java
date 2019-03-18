package org.quick.widget.core.layout;

import java.util.List;

import org.observe.Observable;
import org.quick.core.QuickLayout;
import org.quick.core.layout.Orientation;
import org.quick.util.CompoundListener;
import org.quick.widget.core.QuickWidget;

public interface QuickWidgetLayout extends QuickLayout {
	CompoundListener.EventListener<QuickWidget<?>> sizeNeedsChanged = (evented, root, event) -> evented.sizeNeedsChanged();
	CompoundListener.EventListener<QuickWidget<?>> layout = (evented, root, event) -> evented.relayout(false);

	/**
	 * Installs this layout for an element
	 *
	 * @param parent The widget to layout the children of
	 * @param until An observable that will fire when this layout should cease laying out the parent
	 */
	void install(QuickWidget<?> parent, Observable<?> until);

	/**
	 * @param parent The parent that the children will be layed out within
	 * @param children The children to get the width policy for
	 * @return The size policy that determines how a container using this layout should be sized
	 */
	SizeGuide getSizer(QuickWidget<?> parent, Iterable<? extends QuickWidget<?>> children, Orientation orientation);

	/**
	 * Adjusts the position and size of the container's children according to this layout's scheme
	 *
	 * @param parent The container of the children to layout.
	 * @param children The children to adjust within the container
	 */
	void layout(QuickWidget<?> parent, List<? extends QuickWidget<?>> children);
}
