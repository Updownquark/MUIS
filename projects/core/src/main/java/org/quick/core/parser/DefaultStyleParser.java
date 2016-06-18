package org.quick.core.parser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom2.Element;
import org.observe.ObservableValue;
import org.observe.collect.ObservableList;
import org.quick.core.*;
import org.quick.core.mgr.QuickMessageCenter;
import org.quick.core.mgr.QuickState;
import org.quick.core.prop.DefaultExpressionContext;
import org.quick.core.style.StyleAttribute;
import org.quick.core.style.StyleDomain;
import org.quick.core.style.StyleParsingUtils;
import org.quick.core.style.sheet.ParsedStyleSheet;
import org.quick.core.style.sheet.StateGroupTypeExpression;
import org.quick.core.style.sheet.StyleSheet;
import org.quick.core.style.stateful.StateExpression;
import org.quick.util.QuickUtils;

import com.google.common.reflect.TypeToken;

/** Parses .qss XML style files for Quick */
public class DefaultStyleParser implements QuickStyleParser {
	private static final Pattern STATE_PATTERN = Pattern.compile("[a-zA-Z]+[a-zA-Z0-9-]*");
	private final QuickEnvironment theEnvironment;

	/** @param env The environment that this parser is for */
	public DefaultStyleParser(QuickEnvironment env) {
		theEnvironment = env;
	}

	@Override
	public QuickEnvironment getEnvironment() {
		return theEnvironment;
	}

	@Override
	public ParsedStyleSheet parseStyleSheet(URL location, QuickToolkit toolkit, QuickPropertyParser parser, QuickClassView cv,
		QuickMessageCenter msg) throws IOException, QuickParseException {
		Element rootEl;
		try {
			rootEl = new org.jdom2.input.SAXBuilder().build(new java.io.InputStreamReader(location.openStream())).getRootElement();
		} catch (org.jdom2.JDOMException e) {
			throw new QuickParseException("Could not parse quick style XML for " + location, e);
		}

		ParsedStyleSheet styleSheet = new ParsedStyleSheet(msg, ObservableList.constant(TypeToken.of(StyleSheet.class)));
		ExpressionContextStack stack = new ExpressionContextStack(theEnvironment, toolkit);
		stack.push();
		addNamespaces(rootEl, location, stack, msg);
		QuickParseEnv parseEnv = new SimpleParseEnv(cv, msg, DefaultExpressionContext.build().build()); // TODO time variables
		for (Element child : rootEl.getChildren())
			parseStyleElement(child, location, parser, parseEnv, stack, styleSheet);
		return null;
	}

	private void addNamespaces(Element xml, URL location, ExpressionContextStack stack, QuickMessageCenter msg) {
		for (org.jdom2.Namespace ns : xml.getNamespacesIntroduced()) {
			QuickToolkit toolkit;
			try {
				toolkit = theEnvironment.getToolkit(QuickUtils.resolveURL(location, ns.getURI()));
			} catch (MalformedURLException e) {
				msg.error("Invalid URL \"" + ns.getURI() + "\" for toolkit at namespace " + ns.getPrefix(), e);
				continue;
			} catch (IOException e) {
				msg.error("Could not read toolkit " + ns.getPrefix() + ":" + ns.getURI(), e);
				continue;
			} catch (QuickParseException e) {
				msg.error("Could not parse toolkit " + ns.getPrefix() + ":" + ns.getURI(), e);
				continue;
			} catch (QuickException e) {
				msg.error("Could not resolve location of toolkit for namespace " + ns.getPrefix(), e);
				continue;
			}
			try {
				stack.top().getClassView().addNamespace(ns.getPrefix(), toolkit);
			} catch (QuickException e) {
				throw new IllegalStateException("Should not happen", e);
			}
		}
	}

	private void parseStyleElement(Element xml, URL location, QuickPropertyParser parser, QuickParseEnv parseEnv,
		ExpressionContextStack stack,
		ParsedStyleSheet styleSheet) {
		stack.push();
		addNamespaces(xml, location, stack, parseEnv.msg());
		String name = xml.getAttributeValue("name");
		switch (xml.getName()) {
		case "type":
			Class<? extends QuickElement> type;
			try {
				type = stack.top().getClassView().loadMappedClass(name, QuickElement.class);
			} catch (QuickException e) {
				type = null;
				parseEnv.msg().error("Could not load element type", e, "type", name);
			}
			if (type != null) {
				try {
					stack.addType(type);
				} catch (QuickParseException e) {
					parseEnv.msg().error(e.getMessage(), e, "type", type);
				}
			}
			break;
		case "state":
			StateExpression state;
			try {
				state = parseState(name, stack);
			} catch (QuickParseException e) {
				state = null;
				parseEnv.msg().error(e.getMessage(), e, "state", name);
			}
			if (state != null)
				stack.setState(state);
			break;
		case "group":
			try {
				stack.addGroup(name);
			} catch (QuickParseException e) {
				parseEnv.msg().error(e.getMessage(), e, "group", name);
			}
			break;
		case "attach-point":
			try {
				stack.addAttachPoint(name, theEnvironment);
			} catch (QuickParseException e) {
				parseEnv.msg().error(e.getMessage(), e, "attach-point", name);
			}
			break;
		case "domain":
			for (Element child : xml.getChildren()) {
				if (!"attr".equals(child.getName())) {
					parseEnv.msg().error("Only attr elements are allowed under domain elements in style sheets", "name", child.getName());
					continue;
				}
				String attr = child.getAttributeValue("name");
				String valueStr = child.getAttributeValue("value");
				applyStyleValue(name, attr, valueStr, parser, parseEnv, styleSheet, stack);
			}
			break;
		case "attr":
			String domain = xml.getAttributeValue("domain");
			String valueStr = xml.getAttributeValue("value");
			applyStyleValue(domain, name, valueStr, parser, parseEnv, styleSheet, stack);
		}
	}

