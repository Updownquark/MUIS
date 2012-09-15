package org.muis.core.parser;

import java.io.IOException;
import java.io.Reader;

import org.muis.core.MuisClassView;
import org.muis.core.MuisElement;
import org.muis.core.MuisException;
import org.muis.core.MuisToolkit;
import org.muis.core.mgr.MuisMessageCenter;
import org.muis.core.style.DefaultStyleSheet;
import org.muis.core.style.StateExpression;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleSheet;

/** Parses style sheets using the default style syntax */
public class DefaultStyleSheetParser implements StyleSheetParser {
	private static class Context {
		String [] theGroupNames;

		StateExpression theState;

		Class<? extends MuisElement> [] theTypes;

		Context() {
			theGroupNames = new String[0];
			theState = null;
			theTypes = new Class[] {MuisElement.class};
		}

		Context and(String... groupNames) {
			Context ret = new Context();
			ret.theGroupNames = prisms.util.ArrayUtils.mergeInclusive(String.class, theGroupNames, groupNames);
			ret.theState = theState;
			ret.theTypes = theTypes;
			return ret;
		}

		Context and(StateExpression state) {
			Context ret = new Context();
			ret.theGroupNames = theGroupNames;
			ret.theState = theState.and(state).getUnique();
			ret.theTypes = theTypes;
			return ret;
		}

		Context and(Class<? extends MuisElement>... types) {
			Context ret = new Context();
			ret.theGroupNames = theGroupNames;
			ret.theState = theState;
			ret.theTypes = types;
			return ret;
		}
	}

	private static class ParseState {
		int theLine;

		int theChar;

		void next(int ch) {
			if(ch == '\n') {
				theLine++;
				theChar = 0;
			} else if(ch != '\r')
				theChar++;
		}

		@Override
		public String toString() {
			StringBuilder ret = new StringBuilder();
			ret.append("line ").append(theLine + 1).append(", char ").append(theChar + 1);
			return ret.toString();
		}
	}

	@Override
	public StyleSheet parseStyleSheet(java.net.URL location, Reader reader, MuisClassView classView, MuisMessageCenter messager)
		throws IOException, MuisParseException {
		DefaultStyleSheet ret = new DefaultStyleSheet();

		ParseState state = new ParseState();
		Context root = new Context();
		parseTillDone(ret, root, reader, state, classView, messager);
		return ret;
	}

	void parseTillDone(DefaultStyleSheet styleSheet, Context context, Reader reader, ParseState state, MuisClassView classView,
		MuisMessageCenter messager) throws IOException, MuisParseException {
		while(parseNextStyleSet(styleSheet, context, ' ', reader, state, classView, messager));
	}

	boolean parseNextStyleSet(DefaultStyleSheet styleSheet, Context context, char initChar, Reader reader, ParseState state,
		MuisClassView classView, MuisMessageCenter messager) throws IOException, MuisParseException {
		int ch = pastWhiteSpace(initChar, reader, state);
		if(ch < 0)
			return false;
		if(ch == '/')
			passLineComment(reader, state);
		else if(ch == '(')
			parseGroupedStyle(styleSheet, context, reader, state, classView, messager);
		else if(ch == '[')
			parseMultiTypedStyle(styleSheet, context, reader, state, classView, messager);
		else if(ch == '.')
			parseStatefulStyle(styleSheet, context, reader, state, classView, messager);
		else if(ch == '{')
			parseStyleBlock(styleSheet, context, reader, state, classView, messager);
		else if(couldBeStyleDomain((char) ch, true))
			parseStyle(styleSheet, context, (char) ch, reader, state, classView, messager);
		else
			throw new MuisParseException("Illegal character '" + (char) ch + "' at " + state);
		return true;
	}

	private int pastWhiteSpace(int ch, Reader reader, ParseState state) throws IOException {
		if(ch < 0 || !Character.isWhitespace(ch))
			return ch;
		do {
			state.next(ch);
			ch = reader.read();
		} while(ch >= 0 && Character.isWhitespace((char) ch));
		return ch;
	}

