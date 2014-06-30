package org.muis.core.eval.impl;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.Type;
import prisms.lang.eval.PrismsEvaluator;
import prisms.lang.eval.PrismsItemEvaluator;

/** Evaluates the percent operator */
public class PercentEvaluator implements PrismsItemEvaluator<ParsedPercent> {
	@Override
	public EvaluationResult evaluate(ParsedPercent item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		EvaluationResult ret = evaluator.evaluate(item.getValue(), env, asType, withValues);
		if(!ret.getType().isMathable())
			throw new EvaluationException("The percent operator cannot be applied to values of type " + ret.getType(), item, item
				.getValue().getMatch().index);
		Type dbl = new Type(Double.TYPE);
		return new EvaluationResult(dbl, prisms.lang.eval.MathUtils.divide(ret.getType(), ret.getValue(), dbl, 100));
	}
}
