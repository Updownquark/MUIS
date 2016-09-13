package org.quick.core.prop;

import java.awt.Color;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.observe.ObservableValue;
import org.qommons.ColorUtils;
import org.qommons.TriFunction;
import org.qommons.ex.ExFunction;
import org.quick.core.QuickElement;
import org.quick.core.QuickException;
import org.quick.core.QuickParseEnv;
import org.quick.core.QuickToolkit;
import org.quick.core.parser.QuickParseException;
import org.quick.core.parser.QuickPropertyParser;
import org.quick.core.style.Colors;
import org.quick.util.QuickUtils;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

/**
 * A property type understands contains information needed to produce items of a certain type from parseable strings and other types
 *
 * @param <T> The type of value that this property type produces TODO Get rid of all the V types
 */
public final class QuickPropertyType<T> {
	private static final SimpleDateFormat INSTANT_FORMAT = new SimpleDateFormat("ddMMMyyyy HH:mm:ss");

	/**
	 * An interface that a property type can use to parse its own values
	 *
	 * @param <T> The type of property to parse
	 */
	public interface PropertySelfParser<T> {
		/**
		 * @param parser The property parser
		 * @param parseEnv The parse environment
		 * @param value The text to parse
		 * @return The parsed value
		 * @throws QuickParseException If an error occurs parsing the text
		 */
		ObservableValue<T> parse(QuickPropertyParser parser, QuickParseEnv parseEnv, String value) throws QuickParseException;
	}

	private final String theName;
	private final TypeToken<T> theType;
	private final PropertySelfParser<T> theParser;
	private final boolean isSelfParsingByDefault;
	private final Function<Integer, String> theReferenceReplacementGenerator;
	private final List<TypeMapping<?, ?>> theMappings;
	private final ExpressionContext theContext;
	private final Function<? super T, String> thePrinter;
	private final boolean isAction;

	private QuickPropertyType(String name, TypeToken<T> type, PropertySelfParser<T> parser, boolean parseSelfByDefault,
		Function<Integer, String> replacementGen, List<TypeMapping<?, ?>> mappings, Function<? super T, String> printer,
		ExpressionContext ctx, boolean action) {
		theName = name;
		theType = type;
		theParser = parser;
		isSelfParsingByDefault = parseSelfByDefault;
		theReferenceReplacementGenerator = replacementGen;
		theMappings = Collections.unmodifiableList(new ArrayList<>(mappings));
		theContext = ctx;
		thePrinter = printer;
		isAction = action;
	}

	/** @return This property type's name */
	public String getName() {
		return theName;
	}

	/** @return The java type that this property type parses strings into instances of */
	public TypeToken<T> getType(){
		return theType;
	}

	/** @return Whether this property type is an action type */
	public boolean isAction() {
		return isAction;
	}

	/** @return A parser that knows how to parse property values of this type. May be null if the property type cannot parse itself. */
	public PropertySelfParser<T> getSelfParser() {
		return theParser;
	}

	/** @return Whether this property type parses its own values by default */
	public boolean isSelfParsingByDefault() {
		return isSelfParsingByDefault;
	}

	/** @return A function that generates reference names for values to be injected into a parsed value */
	public Function<Integer, String> getReferenceReplacementGenerator() {
		return theReferenceReplacementGenerator;
	}

	/** @return The expression context for this property type */
	public ExpressionContext getContext() {
		return theContext;
	}

	/**
	 * @param type The type to check
	 * @return Whether objects of the given type can be converted to items of this property's type
	 */
	public boolean canAccept(TypeToken<?> type) {
		return canConvert(type, theType);
	}

	/**
	 * @param type The type to check
	 * @return Whether items of this property's type can be converted to objects of the given type
	 */
	public boolean canConvertTo(TypeToken<?> type) {
		return canConvert(theType, type);
	}

	/**
	 * @param from The type to convert from
	 * @param to The type to convert to
	 * @return Whether this property knows how to convert from items of the from type to items of the to type
	 */
	public boolean canConvert(TypeToken<?> from, TypeToken<?> to) {
		if (QuickUtils.isAssignableFrom(from, to))
			return true;
		for (TypeMapping<?, ?> mapping : theMappings) {
			if (QuickUtils.isAssignableFrom(mapping.getFromType(), from) && QuickUtils.isAssignableFrom(to, mapping.getToType()))
				return true;
			else if (mapping.getReverseMap() != null && QuickUtils.isAssignableFrom(mapping.getToType(), from)
				&& QuickUtils.isAssignableFrom(to, mapping.getFromType()))
				return true;
		}
		return false;
	}

