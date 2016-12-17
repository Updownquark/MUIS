package org.quick.core.model;

import java.util.*;
import java.util.stream.Collectors;

import org.qommons.ArrayUtils;
import org.qommons.SimpleMapEntry;

/** Simple configuration for Quick models */
public class QuickModelConfig {
	private final List<ConfigPoint> theConfigPoints;
	private final String theText;

	private QuickModelConfig(List<ConfigPoint> pts, String text) {
		theConfigPoints = Collections.unmodifiableList(pts);
		theText = text;
	}

	/** @return The text of the config */
	public String getText() {
		return theText;
	}

	/** @return The name of all sub-configuration points in this config */
	public List<String> getConfigPoints() {
		return theConfigPoints.stream().map(cp -> cp.name).collect(Collectors.toList());
	}

	/**
	 * @param configPoint The name of the sub-config to get
	 * @return The sub-configuration point with the given name in this config, or null if none exists
	 */
	public Object get(String configPoint) {
		Optional<ConfigPoint> cfg = theConfigPoints.stream().filter(cp -> cp.name.equals(configPoint)).findFirst();
		return cfg.map(cp -> cp.value).orElse(null);
	}

	/**
	 * @param configPoint The name of the sub-config to get
	 * @return The sub-configuration point with the given name in this config as a string, or null if none exists
	 */
	public String getString(String configPoint) {
		return getString(configPoint, null);
	}

	/**
	 * @param configPoint The name of the sub-config to get
	 * @param defValue The default value to return if the config point does not exist
	 * @return The sub-configuration point with the given name in this config as a string, or null if none exists
	 */
	public String getString(String configPoint, String defValue) {
		Optional<ConfigPoint> cfg = theConfigPoints.stream().filter(cp -> cp.name.equals(configPoint)).findFirst();
		return cfg.map(cp -> cp.value.getText()).orElse(defValue);
	}

	/**
	 * @param configPoint The name of the sub-config to get
	 * @return The sub-configuration point with the given name in this config as a model, or null if none exists
	 */
	public QuickModelConfig getChild(String configPoint) {
		Optional<ConfigPoint> cfg = theConfigPoints.stream().filter(cp -> cp.name.equals(configPoint)).findFirst();
		return cfg.map(cp -> cp.value).orElse(null);
	}

	/**
	 * @param configPoint The name of the config point
	 * @return The values of all config points in this model config with the given name
	 */
	public List<Object> getValues(String configPoint) {
		return theConfigPoints.stream().filter(cp -> cp.name.equals(configPoint)).map(cp -> cp.value).collect(Collectors.toList());
	}

	/** @return All config points in this model config */
	public List<Map.Entry<String, QuickModelConfig>> getAllConfigs() {
		return theConfigPoints.stream().map(cp -> new SimpleMapEntry<>(cp.name, cp.value)).collect(Collectors.toList());
	}

	/**
	 * @param configPoints The names of the config points to exclude
	 * @return A {@link QuickModelConfig} that is the same as this one but without the given config points
	 */
	public QuickModelConfig without(String... configPoints) {
		return new QuickModelConfig(
			theConfigPoints.stream().filter(cp -> !ArrayUtils.contains(configPoints, cp.name)).collect(Collectors.toList()), theText);
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		if (theText != null) {
			ret.append(theText);
			if (!theConfigPoints.isEmpty())
				ret.append(' ');
		}
		if (!theConfigPoints.isEmpty())
			ret.append(theConfigPoints);
		return ret.toString();
	}

	/** @return A builder for model configs */
	public static Builder build() {
		return new Builder();
	}

	/** Builds {@link QuickModelConfig}s */
	public static class Builder {
		private final List<ConfigPoint> theConfigPoints;
		private String theText;

		Builder() {
			theConfigPoints = new ArrayList<>();
		}

		/**
		 * Adds a text configuration point
		 *
		 * @param configPoint The name of the config point
		 * @param value The value text for the config point
		 * @return This builder
		 */
		public Builder add(String configPoint, String value) {
			return addChild(configPoint, QuickModelConfig.build().withText(value).build());
		}

		/**
		 * Adds a sub-configuration point
		 *
		 * @param name The name of the config point
		 * @param model The config point
		 * @return This builder
		 */
		public Builder addChild(String name, QuickModelConfig model) {
			theConfigPoints.add(new ConfigPoint(name, model));
			return this;
		}

		/**
		 * @param text The text for this config
		 * @return This builder
		 */
		public Builder withText(String text) {
			theText = text;
			return this;
		}

		/**
		 * Builds the config
		 *
		 * @return The built model config
		 */
		public QuickModelConfig build() {
			return new QuickModelConfig(theConfigPoints, theText);
		}
	}

	private static final class ConfigPoint {
		String name;
		QuickModelConfig value;

		ConfigPoint(String name, QuickModelConfig value) {
			this.name = name;
			this.value = value;
		}

		@Override
		public String toString() {
			return name + "=" + value;
		}
	}
}
