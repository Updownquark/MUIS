package org.quick.core.parser;

import org.observe.ObservableValue;
import org.quick.core.QuickEnvironment;
import org.quick.core.QuickProperty;

public interface QuickAttributeParser {
	QuickEnvironment getEnvironment();

	<T> ObservableValue<T> parseProperty(QuickProperty.PropertyType<T> type, String value);
}