	/**
	 * Casts any object to an appropriate value of this type, or returns null if the given value cannot be interpreted as an instance of
	 * this property's type. This method may choose to convert liberally by creating new instances of this type corresponding to instances
	 * of other types, or it may choose to be conservative, only returning non-null for instances of this type.
	 *
	 * @param <X> The type of the value to be cast
	 * @param type The run-time type of the value to cast
	 * @param value The value to cast
	 * @return An instance of this type whose value matches the parameter in some sense, or null if the conversion cannot be made
	 * @throws QuickException If an exception occurs in a conversion that should succeed
	 */
	public <X> T cast(TypeToken<X> type, X value) throws QuickException {
		return convert(type, value, theType);
	}

	/**
	 * Casts an object of this type to an appropriate value of the given type, or returns null if the given value cannot be interpreted as
	 * an instance of the given type. This method may choose to convert liberally by creating new instances of this type corresponding to
	 * instances of other types, or it may choose to be conservative, only returning non-null for instances of this type.
	 *
	 * @param <X> The type of the value to cast to
	 * @param type The run-time type of the value to cast to
	 * @param value The value to cast
	 * @return An instance of the given type, or null if the conversion cannot be made
	 * @throws QuickException If an exception occurs in a conversion that should succeed
	 */
	public <X> X castTo(TypeToken<X> type, T value) throws QuickException {
		return convert(theType, value, type);
	}

	/**
	 * @param <F> The compile-time type to convert from
	 * @param <X> The compile-time type to convert to
	 * @param fromType The type to convert from
	 * @param value The value to convert
	 * @param toType The type to convert to
	 * @return The converted value, or null if the conversion could not be made
	 * @throws QuickException If an exception occurs in a conversion that should succeed
	 */
	public <F, X> X convert(TypeToken<F> fromType, F value, TypeToken<X> toType) throws QuickException {
		if (QuickUtils.isAssignableFrom(toType, fromType))
			return QuickUtils.convert(toType, value);
		for (TypeMapping<?, ?> mapping : theMappings) {
			if (QuickUtils.isAssignableFrom(mapping.getFromType(), fromType) && QuickUtils.isAssignableFrom(toType, mapping.getToType()))
				return doCast(mapping, value, toType);
			else if (mapping.getReverseMap() != null && QuickUtils.isAssignableFrom(mapping.getToType(), fromType)
				&& QuickUtils.isAssignableFrom(toType, mapping.getFromType()))
				return doReverseCast(mapping, value, toType);
		}
		return null;
	}

	private static <F, X> X doCast(TypeMapping<F, ?> mapping, Object value, TypeToken<X> toType) throws QuickException {
		F from = QuickUtils.convert(mapping.getFromType(), value);
		Object mapped = mapping.getMap().apply(from);
		return QuickUtils.convert(toType, mapped);
	}

	private static <F, X> X doReverseCast(TypeMapping<?, F> mapping, Object value, TypeToken<X> toType) throws QuickException {
		F from = QuickUtils.convert(mapping.getToType(), value);
		Object mapped = mapping.getReverseMap().apply(from);
		return QuickUtils.convert(toType, mapped);
	}

	/**
	 * Prints a value
	 *
	 * @param value The value to print
	 * @return The string representation of the value
	 */
	public String toString(T value) {
		if (thePrinter != null)
			return thePrinter.apply(value);
		else
			return String.valueOf(value);
	}

	@Override
	public String toString() {
		return theName;
	}

	/**
	 * Builds a value property
	 *
	 * @param <T> The compile-time type of the property
	 * @param name The name for the property type
	 * @param type The run-time type for the property type
	 * @return A builder for the property type
	 */
	public static <T> QuickPropertyType.Builder<T> build(String name, TypeToken<T> type) {
		return new Builder<>(name, type, false);
	}

	/**
	 * Builds an action property
	 *
	 * @param <T> The compile-time type of the property
	 * @param name The name for the property type
	 * @param type The run-time type for the property type
	 * @return A builder for the property type
	 */
	public static <T> QuickPropertyType.Builder<T> buildAction(String name, TypeToken<T> type) {
		return new Builder<>(name, type, true);
	}

