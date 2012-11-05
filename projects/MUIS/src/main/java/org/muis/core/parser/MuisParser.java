package org.muis.core.parser;

import java.io.IOException;

import org.muis.core.MuisEnvironment;
import org.muis.core.MuisToolkit;

/** Parses MUIS structures from XML */
public interface MuisParser
{
	/** @return The environment that this parser operates in */
	MuisEnvironment getEnvironment();

	/**
	 * Fills in the given toolkit's information by parsing its location
	 *
	 * @param toolkit The toolkit to parse
	 * @throws IOException If an error occurs reading the toolkit information
	 * @throws MuisParseException If an error occurs parsing the toolkit information
	 */
	void fillToolkit(MuisToolkit toolkit) throws IOException, MuisParseException;

	/**
	 * Fills in the given toolkit's style information by parsing its location
	 *
	 * @param toolkit The toolkit to parse
	 * @throws IOException If an error occurs reading the toolkit or style information
	 * @throws MuisParseException If an error occurs parsing the style information
	 */
	void fillToolkitStyles(MuisToolkit toolkit) throws IOException, MuisParseException;

	/**
	 * Parses a document's structure from XML
	 *
	 * @param location The location for the document
	 * @param reader The reader to the XML document
	 * @param graphics The document's source for graphics
	 * @return The parsed document structure
	 * @throws IOException If an error occurs reading the XML document
	 * @throws MuisParseException If an unrecoverable error occurs parsing the document into MUIS format
	 */
	MuisDocumentStructure parseDocument(java.net.URL location, java.io.Reader reader) throws IOException,
		MuisParseException;

	/**
	 * Parses widget structure from XML
	 *
	 * @param location The location of the XML file to read
	 * @param reader The reader to the XML structure
	 * @param msg The message center to report parsing errors to
	 * @return The widget structure of the XML data in the file
	 * @throws IOException If an error occurs reading the file
	 * @throws MuisParseException If an unrecoverable error occurs parsing the XML into MUIS format
	 */
	WidgetStructure parseContent(java.net.URL location, java.io.Reader reader, org.muis.core.mgr.MuisMessageCenter msg) throws IOException,
		MuisParseException;
}
