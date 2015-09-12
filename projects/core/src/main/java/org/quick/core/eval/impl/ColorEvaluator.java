package org.quick.core.eval.impl;

import static org.quick.core.style.Colors.hexInt;
import static org.quick.core.style.Colors.hsb;
import static org.quick.core.style.Colors.rgb;

import java.awt.Color;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.Type;
import prisms.lang.eval.PrismsEvaluator;
import prisms.lang.eval.PrismsItemEvaluator;

/** Evaluates parsed colors */
public class ColorEvaluator implements PrismsItemEvaluator<ParsedColor> {
	@Override
	public EvaluationResult evaluate(ParsedColor item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		String str = item.getHexValue();
		if(item.isRgb())
			return new EvaluationResult(new Type(Color.class), rgb(hexInt(str, 0), hexInt(str, 2), hexInt(str, 4)));
		else
			return new EvaluationResult(new Type(Color.class), hsb(hexInt(str, 0), hexInt(str, 2), hexInt(str, 4)));
	}
}