	private boolean couldBeMuisType(char ch, boolean init) {
		// TODO this should be standardized somewhere
		if(ch >= 'A' && ch <= 'Z')
			return true;
		else if(ch >= 'a' && ch <= 'z')
			return true;
		else if(ch == '_')
			return true;
		if(init)
			return false;
		if(ch == '-')
			return true;
		return false;
	}

	private boolean couldBeGroupName(char ch, boolean init) {
		// TODO this should be standardized somewhere
		return couldBeMuisType(ch, init);
	}

	private boolean couldBeStyleDomain(char ch, boolean init) {
		return couldBeMuisType(ch, init);
	}

	void passLineComment(Reader reader, ParseState state) throws IOException, MuisParseException {
		int ch = reader.read();
		if(ch != '/')
			throw new MuisParseException("Illegal character '" + (char) ch + "' at " + state);
		state.next(ch);
		do {
			ch = reader.read();
			state.next(ch);
		} while(ch >= 0 && ch != '\n');
	}

	void parseGroupedStyle(DefaultStyleSheet styleSheet, Context context, Reader reader, ParseState state, MuisClassView classView,
		MuisMessageCenter messager) throws IOException, MuisParseException {
		int ch = reader.read();
		java.util.ArrayList<String> groupNames = new java.util.ArrayList<>();
		StringBuilder groupName = new StringBuilder();
		boolean hadColon = false;
		while(true) {
			groupName.setLength(0);
			while(couldBeGroupName((char) ch, groupName.length() == 0) || (ch == ':' && !hadColon)) {
				if(ch == ':')
					hadColon = true;
				groupName.append((char) ch);
				state.next(ch);
				ch = reader.read();
			}
			if(groupName.length() == 0)
				messager.error("Empty or invalid group name at " + state);
			else
				groupNames.add(groupName.toString());
			ch = pastWhiteSpace(ch, reader, state);
			if(ch == ',')
				ch = pastWhiteSpace(' ', reader, state);
			else
				break;
		}
		if(ch != ')')
			throw new MuisParseException("')' expected after group name \"" + groupName + "\" at " + state);
		ch = pastWhiteSpace(' ', reader, state);
		if(!parseNextStyleSet(styleSheet, context.and(groupNames.toArray(new String[groupNames.size()])), (char) ch, reader, state,
			classView, messager)) {
			StringBuilder errMsg = new StringBuilder("No style block after group declaration (");
			for(int g = 0; g < groupNames.size(); g++) {
				if(g > 0)
					errMsg.append(", ");
				errMsg.append(groupNames.get(g));
			}
			errMsg.append(") at ").append(state);
			throw new MuisParseException(errMsg.toString());
		}
	}

