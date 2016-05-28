package org.quick.core.prop;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.reflect.TypeToken;

/**
 * A property type understands how to produce items of a certain type from parseable strings and other types
 *
 * @param <T> The type of value that this property type produces TODO Get rid of all the V types
 */
public final class QuickPropertyType<T> {
	public static class TypeMapping<F, T> {
		final TypeToken<F> from;

		final TypeToken<T> to;

		final Function<? super F, ? extends T> map;

		TypeMapping(TypeToken<F> from, TypeToken<T> to, Function<? super F, ? extends T> map) {
			this.from = from;
			this.to = to;
			this.map = map;
		}
	}

	public static class Unit<F, T> extends QuickPropertyType.TypeMapping<F, T> {
		public final String name;

		public Unit(String name, TypeToken<F> from, TypeToken<T> to, Function<? super F, ? extends T> operator) {
			super(from, to, operator);
			this.name = name;
		}
	}

	private final TypeToken<T> theType;
	private final List<QuickPropertyType.TypeMapping<?, T>> theMappings;
	private final List<Function<String, ?>> theValueSuppliers;
	private final List<QuickPropertyType.Unit<?, ?>> theUnits;
	private final ExpressionContext theContext;
	private final Function<? super T, String> thePrinter;

	private QuickPropertyType(TypeToken<T> type, List<QuickPropertyType.TypeMapping<?, T>> mappings,
		List<Function<String, ?>> valueSuppliers, List<QuickPropertyType.Unit<?, ?>> units, Function<? super T, String> printer,
		ExpressionContext ctx) {
		theType = type;
		theMappings = Collections.unmodifiableList(new ArrayList<>(mappings));
		theValueSuppliers = Collections.unmodifiableList(new ArrayList<>(valueSuppliers));
		theUnits = Collections.unmodifiableList(new ArrayList<>(units));
		theContext = ctx;
		thePrinter = printer;
	}

	/** @return The java type that this property type parses strings into instances of */
	public TypeToken<T> getType(){
		return theType;
	}

	/**
	 * @param type The type to check
	 * @return Whether objects of the given type can be converted to items of this property's type
	 */
	public boolean canAccept(TypeToken<?> type) {
		if(theType.isAssignableFrom(type))
			return true;
		for(QuickPropertyType.TypeMapping<?, T> mapping : theMappings)
			if(mapping.from.isAssignableFrom(type))
				return true;
		return false;
	}

	/**
	 * Casts any object to an appropriate value of this type, or returns null if the given value cannot be interpreted as an instance of
	 * this property's type. This method may choose to convert liberally by creating new instances of this type corresponding to
	 * instances of other types, or it may choose to be conservative, only returning non-null for instances of this type.
	 *
	 * @param <X> The type of the value to be cast
	 * @param <V> The type of value cast by this property type
	 * @param type The run-time type of the value to cast
	 * @param value The value to cast
	 * @return An instance of this type whose value matches the parameter in some sense, or null if the conversion cannot be made
	 */
	public <X, V extends T> V cast(TypeToken<X> type, X value) {
		V cast = null;
		if (isAssignableFrom(theType, type))
			cast = (V) value;
		boolean mappingFound = false;
		for(QuickPropertyType.TypeMapping<?, T> mapping : theMappings)
			if (isAssignableFrom(mapping.from, type)) {
				mappingFound = true;
				cast = ((QuickPropertyType.TypeMapping<? super X, V>) mapping).map.apply(value);
			}
		if(!mappingFound)
			return null;
		return cast;
	}

	public static boolean isAssignableFrom(TypeToken<?> left, TypeToken<?> right) {
		if (left.isAssignableFrom(right))
			return true;
		// TODO Handle primitive conversions
		else if (left.isPrimitive() && left.wrap().isAssignableFrom(right))
			return true;
		return false;
	}

	public static <T> QuickPropertyType.Builder<T> build(TypeToken<T> type) {
		return new Builder<>(type);
	}

	public static class Builder<T> {
		private final TypeToken<T> theType;
		private final List<QuickPropertyType.TypeMapping<?, T>> theMappings;
		private final List<Function<String, ?>> theValueSuppliers;
		private final List<QuickPropertyType.Unit<?, ?>> theUnits;
		private DefaultExpressionContext.Builder theCtxBuilder;
		private Function<? super T, String> thePrinter;

		private Builder(TypeToken<T> type) {
			theType = type;
			theMappings = new ArrayList<>();
			theValueSuppliers = new ArrayList<>();
			theUnits = new ArrayList<>();
			theCtxBuilder = DefaultExpressionContext.build();
		}

		public <F> Builder<T> map(TypeToken<F> from, Function<? super F, ? extends T> map) {
			theMappings.add(new QuickPropertyType.TypeMapping<>(from, theType, map));
			return this;
		}

		public Builder<T> withValues(Function<String, ?> values) {
			theValueSuppliers.add(values);
			return this;
		}

		public <F, T2> Builder<T> withUnit(String name, TypeToken<F> from, TypeToken<T2> to,
			Function<? super F, ? extends T2> operator) {
			theUnits.add(new QuickPropertyType.Unit<>(name, from, to, operator));
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
			return new QuickPropertyType<>(theType, theMappings, theValueSuppliers, theUnits, thePrinter, theCtxBuilder.build());
		}
	}

	public static final QuickPropertyType<String> string = QuickPropertyType.build(TypeToken.of(String.class))
		.map(TypeToken.of(CharSequence.class), seq -> seq.toString())//
		.build();

	public static final QuickPropertyType<Boolean> boole = QuickPropertyType.build(TypeToken.of(Boolean.class)).build();

	public static final QuickPropertyType<Integer> integer = QuickPropertyType.build(TypeToken.of(Integer.class))
		.map(TypeToken.of(Number.class), num -> num.intValue())//
		.map(TypeToken.of(Long.class), l -> l.intValue())//
		.map(TypeToken.of(Character.class), c -> (int) c.charValue())//
		.build();

	public static final QuickPropertyType<Double> floating = QuickPropertyType.build(TypeToken.of(Double.class))
		.map(TypeToken.of(Number.class), num -> num.doubleValue())//
		.map(TypeToken.of(Long.class), l -> l.doubleValue())//
		.map(TypeToken.of(Character.class), c -> (double) c.charValue())//
		.build();

	public static final QuickPropertyType<Instant> instant = QuickPropertyType.build(TypeToken.of(Instant.class))
		.map(TypeToken.of(Long.class), l -> Instant.ofEpochMilli(l))//
		.map(TypeToken.of(Date.class), date -> date.toInstant())//
		.map(TypeToken.of(Calendar.class), cal -> cal.toInstant())//
		.build();

	public static final QuickPropertyType<Duration> duration = QuickPropertyType.build(TypeToken.of(Duration.class))
		.map(TypeToken.of(Long.class), l -> Duration.ofMillis(l))//
		.withUnit("y", TypeToken.of(Long.class), TypeToken.of(Duration.class), l -> Duration.of(l, ChronoUnit.YEARS))//
		.build();
}