package org.muis.core.style.sheet;

import prisms.lang.EvaluationResult;
import prisms.lang.ParsedItem;

/** Implements a constant style attribute value */
public class ConstantItem extends ParsedItem {
	private EvaluationResult theValue;

	/**
	 * @param type The type of the value
	 * @param value The value
	 */
	public ConstantItem(prisms.lang.Type type, Object value) {
		theValue = new EvaluationResult(type, value);
	}

	/** @return This item's type and value */
	public EvaluationResult get() {
		return theValue;
	}

	@Override
	public ParsedItem [] getDependents() {
		return new prisms.lang.ParsedItem[0];
	}

	@Override
	public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
		throw new IllegalArgumentException("No such dependent " + dependent);
	}

	@Override
	public String toString() {
		if(theValue.getValue() != null)
			return theValue.getValue().toString();
		else if(theValue.getType() != null)
			return theValue.getType().toString();
		else if(theValue.getPackageName() != null)
			return theValue.getPackageName();
		else
			return "?";
	}
}
