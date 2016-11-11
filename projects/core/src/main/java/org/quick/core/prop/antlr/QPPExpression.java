package org.quick.core.prop.antlr;

import org.antlr.v4.runtime.ParserRuleContext;

abstract class QPPExpression<N extends ParserRuleContext> {
	private final N theCtx;
	private String theError;

	protected QPPExpression(N ctx) {
		theCtx = ctx;
	}

	public N getContext() {
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