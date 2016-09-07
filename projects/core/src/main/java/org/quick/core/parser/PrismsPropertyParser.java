package org.quick.core.parser;

import java.io.IOException;
import java.lang.reflect.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.observe.*;
import org.observe.collect.ObservableList;
import org.observe.collect.ObservableOrderedCollection;
import org.quick.core.QuickEnvironment;
import org.quick.core.QuickException;
import org.quick.core.QuickParseEnv;
import org.quick.core.model.ObservableActionValue;
import org.quick.core.prop.ExpressionFunction;
import org.quick.core.prop.Unit;
import org.quick.util.QuickUtils;

import com.google.common.reflect.TypeToken;

import prisms.lang.*;
import prisms.lang.types.*;

/** A property parser that uses the parser in prisms.lang to parse values */
public class PrismsPropertyParser extends AbstractPropertyParser {
	private final PrismsParser theParser;

	/** @param env The Quick environment that this parser is to be in */
	public PrismsPropertyParser(QuickEnvironment env) {
		super(env);
		theParser = new PrismsParser();
		try {
			theParser.configure(PrismsPropertyParser.class.getResource("QVX.xml"));
		} catch (IOException e) {
			throw new IllegalStateException("Could not configure property parser", e);
		}
	}

	@Override
	protected <T> ObservableValue<?> parseDefaultValue(QuickParseEnv parseEnv, TypeToken<T> type, String value, boolean action)
		throws QuickParseException {
		ParseMatch[] matches;
		try {
			matches = theParser.parseMatches(value);
		} catch (ParseException e) {
			throw new QuickParseException("Failed to parse " + value, e);
		}
		if (matches.length != 1)
			throw new QuickParseException("One value per property expected: " + value);
		ParsedItem item;
		try {
			item = theParser.parseStructures(new ParseStructRoot(value), matches)[0];
		} catch (ParseException e) {
			throw new QuickParseException("Failed to structure parsed value for " + value, e);
		}
		return evaluateTypeless(parseEnv, type, item, action, action);
	}

	private <T> ObservableValue<? extends T> evaluateTypeChecked(QuickParseEnv parseEnv, TypeToken<T> type, ParsedItem parsedItem,
		boolean actionAccepted, boolean actionRequired) throws QuickParseException {
		ObservableValue<?> result = evaluateTypeless(parseEnv, type, parsedItem, actionAccepted, actionRequired);

		if (!QuickUtils.isAssignableFrom(type, result.getType()))
			throw new QuickParseException(parsedItem.getMatch().text + " evaluates to type " + result.getType()
				+ ", which is not compatible with expected type " + type);
		return (ObservableValue<? extends T>) result;
	}

