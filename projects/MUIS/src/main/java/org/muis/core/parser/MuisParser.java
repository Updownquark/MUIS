package org.muis.core.parser;

import java.io.IOException;

import org.muis.core.*;
import org.muis.core.MuisDocument.GraphicsGetter;

/** Parses MUIS components from XML */
public interface MuisParser
{
	/** @return The environment that this parser operates in */
	MuisEnvironment getEnvironment();

	/**
	 * Gets the toolkit at the given URL
	 *
	 * @param url The URL to the MUIS toolkit
	 * @return The parsed toolkit
	 * @throws IOException If an error occurs reading the XML document
	 * @throws MuisParseException If an error occurs parsing the toolkit
	 */
	MuisToolkit getToolkit(java.net.URL url) throws IOException, MuisParseException;

	/**
	 * Parses a document from XML
	 *
	 * @param location The location for the document
	 * @param reader The reader to the XML document
	 * @param graphics The document's source for graphics
	 * @return The parsed document
	 * @throws IOException If an error occurs reading the XML document
	 * @throws MuisParseException If an unrecoverable error occurs parsing the document into MUIS format
	 */
	MuisDocument parseDocument(java.net.URL location, java.io.Reader reader, GraphicsGetter graphics) throws IOException,
		MuisParseException;

	/**
	 * Parses MUIS content elements from XML
	 *
	 * @param reader The reader to the XML document
	 * @param parent The parent for the root elements in the XML
	 * @param useRootAttrs Whether to apply attributes in the root element of the XML to the parent element
	 * @return The content elements parsed
	 * @throws IOException If an error occurs reading the document
	 * @throws MuisParseException If an unrecoverable error occurs parsing the document into MUIS content
	 */
	MuisElement [] parseContent(java.io.Reader reader, MuisElement parent, boolean useRootAttrs) throws IOException, MuisParseException;
}
