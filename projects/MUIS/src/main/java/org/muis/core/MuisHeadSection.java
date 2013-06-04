package org.muis.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.muis.core.model.MuisAppModel;
import org.muis.core.style.sheet.ParsedStyleSheet;

import prisms.util.Sealable;

/** Metadata for a MUIS document */
public class MuisHeadSection implements Sealable {
	private String theTitle;

	private List<ParsedStyleSheet> theStyleSheets;

	private Map<String, MuisAppModel> theModels;

	private boolean isSealed;

	/** Creates a head section */
	public MuisHeadSection() {
		theStyleSheets = new java.util.ArrayList<>();
		theModels = new HashMap<>(2);
	}

	@Override
	public boolean isSealed() {
		return isSealed;
	}

	@Override
	public void seal() {
		if(!isSealed)
			theStyleSheets = java.util.Collections.unmodifiableList(theStyleSheets);
		isSealed = true;
	}

	/** @return The title for the MUIS document */
	public String getTitle() {
		return theTitle;
	}

	/** @param title The title for the document */
	public void setTitle(String title) {
		if(isSealed)
			throw new SealedException(this);
		theTitle = title;
	}

	/** @return All style sheets specified in this head section */
	public List<ParsedStyleSheet> getStyleSheets() {
		return theStyleSheets;
	}

	/**
	 * @param name The name of the model to get
	 * @return The model of the given name specified in this head section, or null if no so-named model was specified
	 */
	public MuisAppModel getModel(String name) {
		return theModels.get(name);
	}

	/**
	 * @param name The name of the model to add
	 * @param model The model specified in this head section under the given name
	 */
	public void addModel(String name, MuisAppModel model) {
		if(isSealed)
			throw new SealedException(this);
		theModels.put(name, model);
	}
}
