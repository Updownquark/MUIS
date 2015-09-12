package org.quick.core.parser;

import java.util.List;

import prisms.arch.PrismsConfig;
import prisms.lang.PrismsParser;

/** Wraps a prisms parser, allowing the wrapped parser to remain intact while adding additional operators */
public class WrappingPrismsParser extends PrismsParser {
	private final PrismsParser theWrapped;
	private final List<PrismsConfig> theExtraOps;
	private final java.util.Map<String, PrismsConfig> theExtraOpsByName;
	private final List<String> theExtraTerminators;
	private final java.util.Set<String> theClearedTerminators;
	private final List<String> theExtraIgnorables;
	private final org.quick.util.AggregateList<PrismsConfig> theAggregateOps;
	private final org.quick.util.AggregateList<String> theAggregateTerminators;
	private final org.quick.util.AggregateList<String> theAggregateIgnorables;

	/** @param wrap The parser to wrap */
	public WrappingPrismsParser(PrismsParser wrap) {
		setDebugger(wrap.getDebugger());
		theWrapped = wrap;
		theExtraOps = new java.util.ArrayList<>();
		theExtraOpsByName = new java.util.HashMap<>();
		theExtraTerminators = new java.util.ArrayList<>();
		theClearedTerminators = new java.util.HashSet<>();
		theExtraIgnorables = new java.util.ArrayList<>();
		theAggregateOps = new org.quick.util.AggregateList<>(theExtraOps, theWrapped.getOperators());
		theAggregateTerminators = new org.quick.util.AggregateList<>(theClearedTerminators, theWrapped.getTerminators(), theExtraTerminators);
		theAggregateIgnorables = new org.quick.util.AggregateList<>(null, theWrapped.getIgnorables(), theExtraIgnorables);
	}

	@Override
	public List<PrismsConfig> getOperators() {
		return theAggregateOps;
	}

	@Override
	public PrismsConfig getOperator(String name) {
		PrismsConfig ret = theExtraOpsByName.get(name);
		if(ret == null)
			ret = theWrapped.getOperator(name);
		return ret;
	}

	@Override
	public void addTerminator(String terminator) {
		if(theExtraTerminators == null)
			super.addTerminator(terminator);
		else
			theExtraTerminators.add(terminator);
	}

	@Override
	public void removeTerminator(String terminator) {
		theClearedTerminators.add(terminator);
	}

	@Override
	public void clearTerminators() {
		theExtraTerminators.clear();
		theClearedTerminators.clear();
		theClearedTerminators.addAll(theWrapped.getTerminators());
	}

	@Override
	public List<String> getTerminators() {
		return theAggregateTerminators;
	}

	@Override
	public List<String> getIgnorables() {
		return theAggregateIgnorables;
	}

	@Override
	protected void operatorAdded(PrismsConfig op) {
		int index = java.util.Collections.binarySearch(theExtraOps, op, operatorCompare);
		if(index >= 0) {
			for(; index < theExtraOps.size() && operatorCompare.compare(theExtraOps.get(index), op) == 0; index++);
		} else
			index = -(index + 1);
		theExtraOps.add(op);
		theExtraOpsByName.put(op.get("name"), op);
		if(op.is("ignorable", false))
			theExtraIgnorables.add(op.get("name"));
	}
}
