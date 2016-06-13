package org.quick.core.parser;

import org.quick.core.QuickClassView;
import org.quick.core.QuickParseEnv;
import org.quick.core.mgr.QuickMessageCenter;
import org.quick.core.prop.ExpressionContext;

public class SimpleParseEnv implements QuickParseEnv {
	private final QuickClassView theClassView;
	private final QuickMessageCenter theMsg;
	private final ExpressionContext theContext;

	public SimpleParseEnv(QuickClassView classView, QuickMessageCenter msg, ExpressionContext ctx) {
		theClassView = classView;
		theMsg = msg;
		theContext = ctx;
	}

	@Override
	public QuickClassView cv() {
		return theClassView;
	}

	@Override
	public QuickMessageCenter msg() {
		return theMsg;
	}

	@Override
	public ExpressionContext getContext() {
		return theContext;
	}
}