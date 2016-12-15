package org.quick.core.parser;

import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

import org.quick.core.QuickEnvironment;
import org.quick.core.QuickToolkit;

/** Parses {@link QuickToolkit}s */
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
}
