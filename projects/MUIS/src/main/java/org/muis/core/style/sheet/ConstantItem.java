package org.muis.core.style.sheet;

import prisms.lang.ParsedItem;

/** Implements a constant style attribute value */
public class ConstantItem extends prisms.lang.ParsedItem {
	private prisms.lang.EvaluationResult theValue;

	/**
	 * @param type The type of the value
	 * @param value The value
	 */
	public ConstantItem(Class<?> type, Object value) {
		theValue = new prisms.lang.EvaluationResult(new prisms.lang.Type(type), value);;
	}

	@Override
	public prisms.lang.ParsedItem[] getDependents() {
		return new prisms.lang.ParsedItem[0];
	}

	@Override
	public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
		throw new IllegalArgumentException("No such dependent " + dependent);
	}

	@Override
	public prisms.lang.EvaluationResult evaluate(prisms.lang.EvaluationEnvironment env, boolean asType, boolean withValues) {
		return theValue;
	}
}
