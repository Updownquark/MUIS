package org.muis.base.model;

import java.awt.Color;

import org.muis.core.MuisException;
import org.muis.core.model.MuisDocumentModel;
import org.muis.core.model.MutableDocumentModel;
import org.muis.core.style.Colors;

/** A utility class containing standard formats */
public class Formats {
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
