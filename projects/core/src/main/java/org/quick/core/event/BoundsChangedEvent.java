package org.quick.core.event;

import org.observe.util.TypeTokens;
import org.quick.core.Rectangle;

import com.google.common.reflect.TypeToken;

/** Fired when an element's position or size changes */
public class BoundsChangedEvent extends QuickPropertyEvent<Rectangle> {
	/** Filters events of this type */
	public static final java.util.function.Function<QuickEvent, BoundsChangedEvent> bounds = value -> {
		return value instanceof BoundsChangedEvent ? (BoundsChangedEvent) value : null;
	};

	/** The {@link TypeToken} for the {@link Rectangle} type */
	public static final TypeToken<Rectangle> BOUNDS_TYPE = TypeTokens.get().of(Rectangle.class);

	/**
	 * @param el The element whose bounds changed
	 * @param initial Whether this represents the population of the initial value of an observable value in response to subscription
	 * @param oldBounds The old bounds
	 * @param newBounds The new bounds
	 * @param cause The cause of this event
	 */
	public BoundsChangedEvent(org.quick.core.QuickElement el, boolean initial, Rectangle oldBounds, Rectangle newBounds, Object cause) {
		super(el, BOUNDS_TYPE, initial, oldBounds, newBounds, cause);
	}
}
