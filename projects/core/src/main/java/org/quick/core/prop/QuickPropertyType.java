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
 * A property type understands how to produce items of a certain type from parseable strings and other types
 *
 * @param <T> The type of value that this property type produces TODO Get rid of all the V types
 */
public final class QuickPropertyType<T> {
	private static final SimpleDateFormat INSTANT_FORMAT = new SimpleDateFormat("ddMMMyyyy HH:mm:ss");

	public static class TypeMapping<F, T> {
		final TypeToken<F> from;

		final TypeToken<T> to;

		final ExFunction<? super F, ? extends T, QuickException> map;

		TypeMapping(TypeToken<F> from, TypeToken<T> to, ExFunction<? super F, ? extends T, QuickException> map) {
			this.from = from;
			this.to = to;
			this.map = map;
		}
	}

	public static class Unit<F, T> extends QuickPropertyType.TypeMapping<F, T> {
		public final String name;

		public Unit(String name, TypeToken<F> from, TypeToken<T> to, ExFunction<? super F, ? extends T, QuickException> operator) {
			super(from, to, operator);
			this.name = name;
		}
	}

	public interface PropertySelfParser<T> {
		ObservableValue<T> parse(QuickPropertyParser parser, QuickParseEnv parseEnv, String value) throws QuickParseException;
	}

	private final String theName;
	private final TypeToken<T> theType;
	private final PropertySelfParser<T> theParser;
	private final boolean isSelfParsingByDefault;
	private final Function<Integer, String> theReferenceReplacementGenerator;
	private final List<TypeMapping<?, T>> theMappings;
	private final List<Function<String, ?>> theValueSuppliers;
	private final List<Unit<?, ?>> theUnits;
	private final ExpressionContext theContext;
	private final Function<? super T, String> thePrinter;

	private QuickPropertyType(String name, TypeToken<T> type, PropertySelfParser<T> parser, boolean parseSelfByDefault,
		Function<Integer, String> replacementGen, List<TypeMapping<?, T>> mappings, List<Function<String, ?>> valueSuppliers,
		List<Unit<?, ?>> units, Function<? super T, String> printer, ExpressionContext ctx) {
		theName = name;
		theType = type;
		theParser = parser;
		isSelfParsingByDefault = parseSelfByDefault;
		theReferenceReplacementGenerator = replacementGen;
		theMappings = Collections.unmodifiableList(new ArrayList<>(mappings));
		theValueSuppliers = Collections.unmodifiableList(new ArrayList<>(valueSuppliers));
		theUnits = Collections.unmodifiableList(new ArrayList<>(units));
		theContext = ctx;
		thePrinter = printer;
	}

	/** @return This property type's name */
	public String getName() {
		return theName;
	}

	/** @return The java type that this property type parses strings into instances of */
	public TypeToken<T> getType(){
		return theType;
	}

	/** @return A parser that knows how to parse property values of this type. May be null if the property type cannot parse itself. */
	public PropertySelfParser<T> getSelfParser() {
		return theParser;
	}

	public boolean isSelfParsingByDefault() {
		return isSelfParsingByDefault;
	}

	public Function<Integer, String> getReferenceReplacementGenerator() {
		return theReferenceReplacementGenerator;
	}

	public List<Function<String, ?>> getValueSuppliers() {
		return theValueSuppliers;
	}

	public List<Unit<?, ?>> getUnits() {
		return theUnits;
	}

	public ExpressionContext getContext() {
		return theContext;
	}

	/**
	 * @param type The type to check
	 * @return Whether objects of the given type can be converted to items of this property's type
	 */
	public boolean canAccept(TypeToken<?> type) {
		if(theType.isAssignableFrom(type))
			return true;
		for (QuickPropertyType.TypeMapping<?, T> mapping : theMappings)
			if(mapping.from.isAssignableFrom(type))
				return true;
		return false;
	}

	/**
	 * Casts any object to an appropriate value of this type, or returns null if the given value cannot be interpreted as an instance of
	 * this property's type. This method may choose to convert liberally by creating new instances of this type corresponding to instances
	 * of other types, or it may choose to be conservative, only returning non-null for instances of this type.
	 *
	 * @param <X> The type of the value to be cast
	 * @param <V> The type of value cast by this property type
	 * @param type The run-time type of the value to cast
	 * @param value The value to cast
	 * @return An instance of this type whose value matches the parameter in some sense, or null if the conversion cannot be made
	 * @throws QuickException If an exception occurs in a conversion that should succeed
	 */
	public <X, V extends T> V cast(TypeToken<X> type, X value) throws QuickException {
		V cast = null;
		if (QuickUtils.isAssignableFrom(theType, type))
			cast = (V) value;
		boolean mappingFound = false;
		for (QuickPropertyType.TypeMapping<?, T> mapping : theMappings)
			if (QuickUtils.isAssignableFrom(mapping.from, type)) {
				mappingFound = true;
				cast = ((QuickPropertyType.TypeMapping<? super X, V>) mapping).map.apply(value);
			}
		if(!mappingFound)
			return null;
		return cast;
	}

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

