package org.muis.core.parser;

import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

import org.jdom2.CDATA;
import org.jdom2.Element;
import org.jdom2.Text;
import org.muis.core.*;
import org.muis.core.style.sheet.ParsedStyleSheet;

/** Parses MUIS components using the JDOM library */
public class MuisDomParser implements MuisParser {
	private MuisEnvironment theEnvironment;

	private XmlStylesheetParser theStyleSheetParser;

	/** @param env The environment for the parser to operate in */
	public MuisDomParser(MuisEnvironment env) {
		theEnvironment = env;
		theStyleSheetParser = new XmlStylesheetParser();
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
			ParsedStyleSheet styleSheet = parseStyleSheet(el, toolkit.getURI(), null, theEnvironment.msg());
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
	public MuisDocument parseDocument(URL location, Reader reader, org.muis.core.MuisDocument.GraphicsGetter graphics)
		throws MuisParseException, IOException {
		Element rootEl;
		try {
			rootEl = new org.jdom2.input.SAXBuilder().build(reader).getRootElement();
		} catch(org.jdom2.JDOMException e) {
			throw new MuisParseException("Could not parse document XML", e);
		}
		MuisDocument doc = new MuisDocument(theEnvironment, location, graphics);
		if(rootEl.getTextTrim().length() > 0)
			doc.msg().warn("Text found under root element: " + rootEl.getTextTrim());
		doc.initDocument(this);
		initClassView(doc, rootEl);
		Element [] head = rootEl.getChildren("head").toArray(new Element[0]);
		if(head.length > 1)
			doc.msg().error("Multiple head elements in document XML");
		if(head.length > 0) {
			if(head[0].getTextTrim().length() > 0)
				doc.msg().warn("Text found in head section: " + head[0].getTextTrim());
			String title = head[0].getChildTextTrim("title");
			if(title != null)
				doc.getHead().setTitle(title);
		}
		for(Element styleSheetEl : head[0].getChildren("style-sheet")) {
			ParsedStyleSheet styleSheet = parseStyleSheet(styleSheetEl, location, doc.getClassView(), doc.msg());
			if(styleSheet != null) {
				doc.getStyle().addStyleSheet(styleSheet);
				styleSheet.seal();
			}
		}
		Element [] body = rootEl.getChildren("body").toArray(new Element[0]);
		if(body.length > 1)
			doc.msg().error("Multiple body elements in document XML");
		if(body.length == 0)
			throw new MuisParseException("No body in document XML");
		for(Element el : rootEl.getChildren()) {
			if(el.getName().equals("head") || el.getName().equals("body"))
				continue;
			doc.msg().error("Extra element " + el.getName() + " in document XML");
		}
		doc.getRoot().init(doc, theEnvironment.getCoreToolkit(), getClassView(doc, doc.getRoot(), body[0]), null, null, body[0].getName());
		applyAttributes(doc.getRoot(), body[0]);
		MuisElement [] content = parseContent(body[0], doc.getRoot());
		doc.getRoot().initChildren(content);
		return doc;
	}

	/**
	 * Initialized aclass view for a new document
	 *
	 * @param muis The docuent to modify the class view for
	 * @param xml The xml element to get namespaces to map
	 */
	protected void initClassView(MuisDocument muis, Element xml) {
		MuisClassView ret = muis.getClassView();
		for(org.jdom2.Namespace ns : xml.getNamespacesIntroduced()) {
			MuisToolkit toolkit;
			try {
				toolkit = theEnvironment.getToolkit(MuisUtils.resolveURL(muis.getLocation(), ns.getURI()));
			} catch(MuisParseException e) {
				muis.msg().error("Could not parse toolkit " + ns.getPrefix() + ":" + ns.getURI(), e);
				continue;
			} catch(MuisException e) {
				muis.msg().error("Invalid URL \"" + ns.getURI() + "\" for toolkit at namespace " + ns.getPrefix(), e);
				continue;
			} catch(IOException e) {
				muis.msg().error("Could not read toolkit " + ns.getPrefix() + ":" + ns.getURI(), e);
				continue;
			}
			try {
				ret.addNamespace(ns.getPrefix(), toolkit);
			} catch(MuisException e) {
				muis.msg().error("Could not add namespace", e);
			}
		}
		ret.seal();
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
	protected ParsedStyleSheet parseStyleSheet(Element styleSheetEl, URL reference, MuisClassView classView,
		org.muis.core.mgr.MuisMessageCenter msg) {
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
				ParsedStyleSheet ret = theStyleSheetParser.parse(ssLoc, theEnvironment, msg);
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
		try {
			ParsedStyleSheet ret = theStyleSheetParser.parse(styleSheetEl, classView, msg);
			ret.startAnimation();
			ret.seal();
			return ret;
		} catch(MuisParseException e) {
			msg.error("Could not read or parse inline style sheet", e, "element", styleSheetEl);
			return null;
		}
	}

	/**
	 * Parses namespaces associated with a style element
	 *
	 * @param doc The MUIS document that the style is for
	 * @param xml The XML element to parse the namespaces from
	 * @param namespaces The map to put the toolkits mapped to namespaces in the XML element
	 */
	protected void applyNamespaces(MuisDocument doc, Element xml, java.util.Map<String, MuisToolkit> namespaces) {
		for(org.jdom2.Namespace ns : xml.getNamespacesIntroduced()) {
			MuisToolkit toolkit;
			try {
				toolkit = theEnvironment.getToolkit(new URL(ns.getURI()));
			} catch(MalformedURLException e) {
				doc.msg().error("Invalid URL \"" + ns.getURI() + "\" for toolkit at namespace " + ns.getPrefix(), e);
				continue;
			} catch(IOException e) {
				doc.msg().error("Could not read toolkit " + ns.getPrefix() + ":" + ns.getURI(), e);
				continue;
			} catch(MuisParseException e) {
				doc.msg().error("Could not parse toolkit " + ns.getPrefix() + ":" + ns.getURI(), e);
				continue;
			}
			namespaces.put(ns.getPrefix(), toolkit);
		}
	}

	@Override
	public MuisElement [] parseContent(Reader reader, MuisElement parent, boolean useRootAttrs) throws IOException, MuisParseException {
		Element rootEl;
		try {
			rootEl = new org.jdom2.input.SAXBuilder().build(reader).getRootElement();
		} catch(org.jdom2.JDOMException e) {
			throw new MuisParseException("Could not parse document XML", e);
		}
		if(useRootAttrs)
			applyAttributes(parent, rootEl);
		return parseContent(rootEl, parent);
	}

	/**
	 * Creates a fully-initialized class view for a new element
	 *
	 * @param doc The document to create the class view for
	 * @param muis The element to create the class view for
	 * @param xml The xml element to get namespaces to map
	 * @return The class view for the element
	 */
	protected MuisClassView getClassView(MuisDocument doc, MuisElement muis, Element xml) {
		MuisClassView ret = new MuisClassView(theEnvironment, muis);
		for(org.jdom2.Namespace ns : xml.getNamespacesIntroduced()) {
			MuisToolkit toolkit;
			try {
				toolkit = theEnvironment.getToolkit(MuisUtils.resolveURL(doc.getLocation(), ns.getURI()));
			} catch(MalformedURLException e) {
				muis.msg().error("Invalid URL \"" + ns.getURI() + "\" for toolkit at namespace " + ns.getPrefix(), e);
				continue;
			} catch(IOException e) {
				muis.msg().error("Could not read toolkit " + ns.getPrefix() + ":" + ns.getURI(), e);
				continue;
			} catch(MuisParseException e) {
				muis.msg().error("Could not parse toolkit " + ns.getPrefix() + ":" + ns.getURI(), e);
				continue;
			} catch(MuisException e) {
				muis.msg().error("Could not resolve location of toolkit for namespace " + ns.getPrefix(), e);
				continue;
			}
			try {
				ret.addNamespace(ns.getPrefix(), toolkit);
			} catch(MuisException e) {
				muis.msg().error("Could not add namespace", e);
			}
		}
		ret.seal();
		return ret;
	}

	/**
	 * Applies attributes in an XML element to a MUIS element
	 *
	 * @param muis The MUIS element to apply the attributes to
	 * @param xml The XML element to get the attributes from
	 */
	protected void applyAttributes(MuisElement muis, Element xml) {
		for(org.jdom2.Attribute attr : xml.getAttributes()) {
			if(attr.getName().matches("group[0-9]*"))
				applyElementGroups(muis, attr.getValue());
			else if(attr.getName().matches("attach[0-9]*"))
				applyElementAttaches(muis, attr.getValue());
			try {
				muis.atts().set(attr.getName(), attr.getValue());
			} catch(MuisException e) {
				muis.msg().error("Could not set attribute--stage is not init?", e);
			}
		}
	}

	/**
	 * Adds an element to groups by name
	 *
	 * @param element The element to add to the groups
	 * @param groupValue The names of the groups, separated by whitespace
	 */
	protected void applyElementGroups(MuisElement element, String groupValue) {
		String [] groupNames = groupValue.split("\\w*");
		for(String name : groupNames) {
			org.muis.core.style.attach.NamedStyleGroup group = element.getDocument().getGroup(name);
			if(group == null) {
				element.msg().warn("No such group named \"" + name + "\"");
				continue;
			}
			element.getStyle().addGroup(group);
		}
	}

	/**
	 * Attaches {@link MuisElementAttachment}s to an element
	 *
	 * @param muis The element to attach to
	 * @param attachAttr The attribute value containing the attachments
	 */
	protected void applyElementAttaches(MuisElement muis, String attachAttr) {
		String [] attaches = attachAttr.split("\\w*,\\w*");
		for(String attach : attaches) {
			String ns = null, tag = attach;
			int index = attach.indexOf(":");
			if(index >= 0) {
				ns = attach.substring(0, index);
				tag = attach.substring(index + 1);
			}
			MuisToolkit toolkit = muis.getClassView().getToolkit(ns);
			if(toolkit == null) {
				muis.msg().error("No such toolkit mapped to namespace \"" + ns + "\"");
				continue;
			}
			String attachClassName = toolkit.getMappedClass(tag);
			if(attachClassName == null) {
				muis.msg().warn("No attachment class mapped to " + tag + " in toolkit " + toolkit.getName());
				continue;
			}
			Class<? extends MuisElementAttachment> clazz;
			try {
				clazz = toolkit.loadClass(attachClassName, MuisElementAttachment.class);
			} catch(MuisException e) {
				muis.msg().warn("Could not load attachment class " + attachClassName + " from toolkit " + toolkit.getName(), e);
				continue;
			}
			MuisElementAttachment attachInstance;
			try {
				attachInstance = clazz.newInstance();
			} catch(Throwable e) {
				muis.msg().error("Could not instantiate attachment class " + attachClassName, e);
				continue;
			}
			attachInstance.attach(muis);
		}
	}

	/**
	 * Parses MUIS content from an XML element
	 *
	 * @param xml The XML element to parse the content of
	 * @param parent The parent element whose content to parse
	 * @return The parsed and initialized content
	 */
	protected MuisElement [] parseContent(Element xml, MuisElement parent) {
		MuisElement [] ret = new MuisElement[0];
		for(org.jdom2.Content content : xml.getContent())
			if(content instanceof Element) {
				Element child = (Element) content;
				MuisElement newChild;
				try {
					newChild = createElement(child, parent);
				} catch(MuisParseException e) {
					parent.msg().error("Could not create MUIS element for " + xml.getQualifiedName(), e);
					continue;
				}
				applyAttributes(newChild, child);
				ret = prisms.util.ArrayUtils.add(ret, newChild);
				MuisElement [] subContent = parseContent(child, newChild);
				newChild.initChildren(subContent);
			} else if(content instanceof Text || content instanceof CDATA) {
				String text;
				if(content instanceof Text)
					text = ((Text) content).getTextNormalize();
				else
					text = ((CDATA) content).getTextTrim().replaceAll("\r", "");
				if(text.length() == 0)
					continue;
				MuisTextElement newChild = new MuisTextElement(text);
				ret = prisms.util.ArrayUtils.add(ret, newChild);
				newChild.init(parent.getDocument(), theEnvironment.getCoreToolkit(), parent.getDocument().getClassView(), parent, null,
					content instanceof CDATA ? "CDATA" : null);
				newChild.initChildren(new MuisElement[0]);
			}
		return ret;
	}

	/**
	 * Creates an element, initializing it but not its content
	 *
	 * @param xml The XML element representing the element to create
	 * @param parent The parent for the element to create
	 * @return The new element
	 * @throws MuisParseException If an error occurs creating the element
	 */
	protected MuisElement createElement(Element xml, MuisElement parent) throws MuisParseException {
		String ns = xml.getNamespacePrefix();
		if(ns.length() == 0)
			ns = null;
		MuisToolkit toolkit = parent.getClassView().getToolkit(ns);
		if(toolkit == null && ns == null)
			toolkit = theEnvironment.getCoreToolkit();
		String nsStr = ns == null ? "default namespace" : "namespace " + ns;
		if(toolkit == null)
			throw new MuisParseException("No MUIS toolkit mapped to " + nsStr);
		String className = toolkit.getMappedClass(xml.getName());
		if(className == null)
			throw new MuisParseException("No tag name " + xml.getName() + " mapped for " + nsStr);
		Class<? extends MuisElement> muisClass;
		try {
			muisClass = toolkit.loadClass(className, MuisElement.class);
		} catch(MuisException e) {
			throw new MuisParseException("Could not load MUIS element class " + className, e);
		}
		MuisElement ret;
		try {
			ret = muisClass.newInstance();
		} catch(Throwable e) {
			throw new MuisParseException("Could not instantiate MUIS element class " + className, e);
		}
		MuisClassView classView = getClassView(parent.getDocument(), ret, xml);
		ret.init(parent.getDocument(), toolkit, classView, parent, ns, xml.getName());
		return ret;
	}
}
