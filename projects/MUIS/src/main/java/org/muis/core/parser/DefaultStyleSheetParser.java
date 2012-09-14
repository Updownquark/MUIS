package org.muis.core.parser;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import org.muis.core.MuisElement;
import org.muis.core.MuisException;
import org.muis.core.MuisToolkit;
import org.muis.core.style.DefaultStyleSheet;
import org.muis.core.style.StateExpression;
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
	public StyleSheet parseStyleSheet(Reader reader, Map<String, MuisToolkit> toolkits) throws IOException, MuisParseException {
		DefaultStyleSheet ret = new DefaultStyleSheet();

		ParseState state = new ParseState();
		Context root = new Context();
		parseTillDone(ret, root, reader, state, toolkits);
		return ret;
	}

	void parseTillDone(DefaultStyleSheet styleSheet, Context context, Reader reader, ParseState state, Map<String, MuisToolkit> toolkits)
		throws IOException, MuisParseException {
		while(parseNextStyleSet(styleSheet, context, ' ', reader, state, toolkits));
	}

	boolean parseNextStyleSet(DefaultStyleSheet styleSheet, Context context, char initChar, Reader reader, ParseState state,
		Map<String, MuisToolkit> toolkits) throws IOException, MuisParseException {
		int ch = pastWhiteSpace(initChar, reader, state);
		if(ch < 0)
			return false;
		if(ch == '(')
			parseGroupedStyle(styleSheet, context, reader, state, toolkits);
		else if(ch == '[')
			parseMultiTypedStyle(styleSheet, context, reader, state, toolkits);
		else if(ch == '.')
			parseStatefulStyle(styleSheet, context, reader, state, toolkits);
		else if(ch == '{')
			parseStyleBlock(styleSheet, context, reader, state, toolkits);
		else if(couldBeStyleDomain((char) ch, true))
			parseStyle(styleSheet, context, (char) ch, reader, state, toolkits);
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
		else if(ch >= 'a' || ch <= 'z')
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

	void parseGroupedStyle(DefaultStyleSheet styleSheet, Context context, Reader reader, ParseState state, Map<String, MuisToolkit> toolkits)
		throws IOException, MuisParseException {
		int ch = reader.read();
		java.util.ArrayList<String> groupNames = new java.util.ArrayList<>();
		StringBuilder groupName = new StringBuilder();
		while(true) {
			groupName.setLength(0);
			while(couldBeGroupName((char) ch, groupName.length() == 0)) {
				groupName.append((char) ch);
				state.next(ch);
				ch = reader.read();
			}
			if(groupName.length() == 0)
				throw new MuisParseException("Empty or invalid group name at " + state);
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
			toolkits)) {
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

	void parseMultiTypedStyle(DefaultStyleSheet styleSheet, Context context, Reader reader, ParseState state,
		Map<String, MuisToolkit> toolkits) throws IOException, MuisParseException {
		int ch = reader.read();
		java.util.ArrayList<String> typeNames = new java.util.ArrayList<>();
		StringBuilder typeName = new StringBuilder();
		while(true) {
			typeName.setLength(0);
			while(couldBeMuisType((char) ch, typeName.length() == 0)) {
				typeName.append((char) ch);
				state.next(ch);
				ch = reader.read();
			}
			if(typeName.length() == 0)
				throw new MuisParseException("Empty or invalid type name at " + state);
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
			types[i] = parseType(typeNames.get(i), toolkits, state);
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
				throw new MuisParseException(errMsg.toString());
			}
		}
		ch = pastWhiteSpace(' ', reader, state);
		if(!parseNextStyleSet(styleSheet, context.and(types), (char) ch, reader, state, toolkits)) {
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

	Class<? extends MuisElement> parseType(String typeName, Map<String, MuisToolkit> toolkits, ParseState state) throws MuisParseException {
		int colon = typeName.indexOf(':');
		String ns = colon < 0 ? "" : typeName.substring(0, colon);
		if(colon >= 0)
			typeName = typeName.substring(colon + 1);
		MuisToolkit toolkit = toolkits.get(ns);
		if(toolkit == null)
			throw new MuisParseException("No toolkit defined for namespace \"" + ns + "\" near " + state);
		String className = toolkit.getMappedClass(typeName);
		if(className == null)
			throw new MuisParseException("No type mapped as \"" + typeName + "\" in toolkit " + toolkit.getName() + " (namespace \"" + ns
				+ "\")");
		Class<?> ret;
		try {
			ret = toolkit.loadClass(className, MuisElement.class);
		} catch(MuisException e) {
			throw new MuisParseException(e.getMessage() + " near " + state, e);
		}
		return (Class<? extends MuisElement>) ret;
	}

	void parseTypedStyle(DefaultStyleSheet styleSheet, Context context, char initChar, Reader reader, ParseState state,
		Map<String, MuisToolkit> toolkits) throws IOException, MuisParseException {
		StringBuilder typeName = new StringBuilder(initChar);
		int ch = reader.read();
		while(couldBeMuisType((char) ch, typeName.length() == 0)) {
			typeName.append((char) ch);
			state.next(initChar);
			ch = reader.read();
		}
		Class<? extends MuisElement> type = parseType(typeName.toString(), toolkits, state);
		ch = pastWhiteSpace(ch, reader, state);
		if(!parseNextStyleSet(styleSheet, context.and(type), (char) ch, reader, state, toolkits))
			throw new MuisParseException("No style block after type declaration " + typeName + " at " + state);
	}

	void parseStatefulStyle(DefaultStyleSheet styleSheet, Context context, Reader reader, ParseState state,
		Map<String, MuisToolkit> toolkits) throws IOException, MuisParseException {
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
				throw new MuisParseException("Empty or invalid state name at " + state);
			stateNames.add(stateName.toString());
			ch = pastWhiteSpace(ch, reader, state);
			if(ch == '_')
				ch = pastWhiteSpace(' ', reader, state);
			else
				break;
		}
		ch = pastWhiteSpace(' ', reader, state);
		// TODO BIG DEAL!! How do we know the state priorities?? Probably need a state registry in the document or something.
		StateExpression expr = new StateExpression.Simple(new org.muis.core.mgr.MuisState(stateNames.get(0), 1));
		for(int i = 1; i < stateNames.size(); i++)
			expr = expr.or(new StateExpression.Simple(new org.muis.core.mgr.MuisState(stateNames.get(i), 1)));
		if(!parseNextStyleSet(styleSheet, context.and(expr), (char) ch, reader, state, toolkits)) {
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

	void parseStyleBlock(DefaultStyleSheet styleSheet, Context context, Reader reader, ParseState state, Map<String, MuisToolkit> toolkits)
		throws IOException, MuisParseException {
		int ch = -1;
		while(parseNextStyleSet(styleSheet, context, ' ', reader, state, toolkits)) {
			ch = reader.read();
			if(ch == '}')
				break;
		}
		if(ch != '}')
			throw new MuisParseException("'}' expected after style block at " + state);
	}

	void parseStyle(DefaultStyleSheet styleSheet, Context context, char ch, Reader reader, ParseState state,
		Map<String, MuisToolkit> toolkits) throws IOException, MuisParseException {
		// TODO
	}
}
