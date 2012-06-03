package org.wam.parser;

import java.io.IOException;

import org.wam.core.*;
import org.wam.core.WamDocument.GraphicsGetter;

/**
 * Parses WAM components from XML
 */
public interface WamParser
{
	/**
	 * Gets the toolkit at the given URL
	 * 
	 * @param uri The URL to the WAM toolkit
	 * @param doc The document to parse the toolkit for
	 * @return The parsed toolkit
	 * @throws IOException If an error occurs reading the XML document
	 * @throws WamParseException If an error occurs parsing the toolkit
	 */
	WamToolkit getToolkit(String uri, WamDocument doc) throws IOException, WamParseException;

	/**
	 * Parses a document from XML
	 * 
	 * @param reader The reader to the XML document
	 * @param graphics The document's source for graphics
	 * @return The parsed document
	 * @throws IOException If an error occurs reading the XML document
	 * @throws WamParseException If an unrecovrable error occurs parsing the document into WAM
	 *         format
	 */
	WamDocument parseDocument(java.io.Reader reader, GraphicsGetter graphics) throws IOException,
		WamParseException;

	/**
	 * Parses WAM content elements from XML
	 * 
	 * @param reader The reader to the XML document
	 * @param parent The parent for the root elements in the XML
	 * @param useRootAttrs Whether to apply attributes in the root element of the XML to the parent
	 *        element
	 * @return The content elements parsed
	 * @throws IOException If an error occurs reading the document
	 * @throws WamParseException If an unrecoverable error occurs parsing the document into WAM
	 *         content
	 */
	WamElement [] parseContent(java.io.Reader reader, WamElement parent, boolean useRootAttrs)
		throws IOException, WamParseException;
}
