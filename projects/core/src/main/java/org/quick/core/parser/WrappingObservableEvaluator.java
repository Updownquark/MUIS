package org.quick.core.parser;

import org.quick.core.eval.impl.ObservableEvaluator;
import org.quick.core.eval.impl.ObservableItemEvaluator;

import prisms.lang.ParsedItem;
import prisms.lang.eval.PrismsItemEvaluator;

/** Wraps an evaluator, keeping the wrapped evaluator pristine while adding operator types useable to this evaluator */
public class WrappingObservableEvaluator extends ObservableEvaluator {
	private final ObservableEvaluator theWrapped;
	private final prisms.util.SubClassMap<ParsedItem, PrismsItemEvaluator<?>> theExtraEvaluators;
	private final prisms.util.SubClassMap<ParsedItem, ObservableItemEvaluator<?>> theExtraObservableEvaluators;

	/** @param wrap The evaluator to wrap */
	public WrappingObservableEvaluator(ObservableEvaluator wrap) {
		theWrapped = wrap;
		theExtraEvaluators = new prisms.util.SubClassMap<>();
		theExtraObservableEvaluators = new prisms.util.SubClassMap<>();
	}

	@Override
	public <T extends ParsedItem> void addEvaluator(Class<T> type, PrismsItemEvaluator<? super T> evaluator) {
		theExtraEvaluators.put(type, evaluator);
	}

	@Override
	public <T extends ParsedItem> void addEvaluator(Class<T> type, ObservableItemEvaluator<? super T> evaluator) {
		theExtraObservableEvaluators.put(type, evaluator);
	}

	@Override
	public <T extends ParsedItem> PrismsItemEvaluator<? super T> getEvaluatorFor(Class<T> type) {
		PrismsItemEvaluator<?> ret = theExtraEvaluators.get(type);
		if(ret != null)
			return (PrismsItemEvaluator<T>) ret;
		return theWrapped.getEvaluatorFor(type);
	}

	@Override
	public <T extends ParsedItem> ObservableItemEvaluator<? super T> getObservableEvaluatorFor(Class<T> type) {
		ObservableItemEvaluator<?> ret = theExtraObservableEvaluators.get(type);
		if(ret != null)
			return (ObservableItemEvaluator<T>) ret;
		return theWrapped.getObservableEvaluatorFor(type);
	}
}