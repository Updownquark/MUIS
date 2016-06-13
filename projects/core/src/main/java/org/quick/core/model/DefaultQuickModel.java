package org.quick.core.model;

import java.util.*;
import java.util.function.Function;

import org.observe.ObservableValue;
import org.observe.SimpleSettableValue;
import org.quick.core.QuickException;
import org.quick.core.QuickParseEnv;
import org.quick.core.parser.QuickParseException;
import org.quick.core.parser.QuickPropertyParser;

import com.google.common.reflect.TypeToken;

/** The default (typically XML-specified) implementation for QuickAppModel */
public class DefaultQuickModel implements QuickAppModel {
	private final Map<String, Object> theFields;

	private DefaultQuickModel(Map<String, Object> fields) {
		theFields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
	}

	@Override
	public Set<String> getFields() {
		return theFields.keySet();
	}

	@Override
	public Object getField(String name) {
		return theFields.get(name);
	}

	/**
	 * The default model-building method. Instantiates a builder from the "builder" attribute (for quick-style type string) or
	 * "builder-class") attribute (for fully-qualified class name) and passes the rest of the config to it.
	 *
	 * @param config The model config to use to build the model
	 * @param parser The attribute parser to parse any specified values
	 * @param parseEnv The parse environment for parsing any specified values
	 * @return The fully-built Quick model
	 * @throws QuickParseException If the model cannot be built
	 */
	public static QuickAppModel buildQuickModel(QuickModelConfig config, QuickPropertyParser parser, QuickParseEnv parseEnv)
		throws QuickParseException {
		Class<? extends QuickModelBuilder> builderClass;
		String builderAtt = config.getString("builder");
		if (builderAtt != null) {
			if (config.get("builder-class") != null) {
				throw new QuickParseException("builder-class may not specified when builder is");
			}
			try {
				builderClass = parseEnv.cv().loadMappedClass(builderAtt, QuickModelBuilder.class);
			} catch (QuickException e) {
				throw new QuickParseException("Error creating model", e);
			}
		} else {
			builderAtt = config.getString("builder-class");
			if (builderAtt == null) {
				throw new QuickParseException("No builder or builder-class attribute for model");
			}
			try {
				builderClass = parseEnv.cv().loadClass(builderAtt).asSubclass(QuickModelBuilder.class);
			} catch (ClassNotFoundException | ClassCastException e) {
				throw new QuickParseException("Error creating model", e);
			}
		}
		QuickModelBuilder builder;
		try {
			builder = builderClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new QuickParseException("Error creating model", e);
		}
		try {
			return builder.buildModel(config.without("builder", "builder-class"), parser, parseEnv);
		} catch (RuntimeException e) {
			throw new QuickParseException("Error creating model", e);
		}
	}

	public Builder build() {
		return new Builder();
	}

	public static class Builder implements QuickModelBuilder {
		private static QuickModelConfigValidator VALIDATOR;
		static {
			VALIDATOR = QuickModelConfigValidator.build().forConfig("value", b -> {
				b.forConfig("name", b2 -> {
					b2.required();
				}).withText(false);
			}).forConfig("variable", b -> {
				b.forConfig("name", b2 -> {
					b2.required();
				}).withText(false);
			}).forConfig("action", b -> {
				b.forConfig("name", b2 -> {
					b2.required();
				}).withText(false);
			}).forConfig("model", b -> {
				b.forConfig("name", b2 -> {
					b2.required();
				});
			}).forConfig("switch", b -> {
				b.forConfig("name", b2 -> {
					b2.required();
				})//
					.forConfig("value", b2 -> {
						b2.required();
					})//
					.forConfig("case", b2 -> {
						b2.forConfig("from", b3 -> {
							b3.required();
						});
					})//
					.forConfig("default", b3 -> {
						b3.withText(false);
					}).withText(false);
			}).build();
		}

		private final Map<String, Object> theFields;

		public Builder() { // Public so it can be used from quick XML
			theFields = new LinkedHashMap<>();
		}

