package org.quick.core.model;

import org.observe.ObservableValue;
import org.quick.core.eval.impl.ObservableEvaluator;
import org.quick.core.parser.QuickParseException;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.PrismsParser;

/** Parses references to values from property values */
public interface QuickValueReferenceParser {
	/** @return The prisms parser used by this model parser */
	PrismsParser getParser();

	/** @return The evaluator used by this model parser */
	ObservableEvaluator getEvaluator();

	/** @return The environment used by this model parser */
	EvaluationEnvironment getEvaluationEnvironment();

	/**
	 * @param mvr The value to parse
	 * @param asType Whether to evaluate the result as a type or an instance
	 * @return The parsed model value reference
	 * @throws QuickParseException If an error occurs parsing the reference
	 */
	ObservableValue<?> parse(String mvr, boolean asType) throws QuickParseException;
}
