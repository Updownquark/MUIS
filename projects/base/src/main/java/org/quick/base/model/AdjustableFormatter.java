package org.quick.base.model;

import org.quick.core.model.QuickDocumentModel;

/**
 * A QuickFormatter that knows how to adjust values of its type
 *
 * @param <T> The type of values that this formatter understands
 */
public interface AdjustableFormatter<T> extends QuickFormatter<T> {
	/**
	 * @param value The value to increment
	 * @return The incremented value
	 */
	T increment(T value);
	/**
	 * @param value The value to increment
	 * @return Null if the increment operation is enabled, or a message detailing why it is disabled
	 */
	String isIncrementEnabled(T value);
	/**
	 * @param value The value to decrement
	 * @return The decremented value
	 */
	T decrement(T value);
	/**
	 * @param value The value to decrement
	 * @return Null if the decrement operation is enabled, or a message detailing why it is disabled
	 */
	String isDecrementEnabled(T value);

	/**
	 * A factory that produces AdjustableFormatters
	 *
	 * @param <T> The type of formatters that this factory produces
	 */
	public interface Factory<T> extends QuickFormatter.Factory<T> {
		@Override
		AdjustableFormatter<T> create(QuickDocumentModel doc);
	}
}
