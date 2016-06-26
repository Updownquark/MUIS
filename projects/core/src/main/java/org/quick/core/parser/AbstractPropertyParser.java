package org.quick.core.parser;

import java.util.ArrayList;
import java.util.List;

import org.observe.ObservableAction;
import org.observe.ObservableValue;
import org.quick.core.QuickClassView;
import org.quick.core.QuickEnvironment;
import org.quick.core.QuickException;
import org.quick.core.QuickParseEnv;
import org.quick.core.prop.DefaultExpressionContext;
import org.quick.core.prop.QuickProperty;
import org.quick.core.prop.QuickPropertyType;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

/** A partial implementation of the property parser. Handles directives and property self-parsing. */
public abstract class AbstractPropertyParser implements QuickPropertyParser {
	/** Represents a directive inside a property value */
	protected static class Directive {
		/** The location of the directive in the property value text */
		public final int start;
		/** The length of the text representing the directive in the property value text */
		public final int length;
		/** The type of the directive */
		public final String type;
		/** The contents of the directive to parse */
		public final String contents;

		/**
		 * @param start The location of the directive in the property value text
		 * @param length The length of the text representing the directive in the property value text
		 * @param type The type of the directive
		 * @param contents The contents of the directive to parse
		 */
		@SuppressWarnings("hiding")
		public Directive(int start, int length, String type, String contents) {
			this.start = start;
			this.length = length;
			this.type = type;
			this.contents = contents;
		}
	}
	/** The type of directive representing parsing by the "default" method ({@link #parseDefaultValue(QuickParseEnv, String, boolean)} */
	public static final String DEFAULT_PARSE_DIRECTIVE = "$";
	/** The type of directive representing parsing by the property's self-parser ({@link QuickPropertyType#getSelfParser()} */
	public static final String SELF_PARSE_DIRECTIVE = "#";
	private final QuickEnvironment theEnvironment;

	/** @param env The Quick environment for this parser */
	public AbstractPropertyParser(QuickEnvironment env) {
		theEnvironment = env;
	}

	@Override
	public QuickEnvironment getEnvironment() {
		return theEnvironment;
	}

	@Override
	public ObservableAction parseAction(QuickParseEnv parseEnv, String value) throws QuickParseException {
		return parseProperty((QuickProperty<ObservableAction>) null, parseEnv, value, true).get();
	}

	@Override
	public <T> ObservableValue<T> parseProperty(QuickProperty<T> property, QuickParseEnv parseEnv, String value)
		throws QuickParseException {
		return parseProperty(property, parseEnv, value, isActionType(property.getType().getType()));
	}

	private boolean isActionType(TypeToken<?> type) {
		return TypeToken.of(ObservableAction.class).isAssignableFrom(type);
	}

	private <T> ObservableValue<T> parseProperty(QuickProperty<T> property, QuickParseEnv parseEnv, String value, boolean action)
		throws QuickParseException {
		checkValueDirectives(value);

		ObservableValue<?> parsedValue;
		if (property != null && property.getType().isSelfParsingByDefault())
			parsedValue = parseByType(property, parseEnv, value, SELF_PARSE_DIRECTIVE, action);
		else
			parsedValue = parseByType(property, parseEnv, value, DEFAULT_PARSE_DIRECTIVE, action);
		if (action && isActionType(parsedValue.getType()))
			throw new QuickParseException("Parser error: " + parsedValue.getType() + " is not an action type");
		if (property != null && !property.getType().canAccept(parsedValue.getType()))
			throw new QuickParseException("Type of parsed value for property " + property + " is unacceptable: " + parsedValue.getType());
		return parsedValue.mapV(property.getType().getType(), v -> {
			try {
				return property.getType().cast((TypeToken<Object>) parsedValue.getType(), v);
			} catch (QuickException e) {
				parseEnv.msg().error("Could not convert property value " + v + " to acceptable value for property " + property, e);
				return null;// I guess?
			}
		});
	}

	private <T> ObservableValue<?> parseByType(QuickProperty<T> property, QuickParseEnv parseEnv, String value, String type, boolean action)
		throws QuickParseException {
		List<String> text = new ArrayList<>();
		List<ObservableValue<?>> inserts = new ArrayList<>();
		int start = 0;
		Directive directive = parseNextDirective(value, start);
		while (directive != null) {
			text.add(value.substring(start, directive.start));
			ObservableValue<?> contentValue = parseByType(property, parseEnv, directive.contents, directive.type, action);
			inserts.add(contentValue);
			start = directive.start + directive.length;
			directive = parseNextDirective(value, start);
		}
		text.add(value.substring(start));
		DefaultExpressionContext.Builder ctx = DefaultExpressionContext.build().withParent(parseEnv.getContext());
		QuickClassView cv;
		if (property != null) {
			cv = new org.quick.core.QuickClassView(theEnvironment, parseEnv.cv(), null);
			// TODO Add the property's toolkit and the property type's toolkit to the class view
			// TODO Add property's and property type's variables, functions, etc. into the context
		} else
			cv = parseEnv.cv();
		// Add reference replacement variables
		List<String> replacements = new ArrayList<>();
		for (int i = 0; i < inserts.size(); i++) {
			String replacement = getReferenceReplacement(type, property, i);
			replacements.add(replacement);
			if (replacement != null) {
				ctx.withValue(replacement, inserts.get(i));
			}
		}
		QuickParseEnv internalParseEnv = new SimpleParseEnv(cv, parseEnv.msg(), ctx.build());

		ObservableValue<?> parsedValue;
		if (inserts.isEmpty()) {
			parsedValue = parseValue(type, property, internalParseEnv, value, action);
		} else if (replacements.stream().anyMatch(r -> r != null)) {
			StringBuilder builtText = new StringBuilder(text.get(0));
			for (int i = 0; i < inserts.size(); i++)
				builtText.append(replacements.get(i)).append(text.get(i + 1));
			parsedValue = parseValue(type, property, internalParseEnv, builtText.toString(), action);
		} else {
			parsedValue = ObservableValue.flatten(new StringBuildingReferenceValue<>(property, internalParseEnv, text, inserts, type));
		}
		return parsedValue;
	}

