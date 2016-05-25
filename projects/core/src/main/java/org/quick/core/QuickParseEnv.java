package org.quick.core;

import org.quick.core.mgr.QuickMessageCenter;
import org.quick.core.parser.QuickAttributeParser;

/** An environment needed to parse entities in MUIS */
public interface QuickParseEnv {
	/** @return The class view to use in parsing, if needed */
	QuickClassView cv();
	/** @return The message center to report parsing errors to */
	QuickMessageCenter msg();
	/** @return The parser to parse reference values with */
	QuickAttributeParser getAttributeParser();
}