package org.quick.core.parser;

import java.io.IOException;

import org.quick.core.QuickEnvironment;
import org.quick.core.QuickToolkit;

/** Parses MUIS structures from XML */
public interface QuickParser {
	/** @return The environment that this parser operates in */
	QuickEnvironment getEnvironment();

	/**
	 * Fills in the given toolkit's information by parsing its location
	 *
	 * @param toolkit The toolkit to parse
	 * @throws IOException If an error occurs reading the toolkit information
	 * @throws QuickParseException If an error occurs parsing the toolkit information
	 */
	void fillToolkit(QuickToolkit toolkit) throws IOException, QuickParseException;

	/**
	 * Fills in the given toolkit's style information by parsing its location
	 *
	 * @param toolkit The toolkit to parse
	 * @throws IOException If an error occurs reading the toolkit or style information
	 * @throws QuickParseException If an error occurs parsing the style information
	 */
	void fillToolkitStyles(QuickToolkit toolkit) throws IOException, QuickParseException;

	/**
	 * Parses a document's structure from XML
	 *
	 * @param location The location for the document
	 * @param reader The reader to the XML document
	 * @param graphics The document's source for graphics
	 * @return The parsed document structure
	 * @throws IOException If an error occurs reading the XML document
	 * @throws QuickParseException If an unrecoverable error occurs parsing the document into MUIS format
	 */
	QuickDocumentStructure parseDocument(java.net.URL location, java.io.Reader reader) throws IOException,
		QuickParseException;

	/**
	 * Parses a document's structure from XML, specifying an initial class view and message center
	 *
	 * @param location The location of the XML file to read
	 * @param reader The reader to the XML structure
	 * @param rootClassView The class view for the root of the widget structure
	 * @param msg The message center to report parsing errors to
	 * @return The parsed document structure
	 * @throws IOException If an error occurs reading the file
	 * @throws QuickParseException If an unrecoverable error occurs parsing the document into MUIS format
	 */
	QuickDocumentStructure parseDocument(java.net.URL location, java.io.Reader reader, org.quick.core.QuickClassView rootClassView,
		org.quick.core.mgr.QuickMessageCenter msg) throws IOException, QuickParseException;
}