	private <T> ObservableValue<?> evaluateTypeless(QuickParseEnv parseEnv, TypeToken<T> type, ParsedItem parsedItem,
		boolean actionAccepted, boolean actionRequired) throws QuickParseException {
		// Sort from easiest to hardest
		// Literals first
		if (parsedItem instanceof ParsedNull) {
			if (actionRequired)
				throw new QuickParseException("null literal cannot be an action");
			return ObservableValue.constant(type, null);
		} else if (parsedItem instanceof ParsedNumber) {
			if (actionRequired)
				throw new QuickParseException("number literal cannot be an action");
			ParsedNumber num = (ParsedNumber) parsedItem;
			return ObservableValue.constant((TypeToken<Number>) TypeToken.of(num.getValue().getClass()).unwrap(), num.getValue());
		} else if (parsedItem instanceof ParsedString) {
			if (actionRequired)
				throw new QuickParseException("String literal cannot be an action");
			return ObservableValue.constant(TypeToken.of(String.class), ((ParsedString) parsedItem).getValue());
		} else if (parsedItem instanceof ParsedBoolean) {
			if (actionRequired)
				throw new QuickParseException("boolean literal cannot be an action");
			return ObservableValue.constant(TypeToken.of(Boolean.TYPE), ((ParsedBoolean) parsedItem).getValue());
		} else if (parsedItem instanceof ParsedChar) {
			if (actionRequired)
				throw new QuickParseException("char literal cannot be an action");
			return ObservableValue.constant(TypeToken.of(Character.TYPE), ((ParsedChar) parsedItem).getValue());
			// Now easy operations
		} else if (parsedItem instanceof ParsedParenthetic) {
			return evaluateTypeChecked(parseEnv, type, ((ParsedParenthetic) parsedItem).getContent(), actionAccepted, actionRequired);
		} else if (parsedItem instanceof ParsedType) {
			throw new QuickParseException("Types cannot be evaluated to a value");
		} else if (parsedItem instanceof ParsedInstanceofOp) {
			if (actionRequired)
				throw new QuickParseException("instanceof operation cannot be an action");
			ParsedInstanceofOp instOf = (ParsedInstanceofOp) parsedItem;
			ParsedType parsedType = (ParsedType) instOf.getType();
			if (parsedType.getParameterTypes().length > 0)
				throw new QuickParseException(
					"instanceof checks cannot be performed against parameterized types: " + parsedType.getMatch().text);
			TypeToken<?> testType = evaluateType(parseEnv, parsedType, type);
			ObservableValue<?> var = evaluateTypeChecked(parseEnv, TypeToken.of(Object.class), instOf.getVariable(), actionAccepted, false);
			if (!testType.getRawType().isInterface() && !var.getType().getRawType().isInterface()) {
				if (testType.isAssignableFrom(var.getType())) {
					parseEnv.msg().warn(instOf.getVariable().getMatch().text + " is always an instance of " + parsedType.getMatch().text
						+ " (if non-null)");
					return var.mapV(MathUtils.BOOLEAN, v -> v != null, true);
				} else if (!var.getType().isAssignableFrom(testType)) {
					parseEnv.msg().error(instOf.getVariable().getMatch().text + " is never an instance of " + parsedType.getMatch().text);
					return ObservableValue.constant(MathUtils.BOOLEAN, false);
				} else {
					return var.mapV(MathUtils.BOOLEAN, v -> testType.getRawType().isInstance(v), true);
				}
			} else {
				return var.mapV(MathUtils.BOOLEAN, v -> testType.getRawType().isInstance(v), true);
			}
		} else if (parsedItem instanceof ParsedCast) {
			if (actionRequired)
				throw new QuickParseException("Cast cannot be an action");
			ParsedCast cast = (ParsedCast) parsedItem;
			TypeToken<?> testType = evaluateType(parseEnv, (ParsedType) cast.getType(), type);
			ObservableValue<?> var = evaluateTypeChecked(parseEnv, TypeToken.of(Object.class), cast.getValue(), actionAccepted, false);
			if (!testType.getRawType().isInterface() && !var.getType().getRawType().isInterface()) {
				if (testType.isAssignableFrom(var.getType())) {
					parseEnv.msg().warn(
						cast.getValue().getMatch().text + " is always an instance of " + cast.getType().getMatch().text + " (if non-null)");
					return var.mapV((TypeToken<Object>) testType, v -> v, true);
				} else if (!var.getType().isAssignableFrom(testType)) {
					parseEnv.msg().error(cast.getValue().getMatch().text + " is never an instance of " + cast.getType().getMatch().text);
					return var.mapV((TypeToken<Object>) testType, v -> {
						if (v == null)
							return null;
						else
							throw new ClassCastException(v + " is not an instance of " + testType);
					}, true);
				} else {
					return var.mapV((TypeToken<Object>) testType, v -> {
						if (testType.getRawType().isInstance(v))
							return v;
						else
							throw new ClassCastException(v + " is not an instance of " + testType);
					}, true);
				}
			} else {
				return var.mapV((TypeToken<Object>) testType, v -> {
					if (testType.getRawType().isInstance(v))
						return v;
					else
						throw new ClassCastException(v + " is not an instance of " + testType);
				}, true);
			}
			// Harder operations
		} else if (parsedItem instanceof ParsedUnaryOp) {
			ParsedUnaryOp op = (ParsedUnaryOp) parsedItem;
			if (actionRequired)
				throw new QuickParseException("Unary operation " + op.getName() + " cannot be an action");
			ObservableValue<?> arg1 = evaluateTypeChecked(parseEnv, TypeToken.of(Object.class), op.getOp(), actionAccepted, false);
			return mapUnary(arg1, op);
		} else if (parsedItem instanceof ParsedBinaryOp) {
			ParsedBinaryOp op = (ParsedBinaryOp) parsedItem;
			if (actionRequired)
				throw new QuickParseException("Binary operation " + op.getName() + " cannot be an action");
			ObservableValue<?> arg1 = evaluateTypeChecked(parseEnv, TypeToken.of(Object.class), op.getOp1(), actionAccepted, false);
			ObservableValue<?> arg2 = evaluateTypeChecked(parseEnv, TypeToken.of(Object.class), op.getOp2(), actionAccepted, false);
			return combineBinary(arg1, arg2, op);
		} else if (parsedItem instanceof ParsedConditional) {
			if (actionRequired)
				throw new QuickParseException("Conditionals cannot be an action");
			ParsedConditional cond = (ParsedConditional) parsedItem;
			ObservableValue<?> condition = evaluateTypeChecked(parseEnv, TypeToken.of(Object.class), cond.getCondition(), actionAccepted,
				false);
			if (!TypeToken.of(Boolean.class).isAssignableFrom(condition.getType().wrap()))
				throw new QuickParseException(
					"Condition in " + cond.getMatch().text + " evaluates to type " + condition.getType() + ", which is not boolean");
			ObservableValue<? extends T> affirm = evaluateTypeChecked(parseEnv, type, cond.getAffirmative(), actionAccepted, false);
			ObservableValue<? extends T> neg = evaluateTypeChecked(parseEnv, type, cond.getNegative(), actionAccepted, false);
			return ObservableValue.flatten(((ObservableValue<Boolean>) condition).mapV(v -> v ? affirm : neg));
			// Assignments
		} else if (parsedItem instanceof ParsedAssignmentOperator) {
			if (!actionAccepted)
				throw new QuickParseException("Assignment operator must be an action");
			ParsedAssignmentOperator op = (ParsedAssignmentOperator) parsedItem;
			ObservableValue<?> arg1 = evaluateTypeChecked(parseEnv, TypeToken.of(Object.class), op.getVariable(), actionAccepted, false);
			if (!(arg1 instanceof SettableValue))
				throw new QuickParseException(op.getVariable().getMatch().text + " does not parse to a settable value");
			SettableValue<Object> settable = (SettableValue<Object>) arg1;
			ObservableValue<?> assignValue;
			if (op.getOperand() != null) {
				ObservableValue<?> arg2 = evaluateTypeChecked(parseEnv, TypeToken.of(Object.class), op.getOperand(), actionAccepted, false);
				assignValue = getBinaryAssignValue(settable, arg2, op);
			} else
				assignValue = getUnaryAssignValue(settable, op);
			return makeActionValue(settable, assignValue, op.isPrefix());
			// Array operations
		} else if (parsedItem instanceof ParsedArrayIndex) {
			if (actionRequired)
				throw new QuickParseException("Array index operation cannot be an action");
			ParsedArrayIndex pai = (ParsedArrayIndex) parsedItem;
			ObservableValue<?> array = evaluateTypeChecked(parseEnv, TypeToken.of(Object.class), pai.getArray(), actionAccepted, false);
			TypeToken<? extends T> resultType;
			{
				TypeToken<?> testResultType;
				if (array.getType().isArray()) {
					testResultType = array.getType().getComponentType();
				} else if (TypeToken.of(List.class).isAssignableFrom(array.getType())) {
					testResultType = array.getType().resolveType(List.class.getTypeParameters()[0]);
				} else if (TypeToken.of(ObservableOrderedCollection.class).isAssignableFrom(array.getType())) {
					testResultType = array.getType().resolveType(ObservableOrderedCollection.class.getTypeParameters()[0]);
				} else {
					throw new QuickParseException("array value in " + parsedItem.getMatch().text + " evaluates to type " + array.getType()
						+ ", which is not indexable");
				}
				if (!QuickUtils.isAssignableFrom(type, testResultType))
					throw new QuickParseException("array value in " + parsedItem.getMatch().text + " evaluates to type " + array.getType()
						+ " which is not indexable to type " + type);
				resultType = (TypeToken<? extends T>) testResultType;
			}
			ObservableValue<?> index = evaluateTypeChecked(parseEnv, TypeToken.of(Object.class), pai.getIndex(), actionAccepted, false);
			if (!TypeToken.of(Long.class).isAssignableFrom(array.getType().wrap())) {
				throw new QuickParseException("index value in " + parsedItem.getMatch().text + " evaluates to type " + index.getType()
					+ ", which is not a valid index");
			}
			if (TypeToken.of(ObservableList.class).isAssignableFrom(array.getType())) {
				return SettableValue.flatten(((ObservableValue<ObservableList<? extends T>>) array)
					.combineV((list, idx) -> list.observeAt(((Number) idx).intValue(), null), index));
			} else if (TypeToken.of(ObservableOrderedCollection.class).isAssignableFrom(array.getType())) {
				return ObservableValue.flatten(((ObservableValue<ObservableOrderedCollection<? extends T>>) array)
					.combineV((coll, idx) -> coll.observeAt(((Number) idx).intValue(), null), index));
			} else
				return array.combineV((TypeToken<T>) resultType, (BiFunction<Object, Number, T>) (a, i) -> {
					int idx = i.intValue();
					if (TypeToken.of(Object[].class).isAssignableFrom(array.getType())) {
						return ((T[]) a)[idx];
					} else if (array.getType().isArray()) {
						return (T) java.lang.reflect.Array.get(a, idx);
					} else/* if (TypeToken.of(List.class).isAssignableFrom(array.getType()))*/ {
						return ((List<? extends T>) a).get(idx);
					}
				}, (ObservableValue<? extends Number>) index, true);
		} else if (parsedItem instanceof ParsedArrayInitializer) {
			if (actionRequired)
				throw new QuickParseException("Array init operation cannot be an action");
			ParsedArrayInitializer arrayInit = (ParsedArrayInitializer) parsedItem;
			TypeToken<?> arrayType = evaluateType(parseEnv, arrayInit.getType(), type);
			if (arrayInit.getSizes().length > 0) {
				if (arrayInit.getStored("valueSet") != null)
					throw new QuickParseException(
						"Array sizes may not be specified for array initialization when a value list is specified as well");
				ObservableValue<? extends Number>[] sizes = new ObservableValue[arrayInit.getSizes().length];
				for (int i = 0; i < sizes.length; i++) {
					arrayType = QuickUtils.arrayTypeOf(arrayType);
					ObservableValue<?> size_i = evaluateTypeChecked(parseEnv, TypeToken.of(Object.class), arrayInit.getSizes()[i],
						actionAccepted,
						false);
					if (!QuickUtils.isAssignableFrom(TypeToken.of(Integer.TYPE), size_i.getType()))
						throw new QuickParseException("Array size " + arrayInit.getSizes()[i].getMatch().text + " parses to type "
							+ size_i.getType() + ", which is not a valid array size type");
					sizes[i] = (ObservableValue<? extends Number>) size_i;
				}

				TypeToken<?> fArrayType = arrayType;
				return new ObservableValue.ComposedObservableValue<>((TypeToken<Object>) arrayType, new Function<Object[], Object>() {
					@Override
					public Object apply(Object[] args) {
						return makeArray(fArrayType.getRawType(), args, 0);
					}

					private Object makeArray(Class<?> arrayClass, Object[] args, int argIdx) {
						int size = ((Number) args[argIdx]).intValue();
						Object array = Array.newInstance(arrayClass, size);
						if (argIdx < args.length - 1) {
							Class<?> elementClass = arrayClass.getComponentType();
							for (int i = 0; i < size; i++)
								Array.set(array, i, makeArray(elementClass, args, argIdx + 1));
						}
						return array;
					}
				}, true, sizes);
			} else if (arrayInit.getStored("valueSet") != null) {
				ObservableValue<?>[] elements = new ObservableValue[arrayInit.getElements().length];
				TypeToken<?> componentType = arrayType.getComponentType();
				for (int i = 0; i < elements.length; i++) {
					ObservableValue<?> element_i = evaluateTypeChecked(parseEnv, TypeToken.of(Object.class), arrayInit.getElements()[i],
						actionAccepted, false);
					if (!QuickUtils.isAssignableFrom(componentType, element_i.getType()))
						throw new QuickParseException("Array element " + arrayInit.getElements()[i].getMatch().text + " parses to type "
							+ element_i.getType() + ", which cannot be cast to " + componentType);
					elements[i] = element_i;
				}

				TypeToken<?> fArrayType = arrayType;
				return new ObservableValue.ComposedObservableValue<>((TypeToken<Object>) arrayType, new Function<Object[], Object>() {
					@Override
					public Object apply(Object[] args) {
						Object array = Array.newInstance(fArrayType.getRawType(), elements.length);
						for (int i = 0; i < args.length; i++)
							Array.set(array, i, args[i]);
						return array;
					}
				}, true, elements);
			} else
				throw new QuickParseException("Either array sizes or a value list must be specifieded for array initialization");
			// Now pulling stuff from the context
			// Identifier, placeholder, and unit
		} else if (parsedItem instanceof ParsedIdentifier) {
			if (actionRequired)
				throw new QuickParseException("Identifier cannot be an action");
			String name = ((ParsedIdentifier) parsedItem).getName();
			ObservableValue<?> result = parseEnv.getContext().getVariable(name);
			if (result == null)
				throw new QuickParseException("No such variable " + name);
			return result;
		} else if (parsedItem instanceof ParsedPlaceHolder) {
			ObservableValue<?> result = parseEnv.getContext().getVariable(((ParsedPlaceHolder) parsedItem).getName());
			if (result == null)
				throw new QuickParseException("Unrecognized placeholder: " + parsedItem.getMatch().text);
			return result;
		} else if (parsedItem instanceof ParsedUnitValue) {
			if (actionRequired)
				throw new QuickParseException("Unit value cannot be an action");
			ParsedUnitValue unitValue = (ParsedUnitValue) parsedItem;
			String unitName = unitValue.getUnit();
			List<Unit<?, ?>> units = parseEnv.getContext().getUnits(unitName, new ArrayList<>());
			if (units.isEmpty())
				throw new QuickParseException("Unrecognized unit " + unitName);
			Unit<?, ?> bestUnit = null;
			InvokableMatch bestMatch = null;
			for (Unit<?, ?> u : units) {
				if (!QuickUtils.isAssignableFrom(type, u.getToType()))
					continue;
				InvokableMatch match = getMatch(new TypeToken[] { u.getFromType() }, false, new ParsedItem[] { unitValue.getValue() },
					parseEnv, type, actionAccepted);
				if (match != null && (bestMatch == null || match.distance < bestMatch.distance)) {
					bestUnit = u;
					bestMatch = match;
				}
			}
			if (bestUnit == null)
				throw new QuickParseException("Unit " + unitName + " cannot convert from " + unitValue.getValue() + " to type " + type);
			ObservableValue<?> value = bestMatch.parameters[0];
			Unit<?, ?> unit = bestUnit;
			return value.mapV(type, v -> {
				try {
					Object unitMapped = ((Unit<Object, ?>) unit).getMap().apply(QuickUtils.convert(unit.getFromType(), v));
					return QuickUtils.convert(type, unitMapped);
				} catch (QuickException e) {
					parseEnv.msg().error("Unit " + unit.getName() + " could not convert value " + v + " to " + unit.getToType(), e);
					return null; // TODO What to do with this?
				}
			});
			// Now harder operations
		} else if (parsedItem instanceof ParsedConstructor) {
			if (actionRequired)
				throw new QuickParseException("Constructor cannot be an action");
			ParsedConstructor constructor = (ParsedConstructor) parsedItem;
			TypeToken<?> typeToCreate = evaluateType(parseEnv, constructor.getType(), type);
			Constructor<?> bestConstructor = null;
			InvokableMatch bestMatch = null;
			for (Constructor<?> c : typeToCreate.getRawType().getConstructors()) {
				if (!c.isAccessible())
					continue;
				InvokableMatch match = getMatch(c.getGenericParameterTypes(), c.isVarArgs(), constructor.getArguments(), parseEnv, type,
					actionAccepted);
				if (match != null && (bestMatch == null || match.distance < bestMatch.distance)) {
					bestMatch = match;
					bestConstructor = c;
				}
			}
			if (bestConstructor == null)
				throw new QuickParseException("No such constructor found: " + parsedItem);
			Constructor<?> toInvoke = bestConstructor;
			return new ObservableValue.ComposedObservableValue<>((TypeToken<Object>) typeToCreate, args -> {
				try {
					return toInvoke.newInstance(args);
				} catch (Exception e) {
					parseEnv.msg().error("Invocation failed for " + constructor, e);
					return null; // TODO What to do with this?
				}
			}, true, bestMatch.parameters);
		} else if (parsedItem instanceof ParsedMethod) {
			ParsedMethod method = (ParsedMethod) parsedItem;
			if (actionRequired && !method.isMethod())
				throw new QuickParseException("Field access cannot be an action");

			if (method.getContext() == null) {
				if (method.isMethod()) {
					// A function
					return evaluateFunction(method, parseEnv, type, actionAccepted);
				} else {
					// A variable
					ObservableValue<?> result = parseEnv.getContext().getVariable(method.getName());
					if (result == null)
						throw new QuickParseException(method.getName() + " cannot be resolved");
					return result;
				}
			} else {
				// May be a method invocation on a value or a static invocation on a type
				String rootCtx = getRootContext(method.getContext());
				Class<?> contextType = null;
				if (rootCtx != null && parseEnv.getContext().getVariable(rootCtx) == null) {
					// May be a static invocation on a type
					try {
						contextType = rawType(method.getContext().toString(), parseEnv);
					} catch (QuickParseException e) {
						// We'll throw a different exception later if we can't resolve it
					}
				}
				if (contextType != null) {
					return evaluateStatic(method, contextType, parseEnv, type, actionAccepted);
				} else {
					// Not a static invocation. Evaluate the context. Let that evaluation throw the exception if needed.
					ObservableValue<?> context = evaluateTypeChecked(parseEnv, TypeToken.of(Object.class), method.getContext(),
						actionAccepted, false);
					return evaluateMethod(method, context, parseEnv, type, actionAccepted);
				}
			}
		} else
			throw new QuickParseException("Unrecognized parsed item type: " + parsedItem.getClass());
	}

