package org.muis.core;

import java.awt.Color;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import org.muis.core.eval.impl.ObservableEvaluator;
import org.muis.core.eval.impl.ParsedColor;
import org.muis.core.parser.MuisParseException;
import org.muis.core.parser.WrappingObservableEvaluator;
import org.muis.core.parser.WrappingPrismsParser;
import org.muis.core.rx.ObservableValue;
import org.muis.core.style.Colors;
import org.muis.util.MuisUtils;

import prisms.arch.PrismsConfig;
import prisms.lang.*;
import prisms.lang.types.ParsedFunctionDeclaration;

/**
 * Represents a property in MUIS
 *
 * @param <T> The type of values that may be associated with the property
 */
public abstract class MuisProperty<T> {
	/**
	 * A property type understands how to produce items of a certain type from parseable strings and other types
	 *
	 * @param <T> The type of value that this property type produces TODO Get rid of all the V types
	 */
	public static interface PropertyType<T> {
		/** @return The java type that this property type parses strings into instances of */
		Type getType();

		/**
		 * Parses an property value from a string representation
		 *
		 * @param env The parsing environment
		 * @param value The string representation to parse
		 * @return The parsed property value
		 * @throws MuisException If a fatal parsing error occurs
		 */
		ObservableValue<? extends T> parse(MuisParseEnv env, String value) throws MuisException;

		/**
		 * Casts any object to an appropriate value of this type, or returns null if the given value cannot be interpreted as an instance of
		 * this property's type. This method may choose to convert liberally by creating new instances of this type corresponding to
		 * instances of other types, or it may choose to be conservative, only returning non-null for instances of this type.
		 *
		 * @param <V> The type of value cast by this property type
		 * @param value The value to cast
		 * @return An instance of this type whose value matches the parameter in some sense, or null if the conversion cannot be made
		 */
		<V extends T> V cast(Object value);
	}

	/**
	 * An extension of PropertyType that knows how to convert values of the type back to strings for printing
	 *
	 * @param <T> The type of the value that this property type produces
	 */
	public static interface PrintablePropertyType<T> extends PropertyType<T> {
		/**
		 * @param value The value to print
		 * @return The user-friendly printed value
		 */
		public String toString(T value);
	}

	/**
	 * A property validator places constraints on the value of a property
	 *
	 * @param <T> The type of value that this validator can validate
	 */
	public static interface PropertyValidator<T> {
		/**
		 * @param value The value to check
		 * @return Whether the value is valid by this validator's constraints
		 */
		boolean isValid(T value);

		/**
		 * @param value The value to check
		 * @throws MuisException If the value was not valid by this validator's constraints. The message in this exception will be as
		 *             descriptive and user-friendly as possible.
		 */
		void assertValid(T value) throws MuisException;
	}

	/**
	 * An abstract class to help {@link MuisProperty.PropertyType}s and {@link MuisProperty.PropertyValidator}s generate better error
	 * messages
	 *
	 * @param <T> The type of property that this helper is for
	 */
	public static abstract class PropertyHelper<T> {
		private MuisProperty<T> theProperty;

		/** @return The property that this helper is for */
		public MuisProperty<T> getProperty() {
			return theProperty;
		}

		void setProperty(MuisProperty<T> property) {
			theProperty = property;
		}

		/** @return The concatenation of this helper's property's type name and its name. Useful for error messages. */
		protected String propName() {
			if(theProperty == null)
				return "";
			return theProperty.getPropertyTypeName() + " " + theProperty.getName();
		}
	}

	/**
	 * A property type that is helped by {@link MuisProperty.PropertyHelper}
	 *
	 * @param <T> The type of property that this type is for
	 */
	public static abstract class AbstractPropertyType<T> extends PropertyHelper<T> implements PropertyType<T> {
	}

	/**
	 * A property type that is helped by {@link MuisProperty.PropertyHelper}
	 *
	 * @param <T> The type of property that this validator is for
	 */
	public static abstract class AbstractPropertyValidator<T> extends PropertyHelper<T> implements PropertyValidator<T> {
	}

	private static abstract class AbstractPrintablePropertyType<T> extends AbstractPropertyType<T> implements PrintablePropertyType<T> {
	}

	/**
	 * Parses MUIS properties using a PrismsParser
	 *
	 * @param <T> The type of the property
	 */
	public static class PrismsParsedPropertyType<T> extends AbstractPropertyType<T> {
		private final Type theType;