	void parseMultiTypedStyle(DefaultStyleSheet styleSheet, Context context, Reader reader, ParseState state, MuisClassView classView,
		MuisMessageCenter messager) throws IOException, MuisParseException {
		int ch = reader.read();
		java.util.ArrayList<String> typeNames = new java.util.ArrayList<>();
		StringBuilder typeName = new StringBuilder();
		boolean hadColon = false;
		while(true) {
			typeName.setLength(0);
			while(couldBeMuisType((char) ch, typeName.length() == 0) || (ch == ':' && !hadColon)) {
				if(ch == ':')
					hadColon = true;
				typeName.append((char) ch);
				state.next(ch);
				ch = reader.read();
			}
			if(typeName.length() == 0)
				messager.error("Empty or invalid type name at " + state);
			else
				typeNames.add(typeName.toString());
			ch = pastWhiteSpace(ch, reader, state);
			if(ch == ',')
				ch = pastWhiteSpace(' ', reader, state);
			else
				break;
		}
		if(ch != ']')
			throw new MuisParseException("']' expected after type name \"" + typeName + "\" at " + state);

		// Parse the types
		Class<? extends MuisElement> [] types = new Class[typeNames.size()];
		for(int i = 0; i < types.length; i++) {
			types[i] = parseType(typeNames.get(i), classView, messager, state);
			if(types[i] == null)
				types = prisms.util.ArrayUtils.remove(types, i);
		}
		// Ensure every type is a subtype of (and not the same as) at least one type in the current context
		for(int i = 0; i < types.length; i++) {
			boolean isSubtype = false;
			for(Class<?> ctxType : context.theTypes)
				if(ctxType != types[i] && ctxType.isAssignableFrom(types[i])) {
					isSubtype = true;
					break;
				}
			if(!isSubtype) {
				StringBuilder errMsg = new StringBuilder("Type " + typeNames.get(i) + " (resolved to " + types[i].getName()
					+ ") is not a sub type of any of the types in the current context (");
				for(int j = 0; j < context.theTypes.length; j++) {
					if(j > 0)
						errMsg.append(", ");
					errMsg.append(context.theTypes[j].getName());
				}
				errMsg.append(") at ").append(state);
				messager.warn(errMsg.toString());
			}
		}
		ch = pastWhiteSpace(' ', reader, state);
		if(!parseNextStyleSet(styleSheet, types.length == 0 ? null : context.and(types), (char) ch, reader, state, classView, messager)) {
			StringBuilder errMsg = new StringBuilder("No style block after type declaration [");
			for(int g = 0; g < typeNames.size(); g++) {
				if(g > 0)
					errMsg.append(", ");
				errMsg.append(typeNames.get(g));
			}
			errMsg.append("] at ").append(state);
			throw new MuisParseException(errMsg.toString());
		}
	}

	Class<? extends MuisElement> parseType(String typeName, MuisClassView classView, MuisMessageCenter messager, ParseState state)
		throws MuisParseException {
		int colon = typeName.indexOf(':');
		String ns = colon < 0 ? "" : typeName.substring(0, colon);
		if(colon >= 0)
			typeName = typeName.substring(colon + 1);
		MuisToolkit toolkit = classView.getToolkit(ns);
		if(toolkit == null) {
			messager.error("No toolkit defined for namespace \"" + ns + "\" near " + state);
			return null;
		}
		String className = toolkit.getMappedClass(typeName);
		if(className == null) {
			messager.error("No type mapped as \"" + typeName + "\" in toolkit " + toolkit.getName() + " (namespace \"" + ns + "\")");
			return null;
		}
		Class<?> ret;
		try {
			ret = toolkit.loadClass(className, MuisElement.class);
		} catch(MuisException e) {
			messager.error(e.getMessage() + " near " + state, e);
			return null;
		}
		return (Class<? extends MuisElement>) ret;
	}

	void parseTypedStyle(DefaultStyleSheet styleSheet, Context context, char initChar, Reader reader, ParseState state,
		MuisClassView classView, MuisMessageCenter messager) throws IOException, MuisParseException {
		StringBuilder typeName = new StringBuilder(initChar);
		int ch = reader.read();
		while(couldBeMuisType((char) ch, typeName.length() == 0)) {
			typeName.append((char) ch);
			state.next(initChar);
			ch = reader.read();
		}
		Class<? extends MuisElement> type = parseType(typeName.toString(), classView, messager, state);
		ch = pastWhiteSpace(ch, reader, state);
		if(!parseNextStyleSet(styleSheet, type == null ? null : context.and(type), (char) ch, reader, state, classView, messager))
			throw new MuisParseException("No style block after type declaration " + typeName + " at " + state);
	}

