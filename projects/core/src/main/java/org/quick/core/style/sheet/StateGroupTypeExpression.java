package org.quick.core.style.sheet;

import org.qommons.ArrayUtils;
import org.quick.core.QuickElement;
import org.quick.core.QuickTemplate;
import org.quick.core.style.stateful.StateExpression;

/**
 * The expression type for style sheets, allowing style attribute values to be applied in potentially state-, group-, and type-dependent
 * ways.
 *
 * @param <E> The element type of this expression
 */
public class StateGroupTypeExpression<E extends org.quick.core.QuickElement> implements
	org.quick.core.style.StyleExpression<StateGroupTypeExpression<?>> {
	private final StateExpression theState;

	private final String theGroupName;

	private final Class<E> theType;

	private final TemplateRole theTemplatePath;

	/**
	 * @param state The state expression of this condition, or null if this expression is to be state-independent.
	 * @param group The group name of this condition, or null if this expression is to be group-independent.
	 * @param type The element type of this condition, or null if this expression is to be type-independent.
	 * @param templatePath The role path that this condition applies to
	 */
	public StateGroupTypeExpression(StateExpression state, String group, Class<E> type, TemplateRole templatePath) {
		if(type == null)
			type = (Class<E>) QuickElement.class;
		theState = state;
		theGroupName = group;
		theType = type;
		theTemplatePath = templatePath;
	}

	@Override
	public int getWhenTrue(StateGroupTypeExpression<?> expr) {
		if(!(ArrayUtils.equals(expr.theGroupName, theGroupName) && expr.theType.isAssignableFrom(theType)))
			return 0;
		if(theTemplatePath != null) {
			if(expr.theTemplatePath == null)
				return 0;
			if(!theTemplatePath.containsPath(expr.theTemplatePath))
				return 0;
		}
		if(theState == null)
			return 1;
		if(expr.theState == null)
			return 0;
		return theState.getWhenTrue(expr.theState);
	}

	@Override
	public int getWhenFalse(StateGroupTypeExpression<?> expr) {
		if(!(ArrayUtils.equals(expr.theGroupName, theGroupName) && expr.theType.isAssignableFrom(theType)))
			return 0;
		if(theTemplatePath != null && !theTemplatePath.equals(expr.theTemplatePath))
			return 0;
		if(theState == null)
			return 0;
		if(expr.theState == null)
			return -1;
		return theState.getWhenFalse(expr.theState);
	}

	/**
	 * Prioritizes expressions by type, state, and group, in that order
	 *
	 * @see org.quick.core.style.StyleExpression#getPriority()
	 */
	@Override
	public int getPriority() {
		int ret = 0;
		if(theTemplatePath != null)
			ret += theTemplatePath.getDepth() * 1000;
		else if(theType != null) {
			Class<?> type = theType;
			while(!(type == QuickElement.class)) {
				ret += 100;
				type = type.getSuperclass();
			}
		}
		if(theState != null)
			ret += theState.getPriority();
		if(theGroupName != null)
			ret += 1;
		return ret;
	}

	/** @return The state expression of this condition, or null if this expression is state-independent. */
	public StateExpression getState() {
		return theState;
	}

	/** @return The group name of this condition, or null if this expression is group-independent. */
	public String getGroupName() {
		return theGroupName;
	}

	/** @return The element type of this condition, or null if this expression is type-independent. */
	public Class<E> getType() {
		return theType;
	}

	/** @return The role path in {@link QuickTemplate templated widgets} that this expression applies to */
	public TemplateRole getTemplateRole() {
		return theTemplatePath;
	}

	@Override
	public String toString() {
		if(theState == null && theGroupName == null && theType == null)
			return "(null)";
		if(theState != null && theGroupName == null && theType == null)
			return theState.toString();
		StringBuilder ret = new StringBuilder();
		ret.append('(');
		if(theTemplatePath != null)
			ret.append(theTemplatePath);
		else if(theType != null)
			ret.append("type " + theType.getSimpleName());
		if(theGroupName != null) {
			if(ret.length() > 1)
				ret.append(", ");
			ret.append("group " + theGroupName);
		}
		if(theState != null) {
			if(ret.length() > 1)
				ret.append(", ");
			ret.append("state " + theState);
		}

		ret.append(')');
		return ret.toString();
	}
}
