package org.muis.core.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultModelValueReferenceParser implements ModelValueReferenceParser {
	private static final Pattern MVR_PATTERN = Pattern
		.compile("$\\{(([a-zA-Z_\\-][0-9a-zA-Z_\\-]*)(\\.([a-zA-Z_\\\\-][0-9a-zA-Z_\\\\-]*))*\\})");

	private org.muis.core.MuisDocument theDocument;
	private org.muis.core.mgr.MuisMessageCenter theMsg;

	public DefaultModelValueReferenceParser(org.muis.core.MuisDocument doc, org.muis.core.mgr.MuisMessageCenter msg) {
		theDocument = doc;
		theMsg = msg;
	}

	@Override
	public int getNextMVR(String value, int start) {
		return getNextModelValueReference(value, start);
	}

	@Override
	public String extractMVR(String value, int start) {
		return extractModelValueReference(value, start);
	}

	@Override
	public MuisModelValue<?> parseMVR(String mvr) {
		return parseModelValueReference(mvr, theDocument, theMsg);
	}

	public static int getNextModelValueReference(String value, int start) {
		Matcher matcher = MVR_PATTERN.matcher(value);
		if(!matcher.find(start))
			return -1;
		return matcher.start();
	}

	public static String extractModelValueReference(String value, int start) {
		Matcher matcher = MVR_PATTERN.matcher(value);
		if(!matcher.find(start) || matcher.start() != start)
			return null;
		return matcher.group();
	}

	public static MuisModelValue<?> parseModelValueReference(String mvr, org.muis.core.MuisDocument doc,
		org.muis.core.mgr.MuisMessageCenter msg) throws IllegalArgumentException {
		Matcher matcher = MVR_PATTERN.matcher(mvr);
		if(!matcher.matches())
			throw new IllegalArgumentException(mvr + " is not a recognized model value reference");
		return ModelAttributes.getModelValue(doc, matcher.group(1), msg);
	}
}
