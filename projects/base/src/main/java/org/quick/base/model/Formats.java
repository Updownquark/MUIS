package org.quick.base.model;

import java.awt.Color;
import java.util.function.Function;

import org.quick.core.QuickException;
import org.quick.core.model.QuickDocumentModel;
import org.quick.core.model.MutableDocumentModel;
import org.quick.core.style.Colors;

/** A utility class containing standard formats */
public class Formats {
	/** Formats objects by their {@link Object#toString()} methods. Does not support parsing. */
	public static final QuickFormatter<Object> def = new QuickFormatter<Object>() {
		@Override
		public void append(Object value, MutableDocumentModel doc) {
			doc.append(String.valueOf(value));
		}

		@Override
		public Object parse(QuickDocumentModel doc) throws QuickParseException {
			throw new QuickParseException("The default formatter does not support parsing", -1, -1);
		}
	};

	/** A function that is a pass-through for non-null values and returns the default formatter for null values */
	public static final Function<QuickFormatter<?>, QuickFormatter<?>> defNullCatch = (QuickFormatter<?> f) -> {
		return f != null ? f : def;
	};

	/** Simple formatter for strings */
	public static final QuickFormatter<String> string = new QuickFormatter<String>() {
		@Override
		public void append(String value, MutableDocumentModel doc) {
			if(value != null)
				doc.append(value);
		}

		@Override
		public String parse(QuickDocumentModel doc) throws QuickParseException {
			return doc.toString();
		}
	};

	/** Formats integers (type long) */
	public static final QuickFormatter<Number> integer = new QuickFormatter<Number>() {
		@Override
		public void append(Number value, MutableDocumentModel doc) {
			if(value != null)
				doc.append(value.toString());
		}

		@Override
		public Long parse(QuickDocumentModel doc) throws QuickParseException {
			try {
				return Long.parseLong(doc.toString());
			} catch(NumberFormatException e) {
				throw new QuickParseException(e, -1, -1);
			}
		}
	};

	/** Formats and parses colors using the {@link Colors} class */
	public static final QuickFormatter<Color> color = new QuickFormatter<Color>() {
		@Override
		public void append(Color value, MutableDocumentModel doc) {
			if(value != null)
				doc.append(Colors.toString(value));
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
	};
}