	/**
	 * Builds {@link QuickPropertyType}s
	 *
	 * @param <T> The type of the property
	 */
	public static class Builder<T> {
		private final String theName;
		private final TypeToken<T> theType;
		private PropertySelfParser<T> theParser;
		private boolean isSelfParsingByDefault;
		private Function<Integer, String> theReferenceReplacementGenerator;
		private final List<TypeMapping<?, ?>> theMappings;
		private DefaultExpressionContext.Builder theCtxBuilder;
		private Function<? super T, String> thePrinter;
		private final boolean isAction;

		private Builder(String name, TypeToken<T> type, boolean action) {
			theName = name;
			theType = type;
			theMappings = new ArrayList<>();
			theCtxBuilder = DefaultExpressionContext.build();
			isAction = action;
		}

		/**
		 * @param parser The self-parser for the property type
		 * @param parseSelfByDefault Whether to parse our own properties by default
		 * @return This builder
		 */
		public Builder<T> withParser(PropertySelfParser<T> parser, boolean parseSelfByDefault) {
			theParser = parser;
			isSelfParsingByDefault = parseSelfByDefault;
			return this;
		}

		/**
		 * @param replacementGen The reference replacement generator for model value replacements in this property
		 * @return This builder
		 */
		public Builder<T> withRefReplaceGenerator(Function<Integer, String> replacementGen) {
			theReferenceReplacementGenerator = replacementGen;
			return this;
		}

		/**
		 * @param <F> The compile-time type of the value to convert from
		 * @param from The type of value to convert from
		 * @param map The function to convert values
		 * @param reverseMap The function to reverse the conversion. May be null.
		 * @return This builder
		 */
		public <F> Builder<T> map(TypeToken<F> from, ExFunction<? super F, ? extends T, QuickException> map,
			ExFunction<? super T, ? extends F, QuickException> reverseMap) {
			theMappings.add(new TypeMapping<>(from, theType, map, reverseMap));
			return this;
		}

		/**
		 * @param <X> The compile-time type of the value to convert to
		 * @param to The type of value to convert to
		 * @param map The function to convert values
		 * @param reverseMap The function to reverse the conversion. May be null.
		 * @return This builder
		 */
		public <X> Builder<T> mapTo(TypeToken<X> to, ExFunction<? super T, ? extends X, QuickException> map,
			ExFunction<? super X, ? extends T, QuickException> reverseMap) {
			theMappings.add(new TypeMapping<>(theType, to, map, reverseMap));
			return this;
		}

		/**
		 * @param <F> The compile-time type to convert from
		 * @param <X> The compile-time type to convert to
		 * @param from The type to convert from
		 * @param to The type to convert to
		 * @param map The mapping function
		 * @param reverseMap The reverse-mapping function. May be null.
		 * @return This builder
		 */
		public <F, X> Builder<T> mapTypes(TypeToken<F> from, TypeToken<X> to, ExFunction<? super F, ? extends X, QuickException> map,
			ExFunction<? super X, ? extends F, QuickException> reverseMap) {
			theMappings.add(new TypeMapping<>(from, to, map, reverseMap));
			return this;
		}

		/**
		 * @param toString The function to print property values
		 * @return This builder
		 */
		public Builder<T> withToString(Function<? super T, String> toString) {
			thePrinter = toString;
			return this;
		}

		/**
		 * @param builder A function to build up an expression context
		 * @return This builder
		 */
		public Builder<T> buildContext(Consumer<DefaultExpressionContext.Builder> builder) {
			builder.accept(theCtxBuilder);
			return this;
		}

		/** @return The new property type */
		public QuickPropertyType<T> build() {
			if (isSelfParsingByDefault && theParser == null)
				throw new IllegalArgumentException("Cannot parse self by default with no parser");
			return new QuickPropertyType<>(theName, theType, theParser, isSelfParsingByDefault, theReferenceReplacementGenerator,
				theMappings, thePrinter, theCtxBuilder.build(), isAction);
		}
	}

	/** The default property type for string-valued properties */
	public static final QuickPropertyType<String> string = QuickPropertyType.build("string", TypeToken.of(String.class))
		.withParser((parser, env, s) -> ObservableValue.constant(TypeToken.of(String.class), s), true)//
		.map(TypeToken.of(CharSequence.class), seq -> seq.toString(), s -> s)//
		.build();