	public static <T> QuickPropertyType.Builder<T> build(String name, TypeToken<T> type) {
		return new Builder<>(name, type);
	}

	public static class Builder<T> {
		private final String theName;
		private final TypeToken<T> theType;
		private PropertySelfParser<T> theParser;
		private boolean isSelfParsingByDefault;
		private Function<Integer, String> theReferenceReplacementGenerator;
		private final List<TypeMapping<?, T>> theMappings;
		private final List<Function<String, ?>> theValueSuppliers;
		private final List<Unit<?, ?>> theUnits;
		private DefaultExpressionContext.Builder theCtxBuilder;
		private Function<? super T, String> thePrinter;

		private Builder(String name, TypeToken<T> type) {
			theName = name;
			theType = type;
			theMappings = new ArrayList<>();
			theValueSuppliers = new ArrayList<>();
			theUnits = new ArrayList<>();
			theCtxBuilder = DefaultExpressionContext.build();
		}

		public Builder<T> withParser(PropertySelfParser<T> parser, boolean parseSelfByDefault) {
			theParser = parser;
			isSelfParsingByDefault = parseSelfByDefault;
			return this;
		}

		public Builder<T> withRefReplaceGenerator(Function<Integer, String> replacementGen) {
			theReferenceReplacementGenerator = replacementGen;
			return this;
		}

		public <F> Builder<T> map(TypeToken<F> from, ExFunction<? super F, ? extends T, QuickException> map) {
			theMappings.add(new TypeMapping<>(from, theType, map));
			return this;
		}

		public Builder<T> withValues(Function<String, ?> values) {
			theValueSuppliers.add(values);
			return this;
		}

		public <F, T2> Builder<T> withUnit(String name, TypeToken<F> from, TypeToken<T2> to,
			ExFunction<? super F, ? extends T2, QuickException> operator) {
			theUnits.add(new Unit<>(name, from, to, operator));
			return this;
		}

		public Builder<T> withToString(Function<? super T, String> toString) {
			thePrinter = toString;
			return this;
		}

		public Builder<T> buildContext(Consumer<DefaultExpressionContext.Builder> builder) {
			builder.accept(theCtxBuilder);
			return this;
		}

		public QuickPropertyType<T> build() {
			if (isSelfParsingByDefault && theParser == null)
				throw new IllegalArgumentException("Cannot parse self by default with no parser");
			return new QuickPropertyType<>(theName, theType, theParser, isSelfParsingByDefault, theReferenceReplacementGenerator,
				theMappings, theValueSuppliers, theUnits, thePrinter, theCtxBuilder.build());
		}
	}

	public static final QuickPropertyType<String> string = QuickPropertyType.build("string", TypeToken.of(String.class))
		.withParser((parser, env, s) -> ObservableValue.constant(TypeToken.of(String.class), s), true)//
		.map(TypeToken.of(CharSequence.class), seq -> seq.toString())//
		.build();

	public static final QuickPropertyType<Boolean> boole = QuickPropertyType.build("boolean", TypeToken.of(Boolean.class)).build();

	public static final QuickPropertyType<Integer> integer = QuickPropertyType.build("integer", TypeToken.of(Integer.class))
		.map(TypeToken.of(Number.class), num -> num.intValue())//
		.map(TypeToken.of(Long.class), l -> l.intValue())//
		.map(TypeToken.of(Character.class), c -> (int) c.charValue())//
		.build();

	public static final QuickPropertyType<Double> floating = QuickPropertyType.build("float", TypeToken.of(Double.class))
		.map(TypeToken.of(Number.class), num -> num.doubleValue())//
		.map(TypeToken.of(Long.class), l -> l.doubleValue())//
		.map(TypeToken.of(Character.class), c -> (double) c.charValue())//
		.build();

	public static final QuickPropertyType<Instant> instant = QuickPropertyType.build("instant", TypeToken.of(Instant.class))
		.withParser((parser, env, s) -> ObservableValue.constant(TypeToken.of(Instant.class), parseInstant(s)), true)//
		.map(TypeToken.of(Long.class), l -> Instant.ofEpochMilli(l))//
		.map(TypeToken.of(Date.class), date -> date.toInstant())//
		.map(TypeToken.of(Calendar.class), cal -> cal.toInstant())//
		.build();

	private static Instant parseInstant(String s) throws QuickParseException {
		try {
			return INSTANT_FORMAT.parse(s).toInstant();
		} catch (ParseException e) {
			throw new QuickParseException("Could not parse instant from " + s, e);
		}
	}

	public static final Map<String, ChronoUnit> SUPPORTED_CHRONO_UNITS;

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

