package org.quick.core.parser;

import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Attribute;
import org.jdom2.CDATA;
import org.jdom2.Element;
import org.jdom2.Text;
import org.quick.core.QuickClassView;
import org.quick.core.QuickEnvironment;
import org.quick.core.QuickException;
import org.quick.core.QuickToolkit;
import org.quick.core.mgr.QuickMessageCenter;
import org.quick.core.model.QuickModelConfig;
import org.quick.core.style.ImmutableStyleSheet;
import org.quick.util.QuickUtils;

/** Parses Quick components using the JDOM library */
public class QuickDomParser implements QuickDocumentParser {
	private QuickEnvironment theEnvironment;

	/** @param env The environment for the parser to operate in */
	public QuickDomParser(QuickEnvironment env) {
		theEnvironment = env;
	}

	@Override
	public QuickEnvironment getEnvironment() {
		return theEnvironment;
	}

	@Override
	public QuickDocumentStructure parseDocument(URL location, Reader reader, QuickClassView cv, QuickMessageCenter msg)
		throws IOException, QuickParseException {
		Element rootEl;
		try {
			rootEl = new org.jdom2.input.SAXBuilder().build(reader).getRootElement();
		} catch (org.jdom2.JDOMException e) {
			throw new QuickParseException("Could not parse document XML", e);
		}

		QuickClassView classView = getClassView(cv, rootEl, msg, location);
		if (rootEl.getTextTrim().length() > 0)
			msg.warn("Text found under root element: " + rootEl.getTextTrim());
		Element[] headEl = rootEl.getChildren("head").toArray(new Element[0]);
		if (headEl.length > 1)
			msg.error("Multiple head elements in document XML");
		String title = null;
		List<ImmutableStyleSheet> styleSheets = new ArrayList<>();
		Map<String, QuickModelConfig> modelConfigs = new LinkedHashMap<>();
		if (headEl.length > 0) {
			if (headEl[0].getTextTrim().length() > 0)
				msg.warn("Text found in head section: " + headEl[0].getTextTrim());
			title = headEl[0].getChildTextTrim("title");
			for (Element styleSheetEl : headEl[0].getChildren("style-sheet")) {
				String ref = styleSheetEl.getAttributeValue("ref");
				URL ssLoc;
				try {
					ssLoc = QuickUtils.resolveURL(location, ref);
				} catch (QuickException e) {
					msg.error("Could not resolve style sheet location " + ref, e, "element", styleSheetEl);
					return null;
				}
				try {
					ImmutableStyleSheet styleSheet = theEnvironment.getStyleParser().parseStyleSheet(ssLoc, null,
						theEnvironment.getPropertyParser(), classView, msg);
					// TODO It might be better to not call this until the entire document is parsed and ready to render--maybe add this to
					// the EventQueue somehow
					// styleSheet.startAnimation();
					styleSheets.add(styleSheet);
				} catch (Exception e) {
					msg.error("Could not read or parse style sheet at " + ref, e, "element", styleSheetEl);
				}
			}
			for (Element modelEl : headEl[0].getChildren("model")) {
				String name = modelEl.getAttributeValue("name");
				if (name == null) {
					msg.error("No name specified for model", "element", modelEl);
					continue;
				}
				if (modelConfigs.get(name) != null) {
					msg.error("Model \"" + name + "\" specified multiple times", "element", modelEl);
					continue;
				}
				QuickModelConfig modelConfig = parseModelConfig(modelEl, true, msg, name);
				if (modelConfig != null)
					modelConfigs.put(name, modelConfig);
			}
		}
		QuickHeadStructure head = new QuickHeadStructure(title, styleSheets, modelConfigs);
		Element[] body = rootEl.getChildren("body").toArray(new Element[0]);
		if (body.length > 1)
			msg.error("Multiple body elements in document XML");
		if (body.length == 0)
			throw new QuickParseException("No body in document XML");
		for (Element el : rootEl.getChildren()) {
			if (el.getName().equals("head") || el.getName().equals("body"))
				continue;
			msg.error("Extra element " + el.getName() + " in document XML");
		}
		WidgetStructure content = parseContent(new WidgetStructure(null, classView, rootEl.getNamespacePrefix(), rootEl.getName()),
			classView, body[0], msg, location);
		return new QuickDocumentStructure(location, head, content);
	}

