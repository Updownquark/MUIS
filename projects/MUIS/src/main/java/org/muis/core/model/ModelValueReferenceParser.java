package org.muis.core.model;

public interface ModelValueReferenceParser {
	int getNextMVR(String value, int start);

	String extractMVR(String value, int start);

	MuisModelValue<?> parseMVR(String mvr);
}
