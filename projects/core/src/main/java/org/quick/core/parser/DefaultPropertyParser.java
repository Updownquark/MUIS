package org.quick.core.parser;

import org.observe.ObservableValue;
import org.quick.core.QuickEnvironment;
import org.quick.core.prop.ExpressionContext;
import org.quick.core.prop.QuickProperty;

public class DefaultPropertyParser implements QuickPropertyParser {
	private final QuickEnvironment theEnvironment;

	public DefaultPropertyParser(QuickEnvironment env) {
		theEnvironment = env;
	}

	@Override
	public QuickEnvironment getEnvironment() {
		return theEnvironment;
	}

	@Override
	public Runnable parseAction(ExpressionContext ctx, String value) throws QuickParseException {
	}

	@Override
	public <T> ObservableValue<T> parseProperty(QuickProperty<T> property, ExpressionContext ctx, String value) throws QuickParseException {
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
		if (property == null) {
		} else if (property.getType().isSelfParsingByDefault()) {
		} else {
		}
	}
}
