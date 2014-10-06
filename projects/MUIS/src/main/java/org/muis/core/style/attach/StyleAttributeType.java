package org.muis.core.style.attach;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisException;
import org.muis.core.MuisParseEnv;
import org.muis.core.MuisProperty;
import org.muis.core.rx.ObservableValue;
import org.muis.core.style.*;

import prisms.lang.Type;

/** The attribute type to parse styles */
public class StyleAttributeType extends MuisProperty.AbstractPropertyType<SealableStyle> implements
	MuisProperty.PrintablePropertyType<SealableStyle> {
	/** The instance to use for the element style */
	public static StyleAttributeType ELEMENT_TYPE = new StyleAttributeType();

	/** The style attribute on MUIS elements */
	public static final MuisAttribute<SealableStyle> STYLE_ATTRIBUTE = new MuisAttribute<>("style", ELEMENT_TYPE, null,
		new StylePathAccepter());

	/** Creates a style attribute type */
	protected StyleAttributeType() {
	}

	@Override
	public Type getType() {
		return new Type(ElementStyle.class);
	}

	@Override
	public ObservableValue<SealableStyle> parse(MuisParseEnv env, String value) throws MuisException {
		return ObservableValue.constant(parseStyle(env, value));
	}

	/**
	 * Parses a style
	 *
	 * @param env The parsing environment
	 * @param value The string to parse
	 * @return The parsed style
	 * @throws MuisException If an unrecoverable error occurs
	 */
	public static SealableStyle parseStyle(MuisParseEnv env, String value) throws MuisException {
		SealableStyle ret = new SealableStyle();
		String [] styles = StyleParsingUtils.splitStyles(value);
		if(styles == null) {
			ret.seal();
			return ret;
		}
		for(String style : styles) {
			int equalIdx = style.indexOf("=");
			if(equalIdx < 0) {
				env.msg().error("Invalid style: " + style + ".  No '='");
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

			StyleDomain domain = StyleParsingUtils.getStyleDomain(ns, domainName, env.cv());

			if(attrName != null)
				applyStyleAttribute(env, ret, domain, attrName, valueStr);
			else
				applyStyleSet(env, ret, domain, valueStr);
		}
		ret.seal();
		return ret;
	}

	/**
	 * Applies a single style attribute to a style
	 *
	 * @param env The parsing environment
	 * @param style The style to apply the attribute to
	 * @param domain The domain of the style
	 * @param attrName The name of the attribute
	 * @param valueStr The serialized value for the attribute
	 */
	protected static void applyStyleAttribute(MuisParseEnv env, MutableStyle style, StyleDomain domain, String attrName,
		String valueStr) {
		StyleParsingUtils.applyStyleAttribute(env, style, domain, attrName, valueStr);
	}

	/**
	 * Applies a bulk style setting to a style
	 *
	 * @param env The parsing environment
	 * @param style The style to apply the settings to
	 * @param domain The domain that the bulk style is for
	 * @param valueStr The serialized bulk style value
	 */
	protected static void applyStyleSet(MuisParseEnv env, MutableStyle style, StyleDomain domain, String valueStr) {
		StyleParsingUtils.applyStyleSet(env, style, domain, valueStr);
	}

	@Override
	public boolean canCast(Type type) {
		return type.canAssignTo(SealableStyle.class);
	}

	@Override
	public SealableStyle cast(Object value) {
		return value instanceof SealableStyle ? (SealableStyle) value : null;
	}

	@Override
	public String toString(SealableStyle value) {
		StringBuilder ret = new StringBuilder();
		for(StyleAttribute<?> attr : value.localAttributes()) {
			if(ret.length() > 0)
				ret.append(';');
			ret.append(attr.getDomain().getName()).append('.').append(attr.getName()).append('=');
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
