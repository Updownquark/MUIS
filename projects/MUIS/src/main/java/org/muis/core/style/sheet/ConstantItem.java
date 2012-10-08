package org.muis.core.style.sheet;

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
	public prisms.lang.EvaluationResult evaluate(prisms.lang.EvaluationEnvironment env, boolean asType, boolean withValues) {
		return theValue;
	}
}