	void parseStatefulStyle(DefaultStyleSheet styleSheet, Context context, Reader reader, ParseState state, MuisClassView classView,
		MuisMessageCenter messager) throws IOException, MuisParseException {
		int ch = reader.read();
		java.util.ArrayList<String> stateNames = new java.util.ArrayList<>();
		StringBuilder stateName = new StringBuilder();
		while(true) {
			stateName.setLength(0);
			while(couldBeGroupName((char) ch, stateName.length() == 0)) {
				stateName.append((char) ch);
				state.next(ch);
				ch = reader.read();
			}
			if(stateName.length() == 0)
				messager.error("Empty or invalid state name at " + state);
			else
				stateNames.add(stateName.toString());
			ch = pastWhiteSpace(ch, reader, state);
			if(ch == '_')
				ch = pastWhiteSpace(' ', reader, state);
			else
				break;
		}
		ch = pastWhiteSpace(ch, reader, state);
		// TODO BIG DEAL!! How do we know the state priorities?? Probably need a state registry in the document or something.
		StateExpression expr = new StateExpression.Simple(new org.muis.core.mgr.MuisState(stateNames.get(0), 1));
		for(int i = 1; i < stateNames.size(); i++)
			expr = expr.or(new StateExpression.Simple(new org.muis.core.mgr.MuisState(stateNames.get(i), 1)));
		if(!parseNextStyleSet(styleSheet, context.and(expr), (char) ch, reader, state, classView, messager)) {
			StringBuilder errMsg = new StringBuilder("No style block after state declaration ");
			for(int g = 0; g < stateNames.size(); g++) {
				if(g > 0)
					errMsg.append("_");
				errMsg.append(stateNames.get(g));
			}
			errMsg.append(" at ").append(state);
			throw new MuisParseException(errMsg.toString());
		}
	}

	void parseStyleBlock(DefaultStyleSheet styleSheet, Context context, Reader reader, ParseState state, MuisClassView classView,
		MuisMessageCenter messager) throws IOException, MuisParseException {
		int ch = -1;
		while(parseNextStyleSet(styleSheet, context, ' ', reader, state, classView, messager)) {
			ch = reader.read();
			if(ch == '}')
				break;
		}
		if(ch != '}')
			throw new MuisParseException("'}' expected at " + state + " after style block");
	}

	void parseStyle(DefaultStyleSheet styleSheet, Context context, char initChar, Reader reader, ParseState state, MuisClassView classView,
		MuisMessageCenter messager) throws IOException, MuisParseException {
		StringBuilder name = new StringBuilder();
		int ch = initChar;
		boolean hadColon = false;
		do {
			if(ch == ':')
				hadColon = true;
			name.append((char) ch);
			state.next(ch);
			ch = reader.read();
		} while(couldBeStyleDomain((char) ch, false) || (ch == ':' && !hadColon));
		ch = pastWhiteSpace(ch, reader, state);
		String domainName = name.toString();
		if(ch == '=') {
			state.next(ch);
			parseStyleDomainSet(styleSheet, context, domainName, reader, state, classView, messager);
		}
		if(ch != '.')
			throw new MuisParseException("Style declarations must be in the form \"domain.attr=value\". See " + state);

		state.next(ch);
		ch = pastWhiteSpace(' ', reader, state);
		name.setLength(0);
		do {
			name.append((char) ch);
			state.next(ch);
			ch = reader.read();
		} while(couldBeStyleDomain((char) ch, false));
		ch = pastWhiteSpace(ch, reader, state);
		if(ch != '=')
			throw new MuisParseException("Style declarations must be in the form \"domain.attr=value\". See " + state);
		String attrName = name.toString();

		state.next(ch);
		ch = pastWhiteSpace(' ', reader, state);
		name.setLength(0);
		do {
			name.append((char) ch);
			state.next(ch);
			ch = reader.read();
		} while(ch >= 0 && !Character.isWhitespace((char) ch));
		String attrValue = name.toString();

		org.muis.core.style.StyleDomain domain = parseDomain(domainName, state, classView, messager);
		if(domain == null)
			return;

		StyleAttribute<?> styleAttr = null;
		for(StyleAttribute<?> attrib : domain)
			if(attrib.getName().equals(attrName)) {
				styleAttr = attrib;
				break;
			}

		if(styleAttr == null) {
			messager.error("No such attribute " + attrName + " in domain " + domain.getName() + " at " + state);
			return;
		}

		Object value;
		try {
			value = styleAttr.getType().parse(classView, attrValue);
		} catch(org.muis.core.MuisException e) {
			messager.warn("Value " + attrValue + " is not appropriate for style attribute " + attrName + " of domain " + domain.getName(),
				e);
			return;
		}
		if(styleAttr.getValidator() != null)
			try {
				((StyleAttribute<Object>) styleAttr).getValidator().assertValid(value);
			} catch(org.muis.core.MuisException e) {
				messager.warn(e.getMessage());
				return;
			}
		applyStyle(styleSheet, context, styleAttr, value, messager);
	}

