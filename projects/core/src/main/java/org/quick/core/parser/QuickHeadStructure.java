package org.quick.core.parser;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.quick.core.QuickHeadSection;
import org.quick.core.model.QuickModelConfig;
import org.quick.core.style2.ImmutableStyleSheet;

/** The structure of a {@link QuickHeadSection} before some evaluation */
public class QuickHeadStructure {
	private final String theTitle;
	private final List<ImmutableStyleSheet> theStyleSheets;
	private final Map<String, QuickModelConfig> theModelConfigs;

	/**
	 * @param title The document title
	 * @param styleSheets The style sheets for the head section
	 * @param modelConfigs The configuration for the head section's models
	 */
	public QuickHeadStructure(String title, List<ImmutableStyleSheet> styleSheets, Map<String, QuickModelConfig> modelConfigs) {
		theTitle = title;
		theStyleSheets = Collections.unmodifiableList(styleSheets);
		theModelConfigs = Collections.unmodifiableMap(modelConfigs);
	}

	/** @return The document title */
	public String getTitle() {
		return theTitle;
	}

	/** @return The style sheets for the head section */
	public List<ImmutableStyleSheet> getStyleSheets() {
		return theStyleSheets;
	}

	/** @return The configuration for the head section's models */
	public Map<String, QuickModelConfig> getModelConfigs() {
		return theModelConfigs;
	}
}
