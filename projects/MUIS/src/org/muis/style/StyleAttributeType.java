package org.muis.style;

import org.muis.core.MuisElement;
import org.muis.core.MuisException;

/** The attribute type to parse styles */
public class StyleAttributeType implements org.muis.core.MuisAttribute.AttributeType<AttributeStyle>
{
	/** The instance to use */
	public static StyleAttributeType TYPE = new StyleAttributeType();

	/** Protected so developers will use {@link #TYPE} */
	protected StyleAttributeType()
	{
	}

	@Override
	public String validate(MuisElement element, String value)
	{
		return null; // Style error are only warnings
	}

	@Override
	public AttributeStyle parse(MuisElement element, String value) throws MuisException
	{
		AttributeStyle ret = new AttributeStyle(element);
		String [] styles = value.split(";");
		for(String style : styles)
		{
			int equalIdx = style.indexOf("=");
			if(equalIdx < 0)
			{
				element.error("Invalid style: " + style + ".  No '='", null);
				continue;
			}
			String attr = style.substring(0, equalIdx).trim();
			String valueStr = style.substring(equalIdx + 1).trim();
			String ns, domainName, attrName;
			int nsIdx = attr.indexOf(':');
			if(nsIdx >= 0)
			{
				ns = attr.substring(0, nsIdx).trim();
				domainName = attr.substring(nsIdx + 1).trim();
			}
			else
			{
				ns = null;
				domainName = attr;
			}

			int dotIdx = domainName.indexOf('.');
			if(dotIdx >= 0)
			{
				attrName = domainName.substring(dotIdx + 1).trim();
				domainName = domainName.substring(0, dotIdx).trim();
			}
			else
				attrName = null;

			org.muis.core.MuisToolkit toolkit;
			String domainClassName;
			if(ns == null)
			{
				toolkit = element.getToolkit();
				domainClassName = toolkit.getMappedClass(domainName);
				if(domainClassName == null && toolkit != element.getDocument().getDefaultToolkit())
				{
					toolkit = element.getDocument().getDefaultToolkit();
					domainClassName = toolkit.getMappedClass(domainName);
				}
				if(domainClassName == null)
					if(element.getToolkit() != element.getDocument().getDefaultToolkit())
						element.warn("No style domain mapped to " + domainName + " in element's toolkit (" + element.getToolkit().getName()
							+ ") or default toolkit");
					else
						element.warn("No style domain mapped to " + domainName + " in default toolkit");
			}
			else
			{
				toolkit = element.getClassView().getToolkit(ns);
				if(toolkit == null)
				{
					element.warn("No toolkit mapped to namespace " + ns + " for style " + style);
					continue;
				}
				domainClassName = toolkit.getMappedClass(domainName);
				if(domainClassName == null)
				{
					element.warn("No style domain mapped to " + domainName + " in toolkit " + toolkit.getName());
					continue;
				}
			}

			Class<? extends org.muis.style.StyleDomain> domainClass;
			try
			{
				domainClass = toolkit.loadClass(domainClassName, org.muis.style.StyleDomain.class);
			} catch(MuisException e)
			{
				element.warn("Could not load domain class " + domainClassName + " from toolkit " + toolkit.getName(), e);
				continue;
			}
			org.muis.style.StyleDomain domain;
			try
			{
				domain = (org.muis.style.StyleDomain) domainClass.getMethod("getDomainInstance", new Class[0]).invoke(null, new Object[0]);
			} catch(Exception e)
			{
				element.warn("Could not get domain instance", e);
				continue;
			}

			if(attrName != null)
				applyStyleAttribute(ret, domain, attrName, valueStr, element);
			else
				applyStyleSet(ret, domain, valueStr, element);
		}
		return ret;
	}

	/**
	 * Applies a single style attribute to a style
	 *
	 * @param style The style to apply the attribute to
	 * @param domain The domain of the style
	 * @param attrName The name of the attribute
	 * @param valueStr The serialized value for the attribute
	 * @param messager The message center to issue warnings to if there is an error with the style
	 */
	protected void applyStyleAttribute(org.muis.style.MuisStyle style, StyleDomain domain, String attrName, String valueStr,
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
		} catch(MuisException e)
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
	protected void applyStyleSet(org.muis.style.MuisStyle style, StyleDomain domain, String valueStr,
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

	@Override
	public AttributeStyle cast(Object value)
	{
		return value instanceof AttributeStyle ? (AttributeStyle) value : null;
	}
}
