package org.muis.core.parser;

import java.util.List;
import java.util.SortedSet;

import prisms.arch.PrismsConfig;
import prisms.lang.PrismsParser;

public class WrappingPrismsParser extends PrismsParser {
	private final PrismsParser theWrapped;
	private final SortedSet<PrismsConfig> theExtraOps;
	private final java.util.Map<String, PrismsConfig> theExtraOpsByName;
	private final List<String> theExtraTerminators;
	private final java.util.Set<String> theClearedTerminators;
	private final List<String> theExtraIgnorables;
	private final org.muis.util.AggregateSortedSet<PrismsConfig> theAggregateOps;
	private final org.muis.util.AggregateList<String> theAggregateTerminators;
	private final org.muis.util.AggregateList<String> theAggregateIgnorables;

	public WrappingPrismsParser(PrismsParser wrap) {
		theWrapped = wrap;
		theExtraOps = new java.util.TreeSet<>(theWrapped.getOperators().comparator());
		theExtraOpsByName = new java.util.HashMap<>();
		theExtraTerminators = new java.util.ArrayList<>();
		theClearedTerminators = new java.util.HashSet<>();
		theExtraIgnorables = new java.util.ArrayList<>();
		theAggregateOps = new org.muis.util.AggregateSortedSet<>(theExtraOps, theWrapped.getOperators());
		theAggregateTerminators = new org.muis.util.AggregateList<>(theClearedTerminators, theWrapped.getTerminators(),
			theExtraTerminators);
		theAggregateIgnorables = new org.muis.util.AggregateList<>(null, theWrapped.getIgnorables(), theExtraIgnorables);
	}

	@Override
	public SortedSet<PrismsConfig> getOperators() {
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
		theExtraOps.add(op);
		theExtraOpsByName.put(op.get("name"), op);
		if(op.is("ignorable", false))
			theExtraIgnorables.add(op.get("name"));
	}
}