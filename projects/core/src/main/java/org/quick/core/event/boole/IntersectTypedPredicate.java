package org.quick.core.event.boole;

/**
 * Represents the AND operation of two component predicates
 * 
 * @param <F> The type of objects that this predicate can test
 * @param <MT> The intermediate type that the first chid predicate produces
 * @param <MF> The second intermediate type that the second child can test
 * @param <T> The type of objects that this predicate produces
 */
public interface IntersectTypedPredicate<F, MT extends MF, MF, T> extends TypedPredicate<F, T> {
	/** @return This AND operation's first tester */
	TypedPredicate<F, MT> getFirst();

	/** @return This AND operation's second tester */
	TypedPredicate<MF, T> getSecond();
}
