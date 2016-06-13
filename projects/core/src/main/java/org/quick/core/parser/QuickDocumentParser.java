package org.quick.core.parser;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;

import org.quick.core.QuickClassView;
import org.quick.core.QuickEnvironment;
import org.quick.core.mgr.QuickMessageCenter;

public interface QuickDocumentParser {
	QuickEnvironment getEnvironment();

	/**
	 * Parses a document's structure from a resource
	 *
	 * @param location The location for the resource
	 * @param reader The reader to the resource
	 * @param graphics The document's source for graphics
	 * @return The parsed document structure
	 * @throws IOException If an error occurs reading the resource
	 * @throws QuickParseException If an unrecoverable error occurs parsing the document into Quick format
	 */
	default QuickDocumentStructure parseDocument(URL location, Reader reader) throws IOException, QuickParseException {
		return parseDocument(location, reader, getEnvironment().cv(), getEnvironment().msg());
	}

	/**
	 * Parses a document's structure from a resource, specifying an initial class view and message center
	 *
	 * @param location The location of the resource to read
	 * @param reader The reader to the resource structure
	 * @param parseEnv The parse environment in which to parse the document
	 * @param rootClassView The class view for the root of the widget structure
	 * @param msg The message center to report parsing errors to
	 * @return The parsed document structure
	 * @throws IOException If an error occurs reading the file
	 * @throws QuickParseException If an unrecoverable error occurs parsing the document into Quick format
	 */
	QuickDocumentStructure parseDocument(URL location, Reader reader, QuickClassView classView, QuickMessageCenter msg)
		throws IOException, QuickParseException;

}
