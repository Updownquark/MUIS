package org.muis.core.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.muis.core.parser.MuisParseException;

/** The default implementation of {@link ModelValueReferenceParser} */
public class DefaultModelValueReferenceParser implements ModelValueReferenceParser {
	private static final Pattern MVR_PATTERN;

	static {
		Pattern p;
		try {
			p = Pattern.compile("\\$\\{(([a-zA-Z_\\-][0-9a-zA-Z_\\-]*)(\\.([a-zA-Z_\\-][0-9a-zA-Z_\\-]*))*)\\}");
		} catch(RuntimeException e) {
			e.printStackTrace();
			p = null;
		}
		MVR_PATTERN = p;
	}

	private org.muis.core.MuisDocument theDocument;

	/** @param doc The document to get models from */
	public DefaultModelValueReferenceParser(org.muis.core.MuisDocument doc) {
		theDocument = doc;
	}

	@Override
	public int getNextMVR(String value, int start) {
		return getNextModelValueReference(value, start);
	}

	@Override
	public String extractMVR(String value, int start) throws MuisParseException {
		return extractModelValueReference(value, start);
	}

	@Override
	public MuisModelValue<?> parseMVR(String mvr) throws MuisParseException {
		return parseModelValueReference(mvr, theDocument);
	}

	/**
	 * @param value The value to inspect
	 * @param start The starting index to search
	 * @return The index in the value of the start of the next model value reference
	 */
	public static int getNextModelValueReference(String value, int start) {
		Matcher matcher = MVR_PATTERN.matcher(value);
		if(!matcher.find(start))
			return -1;
		return matcher.start();
	}

	/**
	 * @param value The value to parse
	 * @param start The location of the start of a model value reference
	 * @return The extracted model value reference
	 * @throws MuisParseException If an error occurs extracting the reference
	 */
	public static String extractModelValueReference(String value, int start) throws MuisParseException {
		Matcher matcher = MVR_PATTERN.matcher(value);
		if(!matcher.find(start) || matcher.start() != start)
			throw new MuisParseException("No MUIS model detected: " + value.substring(start));
		return matcher.group();
	}

	/**
	 * @param mvr The value to parse
	 * @param doc The document to get models from
	 * @return The parsed model value reference
	 * @throws MuisParseException If an error occurs parsing the reference
	 */
	public static MuisModelValue<?> parseModelValueReference(String mvr, org.muis.core.MuisDocument doc) throws MuisParseException {
		Matcher matcher = MVR_PATTERN.matcher(mvr);
		if(!matcher.matches())
			throw new MuisParseException(mvr + " is not a recognized model value reference");
		return ModelAttributes.getModelValue(doc, matcher.group(1));
	}
}
