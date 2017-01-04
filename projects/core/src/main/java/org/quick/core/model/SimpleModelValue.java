package org.quick.core.model;

import org.observe.SimpleSettableValue;

import com.google.common.reflect.TypeToken;

/**
 * A settable value that has its toString() value set
 * 
 * @param <T> The type of the value
 */
public class SimpleModelValue<T> extends SimpleSettableValue<T> {
	private final String theToString;

	/**
	 * @param type The type for the value
	 * @param nullable Whether this value can take null values
	 * @param toString The toString() value for this value
	 */
	public SimpleModelValue(Class<T> type, boolean nullable, String toString) {
		super(type, nullable);
		theToString = toString;
	}

	/**
	 * @param type The type for the value
	 * @param nullable Whether this value can take null values
	 * @param toString The toString() value for this value
	 */
	public SimpleModelValue(TypeToken<T> type, boolean nullable, String toString) {
		super(type, nullable);
		theToString = toString;
	}

	@Override
	public String toString() {
		return theToString;
	}
}
