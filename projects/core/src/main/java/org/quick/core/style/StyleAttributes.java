package org.quick.core.style;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.observe.ObservableValue;
import org.quick.core.QuickException;
import org.quick.core.QuickParseEnv;
import org.quick.core.parser.QuickParseException;
import org.quick.core.parser.QuickPropertyParser;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.prop.QuickPropertyType;

import com.google.common.reflect.TypeToken;

/** The attribute type to parse styles */
public class StyleAttributes {
	/** The instance to use for the element style */
	public static final QuickPropertyType<QuickStyle> STYLE_TYPE = QuickPropertyType.build("style", TypeToken.of(QuickStyle.class))//
		.withParser((parser, env, str) -> {
			return ObservableValue.constant(TypeToken.of(QuickStyle.class), parseStyle(parser, env, str));
		}, true)//
		.withToString(style2 -> {
			StringBuilder ret = new StringBuilder();
			for (StyleAttribute<?> attr : style2.attributes()) {
				if (ret.length() > 0)
					ret.append(';');
				ret.append(attr.getDomain().getName()).append('.').append(attr.getName()).append('=');
				String valueStr = ((StyleAttribute<Object>) attr).getType().toString(style2.get(attr).get());
				valueStr = valueStr.replaceAll("\"", "'");
				ret.append(valueStr);
			}
			return ret.toString();
		}).build();

	/** The type of the group attribute */
	public static final QuickPropertyType<Set<String>> GROUP_TYPE = QuickPropertyType.build("group", new TypeToken<Set<String>>() {})//
		.withParser((parser, env, str) -> {
			String[] split = str.split(",");
			LinkedHashSet<String> groups = new LinkedHashSet<>();
			for (String s : split)
				groups.add(s.trim());
			return ObservableValue.constant(new TypeToken<Set<String>>() {}, Collections.unmodifiableSet(groups));
		}, true)//
		.build();

	/** The style attribute on Quick elements */
	public static final QuickAttribute<QuickStyle> style = QuickAttribute.build("style", STYLE_TYPE).build();

	/** The group attribute */
	public static final QuickAttribute<Set<String>> group = QuickAttribute.build("group", GROUP_TYPE).build();

	/**
	 * Parses a style
	 *
	 * @param parser The property parser to parse style values
	 * @param env The parsing environment
	 * @param value The string to parse
	 * @return The parsed style
	 * @throws QuickParseException If an unrecoverable error occurs
	 */
	public static QuickStyle parseStyle(QuickPropertyParser parser, QuickParseEnv env, String value) throws QuickParseException {
		String[] styles = StyleParsingUtils.splitStyles(value);
		if (styles == null) {
			return null;
		}
		ImmutableStyle.Builder builder = ImmutableStyle.build(env.msg());
		for (String style2 : styles) {
			int equalIdx = style2.indexOf("=");
			if (equalIdx < 0) {
				env.msg().error("Invalid style: " + style2 + ".  No '='");
				continue;
			}
			String attr = style2.substring(0, equalIdx).trim();
			String valueStr = style2.substring(equalIdx + 1).trim();
			String ns, domainName, attrName;
			int nsIdx = attr.indexOf(':');
			if (nsIdx >= 0) {
				ns = attr.substring(0, nsIdx).trim();
				domainName = attr.substring(nsIdx + 1).trim();
			} else {
				ns = null;
				domainName = attr;
			}

			int dotIdx = domainName.indexOf('.');
			if (dotIdx >= 0) {
				attrName = domainName.substring(dotIdx + 1).trim();
				domainName = domainName.substring(0, dotIdx).trim();
			} else
				attrName = null;

			StyleDomain domain;
			try {
				domain = StyleParsingUtils.getStyleDomain(ns, domainName, env.cv());
			} catch (QuickException e) {
				env.msg().error("Could not get style domain " + domainName, e);
				return null;
			}

			if (attrName != null)
				StyleParsingUtils.applyStyleAttribute(parser, env, domain, attrName, valueStr, builder);
			else
				StyleParsingUtils.applyStyleSet(parser, env, domain, valueStr, builder);
		}
		return builder.build();
	}
}