		/** @param type The type of the property */
		public PrismsParsedPropertyType(Type type) {
			theType = type;
		}

		@Override
		public Type getType() {
			return theType;
		}

		@Override
		public ObservableValue<? extends T> parse(MuisParseEnv env, String value) throws MuisException {
			WrappingPrismsParser parser = new WrappingPrismsParser(env.getValueParser().getParser());
			WrappingObservableEvaluator evaluator = new WrappingObservableEvaluator(env.getValueParser().getEvaluator());
			EvaluationEnvironment evalEnv = env.getValueParser().getEvaluationEnvironment().scope(true);
			mutate(parser, evaluator, evalEnv);
			parser.validateConfig();
			evaluator.seal();

			ParsedItem item;
			try {
				prisms.lang.ParseMatch [] matches = parser.parseMatches(value);
				if(matches.length > 1)
					throw new MuisParseException("A single value is required for this property: " + value);
				item = parser.parseStructures(new prisms.lang.ParseStructRoot(value), matches)[0];
			} catch(prisms.lang.ParseException e) {
				throw new MuisParseException("Parsing failed for property type " + getClass().getSimpleName() + ": " + value, e);
			}

			ObservableValue<?> ret;
			try {
				ret = evaluator.evaluateObservable(item, env.getValueParser().getEvaluationEnvironment());
			} catch(EvaluationException e) {
				throw new MuisParseException("Evaluation failed for property type " + getClass().getSimpleName() + ": " + value, e);
			}
			if(!theType.isAssignable(ret.getType()))
				throw new MuisException("The given value of type " + ret.getType() + " is not compatible with this property's type ("
					+ theType + ")");
			return (ObservableValue<? extends T>) ret;
		}

		/**
		 * May be used by subclasses to modify the prisms parsing types
		 *
		 * @param parser The parser
		 * @param eval The evaluator
		 * @param env The evaluation environment
		 * @throws MuisException If an error occurs mutating the parsing types
		 */
		protected void mutate(PrismsParser parser, ObservableEvaluator eval, EvaluationEnvironment env) throws MuisException {
		}

		@Override
		public <V extends T> V cast(Object value) {
			if(value == null)
				return null;
			if(theType.isAssignableFrom(value.getClass()))
				return (V) value;
			return null;
		}

		@Override
		public String toString() {
			return theType.toString();
		}
	}

	/**
	 * @param env The parsing environment
	 * @param text The text to parse
	 * @return An observable value, if the text is explicitly marked to be parsed as such. Null otherwise.
	 * @throws MuisParseException If an exception occurs parsing the explicitly-marked observable from the text.
	 */
	public static ObservableValue<?> parseExplicitObservable(MuisParseEnv env, String text) throws MuisParseException {
		if(text.startsWith("${") && text.endsWith("}"))
			return env.getValueParser().parse(text.substring(2, text.length() - 1));
		else
			return null;
	}

	private final String theName;

	private final PropertyType<T> theType;

	private final PropertyValidator<T> theValidator;

	/**
	 * @param name The name for the property
	 * @param type The type of the property
	 * @param validator The validator for the property
	 */
	protected MuisProperty(String name, PropertyType<T> type, PropertyValidator<T> validator) {
		theName = name;
		theType = type;
		theValidator = validator;
		if(theType instanceof PropertyHelper)
			((PropertyHelper<T>) theType).setProperty(this);
		if(theValidator instanceof PropertyHelper)
			((PropertyHelper<T>) theValidator).setProperty(this);
	}

	/**
	 * Creates a MUIS property without a validator or a path acceptor
	 *
	 * @param name The name for the property
	 * @param type The type of the property
	 */
	protected MuisProperty(String name, PropertyType<T> type) {
		this(name, type, null);
	}

	/** @return This property's name */
	public String getName() {
		return theName;
	}

	/** @return This property's type */
	public PropertyType<T> getType() {
		return theType;
	}

	/** @return The validator for this property. May be null. */
	public PropertyValidator<T> getValidator() {
		return theValidator;
	}

	/** @return A string describing this sub-type */
	public abstract String getPropertyTypeName();

	@Override
	public String toString() {
		return getPropertyTypeName() + " " + theName + "(" + theType + ")";
	}

