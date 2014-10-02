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

	/** Formats and parses colors using the {@link Colors} class */
	public static final MuisFormatter<Color> color = new MuisFormatter<Color>() {
		@Override
		public void append(Color value, MutableDocumentModel doc) {
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
