package org.muis.core.model;

import org.muis.core.parser.MuisParseException;
import org.muis.core.rx.ObservableValue;

/** Parses references to model values from property values */
public interface ModelValueReferenceParser {
	/**
	 * @param value The value to inspect
	 * @param start The starting index to search
	 * @return The index in the value of the start of the next model value reference
	 */
	int getNextMVR(String value, int start);

	/**
	 * @param value The value to parse
	 * @param start The location of the start of a model value reference
	 * @return The extracted model value reference
	 * @throws MuisParseException If an error occurs extracting the reference
	 */
	String extractMVR(String value, int start) throws MuisParseException;

	/**
	 * @param mvr The value to parse
	 * @return The parsed model value reference
	 * @throws MuisParseException If an error occurs parsing the reference
	 */
	ObservableValue<?> parseMVR(String mvr) throws MuisParseException;
}
