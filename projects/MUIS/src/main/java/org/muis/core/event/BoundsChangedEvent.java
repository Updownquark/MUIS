package org.muis.core.event;

import java.awt.Rectangle;

import org.muis.core.mgr.ElementBounds;

/** Fired when an element's position or size changes */
public abstract class BoundsChangedEvent extends MuisPropertyEvent<Rectangle> {
	/** Filters events of this type */
	public static final java.util.function.Function<MuisEvent, BoundsChangedEvent> bounds = value -> {
		return value instanceof BoundsChangedEvent && !((BoundsChangedEvent) value).isOverridden() ? (BoundsChangedEvent) value : null;
	};

	/**
	 * @param el The element whose bounds changed
	 * @param observable The element bounds that is firing this event
	 * @param oldBounds The old bounds
	 * @param newBounds The new bounds
	 * @param cause The cause of this event
	 */
	public BoundsChangedEvent(org.muis.core.MuisElement el, ElementBounds observable, Rectangle oldBounds, Rectangle newBounds,
		MuisEvent cause) {
		super(el, observable, oldBounds, newBounds, cause);
	}

	@Override
	public ElementBounds getObservable() {
		return (ElementBounds) super.getObservable();
	}
}