	/**
	 * Ensures a property value has valid directives
	 *
	 * @param value The property value text
	 * @throws QuickParseException If the text contains an invalid directive or one that this parser does not recognize
	 */
	protected void checkValueDirectives(String value) throws QuickParseException {
		int depth = 0;
		for (int i = 0; i < value.length(); i++) {
			switch (value.charAt(i)) {
			case '{':
				if (i == 0)
					throw new QuickParseException("Invalid directive: " + value.charAt(i) + " at position " + i);
				switch (value.charAt(i - 1)) {
				case '$':
				case '#':
					throw new QuickParseException(
						"Invalid directive: " + value.charAt(i - 1) + value.charAt(i) + " at position " + (i - 1));
				}
				depth++;
				break;
			case '}':
				if (depth == 0)
					throw new QuickParseException("Unmatched " + value.charAt(i) + " at position " + i);
				depth--;
			}
		}
		if (depth > 0)
			throw new QuickParseException("Unmatched { ");
	}

	/**
	 * Parses the next directive out of a property value text
	 *
	 * @param value The value text for a property
	 * @param start The place to start the search from
	 * @return The next directive in the text after the given starting point
	 */
	protected Directive parseNextDirective(String value, int start) {
		int braceIdx = value.indexOf('{', start);
		if (braceIdx < 0)
			return null;
		int depth = 1;
		int end;
		for (end = start + 1; (depth == 1 && value.charAt(end) != '}'); end++) {
			if (value.charAt(end) == '{')
				depth++;
		}
		return new Directive(braceIdx - 1, end - braceIdx + 1, "" + value.charAt(braceIdx - 1), value.substring(braceIdx + 1, end));
	}

	/**
	 * @param directiveType The type of the directive to replace the reference for
	 * @param property The property to replace the references in
	 * @param index The index of the reference value to generate replacement text for
	 * @return The text to use to replace the reference value in the property value to be parsed
	 */
	protected String getReferenceReplacement(String directiveType, QuickProperty<?> property, int index) {
		switch (directiveType) {
		case DEFAULT_PARSE_DIRECTIVE:
			return "?<" + index + ">";
		case SELF_PARSE_DIRECTIVE:
			if (property.getType().getReferenceReplacementGenerator() != null)
				return property.getType().getReferenceReplacementGenerator().apply(index);
			return null;
		}
		throw new IllegalStateException("Unrecognized directive type: " + directiveType);
	}

	/**
	 * Parses a property value
	 *
	 * @param directiveType The type of the directive to determine how the value is parsed
	 * @param property The property to parse the value for
	 * @param parseEnv The parse environment to use for parsing
	 * @param value The text to parse
	 * @param action Whether the parsed value should be an action
	 * @return The value parsed from the text
	 * @throws QuickParseException If an error occurred parsing the value
	 */
	protected ObservableValue<?> parseValue(String directiveType, QuickProperty<?> property, QuickParseEnv parseEnv, String value,
		boolean action)
		throws QuickParseException {
		switch (directiveType) {
		case DEFAULT_PARSE_DIRECTIVE:
			return parseDefaultValue(parseEnv, value, action);
		case SELF_PARSE_DIRECTIVE:
			return property.getType().getSelfParser().parse(this, parseEnv, value);
		default:
			throw new QuickParseException("Unrecognized directive type: " + directiveType);
		}
	}

	/**
	 * Does the parsing work by the default method
	 *
	 * @param parseEnv The parse environment to use for parsing
	 * @param value The text to parse
	 * @param action Whether the parsed value should be an action
	 * @return The parsed value
	 * @throws QuickParseException If an error occurs parsing the error
	 */
	protected abstract ObservableValue<?> parseDefaultValue(QuickParseEnv parseEnv, String value, boolean action)
		throws QuickParseException;

	/**
	 * Combines text and directive-parsed references into a string and parses the value
	 *
	 * @param <T> The type of the value
	 */
	private class StringBuildingReferenceValue<T> extends ObservableValue.ComposedObservableValue<ObservableValue<T>> {
		private final QuickProperty<T> theProperty;
		private final QuickParseEnv theParseEnv;
		private final String theDirectiveType;
		private final List<String> theText;

		StringBuildingReferenceValue(QuickProperty<T> property, QuickParseEnv parseEnv, List<String> text, List<ObservableValue<?>> inserts,
			String directiveType) {
			super(new TypeToken<ObservableValue<T>>() {}.where(new TypeParameter<T>() {}, property.getType().getType()), null, true,
				inserts.toArray(new ObservableValue[inserts.size()]));
			theProperty = property;
			theParseEnv = parseEnv;
			theDirectiveType = directiveType;
			theText = text;
		}

		@Override
		protected ObservableValue<T> combine(Object[] args) {
			StringBuilder text = new StringBuilder(theText.get(0));
			for (int i = 0; i < args.length; i++) {
				// TODO is there an elegant way to get more control of the way model values are stringified here?
				text.append(args[i]).append(theText.get(i + 1));
			}
			try {
				return theProperty.getType().getSelfParser().parse(AbstractPropertyParser.this, theParseEnv, text.toString());
			} catch (QuickParseException e) {
				theParseEnv.msg().error("Could not parse value " + text + " by directive " + theDirectiveType, e);
				return null; // TODO I guess? Can't think how else to populate a value if parsing fails
			}
		}
	}
}
