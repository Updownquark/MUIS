package org.muis.core.event;

import java.awt.Rectangle;

import org.muis.core.event.boole.TypedPredicate;

/** Fired when an element's position or size changes */
public class BoundsChangedEvent extends MuisPropertyEvent<Rectangle> {
	/** Filters events of this type */
	public static final TypedPredicate<MuisEvent, BoundsChangedEvent> bounds = new TypedPredicate<MuisEvent, BoundsChangedEvent>() {
		@Override
		public BoundsChangedEvent cast(MuisEvent value) {
			return value instanceof BoundsChangedEvent ? (BoundsChangedEvent) value : null;
		}
	};

	/**
	 * @param el The element whose bounds changed
	 * @param oldBounds The old bounds
	 * @param newBounds The new bounds
	 */
	public BoundsChangedEvent(org.muis.core.MuisElement el, Rectangle oldBounds, Rectangle newBounds) {
		super(el, oldBounds, newBounds);
	}
}
