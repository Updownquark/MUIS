package org.quick.core.parser;

import java.io.IOException;
import java.net.URL;

import org.quick.core.QuickEnvironment;
import org.quick.core.QuickParseEnv;
import org.quick.core.QuickToolkit;
import org.quick.core.style.sheet.ParsedStyleSheet;

public interface QuickStyleParser {
	QuickEnvironment getEnvironment();

	ParsedStyleSheet parseStyleSheet(QuickParseEnv parseEnv, QuickToolkit toolkit, URL location) throws IOException, QuickParseException;
}
