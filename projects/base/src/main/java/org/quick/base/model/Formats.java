package org.quick.base.model;

import java.awt.Color;
import java.util.function.Function;

import org.quick.core.QuickException;
import org.quick.core.model.MutableDocumentModel;
import org.quick.core.model.QuickDocumentModel;
import org.quick.core.style.Colors;

import com.google.common.reflect.TypeToken;

/** A utility class containing standard formats */
public class Formats {
	/** Formats objects by their {@link Object#toString()} methods. Does not support parsing. */
	public static final QuickFormatter<Object> def = new QuickFormatter<Object>() {
		@Override
		public TypeToken<Object> getFormatType() {
			return TypeToken.of(Object.class);
		}

		@Override
		public void append(Object value, MutableDocumentModel doc) {
			doc.append(String.valueOf(value));
		}

		@Override
		public TypeToken<?> getParseType() {
			return null;
		}

		@Override
		public Object parse(QuickDocumentModel doc) throws QuickParseException {
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
	public static final QuickFormatter<String> string = new QuickFormatter<String>() {
		@Override
		public TypeToken<String> getFormatType() {
			return TypeToken.of(String.class);
		}

		@Override
		public void append(String value, MutableDocumentModel doc) {
			if(value != null)
				doc.append(value);
		}

		@Override
		public TypeToken<String> getParseType() {
			return TypeToken.of(String.class);
		}

		@Override
		public String parse(QuickDocumentModel doc) throws QuickParseException {
			return doc.toString();
		}

		@Override
		public String toString() {
			return "formats.string";
		}
	};

	/** Formats integers (type long) */
	public static final QuickFormatter<Number> integer = new QuickFormatter<Number>() {
		@Override
		public TypeToken<Number> getFormatType() {
			return TypeToken.of(Number.class);
		}

		@Override
		public void append(Number value, MutableDocumentModel doc) {
			if(value != null)
				doc.append(value.toString());
		}

		@Override
		public TypeToken<Number> getParseType() {
			return TypeToken.of(Number.class);
		}

		@Override
		public Number parse(QuickDocumentModel doc) throws QuickParseException {
			try {
				String str = doc.toString();
				if (str.indexOf('.') >= 0)
					return Double.valueOf(str);
				else
					return Long.valueOf(str);
			} catch(NumberFormatException e) {
				throw new QuickParseException(e, -1, -1);
			}
		}

		@Override
		public String toString() {
			return "formats.integer";
		}
	};

	/** Formats and parses colors using the {@link Colors} class */
	public static final QuickFormatter<Color> color = new QuickFormatter<Color>() {
		@Override
		public TypeToken<Color> getFormatType() {
			return TypeToken.of(Color.class);
		}

		@Override
		public void append(Color value, MutableDocumentModel doc) {
			if(value != null)
				doc.append(Colors.toString(value));
		}

		@Override
		public TypeToken<Color> getParseType() {
			return TypeToken.of(Color.class);
		}

		@Override
		public Color parse(QuickDocumentModel doc) throws QuickParseException {
			Color ret;
			try {
				ret = Colors.parseColor(doc.toString());
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