	private TypeToken<?> evaluateType(QuickParseEnv parseEnv, ParsedType parsedType, TypeToken<?> expected) throws QuickParseException {
		Type reflectType = getReflectType(parsedType, expected, parseEnv);

		return TypeToken.of(reflectType);
	}

	private Type getReflectType(ParsedType parsedType, TypeToken<?> expected, QuickParseEnv parseEnv) throws QuickParseException {
		Type reflectType;
		if (parsedType.isParameterized())
			reflectType = parameterizedType(parsedType.getName(), parsedType.getParameterTypes(), expected, parseEnv);
		else
			reflectType = rawType(parsedType.getName(), parseEnv);

		for (int i = 0; i < parsedType.getArrayDimension(); i++)
			reflectType = arrayType(reflectType);

		if (parsedType.isBounded())
			reflectType = boundedType(reflectType, parsedType.isUpperBound());

		return reflectType;
	}

	private Type parameterizedType(String name, ParsedType[] parameterTypes, TypeToken<?> expected, QuickParseEnv parseEnv)
		throws QuickParseException {
		Class<?> raw = rawType(name, parseEnv);

		Type[] typeArgs = new Type[raw.getTypeParameters().length];
		if (parameterTypes.length == 0) {
			// Diamond operator. Figure out the parameter types.
			for (int i = 0; i < typeArgs.length; i++)
				typeArgs[i] = expected.resolveType(raw.getTypeParameters()[i]).getType();
		} else if (parameterTypes.length != typeArgs.length)
			throw new QuickParseException("Type " + raw.getName() + " cannot be parameterized with " + parameterTypes.length + " types");
		else {
			for (int i = 0; i < typeArgs.length; i++)
				typeArgs[i] = getReflectType(parameterTypes[i], expected.resolveType(raw.getTypeParameters()[i]), parseEnv);
		}

		class ParameterizedType implements java.lang.reflect.ParameterizedType {
			@Override
			public Type getRawType() {
				return raw;
			}

			@Override
			public Type[] getActualTypeArguments() {
				return typeArgs;
			}

			@Override
			public Type getOwnerType() {
				return null;
			}
		}
		return new ParameterizedType();
	}

