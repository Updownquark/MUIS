package org.quick.core.parser;

import org.observe.ObservableAction;
import org.observe.ObservableValue;
import org.quick.core.QuickEnvironment;
import org.quick.core.QuickParseEnv;
import org.quick.core.prop.QuickProperty;

public interface QuickPropertyParser {
	QuickEnvironment getEnvironment();

	ObservableAction parseAction(QuickParseEnv parseEnv, String value) throws QuickParseException;

	<T> ObservableValue<T> parseProperty(QuickProperty<T> property, QuickParseEnv parseEnv, String value) throws QuickParseException;
}
