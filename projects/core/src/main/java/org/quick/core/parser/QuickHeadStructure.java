package org.quick.core.parser;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.quick.core.model.QuickModelConfig;
import org.quick.core.style.sheet.ParsedStyleSheet;

public class QuickHeadStructure {
	private final String theTitle;
	private final List<ParsedStyleSheet> theStyleSheets;
	private final Map<String, QuickModelConfig> theModelConfigs;

	public QuickHeadStructure(String title, List<ParsedStyleSheet> styleSheets, Map<String, QuickModelConfig> modelConfigs) {
		theTitle = title;
		theStyleSheets = Collections.unmodifiableList(styleSheets);
		theModelConfigs = Collections.unmodifiableMap(modelConfigs);
	}

	public String getTitle() {
		return theTitle;
	}

	public List<ParsedStyleSheet> getStyleSheets() {
		return theStyleSheets;
	}

	public Map<String, QuickModelConfig> getModelConfigs() {
		return theModelConfigs;
	}
}
