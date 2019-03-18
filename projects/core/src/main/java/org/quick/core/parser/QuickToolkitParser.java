package org.quick.core.parser;

import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

import org.quick.core.QuickEnvironment;
import org.quick.core.QuickToolkit;
import org.quick.core.QuickWidgetSet;

/** Parses {@link QuickToolkit}s and {@link QuickWidgetSet}s */
public interface QuickToolkitParser {
	/** @return The environment that this parser parses toolkits for */
	QuickEnvironment getEnvironment();

	/**
	 * @param location The location of the toolkit definition
	 * @param onBuild A function to deal with the toolkit before its styles are parsed
	 * @return The parsed and initialized toolkit
	 * @throws IOException If the toolkit's data cannot be read
	 * @throws QuickParseException If the toolkit fails to parse
	 */
	QuickToolkit parseToolkit(URL location, Consumer<QuickToolkit> onBuild) throws IOException, QuickParseException;

	/**
	 * @param location The location of the widget set definition
	 * @return The parsed and initialized widget set
	 * @throws IOException If the widget set's data cannot be read
	 * @throws QuickParseException If the widget set fails to parse
	 */
	QuickWidgetSet<?, ?> parseWidgets(URL location) throws IOException, QuickParseException;
}