		Builder<Duration> durationBuilder = QuickPropertyType.build("duration", TypeToken.of(Duration.class))//
			.withParser((parser, env, s) -> ObservableValue.constant(TypeToken.of(Duration.class), parseDuration(s)), true)//
			.withToString(d -> toString(d))//
			.map(TypeToken.of(Long.class), l -> Duration.ofMillis(l));
		for (Map.Entry<String, ChronoUnit> unit : SUPPORTED_CHRONO_UNITS.entrySet()) {
			durationBuilder.withUnit(unit.getKey(), TypeToken.of(Long.class), TypeToken.of(Duration.class),
				l -> Duration.of(l, unit.getValue()));
		}
		durationBuilder.buildContext(ctx -> {
			ctx//
				.withFunction("+", ExpressionFunction.build(new BiFunction<Duration, Duration, Duration>() {
					@Override
					public Duration apply(Duration t, Duration u) {
						return t.plus(u);
					}
				}))//
				.withFunction("-", ExpressionFunction.build(new BiFunction<Duration, Duration, Duration>() {
					@Override
					public Duration apply(Duration t, Duration u) {
						return t.minus(u);
					}
				}))//
				.withFunction("*", ExpressionFunction.build(new BiFunction<Duration, Long, Duration>() {
					@Override
					public Duration apply(Duration t, Long u) {
						return t.multipliedBy(u);
					}
				}))//
				.withFunction("*", ExpressionFunction.build(new BiFunction<Long, Duration, Duration>() {
					@Override
					public Duration apply(Long t, Duration u) {
						return u.multipliedBy(t);
					}
				}))//
				.withFunction("/", ExpressionFunction.build(new BiFunction<Duration, Long, Duration>() {
					@Override
					public Duration apply(Duration t, Long u) {
						return t.dividedBy(u);
					}
				}));//
		});//
		duration = durationBuilder.build();
	}

	private static Duration parseDuration(String s) throws QuickParseException {
		String[] split = s.split("\\w+");
		if (split.length == 0)
			throw new QuickParseException("No duration in value");
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

	public static final QuickPropertyType<Color> color;

	static {
		Builder<Color> colorBuilder = QuickPropertyType.build("color", TypeToken.of(Color.class))//
			.withParser((parser, env, s) -> ObservableValue.constant(TypeToken.of(Color.class), Colors.parseColor(s)), true)//
			.withToString(c -> Colors.toString(c))//
			.withValues(name -> {
				return Colors.NAMED_COLORS.get(name);
			})//
			.buildContext(ctx -> {
				ctx//
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

	public static final <T extends Enum<T>> QuickPropertyType<T> forEnum(Class<T> enumType) {
		Map<String, T> byName = new HashMap<>();
		for (T value : enumType.getEnumConstants())
			byName.put(QuickUtils.javaToXML(value.name()), value);
		Builder<T> builder = build("enum " + enumType.getName(), TypeToken.of(enumType))//
			.withToString(v -> QuickUtils.javaToXML(v.name()))//
			.withValues(s -> byName.get(s))//
			.withValues(s -> s.equals("numConsts") ? Integer.valueOf(enumType.getEnumConstants().length) : null)//
			.buildContext(ctx -> {
				ctx//
					.withFunction("valueOf", ExpressionFunction.build(new Function<String, T>() {
						@Override
						public T apply(String s) {
							T v = byName.get(s);
							if (v == null)
								throw new IllegalArgumentException("No such " + enumType.getSimpleName() + " " + s);
							return v;
						}
					})).withFunction("name", ExpressionFunction.build(new Function<T, String>() {
						@Override
						public String apply(T v) {
							return QuickUtils.javaToXML(v.name());
						}
					})).withFunction("ordinal", ExpressionFunction.build(new Function<T, Integer>() {
						@Override
						public Integer apply(T v) {
							return v.ordinal();
						}
					})).withFunction("valueOf", ExpressionFunction.build(new Function<Integer, T>() {
						@Override
						public T apply(Integer ord) {
							T[] vals = enumType.getEnumConstants();
							if (ord < 0 || ord >= vals.length)
								throw new IllegalArgumentException("No such " + enumType.getSimpleName() + " at " + ord);
							return vals[ord];
						}
					})).withFunction("next", ExpressionFunction.build(new Function<T, T>() {
						@Override
						public T apply(T v) {
							T[] vals = enumType.getEnumConstants();
							if (v.ordinal() == vals.length - 1)
								throw new IllegalArgumentException("No next value for " + QuickUtils.javaToXML(v.name()));
							return vals[v.ordinal() + 1];
						}
					})).withFunction("previous", ExpressionFunction.build(new Function<T, T>() {
						@Override
						public T apply(T v) {
							if (v.ordinal() == 0)
								throw new IllegalArgumentException("No previous value for " + QuickUtils.javaToXML(v.name()));
							T[] vals = enumType.getEnumConstants();
							return vals[v.ordinal() - 1];
						}
					}));
			});
		return builder.build();
	}

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

	public static final QuickPropertyType<Class<? extends QuickElement>> elementType = forType(QuickElement.class);

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