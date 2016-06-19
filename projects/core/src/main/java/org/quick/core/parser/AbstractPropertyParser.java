package org.quick.core.parser;

import java.util.ArrayList;
import java.util.List;

import org.observe.ObservableValue;
import org.quick.core.QuickClassView;
import org.quick.core.QuickEnvironment;
import org.quick.core.QuickParseEnv;
import org.quick.core.prop.DefaultExpressionContext;
import org.quick.core.prop.QuickProperty;

public abstract class AbstractPropertyParser implements QuickPropertyParser {
	public static final String DEFAULT_PARSE_DIRECTIVE = "$";
	public static final String SELF_PARSE_DIRECTIVE = "#";
	private final QuickEnvironment theEnvironment;

	public AbstractPropertyParser(QuickEnvironment env) {
		theEnvironment = env;
	}

	@Override
	public QuickEnvironment getEnvironment() {
		return theEnvironment;
	}

	@Override
	public Runnable parseAction(QuickParseEnv parseEnv, String value) throws QuickParseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> ObservableValue<T> parseProperty(QuickProperty<T> property, QuickParseEnv parseEnv, String value)
		throws QuickParseException {
		/* Some thoughts on parsing
		 *
		 * property may be null.  Then just parse in the context without regard to type.
		 *
		 * If the property is self-parsing by default, look for ${...} sections in the text and replace those with string representations
		 * of model-parsed values before self-parsing the text.
		 *
		 * If the property is model-parsing by default, use self-parser if the value begins with a "#\w+" pattern.  Otherwise, look for
		 * #{...} sections in the text and replace them with string representations of self-parsed values before model-parsing the text.
		 *
		 * Ideally, the string representations would be supplied by a QuickPropertyType where it makes sense.
		 * Definitely need to have some control over the strings (e.g. replacing colors in an attribute should be named where possible).
		 */
		if (property != null && property.getType().isSelfParsingByDefault()) {
			return parseBy(property, parseEnv, value, false);
		} else {
			return parseBy(property, parseEnv, value, true);
		}
	}

	private <T> ObservableValue<T> parseBy(QuickProperty<T> property, QuickParseEnv parseEnv, String value, boolean parseByDefault)
		throws QuickParseException {
		List<String> text = new ArrayList<>();
		List<ObservableValue<?>> inserts = new ArrayList<>();
		int start = 0;
		int directiveIdx = findNextDirective(value, 0);
		while (directiveIdx >= 0) {
			if (property == null)
				throw new QuickParseException("No directives accepted for property-less parsing (e.g. models)");
			text.add(value.substring(start, directiveIdx));
			String contents = getDirectiveContents(value, directiveIdx);
			ObservableValue<?> contentValue = parseBy(property, parseEnv, contents, isDefaultDirective(value, directiveIdx));
			inserts.add(contentValue);
			start += contents.length();
			directiveIdx = findNextDirective(value, start);
		}
		QuickParseEnv internalParseEnv;
		if (property != null) {
			QuickClassView cv = new org.quick.core.QuickClassView(theEnvironment, parseEnv.cv(), null);
			// TODO Add the property's toolkit and the property type's toolkit to the class view
			DefaultExpressionContext.Builder ctx = DefaultExpressionContext.build().withParent(parseEnv.getContext());
			// TODO Add property's and property type's variables, functions, etc. into the context
			if (property != null && property.getType().acceptsRefs()) {
				// TODO Add the inserts into the context as variables of the form ?<hex>
			}
			internalParseEnv = new SimpleParseEnv(cv, parseEnv.msg(), ctx.build());
		} else
			internalParseEnv = parseEnv;
		if (inserts.isEmpty()) {
			return parseValue(internalParseEnv, value);
		} else if (property != null && !property.getType().acceptsRefs()) {
			return new StringBuildingReferenceValue<>(property, internalParseEnv, text, inserts, parseByDefault);
		} else {
			return new ValueSubstitutingReferenceValue<>(property, internalParseEnv, text, inserts, parseByDefault);
		}
	}

	protected abstract <T> ObservableValue<T> parseValue(QuickParseEnv parseEnv, String value) throws QuickParseException;

	private static int findNextDirective(String value, int start) {
		// TODO
	}

	private static String getDirectiveContents(String value, int position) {
		// TODO
	}

	private static boolean isDefaultDirective(String value, int position) {
		// TODO
	}

	/**
	 * Combines text and directive-parsed references into a string and parses the value
	 *
	 * @param <T> The type of the value
	 */
	private class StringBuildingReferenceValue<T> extends ObservableValue.ComposedObservableValue<T> {
		// TODO Need some control over the toString here (e.g. replacing colors in an attribute should be named where possible)
		StringBuildingReferenceValue(QuickProperty<T> property, QuickParseEnv parseEnv, List<String> text, List<ObservableValue<?>> inserts,
			boolean parseByDefault) {
			// TODO Auto-generated constructor stub
		}
	}

	/**
	 * Replaces directive-parsed references in text with placeholders whose values are supplied by the context
	 *
	 * @param <T> The type of the value
	 */
	private class ValueSubstitutingReferenceValue<T> extends ObservableValue.ComposedObservableValue<T> {
		public ValueSubstitutingReferenceValue(QuickProperty<T> property, QuickParseEnv parseEnv, List<String> text,
			List<ObservableValue<?>> inserts, boolean parseByDefault) {
			// TODO Auto-generated constructor stub
		}
	}
}
