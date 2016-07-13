package org.quick.core.parser;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.observe.ObservableAction;
import org.observe.ObservableValue;
import org.observe.SettableValue;
import org.observe.collect.ObservableList;
import org.observe.collect.ObservableOrderedCollection;
import org.quick.core.QuickEnvironment;
import org.quick.core.QuickException;
import org.quick.core.QuickParseEnv;
import org.quick.core.model.ObservableActionValue;
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
			theParser.configure(PrismsPropertyParser.class.getResource("MVX.xml"));
		} catch (IOException e) {
			throw new IllegalStateException("Could not configure property parser", e);
		}
	}

	@Override
	protected <T> ObservableValue<? extends T> parseDefaultValue(QuickParseEnv parseEnv, TypeToken<T> type, String value, boolean action)
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
		return evaluate(parseEnv, type, item, action, action);
	}

	private <T> ObservableValue<? extends T> evaluate(QuickParseEnv parseEnv, TypeToken<T> type, ParsedItem parsedItem, boolean actionAccepted, boolean actionRequired) throws QuickParseException {
		ObservableValue<?> result;
		// Sort from easiest to hardest
		// Literals first
		if (parsedItem instanceof ParsedNull) {
			if (actionRequired)
				throw new QuickParseException("null literal cannot be an action");
			result = ObservableValue.constant(type, null);
		} else if (parsedItem instanceof ParsedNumber) {
			if (actionRequired)
				throw new QuickParseException("number literal cannot be an action");
			ParsedNumber num = (ParsedNumber) parsedItem;
			result = ObservableValue.constant((TypeToken<Number>) TypeToken.of(num.getValue().getClass()).unwrap(), num.getValue());
		} else if (parsedItem instanceof ParsedString) {
			if (actionRequired)
				throw new QuickParseException("String literal cannot be an action");
			result = ObservableValue.constant(TypeToken.of(String.class), ((ParsedString) parsedItem).getValue());
		} else if (parsedItem instanceof ParsedBoolean) {
			if (actionRequired)
				throw new QuickParseException("boolean literal cannot be an action");
			result = ObservableValue.constant(TypeToken.of(Boolean.TYPE), ((ParsedBoolean) parsedItem).getValue());
		} else if (parsedItem instanceof ParsedChar) {
			if (actionRequired)
				throw new QuickParseException("char literal cannot be an action");
			result = ObservableValue.constant(TypeToken.of(Character.TYPE), ((ParsedChar) parsedItem).getValue());
			// Now easy operations
		} else if (parsedItem instanceof ParsedParenthetic) {
			return evaluate(parseEnv, type, ((ParsedParenthetic) parsedItem).getContent(), actionAccepted, actionRequired);
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
			ObservableValue<?> var = evaluate(parseEnv, TypeToken.of(Object.class), instOf.getVariable(), actionAccepted, false);
			if (!testType.getRawType().isInterface() && !var.getType().getRawType().isInterface()) {
				if (testType.isAssignableFrom(var.getType())) {
					parseEnv.msg().warn(instOf.getVariable().getMatch().text + " is always an instance of " + parsedType.getMatch().text
						+ " (if non-null)");
					result = var.mapV(MathUtils.BOOLEAN, v -> v != null, true);
				} else if (!var.getType().isAssignableFrom(testType)) {
					parseEnv.msg().error(instOf.getVariable().getMatch().text + " is never an instance of " + parsedType.getMatch().text);
					result = ObservableValue.constant(MathUtils.BOOLEAN, false);
				} else {
					result = var.mapV(MathUtils.BOOLEAN, v -> testType.getRawType().isInstance(v), true);
				}
			} else {
				result = var.mapV(MathUtils.BOOLEAN, v -> testType.getRawType().isInstance(v), true);
			}
		} else if (parsedItem instanceof ParsedCast) {
			if (actionRequired)
				throw new QuickParseException("Cast cannot be an action");
			ParsedCast cast = (ParsedCast) parsedItem;
			TypeToken<?> testType = evaluateType(parseEnv, (ParsedType) cast.getType(), type);
			ObservableValue<?> var = evaluate(parseEnv, TypeToken.of(Object.class), cast.getValue(), actionAccepted, false);
			if (!testType.getRawType().isInterface() && !var.getType().getRawType().isInterface()) {
				if (testType.isAssignableFrom(var.getType())) {
					parseEnv.msg().warn(
						cast.getValue().getMatch().text + " is always an instance of " + cast.getType().getMatch().text + " (if non-null)");
					result = var.mapV((TypeToken<Object>) testType, v -> v, true);
				} else if (!var.getType().isAssignableFrom(testType)) {
					parseEnv.msg().error(cast.getValue().getMatch().text + " is never an instance of " + cast.getType().getMatch().text);
					result = var.mapV((TypeToken<Object>) testType, v -> {
						if (v == null)
							return null;
						else
							throw new ClassCastException(v + " is not an instance of " + testType);
					}, true);
				} else {
					result = var.mapV((TypeToken<Object>) testType, v -> {
						if (testType.getRawType().isInstance(v))
							return v;
						else
							throw new ClassCastException(v + " is not an instance of " + testType);
					}, true);
				}
			} else {
				result = var.mapV((TypeToken<Object>) testType, v -> {
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
			ObservableValue<?> arg1 = evaluate(parseEnv, TypeToken.of(Object.class), op.getOp(), actionAccepted, false);
			result = mapUnary(arg1, op);
		} else if (parsedItem instanceof ParsedBinaryOp) {
			ParsedBinaryOp op = (ParsedBinaryOp) parsedItem;
			if (actionRequired)
				throw new QuickParseException("Binary operation " + op.getName() + " cannot be an action");
			ObservableValue<?> arg1 = evaluate(parseEnv, TypeToken.of(Object.class), op.getOp1(), actionAccepted, false);
			ObservableValue<?> arg2 = evaluate(parseEnv, TypeToken.of(Object.class), op.getOp2(), actionAccepted, false);
			result = combineBinary(arg1, arg2, op);
		} else if (parsedItem instanceof ParsedConditional) {
			if (actionRequired)
				throw new QuickParseException("Conditionals cannot be an action");
			ParsedConditional cond = (ParsedConditional) parsedItem;
			ObservableValue<?> condition = evaluate(parseEnv, TypeToken.of(Object.class), cond.getCondition(), actionAccepted,
				false);
			if (!TypeToken.of(Boolean.class).isAssignableFrom(condition.getType().wrap()))
				throw new QuickParseException(
					"Condition in " + cond.getMatch().text + " evaluates to type " + condition.getType() + ", which is not boolean");
			ObservableValue<? extends T> affirm = evaluate(parseEnv, type, cond.getAffirmative(), actionAccepted, false);
			ObservableValue<? extends T> neg = evaluate(parseEnv, type, cond.getNegative(), actionAccepted, false);
			return ObservableValue.flatten(((ObservableValue<Boolean>) condition).mapV(v -> v ? affirm : neg));
			// Assignments
		} else if (parsedItem instanceof ParsedAssignmentOperator) {
			if (!actionAccepted)
				throw new QuickParseException("Assignment operator must be an action");
			ParsedAssignmentOperator op = (ParsedAssignmentOperator) parsedItem;
			ObservableValue<?> arg1 = evaluate(parseEnv, TypeToken.of(Object.class), op.getVariable(), actionAccepted, false);
			if (!(arg1 instanceof SettableValue))
				throw new QuickParseException(op.getVariable().getMatch().text + " does not parse to a settable value");
			SettableValue<Object> settable = (SettableValue<Object>) arg1;
			ObservableValue<?> assignValue;
			if (op.getOperand() != null) {
				ObservableValue<?> arg2 = evaluate(parseEnv, TypeToken.of(Object.class), op.getOperand(), actionAccepted, false);
				assignValue = getBinaryAssignValue(settable, arg2, op);
			} else
				assignValue = getUnaryAssignValue(settable, op);
			result = makeActionValue(settable, assignValue, op.isPrefix());
			// Array operations
		} else if (parsedItem instanceof ParsedArrayIndex) {
			if (actionRequired)
				throw new QuickParseException("Array index operation cannot be an action");
			ParsedArrayIndex pai = (ParsedArrayIndex) parsedItem;
			ObservableValue<?> array = evaluate(parseEnv, TypeToken.of(Object.class), pai.getArray(), actionAccepted, false);
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
			ObservableValue<?> index = evaluate(parseEnv, TypeToken.of(Object.class), pai.getIndex(), actionAccepted, false);
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
					ObservableValue<?> size_i = evaluate(parseEnv, TypeToken.of(Object.class), arrayInit.getSizes()[i],
						actionAccepted, false);
					if (!QuickUtils.isAssignableFrom(TypeToken.of(Integer.TYPE), size_i.getType()))
						throw new QuickParseException("Array size " + arrayInit.getSizes()[i].getMatch().text + " parses to type "
							+ size_i.getType() + ", which is not a valid array size type");
					sizes[i] = (ObservableValue<? extends Number>) size_i;
				}

				TypeToken<?> fArrayType = arrayType;
				result = new ObservableValue.ComposedObservableValue<>((TypeToken<Object>) arrayType,
					new Function<Object[], Object>() {
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
					ObservableValue<?> element_i = evaluate(parseEnv, TypeToken.of(Object.class), arrayInit.getElements()[i],
						actionAccepted, false);
					if (!QuickUtils.isAssignableFrom(componentType, element_i.getType()))
						throw new QuickParseException("Array element " + arrayInit.getElements()[i].getMatch().text + " parses to type "
							+ element_i.getType() + ", which cannot be cast to " + componentType);
					elements[i] = element_i;
				}

				TypeToken<?> fArrayType = arrayType;
				result = new ObservableValue.ComposedObservableValue<>((TypeToken<Object>) arrayType,
					new Function<Object[], Object>() {
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
			result = parseEnv.getContext().getVariable(name);
			if (result == null)
				throw new QuickParseException("No such variable " + name);
		} else if (parsedItem instanceof ParsedPlaceHolder) {
			result = parseEnv.getContext().getVariable(((ParsedPlaceHolder) parsedItem).getName());
			if (result == null)
				throw new QuickParseException("Unrecognized placeholder: " + parsedItem.getMatch().text);
		} else if (parsedItem instanceof ParsedUnitValue) {
			if (actionRequired)
				throw new QuickParseException("Unit value cannot be an action");
			ParsedUnitValue unitValue=(ParsedUnitValue) parsedItem;
			String unitName=unitValue.getUnit();
			Unit<?, ?> unit=parseEnv.getContext().getUnit(unitName);
			if(unit==null)
				throw new QuickParseException("Unrecognized unit "+unitName);
			ObservableValue<?> value=evaluate(parseEnv, unit.getFromType(), unitValue.getValue(), actionAccepted, false);
			result=value.mapV(v->{
				try{
					return ((Unit<Object, ?>) unit).getMap().apply(v);
				} catch(QuickParseException e){
					parseEnv.msg().error("Unit "+unit.getName()+" could not convert value "+v+" to "+unit.getToType(), e);
					return null; // TODO What to do with this?
				}
			});
			// Now harder operations
		} else if (parsedItem instanceof ParsedConstructor) {
			if (actionRequired)
				throw new QuickParseException("Constructor cannot be an action");
			ParsedConstructor constructor = (ParsedConstructor) parsedItem;
			TypeToken<?> typeToCreate = evaluateType(parseEnv, constructor.getType(), type);
			// TODO
			// Evaluate type parameters to see if the result of the constructor can be assigned to the type
			// Sub-evaluate the arguments and pass to the constructor
		} else if (parsedItem instanceof ParsedMethod) {
			ParsedMethod method = (ParsedMethod) parsedItem;
			if (actionRequired && !method.isMethod())
				throw new QuickParseException("Field access cannot be an action");
			// TODO
			// Need to go to the root context and see if it is a variable.
			// If so, we'll just sub-evaluate the context and invoke the method/field reflectively (maybe use TypeToken.method(Method)?
			// If not, see if the context is a type. May or may not be a ParsedType.
			// If so, evaluate the field/method statically (remember to account for things like java.Type.class)
			// Otherwise, throw an exception
			// field/method (including statically-invoked)
		} else
			throw new QuickParseException("Unrecognized parsed item type: " + parsedItem.getClass());

		if (!QuickUtils.isAssignableFrom(type, result.getType()))
			throw new QuickParseException(parsedItem.getMatch().text + " evaluates to type " + result.getType()
				+ ", which is not compatible with expected type " + type);
		return result;
	}

	private TypeToken<?> evaluateType(QuickParseEnv parseEnv, ParsedType parsedType, TypeToken<?> expected) throws QuickParseException {
		String name = parsedType.getName();
		if (name == null) { // Pure wildcard type
			return new TypeToken<List<?>>() {}.resolveType(List.class.getTypeParameters()[0]);
		}
		Class<?> base = null;
		try {
			if (name.contains(".")) {
				base = parseEnv.cv().loadIfMapped(name, null);
			}
			if (base == null)
				base = parseEnv.cv().loadClass(name);
		} catch (QuickException | ClassNotFoundException e) {
			throw new QuickParseException("Could not load class " + name, e);
		}
		// TODO
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

	public static class ParsedUnitValue extends ParsedItem {
		private ParsedItem theValue;
		private String theUnit;

		@Override
		public void setup(PrismsParser parser, ParsedItem parent, ParseMatch match) throws ParseException {
			super.setup(parser, parent, match);
			theValue = parser.parseStructures(this, getStored("value"))[0];
			theUnit = getStored("unit").text;
		}

		public ParsedItem getValue() {
			return theValue;
		}

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

	public static class ParsedPlaceHolder extends ParsedItem {
		private String theName;

		@Override
		public void setup(PrismsParser parser, ParsedItem parent, ParseMatch match) throws ParseException {
			super.setup(parser, parent, match);
			theName = getStored("name").text;
		}

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