	private Class<?> rawType(String name, QuickParseEnv parseEnv) throws QuickParseException {
		int dotIdx = name.lastIndexOf('.');
		if (dotIdx < 0) {
			Class<?> quickType;
			try {
				quickType = parseEnv.cv().loadIfMapped(name, Object.class);
			} catch (QuickException e) {
				throw new QuickParseException(e.getMessage(), e);
			}
			if (quickType != null)
				return quickType;
		}
		try {
			return parseEnv.cv().loadClass(name);
		} catch (ClassNotFoundException e) {
			// Do nothing. We'll throw our own exception if we can't find the class.
		}
		StringBuilder qClassName = new StringBuilder(name);
		while (dotIdx >= 0) {
			qClassName.setCharAt(dotIdx, '$');
			try {
				return parseEnv.cv().loadClass(qClassName.toString());
			} catch (ClassNotFoundException e) {
			}
			dotIdx = qClassName.lastIndexOf(".");
		}
		throw new QuickParseException("Could not load class " + name);
	}

	private Type arrayType(Type reflectType) {
		if (reflectType instanceof Class)
			return Array.newInstance((Class<?>) reflectType, 0).getClass();
		class ArrayType implements GenericArrayType {
			@Override
			public Type getGenericComponentType() {
				return reflectType;
			}
		}
		return new ArrayType();
	}

