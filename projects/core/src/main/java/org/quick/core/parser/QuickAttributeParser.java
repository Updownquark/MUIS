package org.quick.core.parser;

import org.observe.ObservableValue;
import org.quick.core.QuickEnvironment;
import org.quick.core.prop.ExpressionContext;
import org.quick.core.prop.QuickPropertyType;

public interface QuickAttributeParser {
	QuickEnvironment getEnvironment();

	<T> ObservableValue<T> parseProperty(QuickPropertyType<T> type, ExpressionContext ctx, String value);
}
