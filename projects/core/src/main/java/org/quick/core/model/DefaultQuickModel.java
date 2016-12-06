package org.quick.core.model;

import java.util.*;
import java.util.function.Function;

import org.observe.ObservableAction;
import org.observe.ObservableValue;
import org.observe.SettableValue;
import org.observe.SimpleSettableValue;
import org.quick.core.QuickException;
import org.quick.core.QuickParseEnv;
import org.quick.core.parser.QuickParseException;
import org.quick.core.parser.QuickPropertyParser;
import org.quick.core.parser.SimpleParseEnv;
import org.quick.core.prop.DefaultExpressionContext;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.prop.QuickPropertyType;
import org.quick.util.QuickUtils;

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

	/** @return A builder to create a DefaultQuickModel */
	public static BuilderApi build() {
		return new BuilderApi();
	}

	/** A builder for building models from code (as opposed to from a Quick document) */
	public static class BuilderApi {
		private final Map<String, Object> theFields = new LinkedHashMap<>();

		/**
		 * @param name The name of the field to set
		 * @param value The value for the field
		 * @return This builder
		 */
		public BuilderApi with(String name, Object value) {
			theFields.put(name, value);
			return this;
		}

		/** @return The new model */
		public DefaultQuickModel build() {
			return new DefaultQuickModel(theFields);
		}
	}

	/** Builds a {@link DefaultQuickModel} */
	public static class Builder implements QuickModelBuilder {
		private static QuickModelConfigValidator VALIDATOR;
		static {
			VALIDATOR = QuickModelConfigValidator.build().forConfig("value", b -> {
				b.anyTimes().forConfig("name", b2 -> {
					b2.withText(true).required();
				}).forConfig("type", b2 -> {
					b2.withText(true);
				}).withText(false);
			}).forConfig("variable", b -> {
				b.anyTimes().forConfig("name", b2 -> {
					b2.withText(true).required();
				}).forConfig("type", b2 -> {
					b2.withText(true);
				}).forConfig("min", b2 -> {
					b2.withText(true).required();
				}).forConfig("max", b2 -> {
					b2.withText(true).required();
				}).withText(false);
			}).forConfig("action", b -> {
				b.anyTimes().forConfig("name", b2 -> {
					b2.withText(true).required();
				}).forConfig("type", b2 -> {
					b2.withText(true);
				}).withText(false);
			}).forConfig("model", b -> {
				b.anyTimes().forConfig("name", b2 -> {
					b2.withText(true).required();
				});
			}).forConfig("switch", b -> {
				b.anyTimes().forConfig("name", b2 -> {
					b2.withText(true).required();
				}).forConfig("type", b2 -> {
					b2.withText(true);
				})//
					.forConfig("value", b2 -> {
						b2.withText(true).required();
					})//
					.forConfig("case", b2 -> {
						b2.forConfig("value", b3 -> {
							b3.withText(true).required();
						}).withText(true).atLeast(1);
					})//
					.forConfig("default", b3 -> {
						b3.withText(true);
					}).withText(false);
			}).build();
		}

		private final Map<String, Object> theFields;

		/** Creates the builder */
		public Builder() { // Public so it can be used from quick XML
			theFields = new LinkedHashMap<>();
		}

		@Override
		public QuickAppModel buildModel(QuickModelConfig config, QuickPropertyParser parser, QuickParseEnv parseEnv)
			throws QuickParseException {
			VALIDATOR.validate(config);
			QuickAppModel tempModel = new QuickAppModel() {
				private final ObservableValue<QuickAppModel> theSuper;

				{
					ObservableValue<?> res = parseEnv.getContext().getVariable("this");
					if (res != null && TypeToken.of(QuickAppModel.class).isAssignableFrom(res.getType()))
						theSuper = (ObservableValue<QuickAppModel>) res;
					else
						theSuper = null;
				}

				@Override
				public Set<String> getFields() {
					Set<String> fields = new LinkedHashSet<>(theFields.keySet());
					if (theSuper != null) {
						QuickAppModel superModel = theSuper.get();
						if (superModel != null)
							fields.addAll(superModel.getFields());
					}
					return fields;
				}

				@Override
				public Object getField(String name) {
					Object value = theFields.get(name);
					if (value == null && theSuper != null) {
						QuickAppModel superModel = theSuper.get();
						if (superModel != null)
							value = superModel.getField(name);
					}
					return value;
				}
			};
			QuickParseEnv innerEnv = new SimpleParseEnv(parseEnv.cv(), parseEnv.msg(),
				DefaultExpressionContext.build().withParent(parseEnv.getContext())
					.withValue("this", ObservableValue.constant(TypeToken.of(QuickAppModel.class), tempModel)).build());
			for (Map.Entry<String, QuickModelConfig> cfg : config.getAllConfigs()) {
				String name = cfg.getValue().getString("name");
				if (name == null)
					throw new QuickParseException(cfg.getKey() + " exepects a name attribute");
				if (theFields.containsKey(name))
					throw new QuickParseException("Duplicate field: " + name);
				Object value;
				switch (cfg.getKey()) {
				case "value":
					if (cfg.getValue().getText() == null)
						throw new QuickParseException(cfg.getKey() + " expects text");
					value = parseModelValue(cfg.getValue(), parser, innerEnv);
					break;
				case "variable":
					if (cfg.getValue().getText() == null)
						throw new QuickParseException(cfg.getKey() + " expects text");
					value = parseModelVariable(cfg.getValue(), parser, innerEnv);
					break;
				case "action":
					if (cfg.getValue().getText() == null)
						throw new QuickParseException(cfg.getKey() + " expects text");
					value = parseModelAction(cfg.getValue().getText(), parser, innerEnv);
					break;
				case "model":
					value = parseChildModel(cfg.getValue().without("name"), parser, innerEnv);
					break;
				case "switch":
					value = parseModelSwitch(cfg.getValue().without("name"), parser, innerEnv);
					break;
				default:
					throw new QuickParseException("Unrecognized config item: " + cfg.getKey()); // Unnecessary except for compilation
				}
				if (value instanceof ObservableValue.ConstantObservableValue)
					value = ((ObservableValue.ConstantObservableValue<?>) value).get();
				theFields.put(cfg.getValue().getString("name"), value);
			}
			return new DefaultQuickModel(theFields);
		}

		private QuickPropertyType<?> parseType(QuickPropertyParser parser, QuickParseEnv parseEnv, String typeStr) {
			if (typeStr == null)
				return null;
			switch (typeStr) {
			case "boolean":
				return QuickPropertyType.boole;
			case "int":
			case "integer":
				return QuickPropertyType.integer;
			case "floating":
			case "double":
				return QuickPropertyType.floating;
			case "string":
			case "java.lang.String":
				return QuickPropertyType.string;
			case "color":
				return QuickPropertyType.color;
			case "instant":
			case "java.time.Instant":
				return QuickPropertyType.instant;
			case "duration":
			case "java.time.Duration":
				return QuickPropertyType.duration;
			case "resource":
				return QuickPropertyType.resource;
			}
			TypeToken<?> type;
			try {
				type = parser.parseType(parseEnv, typeStr);
			} catch (QuickParseException e) {
				parseEnv.msg().error("Could not parse model value type " + typeStr, e);
				return null;
			}
			return QuickPropertyType.build(typeStr, type).build();
		}

		private <T> ObservableValue<T> parseValue(QuickPropertyType<T> type, String text, QuickPropertyParser parser,
			QuickParseEnv parseEnv)
			throws QuickParseException {
			QuickAttribute<T> prop = type == null ? null : QuickAttribute.build("model-value", type).build();
			return (ObservableValue<T>) parser.parseProperty(prop, parseEnv, text);
		}

		private <T, V> ObservableValue<V> constrainValue(QuickPropertyType<T> type, ObservableValue<T> value, String minConfig,
			String maxConfig, QuickPropertyParser parser, QuickParseEnv parseEnv) {
			if (!(value instanceof SettableValue)) {
				parseEnv.msg().warn("Attempting to constrain unsettable value", "value", value, "min", minConfig, "max", maxConfig);
				return (ObservableValue<V>) value;
			} else if (!QuickUtils.isAssignableFrom(TypeToken.of(Comparable.class), value.getType())) {
				parseEnv.msg().warn("Attempting to constrain uncomparable value", "value", value, "min", minConfig, "max", maxConfig);
				return (ObservableValue<V>) value;
			}
			ObservableValue<?> minValue = null;
			ObservableValue<?> maxValue = null;
			if (minConfig != null) {
				try {
					minValue = parseValue(type, minConfig, parser, parseEnv);
				} catch (QuickParseException e) {
					parseEnv.msg().warn("Could not parse min constraint for value", "value", value, "min", minConfig);
				}
				if (!QuickUtils.isAssignableFrom(TypeToken.of(Comparable.class), minValue.getType())) {
					parseEnv.msg().warn("Min constraint is not comparable", "value", value, "min", minConfig, "minValue", minValue);
					minValue = null;
				}
			}
			if (maxConfig != null) {
				try {
					maxValue = parseValue(type, maxConfig, parser, parseEnv);
				} catch (QuickParseException e) {
					parseEnv.msg().warn("Could not parse max constraint for value", "value", value, "max", maxConfig);
				}
				if (!QuickUtils.isAssignableFrom(TypeToken.of(Comparable.class), minValue.getType())) {
					parseEnv.msg().warn("Max constraint is not comparable", "value", value, "max", minConfig, "maxValue", maxValue);
					minValue = null;
				}
			}

			if (minValue == null && maxValue == null)
				return (ObservableValue<V>) value;

			TypeToken<?> superType = value.getType();
			if (minValue != null && !QuickUtils.isAssignableFrom(superType, minValue.getType())) {
				if (QuickUtils.isAssignableFrom(minValue.getType(), superType))
					superType = minValue.getType();
				else {
					parseEnv.msg().warn("Min constraint is incompatible with value", "value", value, "min", minConfig, "minValue",
						minValue);
				}
			}
			if (maxValue != null && !QuickUtils.isAssignableFrom(superType, maxValue.getType())) {
				if (QuickUtils.isAssignableFrom(maxValue.getType(), superType))
					superType = maxValue.getType();
				else {
					parseEnv.msg().warn("Max constraint is incompatible with value", "value", value, "max", maxConfig, "maxValue",
						maxValue);
				}
			}
			if (!QuickUtils.isAssignableFrom(TypeToken.of(Comparable.class), superType)) {
				parseEnv.msg().warn("Super type of value and constraints, " + superType + " is not comparable", "value", value, "min",
					minConfig, "minValue", minValue, "max", maxConfig, "maxValue", maxValue);
				return (ObservableValue<V>) value;
			}

			TypeToken<V> fType = (TypeToken<V>) superType;
			SettableValue<V> settableValue = (SettableValue<V>) value;
			if (!settableValue.getType().equals(superType)) {
				settableValue = ((SettableValue<T>) settableValue).<V> mapV(fType, (T v) -> QuickUtils.convert(fType, v),
					(V v) -> QuickUtils.convert(value.getType(), v), true);
			}
			ObservableValue<?> fMin = minValue;
			ObservableValue<?> fMax = maxValue;
			settableValue = settableValue.filterAccept(v -> {
				if (fMin != null) {
					V minV = (V) fMin.get();
					if (((Comparable<V>) v).compareTo(minV) < 0)
						return "Value must be at least " + minV;
				}
				if (fMax != null) {
					V maxV = (V) fMax.get();
					if (((Comparable<V>) v).compareTo(maxV) > 0)
						return "Value must be at most " + maxV;
				}
				return null;
			});
			if (minValue != null && !(minValue instanceof ObservableValue.ConstantObservableValue))
				settableValue = settableValue.refresh(minValue.noInit());
			if (maxValue != null && !(maxValue instanceof ObservableValue.ConstantObservableValue))
				settableValue = settableValue.refresh(maxValue.noInit());
			return settableValue;
		}

		private <T> ObservableValue<T> parseModelValue(QuickModelConfig config, QuickPropertyParser parser, QuickParseEnv parseEnv)
			throws QuickParseException {
			QuickPropertyType<T> type = (QuickPropertyType<T>) parseType(parser, parseEnv, config.getString("type"));
			return parseValue(type, config.getText(), parser, parseEnv);
		}

		private <T> SettableValue<T> parseModelVariable(QuickModelConfig config, QuickPropertyParser parser, QuickParseEnv parseEnv)
			throws QuickParseException {
			QuickPropertyType<T> type = (QuickPropertyType<T>) parseType(parser, parseEnv, config.getString("type"));
			ObservableValue<T> obsValue = parseValue(type, config.getText(), parser, parseEnv);
			SettableValue<T> modelVariable = new SimpleSettableValue<>(obsValue.getType(), true);
			((SimpleSettableValue<Object>) modelVariable).set(obsValue.get(), null);
			if (config.get("min") != null | config.get("max") != null)
				modelVariable = (SettableValue<T>) constrainValue(type, modelVariable, config.getString("min"), config.getString("max"),
					parser, parseEnv);
			return modelVariable;
		}

		private ObservableAction<?> parseModelAction(String text, QuickPropertyParser parser, QuickParseEnv parseEnv)
			throws QuickParseException {
			return parser.parseProperty(ModelAttributes.action, parseEnv, text).get();
		}

		private QuickAppModel parseChildModel(QuickModelConfig config, QuickPropertyParser parser, QuickParseEnv parseEnv)
			throws QuickParseException {
			if (config.getString("builder") != null || config.getString("builder-class") != null)
				return buildQuickModel(config, parser, parseEnv);
			else
				return buildModel(config, parser, parseEnv);
		}

		private <T> ObservableValue<T> parseModelSwitch(QuickModelConfig config, QuickPropertyParser parser, QuickParseEnv parseEnv)
			throws QuickParseException {
			QuickPropertyType<T> type = (QuickPropertyType<T>) parseType(parser, parseEnv, config.getString("type"));
			ObservableValue<?> value = parseValue(null, config.getString("value"), parser, parseEnv);
			Map<Object, ObservableValue<?>> mappings = new HashMap<>();
			Map<Object, T> valueMappings = new HashMap<>();
			for (QuickModelConfig caseConfig : (List<QuickModelConfig>) (List<?>) config.getValues("case")) {
				Object from = parseValue(null, ((QuickModelConfig) caseConfig.get("value")).getText(), parser, parseEnv).get();
				ObservableValue<T> to = parseValue(type, caseConfig.getText(), parser, parseEnv);
				mappings.put(from, to);
				T toVal = to.get();
				valueMappings.put(from, toVal);
			}
			boolean allCommon = true;
			TypeToken<?> common = type == null ? null : type.getType();
			if (type == null) {
				for (ObservableValue<?> v : mappings.values()) {
					if (common == null)
						common = v.getType();
					else if (!common.equals(v.getType())) {
						allCommon = false;
						break;
					}
				}
			}
			T def;
			if (config.getString("default") != null && !config.getString("default").equals("null")) {
				ObservableValue<T> defValue = parseValue(type, config.getString("default"), parser, parseEnv);
				if (type == null && allCommon && !common.equals(defValue.getType()))
					allCommon = false;
				def = defValue.get();
			} else
				def = null;
			if (!allCommon)
				common = TypeToken.of(Object.class);
			Function<Object, T> mapFn = v -> {
				T to = valueMappings.get(v);
				if (to == null)
					to = def;
				return to;
			};
			return value.<T> mapV((TypeToken<T>) common, mapFn, true);
		}
	}
}
