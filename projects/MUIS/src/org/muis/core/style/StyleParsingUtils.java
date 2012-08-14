package org.muis.core.style;

/** A few utiliy methods for parsing style information from attribute values */
public class StyleParsingUtils {
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
	public static void applyStyleAttribute(MuisStyle style, StyleDomain domain, String attrName, String valueStr,
		org.muis.core.mgr.MuisMessageCenter messager, org.muis.core.MuisClassView classView) {
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
			value = styleAttr.getType().parse(classView, valueStr);
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
	public static void applyStyleSet(MuisStyle style, StyleDomain domain, String valueStr,
		org.muis.core.mgr.MuisMessageCenter messager, org.muis.core.MuisClassView classView) { // Setting domain attributes in bulk--value
																								// must be JSON
		if(valueStr.length() < 2 || valueStr.charAt(0) != '{' || valueStr.charAt(1) != '}') {
			messager.warn("When only a domain is specified, styles must be in the form {property:value, property:value}");
			return;
		}
		String [] propEntries = valueStr.substring(1, valueStr.length() - 1).split(",");
		for(String propEntry : propEntries) {
			int idx = propEntry.indexOf(':');
			if(idx < 0) {
				messager.warn("Bulk style setting " + propEntry.trim() + " is missing a colon");
				continue;
			}
			String attrName = propEntry.substring(0, idx).trim();
			String propVal = propEntry.substring(idx + 1).trim();
			applyStyleAttribute(style, domain, attrName, propVal, messager, classView);
		}
	}
}
