package org.quick.core.style;

import static org.quick.core.style.StyleExpressionValue.STYLE_EXPRESSION_COMPARE;

import org.observe.collect.ObservableList;
import org.observe.collect.impl.ObservableArrayList;
import org.observe.util.ObservableListWrapper;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

/**
 * A utility class used by {@link SimpleConditionalStyle}
 *
 * @param <E> The type of expression supported by the style set using this class
 * @param <V> The type of the attribute that this holder holds values for
 */
public class StyleValueHolder<E extends StyleExpression<E>, V> extends ObservableListWrapper<StyleExpressionValue<E, V>> {
	private final ConditionalStyle<?, E> theStyle;
	private final StyleAttribute<V> theAttribute;

	private ObservableList<StyleExpressionValue<E, V>> theController;

	/**
	 * @param style The style that this object will hold expressions for
	 * @param attr The attribute that this object will hold expressions for
	 * @param value The initial expression value to hold
	 */
	protected StyleValueHolder(ConditionalStyle<?, E> style, StyleAttribute<V> attr, StyleExpressionValue<E, V> value) {
		super(new ObservableArrayList<>(new TypeToken<StyleExpressionValue<E, V>>(){}
			.where(new TypeParameter<E>(){}, style.getExpressionType())
			.where(new TypeParameter<V>(){}, attr.getType().getType())), false);
		theStyle = style;
		theAttribute = attr;
		theController = getWrapped();
		if(value != null)
			theController.add(value);
	}

	/** @param value The expression value to add to this holder */
	public void addValue(StyleExpressionValue<E, V> value) {
		int index = java.util.Collections.binarySearch(this, value, STYLE_EXPRESSION_COMPARE);
		if (index >= 0) {
			while (index < size() && STYLE_EXPRESSION_COMPARE.compare(get(index), value) == 0)
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
		while (iter.hasNext()) {
			StyleExpressionValue<E, V> next = iter.next();
			if (java.util.Objects.equals(exp, next.getExpression())) {
				iter.remove();
				return fireEvent;
			} else if (next.getExpression() == null || (exp != null && next.getExpression().getWhenTrue(exp) > 0))
				fireEvent = false; // Higher-priority expression that encompasses the given expression--no event
		}
		return false;
	}

	void set(StyleExpressionValue<E, V> sev) {
		boolean found = false;
		java.util.ListIterator<StyleExpressionValue<E, V>> iter = theController.listIterator();
		while (iter.hasNext()) {
			StyleExpressionValue<E, V> next = iter.next();
			if (java.util.Objects.equals(sev.getExpression(), next.getExpression())) {
				found = true;
				iter.set(sev);
				break;
			} else if (STYLE_EXPRESSION_COMPARE.compare(sev, next) < 0) {
				iter.previous();
				iter.add(sev);
				found = true;
				break;
			}
		}
		if (!found)
			iter.add(sev);
	}

	@Override
	public String toString() {
		return theStyle + ".localExpressions(" + theAttribute + ")=" + super.toString();
	}
}