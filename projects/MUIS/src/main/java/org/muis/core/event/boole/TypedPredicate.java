package org.muis.core.event.boole;

/**
 * A predicate that can not only test, but convert objects
 *
 * @param <F> The type of objects that this predicate can test
 * @param <T> The type of objects that this predicate can convert to
 */
public interface TypedPredicate<F, T> {
	/**
	 * @param value The value to test and convert
	 * @return The converted value, or null if the value failed this test
	 */
	T cast(F value);
}
