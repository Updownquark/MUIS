package org.quick.core.event;

import java.awt.Rectangle;

import org.quick.core.mgr.ElementBounds;

/** Fired when an element's position or size changes */
public abstract class BoundsChangedEvent extends QuickPropertyEvent<Rectangle> {
	/** Filters events of this type */
	public static final java.util.function.Function<QuickEvent, BoundsChangedEvent> bounds = value -> {
		return value instanceof BoundsChangedEvent && !((BoundsChangedEvent) value).isOverridden() ? (BoundsChangedEvent) value : null;
	};

	/**
	 * @param el The element whose bounds changed
	 * @param observable The element bounds that is firing this event
	 * @param initial Whether this represents the population of the initial value of an observable value in response to subscription
	 * @param oldBounds The old bounds
	 * @param newBounds The new bounds
	 * @param cause The cause of this event
	 */
	public BoundsChangedEvent(org.quick.core.QuickElement el, ElementBounds observable, boolean initial, Rectangle oldBounds,
		Rectangle newBounds, QuickEvent cause) {
		super(el, observable, initial, oldBounds, newBounds, cause);
	}

	@Override
	public ElementBounds getObservable() {
		return (ElementBounds) super.getObservable();
	}
}