	/** The default property type for boolean-valued properties */
	public static final QuickPropertyType<Boolean> boole = QuickPropertyType.build("boolean", TypeToken.of(Boolean.class)).build();

	/** The default property type for integer-valued properties */
	public static final QuickPropertyType<Integer> integer = QuickPropertyType.build("integer", TypeToken.of(Integer.class))
		.map(TypeToken.of(Number.class), num -> num.intValue(), i -> i)//
		.map(TypeToken.of(Long.class), l -> l.intValue(), i -> Long.valueOf(i))//
		.map(TypeToken.of(Character.class), c -> (int) c.charValue(), null)//
		.build();

	/** The default property type for floating-point-valued properties */
	public static final QuickPropertyType<Double> floating = QuickPropertyType.build("float", TypeToken.of(Double.class))
		.map(TypeToken.of(Number.class), num -> num.doubleValue(), d -> d)//
		.map(TypeToken.of(Long.class), l -> l.doubleValue(), null)//
		.map(TypeToken.of(Character.class), c -> (double) c.charValue(), null)//
		.build();

	/** The default property type for time-instant-valued properties */
	public static final QuickPropertyType<Instant> instant = QuickPropertyType.build("instant", TypeToken.of(Instant.class))
		.withParser((parser, env, s) -> ObservableValue.constant(TypeToken.of(Instant.class), parseInstant(s)), true)//
		.map(TypeToken.of(Long.class), l -> Instant.ofEpochMilli(l), inst -> inst.toEpochMilli())//
		.map(TypeToken.of(Date.class), date -> date.toInstant(), inst -> new Date(inst.toEpochMilli()))//
		.map(TypeToken.of(Calendar.class), cal -> cal.toInstant(), inst -> {
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(inst.toEpochMilli());
			return cal;
		})//
		.build();

	private static Instant parseInstant(String s) throws QuickParseException {
		try {
			return INSTANT_FORMAT.parse(s).toInstant();
		} catch (ParseException e) {
			throw new QuickParseException("Could not parse instant from " + s, e);
		}
	}

	/** All chronological units (h, m, s, etc.) supported by the {@link #duration} property type */
	public static final Map<String, ChronoUnit> SUPPORTED_CHRONO_UNITS;

	/** The default property type for time-duration-valued properties */
	public static final QuickPropertyType<Duration> duration;

	static {
		LinkedHashMap<String, ChronoUnit> chronoUnits = new LinkedHashMap<>();
		chronoUnits.put("y", ChronoUnit.YEARS);
		chronoUnits.put("M", ChronoUnit.MONTHS);
		chronoUnits.put("d", ChronoUnit.DAYS);
		chronoUnits.put("h", ChronoUnit.HOURS);
		chronoUnits.put("m", ChronoUnit.MINUTES);
		chronoUnits.put("s", ChronoUnit.SECONDS);
		chronoUnits.put("mi", ChronoUnit.MILLIS);
		chronoUnits.put("u", ChronoUnit.MICROS);
		chronoUnits.put("n", ChronoUnit.NANOS);
		SUPPORTED_CHRONO_UNITS = Collections.unmodifiableMap(chronoUnits);

		TypeToken<Duration> dType=TypeToken.of(Duration.class);
		Builder<Duration> durationBuilder = QuickPropertyType.build("duration", dType)//
			.withParser((parser, env, s) -> ObservableValue.constant(dType, parseDuration(s)), true)//
			.withToString(d -> toString(d))//
			.map(TypeToken.of(Long.class), l -> Duration.ofMillis(l), d -> d.toMillis());
		durationBuilder.buildContext(ctx -> {
			for (Map.Entry<String, ChronoUnit> unit : SUPPORTED_CHRONO_UNITS.entrySet())
				ctx.withUnit(unit.getKey(), TypeToken.of(Long.class), dType, l -> Duration.of(l, unit.getValue()),
					d -> d.get(unit.getValue()));
			ctx//
				.withFunction("+",
					ExpressionFunction.build(dType).withArgs(dType, dType)
						.withApply(args -> ((Duration) args.get(0)).plus((Duration) args.get(1))).build())//
				.withFunction("-",
					ExpressionFunction.build(dType).withArgs(dType, dType)
						.withApply(args -> ((Duration) args.get(0)).minus((Duration) args.get(1))).build())//
				.withFunction("*",
					ExpressionFunction.build(dType).withArgs(dType, TypeToken.of(Long.class))
						.withApply(args -> ((Duration) args.get(0)).multipliedBy((Long) args.get(1))).build())//
				.withFunction("*",
					ExpressionFunction.build(dType).withArgs(TypeToken.of(Long.class), dType)
						.withApply(args -> ((Duration) args.get(1)).multipliedBy((Long) args.get(0))).build())//
				.withFunction("/", ExpressionFunction.build(dType).withArgs(dType, TypeToken.of(Long.class))
					.withApply(args -> ((Duration) args.get(0)).dividedBy((Long) args.get(1))).build())//
			;
		});//
		duration = durationBuilder.build();
	}

