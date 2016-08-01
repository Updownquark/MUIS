package org.quick.core;

import java.util.*;

import org.quick.core.style2.ImmutableStyleSheet;

/** Metadata for a Quick document */
public class QuickHeadSection {
	private final String theTitle;
	private final List<ImmutableStyleSheet> theStyleSheets;
	private final Map<String, Object> theModels;

	private QuickHeadSection(String title, List<ImmutableStyleSheet> styleSheets, Map<String, Object> models) {
		theTitle = title;
		theStyleSheets = Collections.unmodifiableList(new ArrayList<>(styleSheets));
		theModels = Collections.unmodifiableMap(new TreeMap<>(models));
	}

	/** @return The title for the Quick document */
	public String getTitle() {
		return theTitle;
	}

	/** @return All style sheets specified in this head section */
	public List<ImmutableStyleSheet> getStyleSheets() {
		return theStyleSheets;
	}

	/**
	 * @param name The name of the model to get
	 * @return The model of the given name specified in this head section, or null if no so-named model was specified
	 */
	public Object getModel(String name) {
		return theModels.get(name);
	}

	/** @return The names of all models in this document */
	public Set<String> getModels() {
		return theModels.keySet();
	}

	/** @return A builder to build a head section */
	public static Builder build() {
		return new Builder();
	}

	/** Builds a {@link QuickHeadSection} */
	public static class Builder {
		private String theTitle;
		private List<ImmutableStyleSheet> theStyleSheets = new ArrayList<>();
		private Map<String, Object> theModels = new TreeMap<>();

		Builder() {}

		/**
		 * @param title The title for the document
		 * @return This builder, for chaining
		 */
		public Builder setTitle(String title) {
			theTitle = title;
			return this;
		}

		/**
		 * @param styleSheet The style sheet to add to the head section
		 * @return This builder, for chaining
		 */
		public Builder addStyleSheet(ImmutableStyleSheet styleSheet) {
			theStyleSheets.add(styleSheet);
			return this;
		}

		/**
		 * @param name The name of the model to add
		 * @param model The model specified in this head section under the given name
		 * @return This builder, for chaining
		 */
		public Builder addModel(String name, Object model) {
			theModels.put(name, model);
			return this;
		}

		/**
		 * @param name The name of the model
		 * @return The model that has been set in this builder for the given name
		 */
		public Object getModel(String name) {
			return theModels.get(name);
		}

		/** @return The built head section */
		public QuickHeadSection build() {
			return new QuickHeadSection(theTitle, theStyleSheets, theModels);
		}
	}
}
