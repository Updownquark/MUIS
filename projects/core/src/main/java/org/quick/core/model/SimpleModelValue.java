package org.quick.core.model;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.observe.util.TypeTokens;
import org.quick.util.QuickValue;

import com.google.common.reflect.TypeToken;

/**
 * A settable value that has its toString() value set
 *
 * @param <T> The type of the value
 */
public class SimpleModelValue<T> extends QuickValue<T> {
	private final String theToString;

	/**
	 * @param type The type for the value
	 * @param nullable Whether this value can take null values
	 * @param toString The toString() value for this value
	 */
	public SimpleModelValue(ReentrantReadWriteLock lock, Class<T> type, boolean nullable, String toString) {
		this(lock, TypeTokens.get().of(type), nullable, toString);
	}

	/**
	 * @param type The type for the value
	 * @param nullable Whether this value can take null values
	 * @param toString The toString() value for this value
	 */
	public SimpleModelValue(ReentrantReadWriteLock lock, TypeToken<T> type, boolean nullable, String toString) {
		super(type, nullable, lock);
		theToString = toString;
	}

	@Override
	public String toString() {
		return theToString;
	}
}