	/**
	 * Creates a fully-initialized class view
	 *
	 * @param parent The parent class view
	 * @param xml The XML element to get namespaces to map
	 * @param msg The message center to report errors to
	 * @param location The location of the XML file
	 * @return The class view for the element
	 */
	protected QuickClassView getClassView(QuickClassView parent, Element xml, QuickMessageCenter msg, URL location) {
		QuickClassView ret = new QuickClassView(theEnvironment, parent,
			parent == null ? null : parent.getToolkitForQName(xml.getQualifiedName()));
		for (org.jdom2.Namespace ns : xml.getNamespacesIntroduced()) {
			QuickToolkit toolkit;
			try {
				toolkit = theEnvironment.getToolkit(QuickUtils.resolveURL(location, ns.getURI()));
			} catch (MalformedURLException e) {
				msg.error("Invalid URL \"" + ns.getURI() + "\" for toolkit at namespace " + ns.getPrefix(), e);
				continue;
			} catch (IOException e) {
				msg.error("Could not read toolkit " + ns.getPrefix() + ":" + ns.getURI(), e);
				continue;
			} catch (QuickParseException e) {
				msg.error("Could not parse toolkit " + ns.getPrefix() + ":" + ns.getURI(), e);
				continue;
			} catch (QuickException e) {
				msg.error("Could not resolve location of toolkit for namespace " + ns.getPrefix(), e);
				continue;
			}
			try {
				ret.addNamespace(ns.getPrefix(), toolkit);
			} catch (QuickException e) {
				msg.error("Could not add namespace", e);
			}
		}
		ret.seal();
		return ret;
	}

	private QuickModelConfig parseModelConfig(Element element, boolean isRoot, QuickMessageCenter msg, String modelName) {
		QuickModelConfig.Builder builder = QuickModelConfig.build();
		builder.withText(element.getTextTrim());
		for (Attribute att : element.getAttributes())
			builder.add(att.getName(), att.getValue());
		for (Element child : element.getChildren()) {
			if (child.getAttributes().isEmpty() && child.getChildren().isEmpty())
				builder.add(child.getName(), child.getTextTrim());
			else {
				QuickModelConfig childModel = parseModelConfig(child, false, msg, modelName);
				if (childModel == null)
					return null;
				builder.addChild(child.getName(), childModel);
			}
		}
		return builder.build();
	}

	/**
	 * @param parent The structure parent
	 * @param xml The XML to parse widget structures from
	 * @param rootClassView The class view for the root of the structure--may be null
	 * @param msg The message center to report errors to
	 * @param location The location of the XML file
	 * @return The Quick-formatted structure of the given element
	 */
	protected WidgetStructure parseContent(WidgetStructure parent, QuickClassView rootClassView, Element xml, QuickMessageCenter msg,
		URL location) {
		String ns = xml.getNamespacePrefix();
		if (ns.length() == 0)
			ns = null;
		QuickClassView classView = getClassView(parent == null ? rootClassView : parent.getClassView(), xml, msg, location);
		WidgetStructure ret = new WidgetStructure(parent, classView, ns, xml.getName());

		for (org.jdom2.Attribute att : xml.getAttributes())
			ret.addAttribute(att.getName(), att.getValue());

		for (org.jdom2.Content content : xml.getContent())
			if (content instanceof Element) {
				ret.addChild(parseContent(ret, null, (Element) content, msg, location));
			} else if (content instanceof Text || content instanceof CDATA) {
				String text;
				if (content instanceof Text)
					text = ((Text) content).getTextNormalize();
				else
					text = ((CDATA) content).getTextTrim().replaceAll("\r", "");
				if (text.length() == 0)
					continue;
				ret.addChild(new QuickText(ret, text, content instanceof CDATA));
			}
		return ret;
	}
}
