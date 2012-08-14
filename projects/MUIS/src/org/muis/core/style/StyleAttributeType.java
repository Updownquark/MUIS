package org.muis.core.style;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisElement;
import org.muis.core.MuisException;

/** The attribute type to parse styles */
public class StyleAttributeType extends org.muis.core.MuisProperty.AbstractPropertyType<MuisStyle> {
	/** The instance to use for the element style */
	public static StyleAttributeType ELEMENT_TYPE = new StyleAttributeType(0);

	/** The instance to use for the element's self style */
	public static StyleAttributeType ELEMENT_SELF_TYPE = new StyleAttributeType(1);

	/** The instance to use for the element's heir style */
	public static StyleAttributeType ELEMENT_HEIR_TYPE = new StyleAttributeType(2);

	/** The style attribute on MUIS elements */
	public static final MuisAttribute<MuisStyle> STYLE_ATTRIBUTE = new MuisAttribute<MuisStyle>("style", ELEMENT_TYPE);

	/** The style attribute on MUIS elements */
	public static final MuisAttribute<MuisStyle> STYLE_SELF_ATTRIBUTE = new MuisAttribute<MuisStyle>("style.self", ELEMENT_SELF_TYPE);

	/** The style attribute on MUIS elements */
	public static final MuisAttribute<MuisStyle> STYLE_HEIR_ATTRIBUTE = new MuisAttribute<MuisStyle>("style.heir", ELEMENT_HEIR_TYPE);

	private final int theType;

	/**
	 * Protected so developers will use {@link #ELEMENT_TYPE}, {@link #ELEMENT_SELF_TYPE}, or {@link #ELEMENT_HEIR_TYPE}
	 *
	 * @param type 0 for the element style, 1 for the element's self style, 2 for the element's heir style
	 */
	protected StyleAttributeType(int type) {
		theType = type;
	}

	@Override
	public Class<ElementStyle> getType() {
		return ElementStyle.class;
	}

	@Override
	public MuisStyle parse(org.muis.core.MuisClassView classView, String value) throws MuisException {
		MuisElement element = classView.getElement();
		if(element == null)
			throw new MuisException("Style attributes may only be parsed for elements");
		MuisStyle ret;
		switch (theType) {
		case 0:
			ret = element.getStyle();
			break;
		case 1:
			ret = element.getStyle().getSelf();
			break;
		case 2:
			ret = element.getStyle().getHeir();
			break;
		default:
			throw new IllegalStateException("StyleAttributeType.type must be 0, 1, or 2; not " + theType);
		}
		String [] styles = value.split(";");
		for(String style : styles) {
			int equalIdx = style.indexOf("=");
			if(equalIdx < 0) {
				element.msg().error("Invalid style: " + style + ".  No '='");
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

			org.muis.core.MuisToolkit toolkit;
			String domainClassName;
			if(ns == null) {
				toolkit = element.getToolkit();
				domainClassName = toolkit.getMappedClass(domainName);
				if(domainClassName == null && toolkit != element.getDocument().getCoreToolkit()) {
					toolkit = element.getDocument().getCoreToolkit();
					domainClassName = toolkit.getMappedClass(domainName);
				}
				if(domainClassName == null)
					if(element.getToolkit() != element.getDocument().getCoreToolkit())
						element.msg().warn(
							"No style domain mapped to " + domainName + " in element's toolkit (" + element.getToolkit().getName()
								+ ") or default toolkit");
					else
						element.msg().warn("No style domain mapped to " + domainName + " in default toolkit");
			} else {
				toolkit = element.getClassView().getToolkit(ns);
				if(toolkit == null) {
					element.msg().warn("No toolkit mapped to namespace " + ns + " for style " + style);
					continue;
				}
				domainClassName = toolkit.getMappedClass(domainName);
				if(domainClassName == null) {
					element.msg().warn("No style domain mapped to " + domainName + " in toolkit " + toolkit.getName());
					continue;
				}
			}

			Class<? extends org.muis.core.style.StyleDomain> domainClass;
			try {
				domainClass = toolkit.loadClass(domainClassName, org.muis.core.style.StyleDomain.class);
			} catch(MuisException e) {
				element.msg().warn("Could not load domain class " + domainClassName + " from toolkit " + toolkit.getName(), e);
				continue;
			}
			org.muis.core.style.StyleDomain domain;
			try {
				domain = (org.muis.core.style.StyleDomain) domainClass.getMethod("getDomainInstance", new Class[0]).invoke(null,
					new Object[0]);
			} catch(Exception e) {
				element.msg().warn("Could not get domain instance", e);
				continue;
			}

			if(attrName != null)
				applyStyleAttribute(ret, domain, attrName, valueStr, element.msg(), element.getClassView());
			else
				applyStyleSet(ret, domain, valueStr, element.msg(), element.getClassView());
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
	 * @param classView The class view to use for parsing if needed
	 */
	protected void applyStyleAttribute(org.muis.core.style.MuisStyle style, StyleDomain domain, String attrName, String valueStr,
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
	protected void applyStyleSet(org.muis.core.style.MuisStyle style, StyleDomain domain, String valueStr,
		org.muis.core.mgr.MuisMessageCenter messager, org.muis.core.MuisClassView classView) {
		StyleParsingUtils.applyStyleSet(style, domain, valueStr, messager, classView);
	}

	@Override
	public ElementStyle cast(Object value) {
		return value instanceof ElementStyle ? (ElementStyle) value : null;
	}
}
