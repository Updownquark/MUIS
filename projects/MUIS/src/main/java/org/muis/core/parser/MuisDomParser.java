package org.muis.core.parser;

import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

import org.jdom2.CDATA;
import org.jdom2.Element;
import org.jdom2.Text;
import org.muis.core.*;
import org.muis.core.mgr.MuisMessageCenter;
import org.muis.core.style.sheet.ParsedStyleSheet;

/** Parses MUIS components using the JDOM library */
public class MuisDomParser implements MuisParser {
	private MuisEnvironment theEnvironment;

	private StyleSheetParser theStyleSheetParser;

	/** @param env The environment for the parser to operate in */
	public MuisDomParser(MuisEnvironment env) {
		theEnvironment = env;
		theStyleSheetParser = new StyleSheetParser();
	}

	@Override
	public MuisEnvironment getEnvironment() {
		return theEnvironment;
	}

	@Override
	public void fillToolkit(MuisToolkit toolkit) throws MuisParseException, IOException {
		Element rootEl;
		try {
			rootEl = new org.jdom2.input.SAXBuilder().build(new java.io.InputStreamReader(toolkit.getURI().openStream())).getRootElement();
		} catch(org.jdom2.JDOMException e) {
			throw new MuisParseException("Could not parse toolkit XML for " + toolkit.getURI(), e);
		}
		String name = rootEl.getChildTextTrim("name");
		if(name == null)
			throw new MuisParseException("No name element for toolkit at " + toolkit.getURI());
		toolkit.setName(name);
		String descrip = rootEl.getChildTextTrim("description");
		if(descrip == null)
			throw new MuisParseException("No description element for toolkit at " + toolkit.getURI());
		toolkit.setDescription(descrip);
		String version = rootEl.getChildTextTrim("version");
		if(version == null)
			throw new MuisParseException("No version element for toolkit at " + toolkit.getURI());
		toolkit.setVersion(version);
		for(Element el : rootEl.getChildren()) {
			String elName = el.getName();
			if(elName.equals("name") || elName.equals("description") || elName.equals("version"))
				continue;
			if(elName.equals("dependencies"))
				for(Element dEl : el.getChildren()) {
					if(dEl.getName().equals("depends")) {
						MuisToolkit dependency;
						try {
							dependency = theEnvironment.getToolkit(MuisUtils.resolveURL(toolkit.getURI(), dEl.getTextTrim()));
						} catch(MuisException e) {
							throw new MuisParseException("Could not resolve or parse dependency " + dEl.getTextTrim() + " of toolkit "
								+ toolkit.getURI());
						}
						toolkit.addDependency(dependency);
					} else if(dEl.getName().equals("classpath")) {
						URL classPath;
						try {
							classPath = MuisUtils.resolveURL(toolkit.getURI(), dEl.getTextTrim());
						} catch(MuisException e) {
							throw new MuisParseException("Could not resolve classpath " + dEl.getTextTrim() + " for toolkit \"" + name
								+ "\" at " + toolkit.getURI(), e);
						}
						try {
							toolkit.addURL(classPath);
						} catch(IllegalStateException e) {
							throw new MuisParseException("Toolkit is already sealed?", e);
						}
					} else
						throw new MuisParseException("Illegal element under " + elName);
				}
			else if(elName.equals("types"))
				for(Element tEl : el.getChildren()) {
					if(!tEl.getName().equals("type"))
						throw new MuisParseException("Illegal element under " + elName);
					String tagName = tEl.getAttributeValue("tag");
					if(tagName == null)
						throw new MuisParseException("tag attribute expected for element \"" + tEl.getName() + "\"");
					if(!checkTagName(tagName))
						throw new MuisParseException("\"" + tagName
							+ "\" is not a valid tag name: tag names may only contain letters, numbers, underscores, dashes, and dots");
					String className = tEl.getTextTrim();
					if(className == null || className.length() == 0)
						throw new MuisParseException("Class name expected for element " + tEl.getName());
					if(!checkClassName(className))
						throw new MuisParseException("\"" + className + "\" is not a valid class name");
					toolkit.map(tagName, className);
				}
			else if(elName.equals("resources"))
				for(Element tEl : el.getChildren()) {
					if(!tEl.getName().equals("resource"))
						throw new MuisParseException("Illegal element under " + elName);
					String tagName = tEl.getAttributeValue("tag");
					if(tagName == null)
						throw new MuisParseException("tag attribute expected for element \"" + tEl.getName() + "\"");
					if(!checkTagName(tagName))
						throw new MuisParseException("\"" + tagName
							+ "\" is not a valid tag name: tag names may only contain letters, numbers, underscores, dashes, and dots");
					String resourceLocation = tEl.getTextTrim();
					if(resourceLocation == null || resourceLocation.length() == 0)
						throw new MuisParseException("Resource location expected for element " + tEl.getName());
					// TODO check validity of resource location
					toolkit.mapResource(tagName, resourceLocation);
				}
			else if(elName.equals("style-sheet")) {
				continue;
			} else if(elName.equals("security"))
				for(Element pEl : el.getChildren()) {
					if(!pEl.getName().equals("permission"))
						throw new MuisParseException("Illegal element under " + elName);
					String typeName = pEl.getAttributeValue("type");
					if(typeName == null)
						throw new MuisParseException("No type name in permission element");
					typeName = typeName.toLowerCase();
					int idx = typeName.indexOf("/");
					String subTypeName = null;
					if(idx >= 0) {
						subTypeName = typeName.substring(idx + 1).trim();
						typeName = typeName.substring(0, idx).trim();
					}
					MuisPermission.Type type = MuisPermission.Type.byKey(typeName);
					if(type == null)
						throw new MuisParseException("No such permission type: " + typeName);
					MuisPermission.SubType[] allSubTypes = type.getSubTypes();
					MuisPermission.SubType subType = null;
					if(allSubTypes != null && allSubTypes.length > 0) {
						if(subTypeName == null)
							throw new MuisParseException("No sub-type specified for permission type " + type);
						for(MuisPermission.SubType st : allSubTypes)
							if(st.getKey().equals(subTypeName))
								subType = st;
						if(subType == null)
							throw new MuisParseException("No such sub-type " + subTypeName + " for permission type " + type);
					} else if(subTypeName != null)
						throw new MuisParseException("No sub-types exist (such as " + subTypeName + ") for permission type " + type);
					boolean req = "true".equalsIgnoreCase(pEl.getAttributeValue("required"));
					String explanation = pEl.getTextTrim();
					String [] params = new String[subType == null ? 0 : subType.getParameters().length];
					if(subType != null)
						for(int p = 0; p < subType.getParameters().length; p++) {
							params[p] = pEl.getAttributeValue(subType.getParameters()[p].getKey());
							String val = subType.getParameters()[p].validate(params[p]);
							if(val != null)
								throw new MuisParseException("Invalid parameter " + subType.getParameters()[p].getName() + ": " + val);
						}
					try {
						toolkit.addPermission(new MuisPermission(type, subType, params, req, explanation));
					} catch(MuisException e) {
						throw new MuisParseException("Unexpected MUIS Exception: toolkit is sealed?", e);
					}
				}
			else
				throw new MuisParseException("Illegal element \"" + elName + "\" under \"" + rootEl.getName() + "\"");
		}
	}

