package org.quick.core.parser;

import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

import org.jdom2.Element;
import org.quick.core.QuickEnvironment;
import org.quick.core.QuickException;
import org.quick.core.QuickPermission;
import org.quick.core.QuickToolkit;
import org.quick.core.style2.ImmutableStyleSheet;
import org.quick.util.QuickUtils;

/** The default parser for Quick toolkits */
public class DefaultToolkitParser implements QuickToolkitParser {
	private final QuickEnvironment theEnvironment;

	/** @param env The environment that this toolkit is to be part of */
	public DefaultToolkitParser(QuickEnvironment env) {
		theEnvironment = env;
	}

	@Override
	public QuickEnvironment getEnvironment() {
		return theEnvironment;
	}

	@Override
	public QuickToolkit parseToolkit(URL location, Consumer<QuickToolkit> onBuild) throws IOException, QuickParseException {
		Element rootEl;
		try {
			rootEl = new org.jdom2.input.SAXBuilder().build(new java.io.InputStreamReader(location.openStream())).getRootElement();
		} catch (org.jdom2.JDOMException e) {
			throw new QuickParseException("Could not parse toolkit XML for " + location, e);
		}
		QuickToolkit.Builder builder = QuickToolkit.build(theEnvironment, location);
		String name = rootEl.getChildTextTrim("name");
		if (name == null)
			throw new QuickParseException("No name element for toolkit at " + location);
		builder.setName(name);
		String descrip = rootEl.getChildTextTrim("description");
		if (descrip == null)
			throw new QuickParseException("No description element for toolkit at " + location);
		builder.setDescription(descrip);
		String version = rootEl.getChildTextTrim("version");
		if (version == null)
			throw new QuickParseException("No version element for toolkit at " + location);
		builder.setVersion(Version.fromString(version));
		for (Element el : rootEl.getChildren()) {
			String elName = el.getName();
			if (elName.equals("name") || elName.equals("description") || elName.equals("version"))
				continue;
			if (elName.equals("dependencies"))
				for (Element dEl : el.getChildren()) {
					if (dEl.getName().equals("depends")) {
						QuickToolkit dependency;
						try {
							dependency = theEnvironment.getToolkit(QuickUtils.resolveURL(location, dEl.getTextTrim()));
						} catch (QuickException e) {
							throw new QuickParseException(
								"Could not resolve or parse dependency " + dEl.getTextTrim() + " of toolkit " + location, e);
						}
						builder.addDependency(dependency);
					} else if (dEl.getName().equals("classpath")) {
						URL classPath;
						try {
							classPath = QuickUtils.resolveURL(location, dEl.getTextTrim());
						} catch (QuickException e) {
							throw new QuickParseException(
								"Could not resolve classpath " + dEl.getTextTrim() + " for toolkit \"" + name + "\" at " + location, e);
						}
						builder.addClassPath(classPath);
					} else
						throw new QuickParseException("Illegal element under " + elName);
				}
			else if (elName.equals("types"))
				for (Element tEl : el.getChildren()) {
					if (!tEl.getName().equals("type"))
						throw new QuickParseException("Illegal element under " + elName);
					String tagName = tEl.getAttributeValue("tag");
					if (tagName == null)
						throw new QuickParseException("tag attribute expected for element \"" + tEl.getName() + "\"");
					if (!checkTagName(tagName))
						throw new QuickParseException("\"" + tagName
							+ "\" is not a valid tag name: tag names may only contain letters, numbers, underscores, dashes, and dots");
					String className = tEl.getTextTrim();
					if (className == null || className.length() == 0)
						throw new QuickParseException("Class name expected for element " + tEl.getName());
					if (!checkClassName(className))
						throw new QuickParseException("\"" + className + "\" is not a valid class name");
					builder.map(tagName, className);
				}
			else if (elName.equals("resources"))
				for (Element tEl : el.getChildren()) {
					if (!tEl.getName().equals("resource"))
						throw new QuickParseException("Illegal element under " + elName);
					String tagName = tEl.getAttributeValue("tag");
					if (tagName == null)
						throw new QuickParseException("tag attribute expected for element \"" + tEl.getName() + "\"");
					if (!checkTagName(tagName))
						throw new QuickParseException("\"" + tagName
							+ "\" is not a valid tag name: tag names may only contain letters, numbers, underscores, dashes, and dots");
					String resourceLocation = tEl.getTextTrim();
					if (resourceLocation == null || resourceLocation.length() == 0)
						throw new QuickParseException("Resource location expected for element " + tEl.getName());
					// TODO check validity of resource location
					builder.mapResource(tagName, resourceLocation);
				}
			else if (elName.equals("style-sheet")) {
				continue;
			} else if (elName.equals("security"))
				for (Element pEl : el.getChildren()) {
					if (!pEl.getName().equals("permission"))
						throw new QuickParseException("Illegal element under " + elName);
					String typeName = pEl.getAttributeValue("type");
					if (typeName == null)
						throw new QuickParseException("No type name in permission element");
					typeName = typeName.toLowerCase();
					int idx = typeName.indexOf("/");
					String subTypeName = null;
					if (idx >= 0) {
						subTypeName = typeName.substring(idx + 1).trim();
						typeName = typeName.substring(0, idx).trim();
					}
					QuickPermission.Type type = QuickPermission.Type.byKey(typeName);
					if (type == null)
						throw new QuickParseException("No such permission type: " + typeName);
					QuickPermission.SubType[] allSubTypes = type.getSubTypes();
					QuickPermission.SubType subType = null;
					if (allSubTypes != null && allSubTypes.length > 0) {
						if (subTypeName == null)
							throw new QuickParseException("No sub-type specified for permission type " + type);
						for (QuickPermission.SubType st : allSubTypes)
							if (st.getKey().equals(subTypeName))
								subType = st;
						if (subType == null)
							throw new QuickParseException("No such sub-type " + subTypeName + " for permission type " + type);
					} else if (subTypeName != null)
						throw new QuickParseException("No sub-types exist (such as " + subTypeName + ") for permission type " + type);
					boolean req = "true".equalsIgnoreCase(pEl.getAttributeValue("required"));
					String explanation = pEl.getTextTrim();
					String[] params = new String[subType == null ? 0 : subType.getParameters().length];
					if (subType != null)
						for (int p = 0; p < subType.getParameters().length; p++) {
							params[p] = pEl.getAttributeValue(subType.getParameters()[p].getKey());
							String val = subType.getParameters()[p].validate(params[p]);
							if (val != null)
								throw new QuickParseException("Invalid parameter " + subType.getParameters()[p].getName() + ": " + val);
						}
					builder.addPermission(new QuickPermission(type, subType, params, req, explanation));
				}
			else
				throw new QuickParseException("Illegal element \"" + elName + "\" under \"" + rootEl.getName() + "\"");
		}
		QuickToolkit toolkit = builder.build();
		onBuild.accept(toolkit);
		for (Element el : rootEl.getChildren("style-sheet")) {
			String ref = el.getAttributeValue("ref");
			URL ssLoc;
			try {
				ssLoc = QuickUtils.resolveURL(location, ref);
			} catch (QuickException e) {
				ssLoc = null;
				theEnvironment.getMessageCenter().error("Could not resolve style sheet location " + ref, e);
			}
			if (ssLoc != null) {
				try {
					ImmutableStyleSheet ret = theEnvironment.getStyleParser().parseStyleSheet(ssLoc, toolkit,
						theEnvironment.getPropertyParser(), theEnvironment.cv(), theEnvironment.msg());
					// If there are fatal errors, the parser will return log the errors and return null
					if (ret != null) {
						// ret.startAnimation();
						builder.addStyleSheet(ret);
					}
				} catch (Exception e) {
					theEnvironment.getMessageCenter().error("Could not read or parse style sheet at " + ref, e);
				}
			}
		}
		return toolkit;
	}

	private boolean checkTagName(String tagName) {
		return tagName.matches("[.A-Za-z0-9_-]+");
	}

	private boolean checkClassName(String className) {
		return className.matches("([A-Za-z_][A-Za-z0-9_]*[\\.$])*[A-Za-z_][A-Za-z0-9_]*");
	}
}
