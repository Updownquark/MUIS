package org.quick.core.prop.antlr;

import org.antlr.v4.runtime.ParserRuleContext;

abstract class QPPExpression {
	private final ParserRuleContext theCtx;
	private String theError;

	protected QPPExpression(ParserRuleContext ctx) {
		theCtx = ctx;
	}

	public ParserRuleContext getContext() {
		return theCtx;
	}

	protected void error(String error) {
		if (theError == null)
			theError = error;
		else
			theError += "\n" + error;
	}

	@Override
	public final String toString() {
		return theCtx.getText();
	}

	public String print() {
		return toString();
	}
}