package org.muis.core.parser;

import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

import org.jdom2.Element;
import org.muis.core.*;

/** Parses MUIS components using the JDOM library */
public class MuisDomParser implements MuisParser
{
	java.util.HashMap<String, MuisToolkit> theToolkits;

	/** Creates a MUIS parser */
	public MuisDomParser()
	{
		theToolkits = new java.util.HashMap<String, MuisToolkit>();
	}

	@SuppressWarnings("resource")
	@Override
	public MuisToolkit getToolkit(URL url, MuisDocument doc) throws MuisParseException, IOException
	{
		MuisToolkit ret = theToolkits.get(url.toString());
		if(ret != null)
			return ret;
		Element rootEl;
		try
		{
			rootEl = new org.jdom2.input.SAXBuilder().build(new java.io.InputStreamReader(url.openStream())).getRootElement();
		} catch(org.jdom2.JDOMException e)
		{
			throw new MuisParseException("Could not parse toolkit XML for " + url, e);
		}
		String name = rootEl.getChildTextTrim("name");
		if(name == null)
			throw new MuisParseException("No name element for toolkit at " + url);
		String descrip = rootEl.getChildTextTrim("description");
		if(descrip == null)
			throw new MuisParseException("No description element for toolkit at " + url);
		String version = rootEl.getChildTextTrim("version");
		if(version == null)
			throw new MuisParseException("No version element for toolkit at " + url);
		if(doc == null || doc.getCoreToolkit() == null)
			ret = new MuisToolkit(url, name, descrip, version);
		else
			ret = new MuisToolkit(url, name, descrip, version);
		for(Element el : rootEl.getChildren())
		{
			String elName = el.getName();
			if(elName.equals("name") || elName.equals("description") || elName.equals("version"))
				continue;
			if(elName.equals("dependencies"))
				for(Element dEl : el.getChildren())
				{
					if(dEl.getName().equals("depends"))
					{
						MuisToolkit dependency;
						try
						{
							dependency = getToolkit(MuisUtils.resolveURL(url, dEl.getTextTrim()), doc);
						} catch(MuisException e)
						{
							doc.msg().error(e.getMessage(), e);
							continue;
						}
						try
						{
							ret.addDependency(dependency);
						} catch(MuisException e)
						{
							throw new MuisParseException("Toolkit is already sealed?", e);
						}
					}
					else if(dEl.getName().equals("classpath"))
					{
						URL classPath;
						try
						{
							classPath = MuisUtils.resolveURL(url, dEl.getTextTrim());
						} catch(MuisException e)
						{
							doc.msg().error("Could not resolve classpath " + dEl.getTextTrim() + " for toolkit \"" + name + "\" at " + url,
								e);
							continue;
						}
						try
						{
							ret.addURL(classPath);
						} catch(IllegalStateException e)
						{
							throw new MuisParseException("Toolkit is already sealed?", e);
						}
					}
					else
						throw new MuisParseException("Illegal element under " + elName);
					if(doc == null || doc.getCoreToolkit() == null)
						throw new MuisParseException("Default toolkit cannot have dependencies");
				}
			else if(elName.equals("types"))
				for(Element tEl : el.getChildren())
				{
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
					try
					{
						ret.map(tagName, className);
					} catch(MuisException e)
					{
						throw new MuisParseException("Toolkit is already sealed?", e);
					}
				}
			else if(elName.equals("resources"))
				for(Element tEl : el.getChildren())
				{
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
					try
					{
						ret.mapResource(tagName, resourceLocation);
					} catch(MuisException e)
					{
						throw new MuisParseException("Toolkit is already sealed?", e);
					}
				}
			else if(elName.equals("security"))
				for(Element pEl : el.getChildren())
				{
					if(!pEl.getName().equals("permission"))
						throw new MuisParseException("Illegal element under " + elName);
					String typeName = pEl.getAttributeValue("type");
					if(typeName == null)
						throw new MuisParseException("No type name in permission element");
					typeName = typeName.toLowerCase();
					int idx = typeName.indexOf("/");
					String subTypeName = null;
					if(idx >= 0)
					{
						subTypeName = typeName.substring(idx + 1).trim();
						typeName = typeName.substring(0, idx).trim();
					}
					MuisPermission.Type type = MuisPermission.Type.byKey(typeName);
					if(type == null)
						throw new MuisParseException("No such permission type: " + typeName);
					MuisPermission.SubType[] allSubTypes = type.getSubTypes();
					MuisPermission.SubType subType = null;
					if(allSubTypes != null && allSubTypes.length > 0)
					{
						if(subTypeName == null)
							throw new MuisParseException("No sub-type specified for permission type " + type);
						for(MuisPermission.SubType st : allSubTypes)
							if(st.getKey().equals(subTypeName))
								subType = st;
						if(subType == null)
							throw new MuisParseException("No such sub-type " + subTypeName + " for permission type " + type);
					}
					else if(subTypeName != null)
						throw new MuisParseException("No sub-types exist (such as " + subTypeName + ") for permission type " + type);
					boolean req = "true".equalsIgnoreCase(pEl.getAttributeValue("required"));
					String explanation = pEl.getTextTrim();
					String [] params = new String[subType == null ? 0 : subType.getParameters().length];
					if(subType != null)
						for(int p = 0; p < subType.getParameters().length; p++)
						{
							params[p] = pEl.getAttributeValue(subType.getParameters()[p].getKey());
							String val = subType.getParameters()[p].validate(params[p]);
							if(val != null)
								throw new MuisParseException("Invalid parameter " + subType.getParameters()[p].getName() + ": " + val);
						}
					try
					{
						ret.addPermission(new MuisPermission(type, subType, params, req, explanation));
					} catch(MuisException e)
					{
						throw new MuisParseException("Unexpected MUIS Exception: toolkit is sealed?", e);
					}
				}
			else
				throw new MuisParseException("Illegal element \"" + elName + "\" under \"" + rootEl.getName() + "\"");
		}
		return ret;
	}