		@Override
		public QuickAppModel buildModel(QuickModelConfig config, QuickPropertyParser parser, QuickParseEnv parseEnv)
			throws QuickParseException {
			VALIDATOR.validate(config);
			for (Map.Entry<String, Object> cfg : config.getAllConfigs()) {
				QuickModelConfig cfgItem = (QuickModelConfig) cfg.getValue();
				String name = cfgItem.getString("name");
				if (name == null)
					throw new QuickParseException(cfg.getKey() + " exepects a name attribute");
				if (theFields.containsKey(name))
					throw new QuickParseException("Duplicate field: " + name);
				Object value;
				switch (cfg.getKey()) {
				case "value":
					if (cfgItem.getText() == null)
						throw new QuickParseException(cfg.getKey() + " expects text");
					value = parseModelValue(cfgItem, parser, parseEnv);
					break;
				case "variable":
					if (cfgItem.getText() == null)
						throw new QuickParseException(cfg.getKey() + " expects text");
					value = parseModelVariable(cfgItem, parser, parseEnv);
					break;
				case "action":
					if (cfgItem.getText() == null)
						throw new QuickParseException(cfg.getKey() + " expects text");
					value = parseModelAction(cfgItem.getText(), parser, parseEnv);
					break;
				case "model":
					value = parseChildModel(cfgItem.without("name"), parser, parseEnv);
					break;
				case "switch":
					value = parseModelSwitch(cfgItem.without("name"), parser, parseEnv);
					break;
				default:
					throw new QuickParseException("Unrecognized config item: " + cfg.getKey()); // Unnecessary except for compilation
				}
				theFields.put(name, value);
			}
			return new DefaultQuickModel(theFields);
		}

		private ObservableValue<?> parseValue(Object config, QuickPropertyParser parser, QuickParseEnv parseEnv)
			throws QuickParseException {
			if (config instanceof String)
				return parser.parseProperty(null, parseEnv.getContext(), (String) config);
			QuickModelConfig cfg = (QuickModelConfig) config;
			return parser.parseProperty(null, parseEnv.getContext(), cfg.getText());
		}

		private Object parseModelValue(QuickModelConfig config, QuickPropertyParser parser, QuickParseEnv parseEnv)
			throws QuickParseException {
			ObservableValue<?> obsValue = parseValue(config, parser, parseEnv);
			if (obsValue instanceof ObservableValue.ConstantObservableValue)
				return ((ObservableValue.ConstantObservableValue<?>) obsValue).get();
			else
				return obsValue;
		}

		private SimpleSettableValue<?> parseModelVariable(QuickModelConfig config, QuickPropertyParser parser, QuickParseEnv parseEnv)
			throws QuickParseException {
			ObservableValue<?> obsValue = parseValue(config, parser, parseEnv);
			SimpleSettableValue<?> modelVariable = new SimpleSettableValue<>(obsValue.getType(), true);
			((SimpleSettableValue<Object>) modelVariable).set(obsValue.get(), null);
			return modelVariable;
		}

		private Runnable parseModelAction(String text, QuickPropertyParser parser, QuickParseEnv parseEnv) throws QuickParseException {
			return parser.parseAction(parseEnv.getContext(), text);
		}

		private QuickAppModel parseChildModel(QuickModelConfig config, QuickPropertyParser parser, QuickParseEnv parseEnv)
			throws QuickParseException {
			if (config.getString("builder") != null || config.getString("builder-class") != null)
				return buildQuickModel(config, parser, parseEnv);
			else
				return buildModel(config, parser, parseEnv);
		}

		private ObservableValue<?> parseModelSwitch(QuickModelConfig config, QuickPropertyParser parser, QuickParseEnv parseEnv)
			throws QuickParseException {
			ObservableValue<?> value = parseValue(config, parser, parseEnv);
			Map<Object, ObservableValue<?>> mappings = new HashMap<>();
			Map<Object, Object> valueMappings = new HashMap<>();
			for (QuickModelConfig caseConfig : (List<QuickModelConfig>) (List<?>) config.getValues("case")) {
				Object from = parseValue(caseConfig.get("from"), parser, parseEnv).get();
				ObservableValue<?> to = parseValue(caseConfig, parser, parseEnv);
				mappings.put(from, to);
				Object toVal = to.get();
				valueMappings.put(from, toVal);
			}
			boolean allCommon = true;
			TypeToken<?> common = null;
			for (ObservableValue<?> v : mappings.values()) {
				if (common == null)
					common = v.getType();
				else if (!common.equals(v.getType())) {
					allCommon = false;
					break;
				}
			}
			Object def;
			if (config.getString("default") != null && !config.getString("default").equals("null")) {
				ObservableValue<?> defValue = parseValue(config.get("default"), parser, parseEnv);
				if (allCommon && !common.equals(defValue))
					allCommon = false;
				def = defValue.get();
			} else
				def = null;
			if (!allCommon)
				common = TypeToken.of(Object.class);
			Function<Object, Object> mapFn = v -> {
				Object to = valueMappings.get(v);
				if (to == null)
					to = def;
				return to;
			};
			return value.<Object> mapV((TypeToken<Object>) common, mapFn, true);
		}
	}
}
