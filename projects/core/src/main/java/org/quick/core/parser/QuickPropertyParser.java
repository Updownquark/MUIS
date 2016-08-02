package org.quick.core.parser;

import org.observe.ObservableValue;
import org.quick.core.QuickEnvironment;
import org.quick.core.QuickParseEnv;
import org.quick.core.model.ObservableActionValue;
import org.quick.core.prop.QuickProperty;

/** Parses property values in models, style sheets, attributes, etc. */
public interface QuickPropertyParser {
	/** @return The environment that this parser parses values in */
	QuickEnvironment getEnvironment();

	/**
	 * @param property The action property to parse the action for
	 * @param parseEnv The parse environment to use
	 * @param value The text to parse
	 * @return The parsed action
	 * @throws QuickParseException If an error occurs parsing the action
	 */
	<T> ObservableActionValue<T> parseAction(QuickProperty<T> property, QuickParseEnv parseEnv, String value) throws QuickParseException;

	/**
	 * @param property The property to parse the value for
	 * @param parseEnv The parse environment to use
	 * @param value The text to parse
	 * @return The parsed value
	 * @throws QuickParseException If an error occurs parsing the value
	 */
	<T> ObservableValue<T> parseProperty(QuickProperty<T> property, QuickParseEnv parseEnv, String value) throws QuickParseException;
}