	@Override
	public boolean equals(Object o) {
		if(o == null || !o.getClass().equals(getClass()))
			return false;
		MuisProperty<?> prop = (MuisProperty<?>) o;
		return prop.theName.equals(theName) && prop.theType.equals(theType);
	}

	@Override
	public int hashCode() {
		return theName.hashCode() * 13 + theType.hashCode() * 7 + (theValidator == null ? 0 : theValidator.hashCode());
	}

	/** A string property type--this type validates anything */
	public static final AbstractPropertyType<String> stringAttr = new AbstractPrintablePropertyType<String>() {
		@Override
		public ObservableValue<String> parse(MuisParseEnv env, String value) throws MuisException {
			ObservableValue<?> ret = parseExplicitObservable(env, value);
			if(ret != null) {
				if(ret.getType().canAssignTo(String.class)) {
				} else if(ret.getType().canAssignTo(CharSequence.class)) {
					ret = ((ObservableValue<? extends CharSequence>) ret).mapV(seq -> {
						return seq.toString();
					});
				} else
					throw new MuisException("Model value " + value + " is not of type string");
			} else
				ret = ObservableValue.constant(value);
			return (ObservableValue<String>) ret;
		}

		@Override
		public String cast(Object value) {
			if(value instanceof String)
				return (String) value;
			else
				return null;
		}

		@Override
		public Type getType() {
			return new Type(String.class);
		}

		@Override
		public String toString(String value) {
			return value;
		}

		@Override
		public String toString() {
			return "string";
		}
	};

	/** A boolean property type--values must be either true or false */
	public static final AbstractPropertyType<Boolean> boolAttr = new PrismsParsedPropertyType<>(new Type(Boolean.TYPE));

	/** An integer property type--values must be valid integers */
	public static final AbstractPropertyType<Long> intAttr = new PrismsParsedPropertyType<Long>(new Type(Long.TYPE)) {
		@Override
		public String toString() {
			return "int";
		}
	};

	/** A floating-point property type--values must be valid real numbers */
	public static final AbstractPropertyType<Double> floatAttr = new PrismsParsedPropertyType<Double>(new Type(Double.TYPE)) {
		@Override
		public String toString() {
			return "float";
		}
	};

	/** Represents a time amount. The value is interpreted in milliseconds. */
	public static final AbstractPropertyType<Long> timeAttr = new AbstractPropertyType<Long>() {
		@Override
		public Type getType() {
			return new Type(Long.class);
		}

		@Override
		public ObservableValue<Long> parse(MuisParseEnv env, String value) throws MuisException {
			ObservableValue<?> retObs = parseExplicitObservable(env, value);
			if(retObs != null) {
				if(retObs.getType().canAssignTo(Long.TYPE)) {
				} else if(retObs.getType().canAssignTo(java.util.Date.class)) {
					retObs = ((ObservableValue<? extends java.util.Date>) retObs).mapV(date -> {
						return date.getTime();
					});
				} else if(retObs.getType().canAssignTo(java.time.Period.class)) {
					retObs = ((ObservableValue<? extends java.time.Period>) retObs).mapV(date -> {
						return date.get(java.time.temporal.ChronoUnit.MILLIS);
					});
				} else
					throw new MuisException("Model value " + value + " is not of type long, Date, or Period");
				return (ObservableValue<Long>) retObs;
			}
			long ret = 0;
			for(String s : value.trim().split("\\s")) {
				char unit = s.charAt(s.length() - 1);
				if(unit >= '0' && unit <= '9') {
					try {
						ret += Long.parseLong(s);
					} catch(NumberFormatException e) {
						throw new MuisException("Illegal time value: " + value, e);
					}
				} else {
					s = s.substring(0, s.length() - 1);
					if(s.length() == 0)
						throw new MuisException("No value in front of unit for time value: " + value);
					long t;
					try {
						t = Long.parseLong(s) * 1000;
					} catch(NumberFormatException e) {
						throw new MuisException("Illegal time value: " + value, e);
					}
					if(unit == 's') {
						ret += t * 1000;
					} else if(unit == 'm') {
						ret += t * 60000;
					} else if(unit == 'h') {
						ret += t * 60 * 60 * 1000;
					} else if(unit == 'd') {
						ret += t * 24 * 60 * 60 * 1000;
					} else
						throw new MuisException("Unrecognized unit '" + unit + "' in time value: " + value);
				}
			}
			return ObservableValue.constant(ret);
		}

		@Override
		public Long cast(Object value) {
			if(value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte)
				return ((Number) value).longValue();
			else if(value instanceof Double || value instanceof Float)
				return (long) Math.round(((Number) value).floatValue() * 1000);
			else
				return null;
		}

		@Override
		public String toString() {
			return "time";
		}
	};

