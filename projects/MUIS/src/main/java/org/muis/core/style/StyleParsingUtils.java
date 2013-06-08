package org.muis.core.style;

import org.muis.core.MuisException;
import org.muis.core.mgr.MuisMessageCenter;

/** A few utility methods for parsing style information from attribute values */
public class StyleParsingUtils {
	/**
	 * Gets a style domain
	 *
	 * @param ns The namespace specifying the toolkit that the domain is from. May be null.
	 * @param domainName The mapped name of the domain class within its toolkit
	 * @param classView The class view to retrieve the domain class from
	 * @return The style domain mapped by the given name in the given toolkit
	 * @throws MuisException If the style domain cannot be retrieved with the given information
	 */
	public static StyleDomain getStyleDomain(String ns, String domainName, org.muis.core.MuisClassView classView) throws MuisException {
		org.muis.core.MuisToolkit toolkit = null;
		String domainClassName;
		if(ns == null) {
			domainClassName = classView.getMappedClass(ns, domainName);
			if(domainClassName == null)
				throw new MuisException("No style domain mapped to " + domainName + " in class view");
		} else {
			toolkit = classView.getToolkit(ns);
			if(toolkit == null) {
				throw new MuisException("No toolkit mapped to namespace " + ns);
			}
			domainClassName = toolkit.getMappedClass(domainName);
			if(domainClassName == null) {
				throw new MuisException("No style domain mapped to " + domainName + " in toolkit " + toolkit.getName() + "(ns=" + ns + ")");
			}
		}

		Class<? extends org.muis.core.style.StyleDomain> domainClass;
		try {
			if(toolkit != null)
				domainClass = toolkit.loadClass(domainClassName, org.muis.core.style.StyleDomain.class);
			else
				domainClass = classView.loadMappedClass(ns, domainName, StyleDomain.class);
		} catch(MuisException e) {
			throw new MuisException("Could not load domain class " + domainClassName + " from toolkit " + toolkit.getName(), e);
		}
		org.muis.core.style.StyleDomain domain;
		try {
			domain = (org.muis.core.style.StyleDomain) domainClass.getMethod("getDomainInstance", new Class[0]).invoke(null, new Object[0]);
		} catch(Exception e) {
			throw new MuisException("Could not get domain instance", e);
		}
		return domain;
	}

	/**
	 * Splits a style value into the individual values or value sets to be parsed. This method has no smarts and only exists because simply
	 * splitting on the ';' would break domain-scoped styles. It always returns something non-null, though it may be zero-length. Real
	 * parsing and error handling are done elsewhere.
	 *
	 * @param value The style value to split
	 * @return The styles or style sets to parse
	 */
	public static String [] splitStyles(String value) {
		java.util.ArrayList<String> ret = new java.util.ArrayList<>(1);
		int begin = 0;
		int bracket = 0;
		int c;
		for(c = 0; c < value.length(); c++) {
			char ch = value.charAt(c);
			if(Character.isWhitespace(ch))
				continue;
			if(ch == ';' && bracket == 0) {
				if(c - begin > 0)
					ret.add(value.substring(begin, c).trim());
				begin = c + 1;
				continue;
			} else if(ch == '{')
				bracket++;
			else if(ch == '}' && bracket > 0)
				bracket--;
		}
		if(c > begin)
			ret.add(value.substring(begin, c).trim());
		return ret.toArray(new String[ret.size()]);
	}

	/**
	 * Applies a single style attribute to a style
	 *
	 * @param style The style to apply the attribute to
	 * @param domain The domain of the style
	 * @param attrName The name of the attribute
	 * @param valueStr The serialized value for the attribute
	 * @param messager The message center to issue warnings to if there is an error with the style
	 * @param classView The class view to use for parsing if needed
	 */
	public static void applyStyleAttribute(MutableStyle style, StyleDomain domain, String attrName, String valueStr,
		MuisMessageCenter messager, org.muis.core.MuisClassView classView) {
		StyleAttribute<?> styleAttr = null;
		for(StyleAttribute<?> attrib : domain)
			if(attrib.getName().equals(attrName)) {
				styleAttr = attrib;
				break;
			}

		if(styleAttr == null) {
			messager.warn("No such attribute " + attrName + " in domain " + domain.getName());
			return;
		}

		Object value;
		try {
			value = styleAttr.getType().parse(classView, valueStr, messager);
		} catch(org.muis.core.MuisException e) {
			messager
				.warn("Value " + valueStr + " is not appropriate for style attribute " + attrName + " of domain " + domain.getName(), e);
			return;
		}
		if(styleAttr.getValidator() != null)
			try {
				((StyleAttribute<Object>) styleAttr).getValidator().assertValid(value);
			} catch(org.muis.core.MuisException e) {
				messager.warn(e.getMessage());
				return;
			}
		style.set((StyleAttribute<Object>) styleAttr, value);
	}

	/**
	 * Applies a bulk style setting to a style
	 *
	 * @param style The style to apply the settings to
	 * @param domain The domain that the bulk style is for
	 * @param valueStr The serialized bulk style value
	 * @param messager The message center to issue warnings to if there are errors with the styles
	 * @param classView The class view to use for parsing if needed
	 */
	public static void applyStyleSet(MutableStyle style, StyleDomain domain, String valueStr, MuisMessageCenter messager,
		org.muis.core.MuisClassView classView) { // Setting domain attributes in bulk
		if(valueStr.length() < 2 || valueStr.charAt(0) != '{' || valueStr.charAt(valueStr.length() - 1) != '}') {
			messager.warn("When only a domain is specified, styles must be in the form {property=value; property=value}");
			return;
		}
		String [] propEntries = valueStr.substring(1, valueStr.length() - 1).split(";");
		for(String propEntry : propEntries) {
			int idx = propEntry.indexOf('=');
			if(idx < 0) {
				messager.warn("Bulk style setting " + propEntry.trim() + " is missing an equals sign");
				continue;
			}
			String attrName = propEntry.substring(0, idx).trim();
			String propVal = propEntry.substring(idx + 1).trim();
			applyStyleAttribute(style, domain, attrName, propVal, messager, classView);
		}
	}
}
