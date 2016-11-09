package org.quick.core.prop.antlr;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.observe.ObservableValue;
import org.observe.SettableValue;
import org.observe.collect.ObservableList;
import org.observe.collect.ObservableOrderedCollection;
import org.quick.core.QuickException;
import org.quick.core.QuickParseEnv;
import org.quick.core.parser.MathUtils;
import org.quick.core.parser.PrismsPropertyParser.ParsedPlaceHolder;
import org.quick.core.parser.PrismsPropertyParser.ParsedUnitValue;
import org.quick.core.parser.QuickParseException;
import org.quick.core.prop.Unit;
import org.quick.util.QuickUtils;

import com.google.common.reflect.TypeToken;

import prisms.lang.ParsedItem;
import prisms.lang.types.*;

public class AntlrPropertyEvaluator {
	private <T> ObservableValue<? extends T> evaluateTypeChecked(QuickParseEnv parseEnv, TypeToken<T> type, QPPExpression<?> parsedItem,
		boolean actionAccepted, boolean actionRequired) throws QuickParseException {
		ObservableValue<?> result = evaluateTypeless(parseEnv, type, parsedItem, actionAccepted, actionRequired);

		if (!QuickUtils.isAssignableFrom(type, result.getType()))
			throw new QuickParseException(parsedItem.getMatch().text + " evaluates to type " + result.getType()
				+ ", which is not compatible with expected type " + type);
		return (ObservableValue<? extends T>) result;
	}

	public static <T> ObservableValue<T> evaluateTypeless(QuickParseEnv parseEnv, TypeToken<T> type, QPPExpression<?> parsedItem,
		boolean actionAccepted, boolean actionRequired) {
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
			return mapUnary(arg1, op, parseEnv);
		} else if (parsedItem instanceof ParsedBinaryOp) {
			ParsedBinaryOp op = (ParsedBinaryOp) parsedItem;
			if (actionRequired)
				throw new QuickParseException("Binary operation " + op.getName() + " cannot be an action");
			ObservableValue<?> arg1 = evaluateTypeChecked(parseEnv, TypeToken.of(Object.class), op.getOp1(), actionAccepted, false);
			ObservableValue<?> arg2 = evaluateTypeChecked(parseEnv, TypeToken.of(Object.class), op.getOp2(), actionAccepted, false);
			return combineBinary(arg1, arg2, op, parseEnv);
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
				assignValue = getBinaryAssignValue(settable, arg2, op, parseEnv);
			} else
				assignValue = getUnaryAssignValue(settable, op, parseEnv);
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
						actionAccepted, false);
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
			ObservableValue<?> result = parseEnv.getContext().getVariable(parsedItem.getMatch().text);
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
			Function<Object, T> forwardMap = v -> {
				try {
					Object unitMapped = ((Unit<Object, ?>) unit).getMap().apply(QuickUtils.convert(unit.getFromType(), v));
					return QuickUtils.convert(type, unitMapped);
				} catch (QuickException e) {
					parseEnv.msg().error("Unit " + unit.getName() + " could not convert value " + v + " to " + unit.getToType(), e);
					return null; // TODO What to do with this?
				}
			};
			Function<T, Object> reverseMap = v -> {
				try {
					Object unitMapped = ((Unit<Object, Object>) unit).getReverseMap().apply(QuickUtils.convert(unit.getToType(), v));
					return QuickUtils.convert(unit.getFromType(), unitMapped);
				} catch (QuickException e) {
					parseEnv.msg().error("Unit " + unit.getName() + " could not convert value " + v + " to " + unit.getFromType(), e);
					return null; // TODO What to do with this?
				}
			};
			if (value instanceof SettableValue && unit.getReverseMap() != null) {
				return ((SettableValue<Object>) value).mapV(type, forwardMap, reverseMap, true);
			} else {
				return value.mapV(type, forwardMap);
			}
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

}