	private static class ColorPropertyType extends PrismsParsedPropertyType<Color> implements PrintablePropertyType<Color> {
		private final PrismsConfig theColorOp;

		private final List<ParsedFunctionDeclaration> theFunctions;

		ColorPropertyType() {
			super(new Type(Color.class));
			theColorOp = new prisms.arch.MutableConfig("entity").set("name", "color").set("order", "1")
				.set("impl", ParsedColor.class.getName()) //
				.getOrCreate("select") //
				/**/.getOrCreate("option") //
				/*		*/.getOrCreate("literal").set("storeAs", "rgb").setValue("#") //
				/*		*/.getParent() //
				/**/.getParent().getOrCreate("option") //
				/*		*/.getOrCreate("literal").set("storeAs", "hsb").setValue("$") //
				/*		*/.getParent() //
				/**/.getParent() //
				.getParent().getOrCreate("charset").set("storeAs", "value").set("pattern", "[0-9][A-F][a-f]{6}");

			theFunctions=new java.util.ArrayList<>();
			// Use the default prisms.lang Grammar.xml to implement some setup declarations to prepare the environment
			PrismsParser setupParser = new PrismsParser();
			try {
				setupParser.configure(PrismsParser.class.getResource("Grammar.xml"));
			} catch(IOException e) {
				throw new IllegalStateException("Could not configure style sheet setup parser", e);
			}

			ArrayList<String> commands = new ArrayList<>();
			// Add constants and functions like rgb(r, g, b) here
			commands.add("java.awt.Color rgb(int r, int g, int b){return " + org.muis.core.style.Colors.class.getName() + ".rgb(r, g, b);}");
			commands.add("java.awt.Color hsb(int h, int s, int b){return " + org.muis.core.style.Colors.class.getName() + ".hsb(h, s, b);}");
			// TODO Add more constants and functions
			// TODO Are these defined by default? Maybe we don't need them here.
			for(String command : commands) {
				try {
					theFunctions.add((ParsedFunctionDeclaration) setupParser.parseStructures(new prisms.lang.ParseStructRoot(command),
						setupParser.parseMatches(command))[0]);
				} catch(ParseException e) {
					System.err.println("Could not compile color property evaluation functions: " + command);
					e.printStackTrace();
				}
			}
		}

		@Override
		protected void mutate(PrismsParser parser, ObservableEvaluator eval, EvaluationEnvironment env) throws MuisException {
			parser.insertOperator(theColorOp);
			eval.addEvaluator(ParsedColor.class, new org.muis.core.eval.impl.ColorEvaluator());

			Type colorType = new Type(Color.class);
			for(String colorName : Colors.getColorNames()) {
				try {
					env.declareVariable(colorName, colorType, true, null, 0);
					env.setVariable(colorName, Colors.parseColor(colorName), null, 0);
				} catch(EvaluationException | MuisException e) {

				}
			}
			for(ParsedFunctionDeclaration func : theFunctions)
				env.declareFunction(func);
		}

		@Override
		public String toString(Color value) {
			return org.muis.core.style.Colors.toString(value);
		}

		@Override
		public String toString() {
			return "color";
		}
	}

	/** A color property type--values must be parse to colors via {@link org.muis.core.style.Colors#parseColor(String)} */
	public static final AbstractPropertyType<Color> colorAttr = new ColorPropertyType();

