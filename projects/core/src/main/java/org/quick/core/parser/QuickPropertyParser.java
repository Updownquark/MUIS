package org.quick.core.parser;

import org.observe.ObservableValue;
import org.quick.core.QuickEnvironment;
import org.quick.core.prop.ExpressionContext;
import org.quick.core.prop.QuickProperty;

public interface QuickPropertyParser {
	QuickEnvironment getEnvironment();

	Runnable parseAction(ExpressionContext ctx, String value) throws QuickParseException;

	<T> ObservableValue<T> parseProperty(QuickProperty<T> type, ExpressionContext ctx, String value) throws QuickParseException;
}
