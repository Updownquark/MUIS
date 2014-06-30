package org.muis.core.eval.impl;

import org.muis.core.rx.ObservableValue;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.ParsedItem;

/**
 * Evaluates a {@link ParsedItem} in an observable way
 *
 * @param <T> The type of parsed item this evaluator can evaluate
 */
public interface ObservableItemEvaluator<T extends ParsedItem> {
	/**
	 * @param item The parsed item to evaluate
	 * @param evaluator The observable evaluator to evaluate dependencies
	 * @param env The evaluation environment in which to evaluate
	 * @return The observable value represented by the parsed item
	 * @throws EvaluationException If an error occurs in evaluation
	 */
	ObservableValue<? extends T> evaluateObservable(T item, ObservableEvaluator evaluator, EvaluationEnvironment env)
		throws EvaluationException;
}
