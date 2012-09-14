package org.muis.core.parser;

/** Parses style sheets for use in MUIS */
public interface StyleSheetParser {
	/**
	 * @param location The location of the style sheet file--may be used for relative paths if not null
	 * @param reader The reader to the style sheet file
	 * @param docParser The MuisParser to use to fetch toolkits
	 * @return The style sheet specified in the given file
	 * @throws java.io.IOException If an error occurs reading the data from the reader
	 * @throws MuisParseException If an error occurs parsing the content from the reader
	 */
	org.muis.core.style.StyleSheet parseStyleSheet(java.net.URL location, java.io.Reader reader, MuisParser docParser)
		throws java.io.IOException, MuisParseException;
}
