package org.muis.core.style.sheet;

import org.muis.core.style.StyleExpression;
import org.muis.core.style.StyleExpressionValue;

/**
 * A special case of StyleExpressionValue for animated style sheets that stores an expression and does not evaluate it until the value is
 * needed
 *
 * @param <E> The type of expression that must evaluate to true for an expression value of a given type in order for the value to be applied
 * @param <V> The type of style value
 */
public class StyleExpressionEvalValue<E extends StyleExpression<E>, V> extends StyleExpressionValue<E, V> {
	private AnimatedStyleSheet theStyleSheet;

	private org.muis.core.style.StyleAttribute<V> theAttr;

	private prisms.lang.ParsedItem theValueExpression;

	/**
	 * @param styleSheet The style sheet that this
	 * @param attr The style attribute that this expression is for
	 * @param expression The style expression that the value is valid for
	 * @param valueExpr The value expression that may be evaluated to a value for the style attribute
	 */
	public StyleExpressionEvalValue(AnimatedStyleSheet styleSheet, org.muis.core.style.StyleAttribute<V> attr, E expression,
		prisms.lang.ParsedItem valueExpr) {
		super(expression, (V) valueExpr);
		theStyleSheet = styleSheet;
		theAttr = attr;
		theValueExpression = valueExpr;
	}

	/** @return The value expression that may be evaluated to a value for the style attribute */
	public prisms.lang.ParsedItem getValueExpression() {
		return theValueExpression;
	}

	@Override
	public V getValue() {
		return theStyleSheet.evaluate(theAttr, theValueExpression);
	}
}