	private static Duration parseDuration(String s) throws QuickParseException {
		String[] split = s.split("\\s+");
		if (split.length == 0)
			throw new QuickParseException("No duration in value: " + s);
		Duration res = Duration.ZERO;
		Pattern dPat = Pattern.compile("(\\d+)([a-zA-Z]+)");
		for (String sp : split) {
			Matcher match = dPat.matcher(sp);
			if (!match.matches())
				throw new QuickParseException("Unrecognized duration: " + sp);
			ChronoUnit unit = SUPPORTED_CHRONO_UNITS.get(match.group(2));
			if (unit == null)
				throw new QuickParseException("Unsupported duration unit: " + match.group(2));
			res = res.plus(Duration.of(Long.parseLong(match.group(1)), unit));
		}
		return res;
	}

	private static String toString(Duration d) {
		StringBuilder str = new StringBuilder();
		if (d.isNegative()) {
			str.append('-');
			d = d.abs();
		}
		boolean hasValue = false;
		for (Map.Entry<String, ChronoUnit> unit : SUPPORTED_CHRONO_UNITS.entrySet()) {
			long unitValue = d.get(unit.getValue());
			if (unitValue > 0) {
				if (hasValue)
					str.append(' ');
				hasValue = true;
				str.append(unitValue).append(unit.getKey());
				d = d.minus(Duration.of(unitValue, unit.getValue()));
			}
		}
		if (!hasValue)
			str.append(0).append("s");
		return str.toString();
	}

	/** The default property type for color-valued properties */
	public static final QuickPropertyType<Color> color;

	static {
		Builder<Color> colorBuilder = QuickPropertyType.build("color", TypeToken.of(Color.class))//
			.withParser((parser, env, s) -> ObservableValue.constant(TypeToken.of(Color.class), Colors.parseColor(s)), true)//
			.withToString(c -> Colors.toString(c))//
			.buildContext(ctx -> {
				ctx//
					.withValueGetter(name -> {
						Color namedColor = Colors.NAMED_COLORS.get(name);
						return namedColor == null ? null : ObservableValue.constant(TypeToken.of(Color.class), namedColor);
					})//
					.withFunction("rgb", ExpressionFunction.build(new TriFunction<Integer, Integer, Integer, Color>() {
						@Override
						public Color apply(Integer r, Integer g, Integer b) {
							return new Color(r, g, b);
						}
					}))//
					.withFunction("hsb", ExpressionFunction.build(new TriFunction<Float, Float, Float, Color>() {
						@Override
						public Color apply(Float h, Float s, Float b) {
							return Color.getHSBColor(h, s, b);
						}
					}))//
					.withFunction("brighten", ExpressionFunction.build(new BiFunction<Color, Float, Color>() {
						@Override
						public Color apply(Color c, Float b) {
							return ColorUtils.bleach(c, b);
						}
					}))//
					.withFunction("darken", ExpressionFunction.build(new BiFunction<Color, Float, Color>() {
						@Override
						public Color apply(Color c, Float d) {
							return ColorUtils.stain(c, d);
						}
					}));
			});

		color = colorBuilder.build();
	}

