package org.quick.core.parser;

import java.io.IOException;
import java.net.URL;

import org.quick.core.QuickClassView;
import org.quick.core.QuickEnvironment;
import org.quick.core.QuickToolkit;
import org.quick.core.mgr.QuickMessageCenter;
import org.quick.core.style.sheet.ParsedStyleSheet;

public interface QuickStyleParser {
	QuickEnvironment getEnvironment();

	ParsedStyleSheet parseStyleSheet(URL location, QuickToolkit toolkit, QuickPropertyParser parser, QuickClassView cv,
		QuickMessageCenter msg) throws IOException, QuickParseException;
}