	private Type boundedType(Type reflectType, boolean upperBound) {
		class WildcardType implements java.lang.reflect.WildcardType {
			private final Type[] upperBounds;
			private final Type[] lowerBounds;

			public WildcardType(Type boundType, boolean upperBnd) {
				upperBounds = upperBnd ? new Type[] { boundType } : new Type[0];
				lowerBounds = upperBnd ? new Type[0] : new Type[] { boundType };
			}

			@Override
			public Type[] getUpperBounds() {
				return upperBounds;
			}

			@Override
			public Type[] getLowerBounds() {
				return lowerBounds;
			}
		}
		return new WildcardType(reflectType, upperBound);
	}

	static class InvokableMatch {
		final ObservableValue<?>[] parameters;
		final double distance;

		InvokableMatch(ObservableValue<?>[] parameters, double distance) {
			this.parameters = parameters;
			this.distance = distance;
		}
	}

	private String getRootContext(ParsedItem ctx) {
		if (ctx instanceof ParsedMethod) {
			ParsedMethod method = (ParsedMethod) ctx;
			if (method.isMethod())
				return null;
			String ret = getRootContext(method.getContext());
			if (ret == null)
				return null;
			return ret + "." + method.getName();
		} else if (ctx instanceof ParsedType) {
			ParsedType type = (ParsedType) ctx;
			if (type.isBounded() || type.isParameterized())
				return null;
			return type.toString();
		} else if (ctx instanceof ParsedIdentifier)
			return ((ParsedIdentifier) ctx).getName();
		else
			return null;
	}