	@Override
	public void fillToolkitStyles(MuisToolkit toolkit) throws IOException, MuisParseException {
		Element rootEl;
		try {
			rootEl = new org.jdom2.input.SAXBuilder().build(new java.io.InputStreamReader(toolkit.getURI().openStream())).getRootElement();
		} catch(org.jdom2.JDOMException e) {
			throw new MuisParseException("Could not parse toolkit XML for " + toolkit.getURI(), e);
		}
		for(Element el : rootEl.getChildren("style-sheet")) {
			ParsedStyleSheet styleSheet = parseStyleSheet(el, toolkit.getURI(), new MuisClassView(theEnvironment, null, toolkit),
				theEnvironment.msg());
			if(styleSheet != null) {
				toolkit.getStyle().addDependency(styleSheet);
			}
		}
	}

	private boolean checkTagName(String tagName) {
		return tagName.matches("[.A-Za-z0-9_-]+");
	}

	private boolean checkClassName(String className) {
		return className.matches("([A-Za-z_][A-Za-z0-9_]*\\.)*[A-Za-z_][A-Za-z0-9_]*");
	}

	@Override
	public MuisDocumentStructure parseDocument(MuisEnvironment env, URL location, Reader reader) throws MuisParseException, IOException {
		return parseDocument(location, reader, null, env.msg());
	}

