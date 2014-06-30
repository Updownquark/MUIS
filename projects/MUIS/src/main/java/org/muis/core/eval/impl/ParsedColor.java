package org.muis.core.eval.impl;

import prisms.lang.ParseException;
import prisms.lang.ParseMatch;
import prisms.lang.ParsedItem;
import prisms.lang.PrismsParser;

/** Represents a color parsed by either #XXXXXX or $XXXXXX */
public class ParsedColor extends ParsedItem {
	private boolean isRgb;

	private String theHexValue;

	@Override
	public void setup(PrismsParser parser, ParsedItem parent, ParseMatch match) throws ParseException {
		super.setup(parser, parent, match);
		isRgb = getStored("rgb") != null;
		theHexValue = getStored("value").text;
	}

	/** @return Whether the parsed color is RGB (#) or HSB ($) */
	public boolean isRgb() {
		return isRgb;
	}

	/** @return This color's hexadecimal value */
	public String getHexValue() {
		return theHexValue;
	}

	@Override
	public ParsedItem [] getDependents() {
		return new ParsedItem[0];
	}

	@Override
	public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
		throw new IllegalArgumentException("No such dependent " + dependent);
	}
}
