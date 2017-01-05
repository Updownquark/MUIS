package org.quick.core.model;

import org.quick.core.QuickParseEnv;
import org.quick.core.parser.QuickParseException;
import org.quick.core.parser.QuickPropertyParser;

/** Builds {@link QuickAppModel}s */
public interface QuickModelBuilder {
	/**
	 * @param name The name for the model
	 * @param config The configuration for the model to build
	 * @param parser The property parser to parse model values
	 * @param parseEnv The parse environment
	 * @return The built model
	 * @throws QuickParseException If the model fails to build
	 */
	QuickAppModel buildModel(String name, QuickModelConfig config, QuickPropertyParser parser, QuickParseEnv parseEnv)
		throws QuickParseException;
}
