package org.quick.core.style;

import org.observe.ObservableValue;
import org.observe.ObservableValueEvent;
import org.observe.Observer;
import org.observe.Subscription;

import com.google.common.reflect.TypeToken;

/**
 * A style value that is contingent upon a boolean expression of some type
 *
 * @param <E> The type of expression that must evaluate to true for an expression value of a given type in order for the value to be applied
 * @param <V> The type of style value
 */
public class StyleExpressionValue<E extends StyleExpression<E>, V> implements ObservableValue<V> {
	/** Compares style expressions such that higher priority ones come out first */
	public static final java.util.Comparator<StyleExpressionValue<? extends StyleExpression<?>, ?>> STYLE_EXPRESSION_COMPARE;

	static {
		STYLE_EXPRESSION_COMPARE = (StyleExpressionValue<? extends StyleExpression<?>, ?> o1,
			StyleExpressionValue<? extends StyleExpression<?>, ?> o2) -> {
			StyleExpression<?> exp1 = o1.getExpression();
			StyleExpression<?> exp2 = o2.getExpression();
			if(exp1 == null)
				return exp2 == null ? 0 : 1;
			if(exp2 == null)
				return -1;
			return exp2.getPriority() - exp1.getPriority();
		};
	}

	private final E theExpression;
	private final ObservableValue<V> theValue;
	private final SafeStyleValue<V> theSafeValue;

	/**
	 * @param expression The expression that must be true for this value to be active
	 * @param value The value for the attribute to enact if the expression is true
	 * @param safeValue The validated value
	 */
	public StyleExpressionValue(E expression, ObservableValue<V> value, SafeStyleValue<V> safeValue) {
		theExpression = expression;
		theValue = value;
		theSafeValue = safeValue;
	}

	/** @return The expression determining when this value applies */
	public E getExpression() {
		return theExpression;
	}

	@Override
	public TypeToken<V> getType() {
		return theValue.getType();
	}

	@Override
	public V get() {
		return theSafeValue != null ? theSafeValue.get() : theValue.get();
	}

	@Override
	public boolean isSafe() {
		return false;
	}

	@Override
	public Subscription subscribe(Observer<? super ObservableValueEvent<V>> observer) {
		return theSafeValue != null ? theSafeValue.subscribe(observer) : theValue.subscribe(observer);
	}

	@Override
	public int hashCode() {
		int ret = 0;
		if(theExpression != null)
			ret += theExpression.hashCode();
		ret *= 13;
		if(theValue != null)
			ret += theValue.hashCode();
		return ret;
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof StyleExpressionValue))
			return false;
		StyleExpressionValue<?, ?> sev = (StyleExpressionValue<?, ?>) obj;
		return org.qommons.ArrayUtils.equals(theExpression, sev.theExpression) && org.qommons.ArrayUtils.equals(theValue, sev.theValue);
	}

	@Override
	public String toString() {
		return theExpression + "=" + theValue;
	}
}
