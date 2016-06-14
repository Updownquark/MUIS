package org.quick.core.parser;

import org.observe.ObservableValue;
import org.quick.core.QuickEnvironment;
import org.quick.core.prop.ExpressionContext;
import org.quick.core.prop.QuickProperty;

public class DefaultPropertyParser implements QuickPropertyParser {
	private final QuickEnvironment theEnvironment;

	public DefaultPropertyParser(QuickEnvironment env) {
		theEnvironment = env;
	}

	@Override
	public QuickEnvironment getEnvironment() {
		return theEnvironment;
	}

	@Override
	public Runnable parseAction(ExpressionContext ctx, String value) throws QuickParseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> ObservableValue<T> parseProperty(QuickProperty<T> type, ExpressionContext ctx, String value) throws QuickParseException {
		// TODO Auto-generated method stub
		return null;
	}
}