	private InvokableMatch getMatch(Type[] paramTypes, boolean varArgs, ParsedItem[] arguments, QuickParseEnv env, TypeToken<?> type,
		boolean actionAccepted) throws QuickParseException {
		TypeToken<?>[] typeTokenParams = new TypeToken[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++)
			typeTokenParams[i] = type.resolveType(paramTypes[i]);
		return getMatch(typeTokenParams, varArgs, arguments, env, type, actionAccepted);
	}

	private InvokableMatch getMatch(TypeToken<?>[] paramTypes, boolean varArgs, ParsedItem[] arguments, QuickParseEnv parseEnv,
		TypeToken<?> type, boolean actionAccepted) throws QuickParseException {
		TypeToken<?>[] argTargetTypes = new TypeToken[arguments.length];
		if (paramTypes.length == arguments.length) {
			argTargetTypes = paramTypes;
		} else if (varArgs) {
			if (arguments.length >= paramTypes.length - 1) {
				for (int i = 0; i < paramTypes.length - 1; i++)
					argTargetTypes[i] = paramTypes[i];
				for (int i = paramTypes.length - 1; i < arguments.length; i++)
					argTargetTypes[i] = paramTypes[paramTypes.length - 1];
			} else
				return null;
		} else
			return null;

		ObservableValue<?>[] args = new ObservableValue[arguments.length];
		for (int i = 0; i < args.length; i++) {
			args[i] = evaluateTypeless(parseEnv, argTargetTypes[i], arguments[i], actionAccepted, false);
			if (!QuickUtils.isAssignableFrom(argTargetTypes[i], args[i].getType()))
				return null;
		}
		double distance = 0;
		for (int i = 0; i < paramTypes.length && i < args.length; i++)
			distance += getDistance(paramTypes[i].wrap(), args[i].getType().wrap());
		return new InvokableMatch(args, distance);
	}