	private StateExpression parseState(String name, ExpressionContextStack stack) throws QuickParseException {
		return parseState(name, stack, new int[] { 0 }, new int[] { name.length() });
	}

	private StateExpression parseState(String name, ExpressionContextStack stack, int[] start, int[] end) throws QuickParseException {
		if (start[0] == end[0])
			return null;
		StateExpression total = null;
		boolean and = false;
		boolean or = false;
		while (start[0] != end[0]) {
			StateExpression next = parseNextState(name, stack, start, end);
			if (total == null)
				total = next;
			else if (and) {
				total = total.and(next);
			} else if (or) {
				total = total.or(next);
			}
			and = or = false;

			while (start[0] != end[0] && Character.isWhitespace(name.charAt(start[0])))
				start[0]++;
			if (start[0] != end[0]) {
				switch (name.charAt(start[0])) {
				case '&':
					and = true;
					start[0]++;
					break;
				case '|':
					or = true;
					start[0]++;
					break;
				default:
					throw new QuickParseException("Unrecognized state expression at char " + start[0]);
				}
			}
		}
		return total;
	}

	private StateExpression parseNextState(String name, ExpressionContextStack stack, int[] start, int[] end) throws QuickParseException {
		while (start[0] != end[0] && Character.isWhitespace(name.charAt(start[0])))
			start[0]++;
		switch (name.charAt(start[0])) {
		case '(':
			int endParen = getEndParen(name);
			if (endParen < 0)
				throw new QuickParseException("Parentheses do not match");
			StateExpression next = parseState(name, stack, new int[] { start[0] + 1 }, new int[] { endParen });
			start[0] = endParen + 1;
			return next;
		case '!':
			start[0]++;
			return parseNextState(name, stack, start, end).not();
		default:
			Matcher matcher = STATE_PATTERN.matcher(name.substring(start[0], end[0]));
			if (!matcher.lookingAt()) {
				throw new QuickParseException("Unrecognized state expression at char " + start[0]);
			}
			QuickState state = findState(matcher.group(), stack);
			start[0] += matcher.end();
			return StateExpression.forState(state);
		}
	}

	private int getEndParen(String name) {
		int depth = 1;
		for (int c = 1; c < name.length(); c++) {
			if (name.charAt(c) == '(')
				depth++;
			else if (name.charAt(c) == ')') {
				depth--;
				if (depth == 0)
					return c;
			}
		}
		return -1;
	}

	private QuickState findState(String name, ExpressionContextStack stack) throws QuickParseException {
		Class<? extends QuickElement>[] types = stack.getTopTypes();
		if (types.length == 0)
			types = new Class[] { QuickElement.class };
		QuickState ret = null;
		for (int i = 0; i < types.length; i++) {
			boolean found = false;
			QuickState[] states = org.quick.util.QuickUtils.getStatesFor(types[i]);
			for (int j = 0; j < states.length; j++)
				if (states[j].getName().equals(name)) {
					found = true;
					if (ret == null)
						ret = states[j];
				}
			if (!found)
				throw new QuickParseException("Element type " + types[i].getName() + " does not support state \"" + name + "\"");
		}
		return ret;
	}

	private void applyStyleValue(String domainName, String attrName, String valueStr, QuickPropertyParser parser, QuickParseEnv parseEnv,
		ParsedStyleSheet styleSheet, ExpressionContextStack stack) {
		String ns;
		int nsIdx = domainName.indexOf(':');
		if (nsIdx >= 0) {
			ns = domainName.substring(0, nsIdx).trim();
			domainName = domainName.substring(nsIdx + 1).trim();
		} else
			ns = null;

		StyleDomain domain;
		try {
			domain = StyleParsingUtils.getStyleDomain(ns, domainName, stack.top().getClassView());
		} catch (QuickException e) {
			parseEnv.msg().error("Could not get style domain " + domainName, e);
			return;
		}

		StyleAttribute<?> styleAttr = null;
		for (StyleAttribute<?> attrib : domain)
			if (attrib.getName().equals(attrName)) {
				styleAttr = attrib;
				break;
			}

		if (styleAttr == null) {
			parseEnv.msg().warn("No such attribute " + attrName + " in domain " + domainName);
			return;
		}

		if(stack.size()>0){
			for(StateGroupTypeExpression<?> expr : stack){
				applyParsedValue(parser, parseEnv, styleAttr, valueStr, styleSheet, expr);
			}
		} else
			applyParsedValue(parser, parseEnv, styleAttr, valueStr, styleSheet, null);
	}

	private static <T> void applyParsedValue(QuickPropertyParser parser, QuickParseEnv env, StyleAttribute<T> styleAttr, String valueStr,
		ParsedStyleSheet styleSheet, StateGroupTypeExpression<?> expr) {
		ObservableValue<T> value;
		try {
			value = parser.parseProperty(styleAttr, env, valueStr);
		} catch (org.quick.core.QuickException e) {
			env.msg().warn("Value " + valueStr + " is not appropriate for style attribute " + styleAttr.getName() + " of domain "
				+ styleAttr.getDomain().getName(), e);
			return;
		}
		styleSheet.set(styleAttr, expr, value);
	}
}
