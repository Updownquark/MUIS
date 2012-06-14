package org.muis.core.style;

/** A few utiliy methods for parsing style information from attribute values */
public class StyleParsingUtils
{
	/**
	 * Applies a single style attribute to a style
	 *
	 * @param style The style to apply the attribute to
	 * @param domain The domain of the style
	 * @param attrName The name of the attribute
	 * @param valueStr The serialized value for the attribute
	 * @param messager The message center to issue warnings to if there is an error with the style
	 */
	public static void applyStyleAttribute(org.muis.core.style.MuisStyle style, StyleDomain domain, String attrName, String valueStr,
		org.muis.core.MuisMessage.MuisMessageCenter messager)
	{
		StyleAttribute<?> styleAttr = null;
		for(StyleAttribute<?> attrib : domain)
			if(attrib.name.equals(attrName))
				styleAttr = attrib;

		if(styleAttr == null)
		{
			messager.warn("No such attribute " + attrName + " in domain " + domain.getName());
			return;
		}

		Object value;
		try
		{
			value = styleAttr.parse(valueStr);
		} catch(org.muis.core.MuisException e)
		{
			messager
				.warn("Value " + valueStr + " is not appropriate for style attribute " + attrName + " of domain " + domain.getName(), e);
			return;
		}
		String error = styleAttr.validate(value);
		if(error != null)
		{
			messager.warn("Value " + valueStr + " is not appropriate for style attribute " + attrName + " of domain " + domain.getName()
				+ ": " + error);
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
	 */
	public static void applyStyleSet(org.muis.core.style.MuisStyle style, StyleDomain domain, String valueStr,
		org.muis.core.MuisMessage.MuisMessageCenter messager)
	{ // Setting domain attributes in bulk--value must be JSON
		if(valueStr.length() < 2 || valueStr.charAt(0) != '{' || valueStr.charAt(1) != '}')
		{
			messager.warn("When only a domain is specified, styles must be in the form {property:value, property:value}");
			return;
		}
		String [] propEntries = valueStr.substring(1, valueStr.length() - 1).split(",");
		for(String propEntry : propEntries)
		{
			int idx = propEntry.indexOf(':');
			if(idx < 0)
			{
				messager.warn("Bulk style setting " + propEntry.trim() + " is missing a colon");
				continue;
			}
			String attrName = propEntry.substring(0, idx).trim();
			String propVal = propEntry.substring(idx + 1).trim();
			applyStyleAttribute(style, domain, attrName, propVal, messager);
		}
	}
}
