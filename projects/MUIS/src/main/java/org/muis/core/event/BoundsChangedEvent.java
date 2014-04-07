package org.muis.core.event;

import java.awt.Rectangle;

import org.muis.core.event.boole.TypedPredicate;

/** Fired when an element's position or size changes */
public abstract class BoundsChangedEvent extends MuisPropertyEvent<Rectangle> {
	/** Filters events of this type */
	public static final TypedPredicate<MuisEvent, BoundsChangedEvent> bounds = new TypedPredicate<MuisEvent, BoundsChangedEvent>() {
		@Override
		public BoundsChangedEvent cast(MuisEvent value) {
			return value instanceof BoundsChangedEvent && !((BoundsChangedEvent) value).isOverridden() ? (BoundsChangedEvent) value : null;
		}
	};

	private final Rectangle theOldValue;

	/**
	 * @param el The element whose bounds changed
	 * @param oldBounds The old bounds
	 * @param newBounds The new bounds
	 */
	public BoundsChangedEvent(org.muis.core.MuisElement el, Rectangle oldBounds, Rectangle newBounds) {
		super(el, newBounds);
		theOldValue = oldBounds;
	}

	/** @return The value of the bounds before it was changed */
	public Rectangle getOldValue() {
		return theOldValue;
	}
}