	/**
	 * Creates a property type for parsing values of an enum
	 *
	 * @param <T> The compile-time type of the enum
	 * @param enumType The enum class
	 * @return The property type for the enum
	 */
	public static final <T extends Enum<T>> QuickPropertyType<T> forEnum(Class<T> enumType) {
		TypeToken<T> typeToken = TypeToken.of(enumType);
		Map<String, T> byName = new HashMap<>();
		for (T value : enumType.getEnumConstants())
			byName.put(QuickUtils.javaToXML(value.name()), value);
		/* These functions used to be anonymous, but they where causing InternalErrors when TypeToken tried to resolve the return type.
		 * (Specifically, the error happened when getGenericInterfaces() was called on the function class.  I tried to replicate this in a
		 * unit test, but wasn't able to trigger the error.)  When they're not anonymous, things work. */
		class EnumValueOfName implements Function<String, T> {
			@Override
			public T apply(String s) {
				T v = byName.get(s);
				if (v == null)
					throw new IllegalArgumentException("No such " + enumType.getSimpleName() + " " + s);
				return v;
			}
		}
		class EnumName implements Function<T, String> {
			@Override
			public String apply(T v) {
				return QuickUtils.javaToXML(v.name());
			}
		}
		class EnumOrdinal implements Function<T, Integer> {
			@Override
			public Integer apply(T v) {
				return v.ordinal();
			}
		}
		class EnumValueOfOrdinal implements Function<Integer, T> {
			@Override
			public T apply(Integer ord) {
				T[] vals = enumType.getEnumConstants();
				if (ord < 0 || ord >= vals.length)
					throw new IllegalArgumentException("No such " + enumType.getSimpleName() + " at " + ord);
				return vals[ord];
			}
		}
		class EnumNext implements Function<T, T> {
			@Override
			public T apply(T v) {
				T[] vals = enumType.getEnumConstants();
				if (v.ordinal() == vals.length - 1)
					throw new IllegalArgumentException("No next value for " + QuickUtils.javaToXML(v.name()));
				return vals[v.ordinal() + 1];
			}
		}
		class EnumPrevious implements Function<T, T> {
			@Override
			public T apply(T v) {
				if (v.ordinal() == 0)
					throw new IllegalArgumentException("No previous value for " + QuickUtils.javaToXML(v.name()));
				T[] vals = enumType.getEnumConstants();
				return vals[v.ordinal() - 1];
			}
		}
		Builder<T> builder = build("enum " + enumType.getName(), TypeToken.of(enumType))//
			.withToString(v -> QuickUtils.javaToXML(v.name()))//
			.buildContext(ctx -> {
				ctx//
					.withValueGetter(s -> {
						T enumValue = byName.get(s);
						return enumValue == null ? null : ObservableValue.constant(typeToken, enumValue);
					})//
					.withValue("numConsts", ObservableValue.constant(TypeToken.of(Integer.TYPE), enumType.getEnumConstants().length))//
					.withFunction("valueOf", ExpressionFunction.build(new EnumValueOfName()))//
					.withFunction("name", ExpressionFunction.build(new EnumName()))//
					.withFunction("ordinal", ExpressionFunction.build(new EnumOrdinal()))//
					.withFunction("valueOf", ExpressionFunction.build(new EnumValueOfOrdinal()))//
					.withFunction("next", ExpressionFunction.build(new EnumNext()))//
					.withFunction("previous", ExpressionFunction.build(new EnumPrevious()));
			});
		return builder.build();
	}

	/**
	 * Creates a property type that parses a java type
	 *
	 * @param <T> The compile-time type of the super class of the type to produce
	 * @param type The super class of the type to produce
	 * @return The property type
	 */
	public static final <T> QuickPropertyType<Class<? extends T>> forType(Class<T> type) {
		TypeToken<Class<? extends T>> typeToken = new TypeToken<Class<? extends T>>() {}.where(new TypeParameter<T>() {},
			TypeToken.of(type));
		return build(type.getName() + " type", typeToken)//
			.withParser((parser, env, s) -> {
				Class<?> res;
				try {
					res = env.cv().loadIfMapped(s, type);
				} catch (QuickException e) {
					throw new QuickParseException("Could not load type " + s, e);
				}
				if (res == null) {
					try {
						res = env.cv().loadClass(s);
					} catch (ClassNotFoundException e) {
						throw new QuickParseException("Could not load type " + s, e);
					}
				}
				if (!type.isAssignableFrom(res))
					throw new QuickParseException("Type " + s + " is not compatible with type " + type.getName());
				return ObservableValue.constant(typeToken, type.asSubclass(type));
			}, true)//
			.build();
	}

