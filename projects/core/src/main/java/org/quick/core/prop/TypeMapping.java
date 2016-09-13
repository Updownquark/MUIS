package org.quick.core.prop;

import org.qommons.ex.ExFunction;
import org.quick.core.QuickException;

import com.google.common.reflect.TypeToken;

/**
 * Represents a conversion from one type to another
 *
 * @param <F> The type that this mapping knows how to convert from
 * @param <T> The type of values that this mapping produces
 */
public class TypeMapping<F, T> {
	private final TypeToken<F> theFromType;
	private final TypeToken<T> theToType;

	private final ExFunction<? super F, ? extends T, QuickException> theMap;
	private final ExFunction<? super T, ? extends F, QuickException> theReverseMap;

	/**
	 * @param from The type that this mapping knows how to convert from
	 * @param to The type of values that this mapping produces
	 * @param map The conversion function
	 * @param reverseMap The reverse conversion function. May be null.
	 */
	public TypeMapping(TypeToken<F> from, TypeToken<T> to, ExFunction<? super F, ? extends T, QuickException> map,
		ExFunction<? super T, ? extends F, QuickException> reverseMap) {
		theFromType = from;
		theToType = to;
		theMap = map;
		theReverseMap = reverseMap;
	}

	/** @return The type that this mapping knows how to convert from */
	public TypeToken<F> getFromType() {
		return theFromType;
	}

	/** @return The type of values that this mapping produces */
	public TypeToken<T> getToType() {
		return theToType;
	}

	/** @return The conversion function */
	public ExFunction<? super F, ? extends T, QuickException> getMap() {
		return theMap;
	}

	/** @return The reverse conversion function */
	public ExFunction<? super T, ? extends F, QuickException> getReverseMap() {
		return theReverseMap;
	}

	@Override
	public String toString() {
		return theFromType + "->" + theToType;
	}
}