package org.quick.core.parser;

import org.quick.core.QuickClassView;
import org.quick.core.QuickParseEnv;
import org.quick.core.mgr.QuickMessageCenter;
import org.quick.core.prop.ExpressionContext;

/** Simple, composed parse environment */
public class SimpleParseEnv implements QuickParseEnv {
	private final QuickClassView theClassView;
	private final QuickMessageCenter theMsg;
	private final ExpressionContext theContext;

	/**
	 * @param classView The class view for the environment
	 * @param msg The message center for the environment
	 * @param ctx The expression context for the environment
	 */
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