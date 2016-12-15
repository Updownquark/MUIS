package org.quick.core.style;

import org.observe.ObservableValue;
import org.quick.core.QuickException;
import org.quick.core.QuickParseEnv;
import org.quick.core.parser.QuickPropertyParser;

/** A few utility methods for parsing style information from attribute values */
public class StyleParsingUtils {
	/**
	 * Gets a style domain
	 *
	 * @param ns The namespace specifying the toolkit that the domain is from. May be null.
	 * @param domainName The mapped name of the domain class within its toolkit
	 * @param classView The class view to retrieve the domain class from
	 * @return The style domain mapped by the given name in the given toolkit
	 * @throws QuickException If the style domain cannot be retrieved with the given information
	 */
	public static StyleDomain getStyleDomain(String ns, String domainName, org.quick.core.QuickClassView classView) throws QuickException {
		org.quick.core.QuickToolkit toolkit = null;
		String domainClassName;
		if (ns == null) {
			domainClassName = classView.getMappedClass(ns, domainName);
			if (domainClassName == null)
				throw new QuickException("No style domain mapped to " + domainName + " in class view");
		} else {
			toolkit = classView.getToolkit(ns);
			if (toolkit == null) {
				throw new QuickException("No toolkit mapped to namespace " + ns);
			}
			domainClassName = toolkit.getMappedClass(domainName);
			if (domainClassName == null) {
				throw new QuickException(
					"No style domain mapped to " + domainName + " in toolkit " + toolkit.getName() + "(ns=" + ns + ")");
			}
		}

		Class<? extends org.quick.core.style.StyleDomain> domainClass;
		try {
			if (toolkit != null)
				domainClass = toolkit.loadClass(domainClassName, org.quick.core.style.StyleDomain.class);
			else
				domainClass = classView.loadMappedClass(ns, domainName, StyleDomain.class);
		} catch (QuickException e) {
			throw new QuickException("Could not load domain class " + domainClassName + " from toolkit " + toolkit.getName(), e);
		}
		org.quick.core.style.StyleDomain domain;
		try {
			domain = (org.quick.core.style.StyleDomain) domainClass.getMethod("getDomainInstance", new Class[0]).invoke(null,
				new Object[0]);
		} catch (Exception e) {
			throw new QuickException("Could not get domain instance", e);
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
	public static String[] splitStyles(String value) {
		java.util.ArrayList<String> ret = new java.util.ArrayList<>(1);
		int begin = 0;
		int bracket = 0;
		int c;
		for (c = 0; c < value.length(); c++) {
			char ch = value.charAt(c);
			if (Character.isWhitespace(ch))
				continue;
			if (ch == ';' && bracket == 0) {
				if (c - begin > 0)
					ret.add(value.substring(begin, c).trim());
				begin = c + 1;
				continue;
			} else if (ch == '{')
				bracket++;
			else if (ch == '}' && bracket > 0)
				bracket--;
		}
		if (c > begin)
			ret.add(value.substring(begin, c).trim());
		return ret.toArray(new String[ret.size()]);
	}

	/**
	 * Applies a single style attribute to a style
	 *
	 * @param parser The property parser to parse the style value
	 * @param env The parsing environment
	 * @param domain The domain of the style
	 * @param attrName The name of the attribute
	 * @param valueStr The serialized value for the attribute
	 * @param setter The style to apply the style value
	 */
	public static void applyStyleAttribute(QuickPropertyParser parser, QuickParseEnv env, StyleDomain domain, String attrName,
		String valueStr, StyleSetter setter) {
		StyleAttribute<?> styleAttr = null;
		for (StyleAttribute<?> attrib : domain)
			if (attrib.getName().equals(attrName)) {
				styleAttr = attrib;
				break;
			}

		if (styleAttr == null) {
			env.msg().warn("No such attribute " + attrName + " in domain " + domain.getName());
			return;
		}

		applyParsedValue(parser, env, styleAttr, valueStr, setter);
	}

	private static <T> void applyParsedValue(QuickPropertyParser parser, QuickParseEnv env, StyleAttribute<T> styleAttr, String valueStr,
		StyleSetter setter) {
		ObservableValue<? extends T> value = null;
		if (valueStr.startsWith("^{") && valueStr.endsWith("}")) {
			value = (ObservableValue<T>) env.getContext().getVariable(valueStr);
			if (value != null) {
				if (!styleAttr.getType().canAccept(value.getType())) {
					env.msg().error("Value " + valueStr + " cannot be assigned to attribute styleAttr");
					return;
				}
			}
		}
		if (value == null) {
			try {
				value = parser.parseProperty(styleAttr, env, valueStr);
			} catch (org.quick.core.QuickException e) {
				env.msg().warn("Value " + valueStr + " is not appropriate for style attribute " + styleAttr.getName() + " of domain "
					+ styleAttr.getDomain().getName(), e);
				return;
			}
		}
		setter.set(styleAttr, value);
	}

	/**
	 * Applies a bulk style setting to a style
	 *
	 * @param parser The property parser to parse style values
	 * @param env The parsing environment
	 * @param domain The domain that the bulk style is for
	 * @param valueStr The serialized bulk style value
	 * @param setter The setter to apply the style values
	 */
	public static void applyStyleSet(QuickPropertyParser parser, QuickParseEnv env, StyleDomain domain, String valueStr,
		StyleSetter setter) {
		// Setting domain attributes in bulk
		if (valueStr.length() < 2 || valueStr.charAt(0) != '{' || valueStr.charAt(valueStr.length() - 1) != '}') {
			env.msg().warn("When only a domain is specified, styles must be in the form {property=value; property=value}");
			return;
		}
		String[] propEntries = valueStr.substring(1, valueStr.length() - 1).split(";");
		for (String propEntry : propEntries) {
			int idx = propEntry.indexOf('=');
			if (idx < 0) {
				env.msg().warn("Bulk style setting " + propEntry.trim() + " is missing an equals sign");
				continue;
			}
			String attrName = propEntry.substring(0, idx).trim();
			String propVal = propEntry.substring(idx + 1).trim();
			applyStyleAttribute(parser, env, domain, attrName, propVal, setter);
		}
	}
}
