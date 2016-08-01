package org.quick.core.parser;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.quick.core.model.QuickModelConfig;
import org.quick.core.style2.ImmutableStyleSheet;

public class QuickHeadStructure {
	private final String theTitle;
	private final List<ImmutableStyleSheet> theStyleSheets;
	private final Map<String, QuickModelConfig> theModelConfigs;

	public QuickHeadStructure(String title, List<ImmutableStyleSheet> styleSheets, Map<String, QuickModelConfig> modelConfigs) {
		theTitle = title;
		theStyleSheets = Collections.unmodifiableList(styleSheets);
		theModelConfigs = Collections.unmodifiableMap(modelConfigs);
	}

	public String getTitle() {
		return theTitle;
	}

	public List<ImmutableStyleSheet> getStyleSheets() {
		return theStyleSheets;
	}

	public Map<String, QuickModelConfig> getModelConfigs() {
		return theModelConfigs;
	}
}
