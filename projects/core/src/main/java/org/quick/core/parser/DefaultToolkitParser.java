package org.quick.core.parser;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;

import org.jdom2.Element;
import org.quick.core.QuickClassView;
import org.quick.core.QuickException;
import org.quick.core.QuickPermission;
import org.quick.core.QuickToolkit;
import org.quick.core.style.sheet.ParsedStyleSheet;
import org.quick.util.QuickUtils;

public class DefaultToolkitParser implements QuickToolkitParser {
	@Override
	public QuickToolkit parseToolkit(URL location, Reader reader) {
		Element rootEl;
		try {
			rootEl = new org.jdom2.input.SAXBuilder().build(new java.io.InputStreamReader(toolkit.getURI().openStream())).getRootElement();
		} catch(org.jdom2.JDOMException e) {
			throw new QuickParseException("Could not parse toolkit XML for " + toolkit.getURI(), e);
		}
		String name = rootEl.getChildTextTrim("name");
		if(name == null)
			throw new QuickParseException("No name element for toolkit at " + toolkit.getURI());
		toolkit.setName(name);
		String descrip = rootEl.getChildTextTrim("description");
		if(descrip == null)
			throw new QuickParseException("No description element for toolkit at " + toolkit.getURI());
		toolkit.setDescription(descrip);
		String version = rootEl.getChildTextTrim("version");
		if(version == null)
			throw new QuickParseException("No version element for toolkit at " + toolkit.getURI());
		toolkit.setVersion(version);
		for(Element el : rootEl.getChildren()) {
			String elName = el.getName();
			if(elName.equals("name") || elName.equals("description") || elName.equals("version"))
				continue;
			if(elName.equals("dependencies"))
				for(Element dEl : el.getChildren()) {
					if(dEl.getName().equals("depends")) {
						QuickToolkit dependency;
						try {
							dependency = theEnvironment.getToolkit(QuickUtils.resolveURL(toolkit.getURI(), dEl.getTextTrim()));
						} catch(QuickException e) {
							throw new QuickParseException(
								"Could not resolve or parse dependency " + dEl.getTextTrim() + " of toolkit " + toolkit.getURI());
						}
						toolkit.addDependency(dependency);
					} else if(dEl.getName().equals("classpath")) {
						URL classPath;
						try {
							classPath = QuickUtils.resolveURL(toolkit.getURI(), dEl.getTextTrim());
						} catch(QuickException e) {
							throw new QuickParseException(
								"Could not resolve classpath " + dEl.getTextTrim() + " for toolkit \"" + name + "\" at " + toolkit.getURI(),
								e);
						}
						try {
							toolkit.addURL(classPath);
						} catch(IllegalStateException e) {
							throw new QuickParseException("Toolkit is already sealed?", e);
						}
					} else
						throw new QuickParseException("Illegal element under " + elName);
				}
			else if(elName.equals("types"))
				for(Element tEl : el.getChildren()) {
					if(!tEl.getName().equals("type"))
						throw new QuickParseException("Illegal element under " + elName);
					String tagName = tEl.getAttributeValue("tag");
					if(tagName == null)
						throw new QuickParseException("tag attribute expected for element \"" + tEl.getName() + "\"");
					if(!checkTagName(tagName))
						throw new QuickParseException("\"" + tagName
							+ "\" is not a valid tag name: tag names may only contain letters, numbers, underscores, dashes, and dots");
					String className = tEl.getTextTrim();
					if(className == null || className.length() == 0)
						throw new QuickParseException("Class name expected for element " + tEl.getName());
					if(!checkClassName(className))
						throw new QuickParseException("\"" + className + "\" is not a valid class name");
					toolkit.map(tagName, className);
				}
			else if(elName.equals("resources"))
				for(Element tEl : el.getChildren()) {
					if(!tEl.getName().equals("resource"))
						throw new QuickParseException("Illegal element under " + elName);
					String tagName = tEl.getAttributeValue("tag");
					if(tagName == null)
						throw new QuickParseException("tag attribute expected for element \"" + tEl.getName() + "\"");
					if(!checkTagName(tagName))
						throw new QuickParseException("\"" + tagName
							+ "\" is not a valid tag name: tag names may only contain letters, numbers, underscores, dashes, and dots");
					String resourceLocation = tEl.getTextTrim();
					if(resourceLocation == null || resourceLocation.length() == 0)
						throw new QuickParseException("Resource location expected for element " + tEl.getName());
					// TODO check validity of resource location
					toolkit.mapResource(tagName, resourceLocation);
				}
			else if(elName.equals("style-sheet")) {
				continue;
			} else if(elName.equals("security"))
				for(Element pEl : el.getChildren()) {
					if(!pEl.getName().equals("permission"))
						throw new QuickParseException("Illegal element under " + elName);
					String typeName = pEl.getAttributeValue("type");
					if(typeName == null)
						throw new QuickParseException("No type name in permission element");
					typeName = typeName.toLowerCase();
					int idx = typeName.indexOf("/");
					String subTypeName = null;
					if(idx >= 0) {
						subTypeName = typeName.substring(idx + 1).trim();
						typeName = typeName.substring(0, idx).trim();
					}
					QuickPermission.Type type = QuickPermission.Type.byKey(typeName);
					if(type == null)
						throw new QuickParseException("No such permission type: " + typeName);
					QuickPermission.SubType [] allSubTypes = type.getSubTypes();
					QuickPermission.SubType subType = null;
					if(allSubTypes != null && allSubTypes.length > 0) {
						if(subTypeName == null)
							throw new QuickParseException("No sub-type specified for permission type " + type);
						for(QuickPermission.SubType st : allSubTypes)
							if(st.getKey().equals(subTypeName))
								subType = st;
						if(subType == null)
							throw new QuickParseException("No such sub-type " + subTypeName + " for permission type " + type);
					} else if(subTypeName != null)
						throw new QuickParseException("No sub-types exist (such as " + subTypeName + ") for permission type " + type);
					boolean req = "true".equalsIgnoreCase(pEl.getAttributeValue("required"));
					String explanation = pEl.getTextTrim();
					String [] params = new String[subType == null ? 0 : subType.getParameters().length];
					if(subType != null)
						for(int p = 0; p < subType.getParameters().length; p++) {
							params[p] = pEl.getAttributeValue(subType.getParameters()[p].getKey());
							String val = subType.getParameters()[p].validate(params[p]);
							if(val != null)
								throw new QuickParseException("Invalid parameter " + subType.getParameters()[p].getName() + ": " + val);
						}
					try {
						toolkit.addPermission(new QuickPermission(type, subType, params, req, explanation));
					} catch(QuickException e) {
						throw new QuickParseException("Unexpected MUIS Exception: toolkit is sealed?", e);
					}
				}
			else
				throw new QuickParseException("Illegal element \"" + elName + "\" under \"" + rootEl.getName() + "\"");
		}
	}

	public void fillToolkitStyles(QuickToolkit toolkit) throws IOException, QuickParseException {
		Element rootEl;
		try {
			rootEl = new org.jdom2.input.SAXBuilder().build(new java.io.InputStreamReader(toolkit.getURI().openStream())).getRootElement();
		} catch(org.jdom2.JDOMException e) {
			throw new QuickParseException("Could not parse toolkit XML for " + toolkit.getURI(), e);
		}
		for(Element el : rootEl.getChildren("style-sheet")) {
			ParsedStyleSheet styleSheet = parseStyleSheet(el, toolkit.getURI(), new QuickClassView(theEnvironment, null, toolkit),
				theEnvironment.msg());
			if(styleSheet != null)
				toolkit.addStyleSheet(styleSheet);
		}
	}

	private boolean checkTagName(String tagName) {
		return tagName.matches("[.A-Za-z0-9_-]+");
	}

	private boolean checkClassName(String className) {
		return className.matches("([A-Za-z_][A-Za-z0-9_]*\\.)*[A-Za-z_][A-Za-z0-9_]*");
	}

}
