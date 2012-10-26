package org.muis.core.style.attach;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisException;
import org.muis.core.style.*;

/** The attribute type to parse styles */
public class StyleAttributeType extends org.muis.core.MuisProperty.AbstractPropertyType<MuisStyle> implements
	org.muis.core.MuisProperty.PrintablePropertyType<MuisStyle> {
	/** The instance to use for the element style */
	public static StyleAttributeType ELEMENT_TYPE = new StyleAttributeType();

	/** The style attribute on MUIS elements */
	public static final MuisAttribute<MuisStyle> STYLE_ATTRIBUTE = new MuisAttribute<MuisStyle>("style", ELEMENT_TYPE, null,
		new StylePathAccepter());

	/** Creates a style attribute type */
	protected StyleAttributeType() {
	}

	@Override
	public Class<ElementStyle> getType() {
		return ElementStyle.class;
	}

	@Override
	public MuisStyle parse(org.muis.core.MuisClassView classView, String value, org.muis.core.mgr.MuisMessageCenter msg)
		throws MuisException {
		SealableStyle ret = new SealableStyle();
		String [] styles = value.split(";");
		for(String style : styles) {
			int equalIdx = style.indexOf("=");
			if(equalIdx < 0) {
				msg.error("Invalid style: " + style + ".  No '='");
				continue;
			}
			String attr = style.substring(0, equalIdx).trim();
			String valueStr = style.substring(equalIdx + 1).trim();
			String ns, domainName, attrName;
			int nsIdx = attr.indexOf(':');
			if(nsIdx >= 0) {
				ns = attr.substring(0, nsIdx).trim();
				domainName = attr.substring(nsIdx + 1).trim();
			} else {
				ns = null;
				domainName = attr;
			}

			int dotIdx = domainName.indexOf('.');
			if(dotIdx >= 0) {
				attrName = domainName.substring(dotIdx + 1).trim();
				domainName = domainName.substring(0, dotIdx).trim();
			} else
				attrName = null;

			StyleDomain domain = StyleParsingUtils.getStyleDomain(ns, domainName, classView);

			if(attrName != null)
				applyStyleAttribute(ret, domain, attrName, valueStr, msg, classView);
			else
				applyStyleSet(ret, domain, valueStr, msg, classView);
		}
		ret.seal();
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
	 * @param classView The class view to use for parsing if needed
	 */
	protected void applyStyleAttribute(MutableStyle style, StyleDomain domain, String attrName, String valueStr,
		org.muis.core.mgr.MuisMessageCenter messager, org.muis.core.MuisClassView classView) {
		StyleParsingUtils.applyStyleAttribute(style, domain, attrName, valueStr, messager, classView);
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
	protected void applyStyleSet(MutableStyle style, StyleDomain domain, String valueStr, org.muis.core.mgr.MuisMessageCenter messager,
		org.muis.core.MuisClassView classView) {
		StyleParsingUtils.applyStyleSet(style, domain, valueStr, messager, classView);
	}

	@Override
	public MuisStyle cast(Object value) {
		return value instanceof SealableStyle ? (SealableStyle) value : null;
	}

	@Override
	public String toString(MuisStyle value) {
		StringBuilder ret = new StringBuilder();
		for(StyleAttribute<?> attr : value.localAttributes()) {
			if(ret.length() > 0)
				ret.append(';');
			ret.append(attr.getDomain().getName()).append('.').append(attr.getName()).append(':');
			String valueStr;
			if(attr.getType() instanceof org.muis.core.MuisProperty.PrintablePropertyType)
				valueStr = ((org.muis.core.MuisProperty.PrintablePropertyType<Object>) attr.getType()).toString(value.get(attr));
			else
				valueStr = String.valueOf(value.get(attr));
			valueStr = valueStr.replaceAll("\"", "'");
			ret.append(valueStr);
		}
		return ret.toString();
	}
}
