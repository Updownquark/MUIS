package org.quick.core.parser;

import java.net.URL;

import org.quick.core.QuickEnvironment;
import org.quick.core.style.sheet.ParsedStyleSheet;

public interface QuickStyleParser {
	QuickEnvironment getEnvironment();

	ParsedStyleSheet parseStyleSheet(URL location);
}