	private boolean checkTagName(String tagName)
	{
		return tagName.matches("[.A-Za-z0-9_-]+");
	}

	private boolean checkClassName(String className)
	{
		return className.matches("([A-Za-z_][A-Za-z0-9_]*\\.)*[A-Za-z_][A-Za-z0-9_]*");
	}

	@Override
	public MuisDocument parseDocument(URL location, Reader reader, org.muis.core.MuisDocument.GraphicsGetter graphics)
		throws MuisParseException, IOException
	{
		MuisToolkit dt;
		try
		{
			dt = getToolkit(MuisDocument.class.getResource("/MuisRegistry.xml"), null);
		} catch(MuisParseException e)
		{
			throw new MuisParseException("Could not parse default toolkit", e);
		}
		Element rootEl;
		try
		{
			rootEl = new org.jdom2.input.SAXBuilder().build(reader).getRootElement();
		} catch(org.jdom2.JDOMException e)
		{
			throw new MuisParseException("Could not parse document XML", e);
		}
		MuisDocument doc = new MuisDocument(location, graphics);
		if(rootEl.getTextTrim().length() > 0)
			doc.msg().warn("Text found under root element: " + rootEl.getTextTrim());
		doc.initDocument(this, dt);
		initClassView(doc, rootEl);
		Element [] head = rootEl.getChildren("head").toArray(new Element[0]);
		if(head.length > 1)
			doc.msg().error("Multiple head elements in document XML");
		if(head.length > 0)
		{
			if(head[0].getTextTrim().length() > 0)
				doc.msg().warn("Text found in head section: " + head[0].getTextTrim());
			String title = head[0].getChildTextTrim("title");
			if(title != null)
				doc.getHead().setTitle(title);
			Element [] styles = head[0].getChildren("style").toArray(new Element[0]);
			for(Element style : styles)
				applyStyle(doc, style);
		}
		Element [] body = rootEl.getChildren("body").toArray(new Element[0]);
		if(body.length > 1)
			doc.msg().error("Multiple body elements in document XML");
		if(body.length == 0)
			throw new MuisParseException("No body in document XML");
		for(Element el : rootEl.getChildren())
		{
			if(el.getName().equals("head") || el.getName().equals("body"))
				continue;
			doc.msg().error("Extra element " + el.getName() + " in document XML");
		}
		doc.getRoot().init(doc, doc.getCoreToolkit(), getClassView(doc, doc.getRoot(), body[0]), null, null, body[0].getName());
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
	protected void initClassView(MuisDocument muis, Element xml)
	{
		MuisClassView ret = muis.getClassView();
		for(org.jdom2.Namespace ns : xml.getNamespacesIntroduced())
		{
			MuisToolkit toolkit;
			try
			{
				toolkit = getToolkit(new URL(ns.getURI()), muis);
			} catch(MalformedURLException e)
			{
				muis.msg().error("Invalid URL \"" + ns.getURI() + "\" for toolkit at namespace " + ns.getPrefix(), e);
				continue;
			} catch(IOException e)
			{
				muis.msg().error("Could not read toolkit " + ns.getPrefix() + ":" + ns.getURI(), e);
				continue;
			} catch(MuisParseException e)
			{
				muis.msg().error("Could not parse toolkit " + ns.getPrefix() + ":" + ns.getURI(), e);
				continue;
			}
			try
			{
				ret.addNamespace(ns.getPrefix(), toolkit);
			} catch(MuisException e)
			{
				muis.msg().error("Could not add namespace", e);
			}
		}
		ret.seal();
	}

	/**
	 * Applies the style element to a document
	 *
	 * @param doc The document containing the styles to apply
	 * @param style The XML element containing the style information
	 */
	protected void applyStyle(MuisDocument doc, Element style)
	{
		java.util.Map<String, MuisToolkit> styleNamespaces = new java.util.HashMap<String, MuisToolkit>();
		applyNamespaces(doc, style, styleNamespaces);
		for(Element groupEl : style.getChildren("group"))
		{
			java.util.Map<String, MuisToolkit> groupNamespaces = new java.util.HashMap<String, MuisToolkit>();
			applyNamespaces(doc, groupEl, groupNamespaces);
			String name = groupEl.getAttributeValue("name");
			if(name == null)
				name = "";
			org.muis.core.style.NamedStyleGroup group = doc.getGroup(name);
			applyStyleGroupAttribs(doc, group, groupEl, styleNamespaces, groupNamespaces, doc.getClassView());
		}
	}

	/**
	 * Parses namespaces associated with a style element
	 *
	 * @param doc The MUIS document that the style is for
	 * @param xml The XML element to parse the namespaces from
	 * @param namespaces The map to put the toolkits mapped to namespaces in the XML element
	 */
	protected void applyNamespaces(MuisDocument doc, Element xml, java.util.Map<String, MuisToolkit> namespaces)
	{
		for(org.jdom2.Namespace ns : xml.getNamespacesIntroduced())
		{
			MuisToolkit toolkit;
			try
			{
				toolkit = getToolkit(new URL(ns.getURI()), doc);
			} catch(MalformedURLException e)
			{
				doc.msg().error("Invalid URL \"" + ns.getURI() + "\" for toolkit at namespace " + ns.getPrefix(), e);
				continue;
			} catch(IOException e)
			{
				doc.msg().error("Could not read toolkit " + ns.getPrefix() + ":" + ns.getURI(), e);
				continue;
			} catch(MuisParseException e)
			{
				doc.msg().error("Could not parse toolkit " + ns.getPrefix() + ":" + ns.getURI(), e);
				continue;
			}
			namespaces.put(ns.getPrefix(), toolkit);
		}
	}

	private <T extends MuisElement> void applyStyleGroupAttribs(MuisDocument doc, org.muis.core.style.TypedStyleGroup<T> group,
		Element groupEl, java.util.Map<String, MuisToolkit> styleNamespaces, java.util.Map<String, MuisToolkit> groupNamespaces,
		MuisClassView classView)
	{
		for(org.jdom2.Attribute attr : groupEl.getAttributes())
		{
			String fullDomain = attr.getQualifiedName();
			String ns = attr.getNamespacePrefix();
			String domainName = attr.getName();
			int idx = domainName.indexOf(".");
			String attrName = null;
			if(idx >= 0)
			{
				attrName = domainName.substring(idx + 1);
				domainName = domainName.substring(0, idx);
			}
			MuisToolkit toolkit = getToolkit(doc, styleNamespaces, groupNamespaces, ns);
			if(toolkit == null)
			{
				doc.msg().error("No toolkit mapped to namespace " + ns);
				continue;
			}
			String domainType = toolkit.getMappedClass(domainName);
			if(domainType == null)
			{
				doc.msg().error("No such style domain " + fullDomain + " in toolkit " + toolkit.getName());
				continue;
			}
			Class<? extends org.muis.core.style.StyleDomain> domainClass;
			try
			{
				domainClass = toolkit.loadClass(domainType, org.muis.core.style.StyleDomain.class);
			} catch(MuisException e)
			{
				doc.msg().error("Could not load style domain " + domainType, e);
				continue;
			}
			org.muis.core.style.StyleDomain domain;
			try
			{
				domain = (org.muis.core.style.StyleDomain) domainClass.getMethod("getDomainInstance", new Class[0]).invoke(null,
					new Object[0]);
			} catch(Exception e)
			{
				doc.msg().error("Could not get domain instance", e);
				continue;
			}
			if(attrName != null)
				org.muis.core.style.StyleParsingUtils.applyStyleAttribute(group, domain, attrName, attr.getValue(), doc.msg(), classView);
			else
				org.muis.core.style.StyleParsingUtils.applyStyleSet(group, domain, attr.getValue(), doc.msg(), classView);
		}
		for(Element typeEl : groupEl.getChildren())
		{
			String fullTypeName = typeEl.getQualifiedName();
			String typeNS = typeEl.getNamespacePrefix();
			String typeName = typeEl.getName();
			MuisToolkit typeToolkit = getToolkit(doc, styleNamespaces, groupNamespaces, typeNS);
			if(typeToolkit == null)
			{
				doc.msg().error("No toolkit mapped to namespace " + typeNS);
				continue;
			}
			String javaTypeName = typeToolkit.getMappedClass(typeName);
			if(javaTypeName == null)
			{
				doc.msg().error("No such element type " + fullTypeName + " in toolkit " + typeToolkit.getName());
				continue;
			}
			Class<? extends MuisElement> typeClass;
			try
			{
				typeClass = typeToolkit.loadClass(javaTypeName, MuisElement.class);
			} catch(MuisException e)
			{
				doc.msg().error("Could not load element class " + javaTypeName, e);
				continue;
			}
			if(!group.getType().isAssignableFrom(typeClass))
			{
				doc.msg().error("Element type " + javaTypeName + " is not a subtype of " + group.getType().getName());
				continue;
			}
			org.muis.core.style.TypedStyleGroup<? extends MuisElement> subGroup = group.insertTypedGroup(typeClass.asSubclass(group
				.getType()));
			applyStyleGroupAttribs(doc, subGroup, typeEl, styleNamespaces, groupNamespaces, classView);
		}
	}

	private MuisToolkit getToolkit(MuisDocument doc, java.util.Map<String, MuisToolkit> styleNS,
		java.util.Map<String, MuisToolkit> groupNS, String ns)
	{
		if(groupNS.containsKey(ns))
			return groupNS.get(ns);
		else if(styleNS.containsKey(ns))
			return styleNS.get(ns);
		else
			return doc.getClassView().getToolkit(ns);
	}

	@Override
	public MuisElement [] parseContent(Reader reader, MuisElement parent, boolean useRootAttrs) throws IOException, MuisParseException
	{
		Element rootEl;
		try
		{
			rootEl = new org.jdom2.input.SAXBuilder().build(reader).getRootElement();
		} catch(org.jdom2.JDOMException e)
		{
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
	protected MuisClassView getClassView(MuisDocument doc, MuisElement muis, Element xml)
	{
		MuisClassView ret = new MuisClassView(doc, muis);
		for(org.jdom2.Namespace ns : xml.getNamespacesIntroduced())
		{
			MuisToolkit toolkit;
			try
			{
				toolkit = getToolkit(MuisUtils.resolveURL(doc.getLocation(), ns.getURI()), doc);
			} catch(MalformedURLException e)
			{
				muis.msg().error("Invalid URL \"" + ns.getURI() + "\" for toolkit at namespace " + ns.getPrefix(), e);
				continue;
			} catch(IOException e)
			{
				muis.msg().error("Could not read toolkit " + ns.getPrefix() + ":" + ns.getURI(), e);
				continue;
			} catch(MuisParseException e)
			{
				muis.msg().error("Could not parse toolkit " + ns.getPrefix() + ":" + ns.getURI(), e);
				continue;
			} catch(MuisException e)
			{
				muis.msg().error("Could not resolve location of toolkit for namespace " + ns.getPrefix(), e);
				continue;
			}
			try
			{
				ret.addNamespace(ns.getPrefix(), toolkit);
			} catch(MuisException e)
			{
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
	protected void applyAttributes(MuisElement muis, Element xml)
	{
		for(org.jdom2.Attribute attr : xml.getAttributes())
		{
			if(attr.getName().matches("group[0-9]*"))
				applyElementGroups(muis, attr.getValue());
			else if(attr.getName().matches("attach[0-9]*"))
				applyElementAttaches(muis, attr.getValue());
			try
			{
				muis.atts().set(attr.getName(), attr.getValue());
			} catch(MuisException e)
			{
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
	protected void applyElementGroups(MuisElement element, String groupValue)
	{
		String [] groupNames = groupValue.split("\\w*");
		for(String name : groupNames)
		{
			org.muis.core.style.NamedStyleGroup group = element.getDocument().getGroup(name);
			if(group == null)
			{
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
	protected void applyElementAttaches(MuisElement muis, String attachAttr)
	{
		String [] attaches = attachAttr.split("\\w*,\\w*");
		for(String attach : attaches)
		{
			String ns = null, tag = attach;
			int index = attach.indexOf(":");
			if(index >= 0)
			{
				ns = attach.substring(0, index);
				tag = attach.substring(index + 1);
			}
			MuisToolkit toolkit = muis.getClassView().getToolkit(ns);
			if(toolkit == null)
			{
				muis.msg().error("No such toolkit mapped to namespace \"" + ns + "\"");
				continue;
			}
			String attachClassName = toolkit.getMappedClass(tag);
			if(attachClassName == null)
			{
				muis.msg().warn("No attachment class mapped to " + tag + " in toolkit " + toolkit.getName());
				continue;
			}
			Class<? extends MuisElementAttachment> clazz;
			try
			{
				clazz = toolkit.loadClass(attachClassName, MuisElementAttachment.class);
			} catch(MuisException e)
			{
				muis.msg().warn("Could not load attachment class " + attachClassName + " from toolkit " + toolkit.getName(), e);
				continue;
			}
			MuisElementAttachment attachInstance;
			try
			{
				attachInstance = clazz.newInstance();
			} catch(Throwable e)
			{
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
	protected MuisElement [] parseContent(Element xml, MuisElement parent)
	{
		MuisElement [] ret = new MuisElement[0];
		for(org.jdom2.Content content : xml.getContent())
			if(content instanceof Element)
			{
				Element child = (Element) content;
				MuisElement newChild;
				try
				{
					newChild = createElement(child, parent);
				} catch(MuisParseException e)
				{
					parent.msg().error("Could not create MUIS element for " + xml.getQualifiedName(), e);
					continue;
				}
				applyAttributes(newChild, child);
				ret = prisms.util.ArrayUtils.add(ret, newChild);
				MuisElement [] subContent = parseContent(child, newChild);
				newChild.initChildren(subContent);
			}
			else if(content instanceof org.jdom2.Text || content instanceof org.jdom2.CDATA)
			{
				String text;
				if(content instanceof org.jdom2.Text)
					text = ((org.jdom2.Text) content).getTextNormalize();
				else
					text = ((org.jdom2.CDATA) content).getTextTrim().replaceAll("\r", "");
				if(text.length() == 0)
					continue;
				MuisTextElement newChild = new MuisTextElement(text);
				ret = prisms.util.ArrayUtils.add(ret, newChild);
				newChild.init(parent.getDocument(), parent.getDocument().getCoreToolkit(), parent.getDocument().getClassView(), parent,
					null, content instanceof org.jdom2.CDATA ? "CDATA" : null);
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
	protected MuisElement createElement(Element xml, MuisElement parent) throws MuisParseException
	{
		String ns = xml.getNamespacePrefix();
		if(ns.length() == 0)
			ns = null;
		MuisToolkit toolkit = parent.getClassView().getToolkit(ns);
		if(toolkit == null && ns == null)
			toolkit = parent.getDocument().getCoreToolkit();
		String nsStr = ns == null ? "default namespace" : "namespace " + ns;
		if(toolkit == null)
			throw new MuisParseException("No MUIS toolkit mapped to " + nsStr);
		String className = toolkit.getMappedClass(xml.getName());
		if(className == null)
			throw new MuisParseException("No tag name " + xml.getName() + " mapped for " + nsStr);
		Class<? extends MuisElement> muisClass;
		try
		{
			muisClass = toolkit.loadClass(className, MuisElement.class);
		} catch(MuisException e)
		{
			throw new MuisParseException("Could not load MUIS element class " + className, e);
		}
		MuisElement ret;
		try
		{
			ret = muisClass.newInstance();
		} catch(Throwable e)
		{
			throw new MuisParseException("Could not instantiate MUIS element class " + className, e);
		}
		MuisClassView classView = getClassView(parent.getDocument(), ret, xml);
		ret.init(parent.getDocument(), toolkit, classView, parent, ns, xml.getName());
		return ret;
	}
}
