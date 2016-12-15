package org.quick.core.parser;

import org.observe.ObservableValue;
import org.quick.core.QuickEnvironment;
import org.quick.core.QuickParseEnv;
import org.quick.core.prop.QuickProperty;

import com.google.common.reflect.TypeToken;

/** Parses property values in models, style sheets, attributes, etc. */
public interface QuickPropertyParser {
	/** @return The environment that this parser parses values in */
	QuickEnvironment getEnvironment();

	/**
	 * @param <T> The compile-time type of the property
	 * @param property The property to parse the value for
	 * @param parseEnv The parse environment to use
	 * @param value The text to parse
	 * @return The parsed value
	 * @throws QuickParseException If an error occurs parsing the value
	 */
	<T> ObservableValue<? extends T> parseProperty(QuickProperty<T> property, QuickParseEnv parseEnv, String value)
		throws QuickParseException;

	TypeToken<?> parseType(QuickParseEnv parseEnv, String value) throws QuickParseException;
}
