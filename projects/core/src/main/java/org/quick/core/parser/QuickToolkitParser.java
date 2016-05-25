package org.quick.core.parser;

import java.io.Reader;
import java.net.URL;

import org.quick.core.QuickToolkit;

public interface QuickToolkitParser {
	QuickToolkit parseToolkit(URL location, Reader reader);
}