	@Override
	public MuisDocumentStructure parseDocument(URL location, Reader reader, MuisClassView rootClassView, MuisMessageCenter msg)
		throws IOException, MuisParseException {
		Element rootEl;
		try {
			rootEl = new org.jdom2.input.SAXBuilder().build(reader).getRootElement();
		} catch(org.jdom2.JDOMException e) {
			throw new MuisParseException("Could not parse document XML", e);
		}

		MuisHeadSection head = new MuisHeadSection();
		MuisClassView classView = getClassView(rootClassView, rootEl, msg, location);
		if(rootEl.getTextTrim().length() > 0)
			msg.warn("Text found under root element: " + rootEl.getTextTrim());
		Element [] headEl = rootEl.getChildren("head").toArray(new Element[0]);
		if(headEl.length > 1)
			msg.error("Multiple head elements in document XML");
		if(headEl.length > 0) {
			if(headEl[0].getTextTrim().length() > 0)
				msg.warn("Text found in head section: " + headEl[0].getTextTrim());
			String title = headEl[0].getChildTextTrim("title");
			if(title != null)
				head.setTitle(title);
			for(Element styleSheetEl : headEl[0].getChildren("style-sheet")) {
				ParsedStyleSheet styleSheet = parseStyleSheet(styleSheetEl, location, classView, msg);
				if(styleSheet != null) {
					head.getStyleSheets().add(styleSheet);
					styleSheet.seal();
				}
			}
			for(Element modelEl : headEl[0].getChildren("model")) {
				String name = modelEl.getAttributeValue("name");
				if(name == null) {
					msg.error("No name specified for model", "element", modelEl);
					continue;
				}
				if(head.getModel(name) != null) {
					msg.error("Model \"" + name + "\" specified multiple times", "element", modelEl);
					continue;
				}
				MuisModel model = parseModel(modelEl, location, classView, msg);
				if(model != null)
					head.addModel(name, model);
			}
		}
		head.seal();
		Element [] body = rootEl.getChildren("body").toArray(new Element[0]);
		if(body.length > 1)
			msg.error("Multiple body elements in document XML");
		if(body.length == 0)
			throw new MuisParseException("No body in document XML");
		for(Element el : rootEl.getChildren()) {
			if(el.getName().equals("head") || el.getName().equals("body"))
				continue;
			msg.error("Extra element " + el.getName() + " in document XML");
		}
		WidgetStructure content = parseContent(
			new WidgetStructure(null, theEnvironment, classView, rootEl.getNamespacePrefix(), rootEl.getName()), null, body[0], msg,
			location);
		return new MuisDocumentStructure(location, head, content);
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
	protected MuisClassView getClassView(MuisClassView parent, Element xml, MuisMessageCenter msg, URL location) {
		MuisClassView ret = new MuisClassView(theEnvironment, parent, parent == null ? null : parent.getToolkitForQName(xml
			.getQualifiedName()));
		for(org.jdom2.Namespace ns : xml.getNamespacesIntroduced()) {
			MuisToolkit toolkit;
			try {
				toolkit = theEnvironment.getToolkit(MuisUtils.resolveURL(location, ns.getURI()));
			} catch(MalformedURLException e) {
				msg.error("Invalid URL \"" + ns.getURI() + "\" for toolkit at namespace " + ns.getPrefix(), e);
				continue;
			} catch(IOException e) {
				msg.error("Could not read toolkit " + ns.getPrefix() + ":" + ns.getURI(), e);
				continue;
			} catch(MuisParseException e) {
				msg.error("Could not parse toolkit " + ns.getPrefix() + ":" + ns.getURI(), e);
				continue;
			} catch(MuisException e) {
				msg.error("Could not resolve location of toolkit for namespace " + ns.getPrefix(), e);
				continue;
			}
			try {
				ret.addNamespace(ns.getPrefix(), toolkit);
			} catch(MuisException e) {
				msg.error("Could not add namespace", e);
			}
		}
		ret.seal();
		return ret;
	}

	/**
	 * Parses a style sheet from a style-sheet element
	 *
	 * @param styleSheetEl The element specifying the style sheet to parse
	 * @param reference The location of the file that the style sheet is referenced from
	 * @param classView The classView to
	 * @param msg The message center to report errors
	 * @return The parsed style sheet, or null if an error occurred (the error must be documented in the document before returning null)
	 */
	protected ParsedStyleSheet parseStyleSheet(Element styleSheetEl, URL reference, MuisClassView classView, MuisMessageCenter msg) {
		String ssLocStr = styleSheetEl.getAttributeValue("ref");
		if(ssLocStr != null) {
			for(org.jdom2.Content content : styleSheetEl.getContent()) {
				if(content instanceof org.jdom2.Comment)
					continue;
				else if(content instanceof Text) {
					if(((Text) content).getTextTrim().length() > 0) {
						msg.error(styleSheetEl.getName() + " elements that refer to a style sheet resource may not have text", "element",
							styleSheetEl);
						return null;
					}
				} else {
					msg.error(styleSheetEl.getName() + " elements that refer to a style sheet resoruce may not have any entitiesy",
						"element", styleSheetEl);
					return null;
				}
			}
			URL ssLoc;
			try {
				ssLoc = MuisUtils.resolveURL(reference, ssLocStr);
			} catch(MuisException e) {
				msg.error("Could not resolve style sheet location " + ssLocStr, e, "element", styleSheetEl);
				return null;
			}
			try {
				ParsedStyleSheet ret = theStyleSheetParser.parse(ssLoc, theEnvironment, new MuisClassView(theEnvironment, classView, null),
					msg);
				ret.setLocation(ssLoc);
				// TODO It might be better to not call this until the entire document is parsed and ready to render--maybe add this to the
				// EventQueue somehow
				ret.startAnimation();
				return ret;
			} catch(IOException | MuisParseException e) {
				msg.error("Could not read or parse style sheet at " + ssLocStr, e, "element", styleSheetEl);
				return null;
			}
		}
		Element temp = styleSheetEl;
		while(temp != null) {
			for(org.jdom2.Namespace ns : temp.getNamespacesIntroduced()) {
				if(classView.getToolkit(ns.getPrefix()) != null || ns.getURI().length() == 0)
					continue;
				try {
					classView.addNamespace(ns.getPrefix(), theEnvironment.getToolkit(MuisUtils.resolveURL(reference, ns.getURI())));
				} catch(MuisException | IOException e) {
					msg.error("Toolkit URI \"" + ns.getURI() + "\" at namespace \"" + ns.getPrefix()
						+ "\" could not be resolved or parsed for style-sheet", e, "element", styleSheetEl);
					continue;
				}
			}
			temp = temp.getParentElement();
		}
		msg.error("No ref specified on style sheet element");
		return null;
		/*try {
			ParsedStyleSheet ret = theStyleSheetParser.parse(styleSheetEl, classView, msg);
			ret.startAnimation();
			ret.seal();
			return ret;
		} catch(MuisParseException e) {
			msg.error("Could not read or parse inline style sheet", e, "element", styleSheetEl);
			return null;
		}*/
	}

	protected MuisModel parseModel(Element modelEl, URL reference, MuisClassView classView, MuisMessageCenter msg) {
		String name = modelEl.getAttributeValue("name");
		String classAtt = modelEl.getAttributeValue("class");
		if(classAtt != null) {
			Class<?> modelType = null;
			for(MuisToolkit tk : classView.getScopedToolkits()) {
				try {
					modelType = tk.loadClass(classAtt);
				} catch(ClassNotFoundException e) {
				}
				if(modelType != null)
					break;
			}
			if(modelType == null) {
				msg.error("No such class \"" + classAtt + "\" found for model \"" + name + "\"", "element", modelEl);
				return null;
			}
			if(MuisModel.class.isAssignableFrom(modelType)) {
				// TODO
			}
			// TODO
		}
	}

	/**
	 * @param parent The structure parent
	 * @param xml The XML to parse widget structures from
	 * @param rootClassView The class view for the root of the structure--may be null
	 * @param msg The message center to report errors to
	 * @param location The location of the XML file
	 * @return The MUIS-formatted structure of the given element
	 */
	protected WidgetStructure parseContent(WidgetStructure parent, MuisClassView rootClassView, Element xml, MuisMessageCenter msg,
		URL location) {
		String ns = xml.getNamespacePrefix();
		if(ns.length() == 0)
			ns = null;
		MuisClassView classView = getClassView(parent == null ? rootClassView : parent.getClassView(), xml, msg, location);
		WidgetStructure ret = new WidgetStructure(parent, theEnvironment, classView, ns, xml.getName());

		for(org.jdom2.Attribute att : xml.getAttributes())
			ret.addAttribute(att.getName(), att.getValue());

		for(org.jdom2.Content content : xml.getContent())
			if(content instanceof Element) {
				ret.addChild(parseContent(ret, null, (Element) content, msg, location));
			} else if(content instanceof Text || content instanceof CDATA) {
				String text;
				if(content instanceof Text)
					text = ((Text) content).getTextNormalize();
				else
					text = ((CDATA) content).getTextTrim().replaceAll("\r", "");
				if(text.length() == 0)
					continue;
				ret.addChild(new MuisText(ret, text, content instanceof CDATA));
			}
		return ret;
	}
}