	/**
	 * Creates a property type that instantiates a type
	 *
	 * @param <T> The compile-time type of the super class of the type of values to produce
	 * @param type The super class of the type of values to produce
	 * @return The property type
	 */
	public static final <T> QuickPropertyType<T> forTypeInstance(Class<T> type) {
		return build(type.getName(), TypeToken.of(type))//
			.withParser((parser, env, s) -> {
				Class<?> res;
				try {
					res = env.cv().loadIfMapped(s, type);
				} catch (QuickException e) {
					throw new QuickParseException("Could not load type " + s, e);
				}
				if (res == null) {
					try {
						res = env.cv().loadClass(s);
					} catch (ClassNotFoundException e) {
						throw new QuickParseException("Could not load type " + s, e);
					}
				}
				String name = s.equals(res.getName()) ? s : (s + "(" + res.getName() + ")");
				if (!type.isAssignableFrom(res))
					throw new QuickParseException("Type " + name + " is not compatible with type " + type.getName());
				Constructor<?> ctor;
				try {
					ctor = res.getConstructor(new Class[0]);
				} catch (NoSuchMethodException e) {
					throw new QuickParseException("No default constructor for type " + name, e);
				} catch (SecurityException e) {
					throw new QuickParseException("Cannot access default constructor for type " + name, e);
				}
				try {
					return ObservableValue.constant(TypeToken.of(type), (T) ctor.newInstance());
				} catch (InstantiationException e) {
					throw new QuickParseException("Cannot instantiate type " + name, e);
				} catch (InvocationTargetException | IllegalAccessException e) {
					throw new QuickParseException("Could not instantiate type " + name, e);
				}
			}, true)//
			.build();
	}

	/** The default property type for QuickElement-type-valued properties */
	public static final QuickPropertyType<Class<? extends QuickElement>> elementType = forType(QuickElement.class);

	/** The default property type for QuickElement-valued properties */
	public static final QuickPropertyType<QuickElement> element = forTypeInstance(QuickElement.class);

	/**
	 * A resource property--values may be:
	 * <ul>
	 * <li>namespace:tag, where <code>tag</code> maps to a resource in the <code>namespace</code> toolkit</li>
	 * <li>A relative path to a resource that may be resolved from the element's toolkit. A <code>namespace:</code> prefix may be used to
	 * specify a different toolkit</li>
	 * <li>An absolute URL. Permissions will be checked before resources at any external URLs are retrieved. TODO cite specific permission.
	 * </li>
	 * </ul>
	 */
	public static final QuickPropertyType<URL> resource = build("resource", TypeToken.of(URL.class))// TODO
		.withParser((parser, env, s) -> {
			int sepIdx = s.indexOf(':');
			String namespace = sepIdx < 0 ? null : s.substring(0, sepIdx);
			String content = sepIdx < 0 ? s : s.substring(sepIdx + 1);
			QuickToolkit toolkit = env.cv().getToolkit(namespace);
			if (toolkit == null)
				try {
					return ObservableValue.constant(TypeToken.of(URL.class), new URL(s));
				} catch (java.net.MalformedURLException e) {
					throw new QuickParseException("resource: Resource property is not a valid URL: \"" + s + "\"", e);
				}
			String mapped = toolkit.getMappedResource(content);
			if (mapped == null)
				throw new QuickParseException("resource: Resource property must map to a declared resource: \"" + content + "\" in toolkit "
					+ toolkit.getName() + " or one of its dependencies");
			if (mapped.contains(":"))
				try {
					return ObservableValue.constant(TypeToken.of(URL.class), new URL(mapped));
				} catch (java.net.MalformedURLException e) {
					throw new QuickParseException("resource: Resource property maps to an invalid URL \"" + mapped + "\" in toolkit "
						+ toolkit.getName() + ": \"" + content + "\"");
				}
			try {
				return ObservableValue.constant(TypeToken.of(URL.class), QuickUtils.resolveURL(toolkit.getURI(), mapped));
			} catch (QuickException e) {
				throw new QuickParseException("resource: Resource property maps to a resource (" + mapped
					+ ") that cannot be resolved with respect to toolkit \"" + toolkit.getName() + "\"'s URL: \"" + content + "\"");
			}
		}, true)//
		.build();
}