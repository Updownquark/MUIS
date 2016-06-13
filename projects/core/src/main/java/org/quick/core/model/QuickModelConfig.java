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

	public String getText() {
		return theText;
	}

	public List<String> getConfigPoints() {
		return theConfigPoints.stream().map(cp -> cp.name).collect(Collectors.toList());
	}

	public Object get(String configPoint) {
		Optional<ConfigPoint> cfg = theConfigPoints.stream().filter(cp -> cp.name.equals(configPoint)).findFirst();
		return cfg.orElse(null);
	}

	public String getString(String configPoint) {
		Optional<ConfigPoint> cfg = theConfigPoints.stream().filter(cp -> cp.name.equals(configPoint) && cp.value instanceof String)
			.findFirst();
		return cfg.map(cp -> (String) cp.value).orElse(null);
	}

	public QuickModelConfig getChild(String configPoint) {
		Optional<ConfigPoint> cfg = theConfigPoints.stream()
			.filter(cp -> cp.name.equals(configPoint) && cp.value instanceof QuickModelConfig).findFirst();
		return cfg.map(cp -> (QuickModelConfig) cp.value).orElse(null);
	}

	public List<Object> getValues(String configPoint) {
		return theConfigPoints.stream().filter(cp -> cp.name.equals(configPoint)).map(cp -> cp.value).collect(Collectors.toList());
	}

	public List<Map.Entry<String, Object>> getAllConfigs() {
		return theConfigPoints.stream().map(cp -> new SimpleMapEntry<>(cp.name, cp.value)).collect(Collectors.toList());
	}

	public QuickModelConfig without(String... configPoints) {
		return new QuickModelConfig(
			theConfigPoints.stream().filter(cp -> !ArrayUtils.contains(configPoints, cp.name)).collect(Collectors.toList()), theText);
	}

	public static Builder build() {
		return new Builder();
	}

	public static class Builder {
		private final List<ConfigPoint> theConfigPoints;
		private String theText;

		Builder() {
			theConfigPoints = new ArrayList<>();
		}

		public Builder add(String configPoint, String value) {
			theConfigPoints.add(new ConfigPoint(configPoint, value));
			return this;
		}

		public Builder addChild(String name, QuickModelConfig model) {
			theConfigPoints.add(new ConfigPoint(name, model));
			return this;
		}

		public Builder withText(String text) {
			theText = text;
			return this;
		}

		public QuickModelConfig build() {
			return new QuickModelConfig(theConfigPoints, theText);
		}
	}

	private static final class ConfigPoint {
		String name;
		Object value;

		ConfigPoint(String name, Object value) {
			this.name = name;
			this.value = value;
		}
	}
}
