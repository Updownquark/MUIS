package org.quick.core.parser;

import java.io.IOException;
import java.net.URL;

import org.quick.core.QuickClassView;
import org.quick.core.QuickEnvironment;
import org.quick.core.QuickToolkit;
import org.quick.core.mgr.QuickMessageCenter;
import org.quick.core.style.ImmutableStyleSheet;

/** Parses style sheets */
public interface QuickStyleParser {
	/** @return The environment that this parser parses style sheets for */
	QuickEnvironment getEnvironment();

	/**
	 * @param location The location of the style sheet
	 * @param toolkit The toolkit that the style sheet is for
	 * @param parser The property parser to parse the style values
	 * @param cv The view of classes available for the style sheet
	 * @param msg The message center to log messages to
	 * @return The parsed style sheet
	 * @throws IOException If the style sheet's data could not be read
	 * @throws QuickParseException If the style sheet fails to parse
	 */
	ImmutableStyleSheet parseStyleSheet(URL location, QuickToolkit toolkit, QuickPropertyParser parser, QuickClassView cv,
		QuickMessageCenter msg) throws IOException, QuickParseException;
}
