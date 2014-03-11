package org.muis.core;

import org.muis.core.mgr.MuisMessageCenter;
import org.muis.core.model.ModelValueReferenceParser;

/** An environment needed to parse entities in MUIS */
public interface MuisParseEnv {
	/** @return The class view to use in parsing, if needed */
	MuisClassView cv();
	/** @return The message center to report parsing errors to */
	MuisMessageCenter msg();
	/** @return The parser to parse model values with */
	ModelValueReferenceParser getModelParser();
}