package org.muis.base.model;

import java.awt.Color;
import java.util.function.Function;

import org.muis.core.MuisException;
import org.muis.core.model.MuisDocumentModel;
import org.muis.core.model.MutableDocumentModel;
import org.muis.core.style.Colors;

/** A utility class containing standard formats */
public class Formats {
	/** Formats objects by their {@link Object#toString()} methods. Does not support parsing. */
	public static final MuisFormatter<Object> def = new MuisFormatter<Object>() {
		@Override
		public void append(Object value, MutableDocumentModel doc) {
			doc.append(String.valueOf(value));
		}

		@Override
		public Object parse(MuisDocumentModel doc) throws MuisParseException {
			throw new MuisParseException("The default formatter does not support parsing", -1, -1);
		}
	};

	/** A function that is a pass-through for non-null values and returns the default formatter for null values */
	public static final Function<MuisFormatter<?>, MuisFormatter<?>> defNullCatch = (MuisFormatter<?> f) -> {
		return f != null ? f : def;
	};

	/** Simple formatter for strings */
	public static final MuisFormatter<String> string = new MuisFormatter<String>() {
		@Override
		public void append(String value, MutableDocumentModel doc) {
			if(value != null)
				doc.append(value);
		}

		@Override
		public String parse(MuisDocumentModel doc) throws MuisParseException {
			return doc.toString();
		}
	};

	/** Formats integers (type long) */
	public static final MuisFormatter<Long> integer = new MuisFormatter<Long>() {
		@Override
		public void append(Long value, MutableDocumentModel doc) {
			if(value != null)
				doc.append(value.toString());
		}

		@Override
		public Long parse(MuisDocumentModel doc) throws MuisParseException {
			try {
				return Long.parseLong(doc.toString());
			} catch(NumberFormatException e) {
				throw new MuisParseException(e, -1, -1);
			}
		}
	};

	/** Formats and parses colors using the {@link Colors} class */
	public static final MuisFormatter<Color> color = new MuisFormatter<Color>() {
		@Override
		public void append(Color value, MutableDocumentModel doc) {
			if(value != null)
				doc.append(Colors.toString(value));
		}

		@Override
		public Color parse(MuisDocumentModel doc) throws MuisParseException {
			Color ret;
			try {
				ret = Colors.parseColor(doc.toString());
			} catch(MuisException e) {
				throw new MuisParseException(e.getMessage(), e, -1, -1);
			}
			return ret;
		}
	};
}
