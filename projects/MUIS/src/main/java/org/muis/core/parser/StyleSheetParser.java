package org.muis.core.parser;

/** Parses style sheets for use in MUIS */
public interface StyleSheetParser {
	/**
	 * @param location The location of the style sheet file--will be null for style information specified in the MUIS document
	 * @param reader The reader for the style sheet content
	 * @param classView The class view to use to get toolkits that may be referred to from the style sheet
	 * @param messager The destination for non-fatal messages.
	 * @return The style sheet specified in the given file
	 * @throws java.io.IOException If an error occurs reading the data from the reader
	 * @throws MuisParseException If an fatal error occurs parsing the content from the reader
	 */
	org.muis.core.style.StyleSheet parseStyleSheet(java.net.URL location, java.io.Reader reader, org.muis.core.MuisClassView classView,
		org.muis.core.mgr.MuisMessageCenter messager) throws java.io.IOException, MuisParseException;
}
