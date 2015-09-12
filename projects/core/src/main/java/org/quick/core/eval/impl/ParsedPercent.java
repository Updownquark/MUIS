package org.quick.core.eval.impl;

import prisms.lang.ParseException;
import prisms.lang.ParseMatch;
import prisms.lang.ParsedItem;
import prisms.lang.PrismsParser;

/** Represents something like "x%" */
public class ParsedPercent extends ParsedItem {
	private ParsedItem theValue;

	@Override
	public void setup(PrismsParser parser, ParsedItem parent, ParseMatch match) throws ParseException {
		super.setup(parser, parent, match);
		theValue = parser.parseStructures(this, getStored("value"))[0];
	}

	/** @return The value that is applied to the percent */
	public ParsedItem getValue() {
		return theValue;
	}

	@Override
	public ParsedItem [] getDependents() {
		return new ParsedItem[] {theValue};
	}

	@Override
	public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
		if(theValue == dependent)
			theValue = toReplace;
		else
			throw new IllegalArgumentException("No such dependent " + dependent);
	}
}
