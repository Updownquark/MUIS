package org.quick.core.parser;

import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

import org.quick.core.QuickEnvironment;
import org.quick.core.QuickToolkit;

public interface QuickToolkitParser {
	QuickEnvironment getEnvironment();

	QuickToolkit parseToolkit(URL location, Consumer<QuickToolkit> onBuild) throws IOException, QuickParseException;
}
