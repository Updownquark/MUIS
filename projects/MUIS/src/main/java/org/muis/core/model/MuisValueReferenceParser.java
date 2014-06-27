package org.muis.core.model;

import org.muis.core.eval.impl.ObservableEvaluator;
import org.muis.core.parser.MuisParseException;
import org.muis.core.rx.ObservableValue;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.PrismsParser;

/** Parses references to values from property values */
public interface MuisValueReferenceParser {
	PrismsParser getParser();

	ObservableEvaluator getEvaluator();

	EvaluationEnvironment getEvaluationEnvironment();

	/**
	 * @param mvr The value to parse
	 * @return The parsed model value reference
	 * @throws MuisParseException If an error occurs parsing the reference
	 */
	ObservableValue<?> parse(String mvr) throws MuisParseException;
}