	void parseStyleDomainSet(DefaultStyleSheet styleSheet, Context context, String domainName, Reader reader, ParseState state,
		MuisClassView classView, MuisMessageCenter messager) throws IOException, MuisParseException {
		int ch = pastWhiteSpace(' ', reader, state);
		if(ch != '{')
			throw new MuisParseException("'{' expected after \"" + domainName + "=\" at " + state);
		state.next(ch);
		ch = pastWhiteSpace(' ', reader, state);

		org.muis.core.style.StyleDomain domain = parseDomain(domainName, state, classView, messager);
		if(domain == null)
			return;
	}

	org.muis.core.style.StyleDomain parseDomain(String domainName, ParseState state, MuisClassView classView, MuisMessageCenter messager) {
		String ns;
		MuisToolkit toolkit;
		String domainClassName;
		if(domainName.indexOf(':') >= 0) {
			ns = domainName.substring(0, domainName.indexOf(':'));
			domainName = domainName.substring(ns.length() + 1);
			toolkit = classView.getToolkit(ns.length() == 0 ? null : ns);
			if(toolkit == null) {
				messager.error("No toolkit mapped to namespace \"" + ns + "\" at " + state);
				return null;
			}
			domainClassName = toolkit.getMappedClass(domainName);
			if(domainClassName == null) {
				messager.error("No style domain mapped to " + domainName + " in toolkit " + toolkit.getName() + " (ns=\"" + ns + "\") at "
					+ state);
				return null;
			}
		} else {
			ns = null;
			toolkit = null;
			domainClassName = null;
			MuisToolkit tk = classView.getToolkit(null);
			if(tk != null) {
				domainClassName = tk.getMappedClass(domainName);
				if(domainClassName != null) {
					ns = "";
					toolkit = tk;
				}
			}

			if(domainClassName == null) {
				for(String namespace : classView.getMappedNamespaces()) {
					tk = classView.getToolkit(namespace);
					domainClassName = tk.getMappedClass(domainName);
					if(domainClassName != null) {
						ns = namespace;
						toolkit = tk;
						break;
					}
				}
			}
			if(domainClassName == null) {
				messager.error("No style domain mapped to " + domainName + " in any scoped toolkit at " + state);
				return null;
			}
		}

		Class<? extends org.muis.core.style.StyleDomain> domainClass;
		try {
			domainClass = toolkit.loadClass(domainClassName, org.muis.core.style.StyleDomain.class);
		} catch(MuisException e) {
			messager.error("Could not load domain class " + domainClassName + " from toolkit " + toolkit.getName() + " (ns=\"" + ns
				+ "\") at " + state, e);
			return null;
		}
		org.muis.core.style.StyleDomain domain;
		try {
			domain = (org.muis.core.style.StyleDomain) domainClass.getMethod("getDomainInstance", new Class[0]).invoke(null, new Object[0]);
		} catch(Exception e) {
			messager.error("Could not get domain instance at " + state, e);
			return null;
		}
		return domain;
	}

	void applyStyle(DefaultStyleSheet styleSheet, Context context, StyleAttribute<?> attr, Object value, MuisMessageCenter messager) {
		if(context == null)
			return;
		for(String group : context.theGroupNames) {
			for(Class<? extends MuisElement> type : context.theTypes) {
				try {
					styleSheet.set((StyleAttribute<Object>) attr, group, type, context.theState, value);
				} catch(IllegalArgumentException e) {
					messager.error(e.getMessage(), e);
				}
			}
		}
	}
}
