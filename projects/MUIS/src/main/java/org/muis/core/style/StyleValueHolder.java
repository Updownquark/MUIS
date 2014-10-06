package org.muis.core.style;

import java.util.List;

import org.muis.core.rx.DefaultObservableList;

import prisms.lang.Type;

/**
 * A utility class used by {@link SimpleConditionalStyle}
 *
 * @param <E> The type of expression supported by the style set using this class
 * @param <V> The type of the attribute that this holder holds values for
 */
public class StyleValueHolder<E extends StyleExpression<E>, V> extends DefaultObservableList<StyleExpressionValue<E, V>> implements
	Cloneable {
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

	private List<StyleExpressionValue<E, V>> theController;

	/** @param value The initial expression value to hold */
	protected StyleValueHolder(StyleExpressionValue<E, V> value) {
		super(new Type(StyleExpressionValue.class, new Type(value.getExpression().getClass()), value.getType()));
		theController = control(null);
		if(value != null)
			theController.add(value);
	}

	/** @param value The expression value to add to this holder */
	public void addValue(StyleExpressionValue<E, V> value) {
		int index = java.util.Collections.binarySearch(this, value, STYLE_EXPRESSION_COMPARE);
		if(index >= 0) {
			while(index < size() && STYLE_EXPRESSION_COMPARE.compare(get(index), value) == 0)
				index++;
		} else {
			index = -(index + 1);
		}
		theController.add(index, value);
	}

	/**
	 * Removes the value for the given expression from this holder
	 *
	 * @param exp The expression to remove the value for
	 * @return Whether this operation might have caused an attribute value to be different for some condition
	 */
	public boolean removeExpression(E exp) {
		boolean fireEvent = true;
		java.util.Iterator<StyleExpressionValue<E, V>> iter = theController.iterator();
		while(iter.hasNext()) {
			StyleExpressionValue<E, V> next = iter.next();
			if(java.util.Objects.equals(exp, next.getExpression())) {
				iter.remove();
				return fireEvent;
			} else if(next.getExpression() == null || (exp != null && next.getExpression().getWhenTrue(exp) > 0))
				fireEvent = false; // Higher-priority expression that encompasses the given expression--no event
		}
		return false;
	}

	void set(StyleExpressionValue<E, V> sev) {
		boolean found = true;
		java.util.ListIterator<StyleExpressionValue<E, V>> iter = theController.listIterator();
		while(iter.hasNext()) {
			StyleExpressionValue<E, V> next = iter.next();
			if(java.util.Objects.equals(sev.getExpression(), next.getExpression())) {
				found = true;
				iter.set(sev);
				break;
			}
		}
		if(!found)
			iter.add(sev);
	}

	@Override
	protected StyleValueHolder<E, V> clone() {
		StyleValueHolder<E, V> ret;
		try {
			ret = (StyleValueHolder<E, V>) super.clone();
		} catch(CloneNotSupportedException e) {
			throw new IllegalStateException(e);
		}
		ret.theController = ret.control(null);
		return ret;
	}
}