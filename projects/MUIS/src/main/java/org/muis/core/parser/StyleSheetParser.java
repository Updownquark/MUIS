package org.muis.core.parser;

/** Parses style sheets for use in MUIS */
public interface StyleSheetParser {
	/**
	 * @param location The location of the style sheet file
	 * @param reader The reader for the style sheet content
	 * @param messager The destination for non-fatal messages.
	 * @return The style sheet specified in the given file
	 * @throws java.io.IOException If an error occurs reading the data from the reader
	 * @throws MuisParseException If an fatal error occurs parsing the content from the reader
	 */
	org.muis.core.style.sheet.StyleSheet parseStyleSheet(java.net.URL location, java.io.Reader reader,
		org.muis.core.mgr.MuisMessageCenter messager) throws java.io.IOException, MuisParseException;
}
