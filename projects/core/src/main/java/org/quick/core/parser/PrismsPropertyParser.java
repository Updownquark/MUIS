package org.quick.core.parser;

import java.io.IOException;

import org.observe.ObservableValue;
import org.quick.core.QuickEnvironment;
import org.quick.core.QuickParseEnv;
import org.quick.core.prop.ExpressionContext;
import org.quick.core.prop.QuickProperty;

import prisms.lang.*;
import prisms.lang.types.*;

public class PrismsPropertyParser extends AbstractPropertyParser {
	private final PrismsParser theParser;

	public PrismsPropertyParser(QuickEnvironment env) {
		super(env);
		theParser = new PrismsParser();
		try {
			theParser.configure(PrismsPropertyParser.class.getResource("MVX.xml"));
		} catch (IOException e) {
			throw new IllegalStateException("Could not configure property parser", e);
		}
	}

	@Override
	protected <T> ObservableValue<T> parseValue(QuickParseEnv parseEnv, String value) {
		ParseMatch[] matches;
		try {
			matches = theParser.parseMatches(value);
		} catch (ParseException e) {
			throw new QuickParseException("Failed to parse " + value, e);
		}
		if (matches.length != 1)
			throw new QuickParseException("One value per property expected: " + value);
		ParsedItem item;
		try {
			item = theParser.parseStructures(new ParseStructRoot(value), matches)[0];
		} catch (ParseException e) {
			throw new QuickParseException("Failed to structure parsed value for " + value, e);
		}
		return toValue(null, ctx, item);
		// TODO Auto-generated method stub
		return null;
	}

	private <T> ObservableValue<T> toValue(QuickProperty<T> property, ExpressionContext ctx, ParsedItem parsedItem)
		throws QuickParseException {
		if (parsedItem instanceof ParsedArrayIndex) {
		} else if (parsedItem instanceof ParsedIdentifier) {
		} else if(parsedItem instanceof ParsedMethod){
		} else if (parsedItem instanceof ParsedParenthetic) {
		} else if (parsedItem instanceof ParsedArrayInitializer) {
		} else if (parsedItem instanceof ParsedAssignmentOperator) {
		} else if (parsedItem instanceof ParsedBinaryOp) {
		} else if (parsedItem instanceof ParsedCast) {
		} else if (parsedItem instanceof ParsedConditional) {
		} else if (parsedItem instanceof ParsedConstructor) {
		} else if (parsedItem instanceof ParsedEnhancedForLoop) {
		} else if (parsedItem instanceof ParsedIfStatement) {
		} else if (parsedItem instanceof ParsedInstanceofOp) {
		} else if (parsedItem instanceof ParsedBoolean) {
		} else if (parsedItem instanceof ParsedChar) {
		} else if (parsedItem instanceof ParsedNull) {
		} else if (parsedItem instanceof ParsedNumber) {
		} else if (parsedItem instanceof ParsedString) {
		} else if (parsedItem instanceof ParsedLoop) {
		} else if (parsedItem instanceof ParsedType) {
		} else if (parsedItem instanceof ParsedUnaryOp) {
		} else if (parsedItem instanceof ParsedUnitValue) {
		} else
			throw new QuickParseException("Unrecognized parsed item type: " + parsedItem.getClass());
	}

	public static class ParsedUnitValue extends ParsedItem {
		private ParsedItem theValue;
		private String theUnit;

		@Override
		public void setup(PrismsParser parser, ParsedItem parent, ParseMatch match) throws ParseException {
			super.setup(parser, parent, match);
			theValue = parser.parseStructures(this, getStored("value"))[0];
			theUnit = getStored("unit").text;
		}

		@Override
		public ParsedItem[] getDependents() {
			return new ParsedItem[] { theValue };
		}

		@Override
		public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
			if (theValue == dependent)
				theValue = toReplace;
			else
				theValue.replace(dependent, toReplace);
		}
	}
}
