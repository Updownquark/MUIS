package org.quick.base.model;

import java.awt.Color;
import java.util.function.Function;

import org.quick.core.QuickException;
import org.quick.core.style.Colors;

import com.google.common.reflect.TypeToken;

/** A utility class containing standard formats */
public class Formats {
	/** Formats objects by their {@link Object#toString()} methods. Does not support parsing. */
	public static final QuickFormatter<Object> def = new SimpleFormatter<Object>() {
		@Override
		public TypeToken<Object> getFormatType() {
			return TypeToken.of(Object.class);
		}

		@Override
		public TypeToken<?> getParseType() {
			return null;
		}

		@Override
		public String format(Object value) {
			return String.valueOf(value);
		}

		@Override
		public Object parse(String text) throws QuickParseException {
			throw new QuickParseException("The default formatter does not support parsing", -1, -1);
		}

		@Override
		public String toString() {
			return "formats.def";
		}
	};

	/** A function that is a pass-through for non-null values and returns the default formatter for null values */
	public static final Function<QuickFormatter<?>, QuickFormatter<?>> defNullCatch = (QuickFormatter<?> f) -> {
		return f != null ? f : def;
	};

	/** Simple formatter for strings */
	public static final QuickFormatter<String> string = new SimpleFormatter<String>() {
		@Override
		public TypeToken<String> getFormatType() {
			return TypeToken.of(String.class);
		}

		@Override
		public TypeToken<String> getParseType() {
			return TypeToken.of(String.class);
		}

		@Override
		public String format(String value) {
			return value;
		}

		@Override
		public String parse(String text) throws QuickParseException {
			return text;
		}

		@Override
		public String toString() {
			return "formats.string";
		}
	};

	/** Formats integers (type long) */
	public static final QuickFormatter<Number> number = new SimpleFormatter<Number>() {
		@Override
		public TypeToken<Number> getFormatType() {
			return TypeToken.of(Number.class);
		}

		@Override
		public TypeToken<Number> getParseType() {
			return TypeToken.of(Number.class);
		}

		@Override
		public String format(Number value) {
			return value.toString();
		}

		@Override
		public Number parse(String text) throws QuickParseException {
			try {
				if (text.indexOf('.') >= 0)
					return Double.valueOf(text);
				else
					return Long.valueOf(text);
			} catch(NumberFormatException e) {
				throw new QuickParseException(e, -1, -1);
			}
		}

		@Override
		public String toString() {
			return "formats.number";
		}
	};

	/** Formats integers */
	public static final AdjustableFormatter<Integer> integer = new SimpleFormatter.SimpleAdjustableFormatter<Integer>() {
		@Override
		public TypeToken<Integer> getFormatType() {
			return TypeToken.of(Integer.class);
		}

		@Override
		public TypeToken<Integer> getParseType() {
			return TypeToken.of(Integer.class);
		}

		@Override
		public String format(Integer value) {
			return value.toString();
		}

		@Override
		public Integer parse(String text) throws QuickParseException {
			try {
				return Integer.valueOf(text);
			} catch (NumberFormatException e) {
				throw new QuickParseException(e, -1, -1);
			}
		}

		@Override
		public Integer increment(Integer value) {
			if (value.intValue() == Integer.MAX_VALUE)
				throw new IllegalStateException(isIncrementEnabled(value));
			else
				return value + 1;
		}

		@Override
		public String isIncrementEnabled(Integer value) {
			if (value.intValue() == Integer.MAX_VALUE)
				return value + " is the maximum integer value";
			else
				return null;
		}

		@Override
		public Integer decrement(Integer value) {
			if (value.intValue() == Integer.MIN_VALUE)
				throw new IllegalStateException(isDecrementEnabled(value));
			else
				return value - 1;
		}

		@Override
		public String isDecrementEnabled(Integer value) {
			if (value.intValue() == Integer.MIN_VALUE)
				return value + " is the minimum integer value";
			else
				return null;
		}

		@Override
		public String toString() {
			return "formats.integer";
		}
	};

	/** Formats and parses colors using the {@link Colors} class */
	public static final QuickFormatter<Color> color = new SimpleFormatter<Color>() {
		@Override
		public TypeToken<Color> getFormatType() {
			return TypeToken.of(Color.class);
		}

		@Override
		public TypeToken<Color> getParseType() {
			return TypeToken.of(Color.class);
		}

		@Override
		public String format(Color value) {
			return value == null ? "" : Colors.toString(value);
		}

		@Override
		public Color parse(String text) throws QuickParseException {
			Color ret;
			try {
				ret = Colors.parseColor(text);
			} catch(QuickException e) {
				throw new QuickParseException(e.getMessage(), e, -1, -1);
			}
			return ret;
		}

		@Override
		public String toString() {
			return "formats.color";
		}
	};
}
