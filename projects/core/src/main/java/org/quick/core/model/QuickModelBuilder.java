package org.quick.core.model;

import org.quick.core.QuickParseEnv;
import org.quick.core.parser.QuickPropertyParser;
import org.quick.core.parser.QuickParseException;

public interface QuickModelBuilder {
	QuickAppModel buildModel(QuickModelConfig config, QuickPropertyParser parser, QuickParseEnv parseEnv) throws QuickParseException;
}
