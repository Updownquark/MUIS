package org.muis.core.parser;

import org.jdom2.Element;
import org.muis.core.MuisDocument;
import org.muis.core.style.sheet.ParsedStyleSheet;

/** Parses instances of {@link ParsedStyleSheet} from XML documents */
public class XmlStylesheetParser {
	/**
	 * Parses a style sheet from an XML document
	 *
	 * @param location The location to get the document from
	 * @param doc The MUIS document which refers (directly or indirectly) to the style sheet
	 * @return The style sheet parsed from the XML resource
	 * @throws java.io.IOException If an error occurs reading the resource
	 * @throws MuisParseException If an error occurs parsing the XML or interpreting it as a style sheet
	 */
	public ParsedStyleSheet parse(java.net.URL location, MuisDocument doc) throws java.io.IOException, MuisParseException {
		Element rootEl;
		try {
			rootEl = new org.jdom2.input.SAXBuilder().build(new java.io.InputStreamReader(location.openStream())).getRootElement();
		} catch(org.jdom2.JDOMException e) {
			throw new MuisParseException("Could not parse style sheet XML for " + location, e);
		}
		ParsedStyleSheet ret = parse(rootEl, doc, doc.getClassView());
		ret.setLocation(location);
		return ret;
	}

	/**
	 * @param element The XML element to parse as a style sheet
	 * @param doc The document that the style sheet is to be parsed for
	 * @param classView The class view required to access toolkits required by the style sheet
	 * @return The style sheet represented by the XML
	 */
	public ParsedStyleSheet parse(Element element, MuisDocument doc, org.muis.core.MuisClassView classView) {
	}
}