	/**
	 * A resource property--values may be:
	 * <ul>
	 * <li>namespace:tag, where <code>tag</code> maps to a resource in the <code>namespace</code> toolkit</li>
	 * <li>A relative path to a resource that may be resolved from the element's toolkit. A <code>namespace:</code> prefix may be used to
	 * specify a different toolkit</li>
	 * <li>An absolute URL. Permissions will be checked before resources at any external URLs are retrieved. TODO cite specific permission.</li>
	 * </ul>
	 */
	public static final AbstractPropertyType<URL> resourceAttr = new AbstractPropertyType<java.net.URL>() {
		@Override
		public ObservableValue<URL> parse(MuisParseEnv env, String value) throws MuisException {
			ObservableValue<?> ret = parseExplicitObservable(env, value);
			if(ret != null) {
				if(ret.getType().canAssignTo(URL.class)) {
				} else if(ret.getType().canAssignTo(CharSequence.class)) {
					ret = ((ObservableValue<? extends CharSequence>) ret).mapV(seq -> {
						try {
							return new URL(seq.toString());
						} catch(java.net.MalformedURLException e) {
							throw new IllegalArgumentException("Malformed URL", e);
						}
					});
				} else
					throw new MuisException("Model value " + value + " is not of type string or URL");
			}
			int sepIdx = value.indexOf(':');
			String namespace = sepIdx < 0 ? null : value.substring(0, sepIdx);
			String content = sepIdx < 0 ? value : value.substring(sepIdx + 1);
			MuisToolkit toolkit = env.cv().getToolkit(namespace);
			if(toolkit == null)
				try {
					return ObservableValue.constant(new URL(value));
				} catch(java.net.MalformedURLException e) {
					throw new MuisException(propName() + ": Resource property is not a valid URL: \"" + value + "\"", e);
				}
			ResourceMapping mapping = toolkit.getMappedResource(content);
			if(mapping == null)
				throw new MuisException(propName() + ": Resource property must map to a declared resource: \"" + value + "\" in toolkit "
					+ toolkit.getName() + " or one of its dependencies");
			if(mapping.getLocation().contains(":"))
				try {
					return ObservableValue.constant(new URL(mapping.getLocation()));
				} catch(java.net.MalformedURLException e) {
					throw new MuisException(propName() + ": Resource property maps to an invalid URL \"" + mapping.getLocation()
						+ "\" in toolkit " + mapping.getOwner().getName() + ": \"" + value + "\"");
				}
			try {
				return ObservableValue.constant(MuisUtils.resolveURL(mapping.getOwner().getURI(), mapping.getLocation()));
			} catch(MuisException e) {
				throw new MuisException(propName() + ": Resource property maps to a resource (" + mapping.getLocation()
					+ ") that cannot be resolved with respect to toolkit \"" + mapping.getOwner().getName() + "\"'s URL: \"" + value + "\"");
			}
		}

		@Override
		public URL cast(Object value) {
			if(value instanceof URL)
				return (URL) value;
			else
				return null;
		}

		@Override
		public Type getType() {
			return new Type(URL.class);
		}

		@Override
		public boolean equals(Object o) {
			return o != null && o.getClass() == getClass();
		}

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}
	};

	/**
	 * A MUIS-type property type--values must be valid types mapped under MUIS
	 *
	 * @param <T> The subtype that the value must map to
	 */
	public static class MuisTypeProperty<T> extends AbstractPropertyType<Class<? extends T>> {
		/** The subtype that the value must map to */
		public final Class<T> type;

		/** @param aType The subtype that the value must map to */
		public MuisTypeProperty(Class<T> aType) {
			type = aType;
		}

		@Override
		public ObservableValue<Class<? extends T>> parse(MuisParseEnv env, String value) throws MuisException {
			if(value == null)
				return null;
			int sep = value.indexOf(':');
			String ns, tag;
			if(sep >= 0) {
				ns = value.substring(0, sep);
				tag = value.substring(sep + 1);
			} else {
				ns = null;
				tag = value;
			}
			MuisToolkit toolkit = null;
			String className = null;
			if(ns != null) {
				toolkit = env.cv().getToolkit(ns);
				if(toolkit == null)
					throw new MuisException(propName() + ": Value " + value + " refers to a toolkit \"" + ns
						+ "\" that is inaccessible from its element");
				className = toolkit.getMappedClass(tag);
				if(className == null)
					throw new MuisException(propName() + ": Value " + value + " refers to a type \"" + tag
						+ "\" that is not mapped within toolkit " + toolkit.getName());
			} else {
				for(MuisToolkit tk : env.cv().getScopedToolkits()) {
					className = tk.getMappedClass(tag);
					if(className != null) {
						toolkit = tk;
						break;
					}
				}
				if(className == null) {
					throw new MuisException(propName() + ": Value " + value + " refers to a type \"" + tag
						+ "\" that is not mapped within a scoped toolkit");
				}
			}
			Class<?> valueClass;
			try {
				valueClass = toolkit.loadClass(className, null);
			} catch(MuisException e) {
				throw new MuisException(propName() + ": Value " + value + " refers to a type that failed to load", e);
			}
			if(!type.isAssignableFrom(valueClass))
				throw new MuisException(propName() + ": Value " + value + " refers to a type (" + valueClass.getName()
					+ ") that is not a subtype of " + type);
			return ObservableValue.constant((Class<? extends T>) valueClass);
		}

		@Override
		public Class<? extends T> cast(Object value) {
			if(!(value instanceof Class<?>))
				return null;
			if(!type.isAssignableFrom((Class<?>) value))
				return null;
			return (Class<? extends T>) value;
		}

		@Override
		public String toString() {
			return type.isPrimitive() ? type.getSimpleName() : type.getName();
		}

		@Override
		public Type getType() {
			return new Type(type.getClass());
		}

		@Override
		public boolean equals(Object o) {
			return o != null && o.getClass() == getClass() && ((MuisTypeProperty<?>) o).type.equals(type);
		}

		@Override
		public int hashCode() {
			return getClass().hashCode() * 7 + type.hashCode();
		}
	}

	/**
	 * An property type that creates actual instances of a specified type
	 *
	 * @param <T> The type of value to create
	 */
	public static final class MuisTypeInstanceProperty<T> extends AbstractPropertyType<T> {
		/** The subtype that the value must map to */
		private final MuisTypeProperty<? extends T> theTypeProperty;

		/** @param aType The subtype that the value must map to */
		public MuisTypeInstanceProperty(Class<? extends T> aType) {
			theTypeProperty = new MuisTypeProperty<>(aType);
		}

		@Override
		public ObservableValue<T> parse(MuisParseEnv env, String value) throws MuisException {
			if(value == null)
				return new ObservableValue.ConstantObservableValue<>(new Type(theTypeProperty.type), null);
			ObservableValue<?> ret = parseExplicitObservable(env, value);
			if(ret != null) {
				if(ret.getType().canAssignTo(theTypeProperty.type)) {
					return (ObservableValue<T>) ret;
				} else
					throw new MuisException("Model value " + value + " is not of type " + theTypeProperty.type);
			}
			Class<? extends T> valueClass = theTypeProperty.parse(env, value).get();
			try {
				return ObservableValue.constant(valueClass.newInstance());
			} catch(InstantiationException e) {
				throw new MuisException(propName() + ": Could not instantiate type " + valueClass.getName(), e);
			} catch(IllegalAccessException e) {
				throw new MuisException(propName() + ": Could not access default constructor of type " + valueClass.getName(), e);
			}
		}

		@Override
		public <V extends T> V cast(Object value) {
			if(value == null)
				return null;
			if(!getType().isAssignableFrom(value.getClass()))
				return null;
			return (V) getType().cast(value);
		}

		@Override
		public String toString() {
			return getType().toString();
		}

		@Override
		public Type getType() {
			return new Type(theTypeProperty.type);
		}

		@Override
		public boolean equals(Object o) {
			return o != null && o.getClass() == getClass() && ((MuisTypeInstanceProperty<?>) o).getType().equals(getType());
		}

		@Override
		public int hashCode() {
			return getClass().hashCode() * 7 + getType().hashCode();
		}
	}

	/** A MuisTypeProperty for a generic MuisElement */
	public static final MuisTypeProperty<MuisElement> elementTypeProp = new MuisTypeProperty<>(MuisElement.class);

	/**
	 * An enumeration property type--validates elements whose value matches any of the values given in the constructor
	 *
	 * @param <T> The enumeration type
	 */
	public static final class MuisEnumProperty<T extends Enum<T>> extends AbstractPropertyType<T> {
		/** The enumeration that this property represents */
		public final Class<T> enumType;

		/** Whether all values in this enum are unique case-insensitively. If this is true, property values are case-insensitive. */
		public final boolean ciUnique;

		/**
		 * Creates an enumeration property from an enumerated type. The options will be all the type's constants in lower-case.
		 *
		 * @param enumClass The enumerated type
		 */
		public MuisEnumProperty(Class<T> enumClass) {
			enumType = enumClass;
			java.util.HashSet<String> values = new java.util.HashSet<>();
			boolean unique = true;
			for(T value : enumType.getEnumConstants())
				if(!values.add(value.name().toLowerCase())) {
					unique = false;
					break;
				}
			ciUnique = unique;
		}

		@Override
		public ObservableValue<T> parse(MuisParseEnv env, String value) throws MuisException {
			if(value == null)
				return null;
			ObservableValue<?> ret = parseExplicitObservable(env, value);
			if(ret != null) {
				if(ret.getType().canAssignTo(enumType)) {
					return (ObservableValue<T>) ret;
				} else if(ret.getType().canAssignTo(String.class)) {
					return ((ObservableValue<String>) ret).mapV(str -> {
						return Enum.valueOf(enumType, str);
					});
				} else
					throw new MuisException("Model value " + value + " is not of type " + enumType.getSimpleName());
			}
			T [] consts = enumType.getEnumConstants();
			for(T e : consts)
				if(ciUnique) {
					if(e.name().equalsIgnoreCase(value))
						return ObservableValue.constant(e);
				} else if(e.name().equals(value))
					return ObservableValue.constant(e);
			throw new MuisException(propName() + ": Value " + value + " does not match any of the allowable values for type "
				+ enumType.getName());
		}

		@Override
		public T cast(Object value) {
			if(enumType.isInstance(value))
				return (T) value;
			else
				return null;
		}

		@Override
		public Type getType() {
			return new Type(enumType);
		}

		@Override
		public boolean equals(Object o) {
			return o != null && o.getClass() == getClass() && ((MuisEnumProperty<?>) o).enumType.equals(enumType);
		}

		@Override
		public int hashCode() {
			return getClass().hashCode() * 7 + enumType.hashCode();
		}
	}

	/**
	 * Wraps another property type, allowing the user to specify predefined values in addition to normal, parseable values
	 *
	 * @param <T> The type of the property
	 */
	public static class NamedValuePropertyType<T> extends AbstractPropertyType<T> {
		private final PropertyType<T> theWrapped;

		private final Map<String, T> theNamedValues;

		private final int theHashCode;

		/**
		 * @param wrap The property type to wrap
		 * @param namedValues Name-value pairs of values that can be specified by name
		 */
		public NamedValuePropertyType(PropertyType<T> wrap, Object... namedValues) {
			this(wrap, compileNamedValues(namedValues, wrap.getType()));
		}

		/**
		 * @param wrap The property type to wrap
		 * @param namedValues Name-value pairs of values that can be specified by name
		 */
		public NamedValuePropertyType(PropertyType<T> wrap, Map<String, T> namedValues) {
			theWrapped = wrap;
			checkTypes(namedValues, wrap.getType());
			java.util.HashMap<String, T> copy = new java.util.HashMap<>(namedValues);
			theNamedValues = java.util.Collections.unmodifiableMap(copy);
			theHashCode = theNamedValues.hashCode();
		}

		private static <T> Map<String, T> compileNamedValues(Object [] nv, Type type) {
			if(nv == null || nv.length == 0)
				return null;
			if(nv.length % 2 != 0)
				throw new IllegalArgumentException("Named values must be pairs in the form name, " + type + ", name, " + type + "...");
			java.util.HashMap<String, T> ret = new java.util.HashMap<>();
			for(int i = 0; i < nv.length; i += 2) {
				if(!(nv[i] instanceof String) || !type.isAssignableFrom(nv[i + 1].getClass()))
					throw new IllegalArgumentException("Named values must be pairs in the form name, " + type + ", name, " + type + "...");
				if(ret.containsKey(nv[i]))
					throw new IllegalArgumentException("Named value \"" + nv[i] + "\" specified multiple times");
				ret.put((String) nv[i], (T) type.cast(nv[i + 1]));
			}
			return ret;
		}

		private static <T> void checkTypes(Map<String, T> map, Type type) {
			if(map == null)
				return;
			for(Map.Entry<?, ?> entry : map.entrySet())
				if(!(entry.getKey() instanceof String) || !type.isAssignableFrom(entry.getValue().getClass()))
					throw new IllegalArgumentException("name-value pairs must be typed String, " + type);
		}

		@Override
		public Type getType() {
			return theWrapped.getType();
		}

		@Override
		public ObservableValue<? extends T> parse(MuisParseEnv env, String value) throws MuisException {
			ObservableValue<?> ret = parseExplicitObservable(env, value);
			if(ret != null) {
				if(theWrapped.getType().isAssignable(ret.getType())) {
					return (ObservableValue<T>) ret;
				} else if(ret.getType().canAssignTo(String.class)) {
					return ((ObservableValue<String>) ret).mapV(str -> {
						if(theNamedValues.containsKey(str))
							return theNamedValues.get(str);
						else
							throw new IllegalArgumentException("Model value " + value + " does not match a predefined value.");
					});
				} else
					throw new MuisException("Model value " + value + " is not of type " + theWrapped.getType());
			}
			if(theNamedValues.containsKey(value))
				return ObservableValue.constant(theNamedValues.get(value));
			return theWrapped.parse(env, value);
		}

		@Override
		public <V extends T> V cast(Object value) {
			return theWrapped.cast(value);
		}

		@Override
		public boolean equals(Object o) {
			if(o.getClass() != getClass())
				return false;
			NamedValuePropertyType<?> nvpt = (NamedValuePropertyType<?>) o;
			return theWrapped.equals(nvpt.theWrapped) && theNamedValues.equals(nvpt.theNamedValues);
		}

		@Override
		public int hashCode() {
			return theHashCode;
		}
	}

	/**
	 * A simple validator that compares values to minimum and maximum values
	 *
	 * @param <T> The type of value to validate
	 */
	public static class ComparableValidator<T> extends AbstractPropertyValidator<T> {
		private final Comparator<T> theCompare;
		private final Comparator<T> theInternalCompare;

		private final T theMin;
		private final T theMax;

		private final int theHashCode;

		/**
		 * Shorter constructor for comparable types
		 *
		 * @param min The minimum value for the property
		 * @param max The maximum value for the property
		 */
		public ComparableValidator(T min, T max) {
			this(min, max, null);
		}

		/**
		 * @param min The minimum value for the property
		 * @param max The maximum value for the property
		 * @param compare The comparator to use to compare. May be null if this type is comparable.
		 */
		public ComparableValidator(T min, T max, Comparator<T> compare) {
			theCompare = compare;
			theMin = min;
			theMax = max;
			if(compare != null)
				theInternalCompare = compare;
			else {
				if(theMin != null && !(theMin instanceof Comparable))
					throw new IllegalArgumentException("No comparator given, but minimum value is not Comparable");
				if(theMax != null && !(theMax instanceof Comparable))
					throw new IllegalArgumentException("No comparator given, but maximum value is not Comparable");
				theInternalCompare = new Comparator<T>() {
					@Override
					public int compare(T o1, T o2) {
						return ((Comparable<T>) o1).compareTo(o2);
					}
				};
			}

			int hc = 0;
			if(theCompare != null)
				hc += theCompare.getClass().hashCode();
			if(theMin != null)
				hc = hc * 7 + theMin.hashCode();
			if(theMax != null)
				hc = hc * 7 + theMax.hashCode();
			theHashCode = hc;
		}

		/** @return The comparator that is used to validate values. May be null if the type is comparable. */
		public Comparator<T> getCompare() {
			return theCompare;
		}

		/** @return The minimum value to validate against */
		public T getMin() {
			return theMin;
		}

		/** @return The maximum value to validate against */
		public T getMax() {
			return theMax;
		}

		@Override
		public boolean isValid(T value) {
			if(theMin != null && theInternalCompare.compare(theMin, value) > 0)
				return false;
			if(theMax != null && theInternalCompare.compare(value, theMax) > 0)
				return false;
			return true;
		}

		@Override
		public void assertValid(T value) throws MuisException {
			if(theMin != null && theInternalCompare.compare(theMin, value) > 0)
				throw new MuisException(propName() + "Value must be at least " + theMin + ": " + value + " is invalid");
			if(theMax != null && theInternalCompare.compare(value, theMax) > 0)
				throw new MuisException(propName() + "Value must be at most " + theMax + ": " + value + " is invalid");
		}

		@Override
		public String toString() {
			if(theMin == null && theMax == null)
				return "Empty comparable validator";
			return "Comparable validator: " + (theMin != null ? theMin + " < " : "") + "value" + (theMax != null ? " < " + theMax : "");
		}

		@Override
		public boolean equals(Object o) {
			if(o == null || o.getClass() != getClass())
				return false;
			ComparableValidator<?> val = (ComparableValidator<?>) o;
			if(theCompare != null) {
				if(val.theCompare == null)
					return false;
				if(theCompare.getClass() != val.theCompare.getClass())
					return false;
			} else if(val.theCompare != null)
				return false;
			if(theMin == null ? val.theMin != null : !theMin.equals(val.theMin))
				return false;
			if(theMax == null ? val.theMax != null : !theMax.equals(val.theMax))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			return theHashCode;
		}
	}
}