	private double getDistance(TypeToken<?> paramType, TypeToken<?> argType) {
		double distance = 0;
		distance += getRawDistance(paramType.getRawType(), argType.getRawType());
		if (paramType.getType() instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) paramType.getType();
			for (int i = 0; i < pt.getActualTypeArguments().length; i++) {
				double paramDist = getDistance(paramType.resolveType(pt.getActualTypeArguments()[i]),
					argType.resolveType(pt.getActualTypeArguments()[i]));
				distance += paramDist / 10;
			}
		}
		return distance;
	}

	private int getRawDistance(Class<?> paramType, Class<?> argType) {
		if (paramType.equals(argType))
			return 0;
		else if (paramType.isAssignableFrom(argType)) {
			int dist = 0;
			while (argType.getSuperclass() != null && paramType.isAssignableFrom(argType.getSuperclass())) {
				argType = argType.getSuperclass();
				dist++;
			}
			if (paramType.isInterface())
				dist += getInterfaceDistance(paramType, argType);
			return dist;
		} else if (argType.isAssignableFrom(paramType))
			return getRawDistance(argType, paramType);
		else if (Number.class.isAssignableFrom(paramType) && Number.class.isAssignableFrom(argType)) {
			int paramNumTypeOrdinal = getNumberTypeOrdinal(paramType);
			int argNumTypeOrdinal = getNumberTypeOrdinal(argType);
			if (paramNumTypeOrdinal < 0 || argNumTypeOrdinal < 0 || argNumTypeOrdinal > paramNumTypeOrdinal)
				throw new IllegalStateException("Shouldn't get here: " + paramType.getName() + " and " + argType.getName());
			return paramNumTypeOrdinal - argNumTypeOrdinal;
		} else
			throw new IllegalStateException("Shouldn't get here: " + paramType.getName() + " and " + argType.getName());
	}

	private int getInterfaceDistance(Class<?> paramType, Class<?> argType) {
		if (paramType.equals(argType))
			return 0;
		for (Class<?> intf : argType.getInterfaces()) {
			if (paramType.isAssignableFrom(intf))
				return getInterfaceDistance(paramType, intf) + 1;
		}
		throw new IllegalStateException("Shouldn't get here");
	}

	private int getNumberTypeOrdinal(Class<?> numType) {
		if (numType == Byte.class)
			return 0;
		else if (numType == Short.class)
			return 1;
		else if (numType == Integer.class)
			return 2;
		else if (numType == Long.class)
			return 3;
		else if (numType == Float.class)
			return 4;
		else if (numType == Double.class)
			return 5;
		else
			return -1;
	}

	private ObservableValue<?> evaluateStatic(ParsedMethod method, Class<?> targetType, QuickParseEnv parseEnv, TypeToken<?> type,
		boolean actionAccepted) throws QuickParseException {
		// Evaluate as a static invocation on the type
		if (method.isMethod()) {
			Method bestMethod = null;
			InvokableMatch bestMatch = null;
			for (Method m : targetType.getMethods()) {
				if ((m.getModifiers() & Modifier.STATIC) == 0 || !m.getName().equals(method.getName()) || !m.isAccessible())
					continue;
				InvokableMatch match = getMatch(m.getGenericParameterTypes(), m.isVarArgs(), method.getArguments(), parseEnv, type,
					actionAccepted);
				if (match != null && (bestMatch == null || match.distance < bestMatch.distance)) {
					bestMatch = match;
					bestMethod = m;
				}
			}
			if (bestMethod == null)
				throw new QuickParseException("No such method found: " + method);
			Method toInvoke = bestMethod;
			return new ObservableValue.ComposedObservableValue<>(
				(TypeToken<Object>) type.resolveType(toInvoke.getGenericReturnType()), args -> {
					try {
						return toInvoke.invoke(null, args);
					} catch (Exception e) {
						parseEnv.msg().error("Invocation failed for static method " + method, e);
						return null; // TODO What to do with this?
					}
				}, true, bestMatch.parameters);
		} else {
			Field field;
			try {
				field = targetType.getField(method.getName());
			} catch (NoSuchFieldException e) {
				throw new QuickParseException("No such field " + targetType.getName() + "." + method.getName(), e);
			} catch (SecurityException e) {
				throw new QuickParseException("Could not access field " + targetType.getName() + "." + method.getName(), e);
			}
			if ((field.getModifiers() & Modifier.STATIC) == 0)
				throw new QuickParseException("Field " + targetType.getName() + "." + field.getName() + " is not static");
			return new ObservableValue<Object>() {
				private final TypeToken<Object> fieldType = (TypeToken<Object>) TypeToken.of(field.getGenericType());

				@Override
				public Subscription subscribe(Observer<? super ObservableValueEvent<Object>> observer) {
					return () -> {
					}; // Can't get notifications on a field change
				}

				@Override
				public boolean isSafe() {
					return true;
				}

				@Override
				public TypeToken<Object> getType() {
					return fieldType;
				}

				@Override
				public Object get() {
					try {
						return field.get(null);
					} catch (Exception e) {
						parseEnv.msg().error("Could not get static field " + targetType.getName() + "." + field.getName(), e);
						return null; // TODO What to do with this?
					}
				}
			};
		}
	}

	private ObservableValue<?> evaluateMethod(ParsedMethod method, ObservableValue<?> context, QuickParseEnv parseEnv, TypeToken<?> type,
		boolean actionAccepted) throws QuickParseException {
		if (method.isMethod()) {
			Method bestMethod = null;
			InvokableMatch bestMatch = null;
			for (Method m : context.getType().getRawType().getMethods()) {
				if (!m.getName().equals(method.getName()) || !m.isAccessible())
					continue;
				InvokableMatch match = getMatch(m.getGenericParameterTypes(), m.isVarArgs(), method.getArguments(), parseEnv, type,
					actionAccepted);
				if (match != null && (bestMatch == null || match.distance < bestMatch.distance)) {
					bestMatch = match;
					bestMethod = m;
				}
			}
			if (bestMethod == null)
				throw new QuickParseException("No such method found: " + method);
			ObservableValue<?>[] composed = new ObservableValue[bestMatch.parameters.length + 1];
			composed[0] = context;
			System.arraycopy(bestMatch.parameters, 0, composed, 1, bestMatch.parameters.length);
			Method toInvoke = bestMethod;
			TypeToken<?> resultType = context.getType().resolveType(toInvoke.getGenericReturnType());
			return new ObservableValue.ComposedObservableValue<>((TypeToken<Object>) resultType, args -> {
				Object ctx = args[0];
				Object[] params = new Object[args.length - 1];
				System.arraycopy(args, 1, params, 0, params.length);
				try {
					return toInvoke.invoke(ctx, params);
				} catch (Exception e) {
					parseEnv.msg().error("Invocation failed for method " + method, e);
					return null; // TODO What to do with this?
				}
			}, true, composed);
		} else {
			Field field;
			try {
				field = context.getType().getRawType().getField(method.getName());
			} catch (NoSuchFieldException e) {
				throw new QuickParseException("No such field " + context + "." + method.getName(), e);
			} catch (SecurityException e) {
				throw new QuickParseException("Could not access field " + context + "." + method.getName(), e);
			}
			if ((field.getModifiers() & Modifier.STATIC) != 0)
				throw new QuickParseException("Field " + context + "." + field.getName() + " is static");
			return context.mapV((TypeToken<Object>) context.getType().resolveType(field.getGenericType()), v -> {
				try {
					return field.get(v);
				} catch (Exception e) {
					parseEnv.msg().error("Could not get field " + context + "." + field.getName(), e);
					return null; // TODO What to do with this?
				}
			});
		}
	}

	private ObservableValue<?> evaluateFunction(ParsedMethod method, QuickParseEnv parseEnv, TypeToken<?> type, boolean actionAccepted)
		throws QuickParseException {
		List<ExpressionFunction<?>> functions = new ArrayList<>();
		parseEnv.getContext().getFunctions(method.getName(), functions);
		ExpressionFunction<?> bestFunction = null;
		InvokableMatch bestMatch = null;
		for (ExpressionFunction<?> fn : functions) {
			InvokableMatch match = getMatch(fn.getArgumentTypes().toArray(new TypeToken[0]), fn.isVarArgs(), method.getArguments(),
				parseEnv, type, actionAccepted);
			if (match != null && (bestMatch == null || match.distance < bestMatch.distance)) {
				bestMatch = match;
				bestFunction = fn;
			}
		}
		if (bestFunction == null)
			throw new QuickParseException("No such function found: " + method);
		ExpressionFunction<?> toInvoke = bestFunction;
		return new ObservableValue.ComposedObservableValue<>((TypeToken<Object>) toInvoke.getReturnType(), args -> {
			try {
				return toInvoke.apply(Arrays.asList(args));
			} catch (RuntimeException e) {
				parseEnv.msg().error("Invocation failed for function " + method, e);
				return null; // TODO What to do with this?
			}
		}, true, bestMatch.parameters);
	}

	private static ObservableValue<?> mapUnary(ObservableValue<?> arg1, ParsedUnaryOp op) throws QuickParseException {
		try {
			switch (op.getName()) {
			case "+":
			case "-":
			case "~":
				try {
					return MathUtils.unaryOp(op.getName(), arg1);
				} catch (IllegalArgumentException e) {
					throw new QuickParseException(e.getMessage(), e);
				}
			default:
				throw new QuickParseException("Unrecognized unary operator: " + op.getName());
			}
		} catch (IllegalArgumentException e) {
			throw new QuickParseException(e.getMessage(), e);
		}
	}

	private static <T> ObservableValue<T> getUnaryAssignValue(SettableValue<T> arg1, ParsedAssignmentOperator op)
		throws QuickParseException {
		ObservableValue<T> result;
		try {
			switch (op.getName()) {
			case "++":
				result = MathUtils.addOne(arg1);
				break;
			case "--":
				result = MathUtils.minusOne(arg1);
				break;
			default:
				throw new QuickParseException("Unrecognized unary assignment operator: " + op.getName());
			}
		} catch (IllegalArgumentException e) {
			throw new QuickParseException(e.getMessage(), e);
		}
		return result;
	}

	private static ObservableValue<?> combineBinary(ObservableValue<?> arg1, ObservableValue<?> arg2, ParsedBinaryOp op)
		throws QuickParseException {
		try {
			switch (op.getName()) {
			case "+":
				TypeToken<String> strType = TypeToken.of(String.class);
				if (strType.isAssignableFrom(arg1.getType()) || strType.isAssignableFrom(arg2.getType())) {
					return arg1.combineV(strType, (a1, a2) -> new StringBuilder().append(a1).append(a2).toString(), arg2, true);
				}
				//$FALL-THROUGH$
			case "-":
			case "*":
			case "/":
			case "%":
			case "<<":
			case ">>":
			case ">>>":
			case "&":
			case "|":
			case "^":
			case "&&":
			case "||":
			case "==":
			case "!=":
			case ">":
			case "<":
			case ">=":
			case "<=":
				return MathUtils.binaryMathOp(op.getName(), arg1, arg2);
			default:
				throw new QuickParseException("Unrecognized binary operator: " + op.getName());
			}
		} catch (IllegalArgumentException e) {
			throw new QuickParseException(e.getMessage(), e);
		}
	}

	private static ObservableValue<?> getBinaryAssignValue(SettableValue<?> arg1, ObservableValue<?> arg2, ParsedAssignmentOperator op)
		throws QuickParseException {
		switch (op.getName()) {
		case "=":
			if (!QuickUtils.isAssignableFrom(arg1.getType(), arg2.getType()))
				throw new QuickParseException(
					op.getOperand().getMatch().text + ", type " + arg2.getType() + ", cannot be assigned to type " + arg1.getType());
			return arg2;
		case "+=":
		case "-=":
		case "*=":
		case "/=":
		case "%=":
		case "&=":
		case "|=":
		case "^=":
		case "<<=":
		case ">>=":
		case ">>>=":
			String mathOp = op.getName().substring(0, op.getName().length() - 1);
			ObservableValue<?> result;
			try {
				result = MathUtils.binaryMathOp(mathOp, arg1, arg2);
			} catch (IllegalArgumentException e) {
				throw new QuickParseException(e.getMessage(), e);
			}
			if (!QuickUtils.isAssignableFrom(arg1.getType(), result.getType()))
				throw new QuickParseException(op.getVariable().getMatch().text + " " + mathOp + " " + op.getOperand().getMatch().text
					+ ", type " + result.getType() + ", cannot be assigned to type " + arg1.getType());
			return result;
		default:
			throw new QuickParseException("Unrecognized binary assignment operator: " + op.getName());
		}
	}

	private static <T> ObservableActionValue<T> makeActionValue(SettableValue<T> settable, ObservableValue<? extends T> value,
		boolean valuePreAction) throws QuickParseException {
		ObservableAction action = settable.assignmentTo(value);
		return new ObservableActionValue<T>() {
			@Override
			public TypeToken<T> getType() {
				return settable.getType();
			}

			@Override
			public boolean isSafe() {
				return value.isSafe();
			}

			@Override
			public T get() {
				if (valuePreAction) {
					T ret = settable.get();
					action.act(null);
					return ret;
				} else {
					action.act(null);
					return settable.get();
				}
			}

			@Override
			public void act(Object cause) throws IllegalStateException {
				action.act(cause);
			}

			@Override
			public ObservableValue<String> isEnabled() {
				return action.isEnabled();
			}
		};
	}

	/** Represents a value with a unit (e.g. 9px for 9 pixels) */
	public static class ParsedUnitValue extends ParsedItem {
		private ParsedItem theValue;
		private String theUnit;

		@Override
		public void setup(PrismsParser parser, ParsedItem parent, ParseMatch match) throws ParseException {
			super.setup(parser, parent, match);
			theValue = parser.parseStructures(this, getStored("value"))[0];
			theUnit = getStored("unit").text;
		}

		/** @return The value of the expression */
		public ParsedItem getValue() {
			return theValue;
		}

		/** @return The unit of the expression */
		public String getUnit() {
			return theUnit;
		}

		@Override
		public ParsedItem[] getDependents() {
			return new ParsedItem[] { theValue };
		}

		@Override
		public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
			if (theValue == dependent)
				theValue = toReplace;
			else
				theValue.replace(dependent, toReplace);
		}
	}

	/** Represents a placeholder for an injected value */
	public static class ParsedPlaceHolder extends ParsedItem {
		private String theName;

		@Override
		public void setup(PrismsParser parser, ParsedItem parent, ParseMatch match) throws ParseException {
			super.setup(parser, parent, match);
			theName = getStored("name").text;
		}

		/** @return The name of the placeholder */
		public String getName() {
			return theName;
		}

		@Override
		public ParsedItem[] getDependents() {
			return new ParsedItem[0];
		}

		@Override
		public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {}
	}
}
