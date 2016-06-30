package org.quick.core.parser;

import java.io.IOException;
import java.util.List;
import java.util.function.BiFunction;

import org.observe.ObservableAction;
import org.observe.ObservableValue;
import org.observe.SettableValue;
import org.observe.collect.ObservableList;
import org.observe.collect.ObservableOrderedCollection;
import org.quick.core.QuickEnvironment;
import org.quick.core.QuickParseEnv;
import org.quick.core.prop.ExpressionResult;
import org.quick.util.QuickUtils;

import com.google.common.reflect.TypeParameter;
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
		return evaluate(parseEnv, type, item, action);
	}

	private <T> ObservableValue<? extends T> evaluate(QuickParseEnv parseEnv, TypeToken<T> type, ParsedItem parsedItem, boolean action)
		throws QuickParseException {
		ObservableValue<?> result;
		if (parsedItem instanceof ParsedArrayIndex) {
			if (action)
				throw new QuickParseException("Array index operation cannot be an action");
			ParsedArrayIndex pai = (ParsedArrayIndex) parsedItem;
			ObservableValue<?> array = evaluate(parseEnv, TypeToken.of(Object.class), pai.getArray(), false);
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
			ObservableValue<?> index = evaluate(parseEnv, TypeToken.of(Object.class), pai.getIndex(), false);
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
		} else if (parsedItem instanceof ParsedIdentifier) {
			if (action)
				throw new QuickParseException("Identifier cannot be an action");
			// TODO
			// model
			// type
		} else if(parsedItem instanceof ParsedMethod){
			// TODO
			// type
			// field/method (including statically-invoked)
		} else if (parsedItem instanceof ParsedParenthetic) {
			return evaluate(parseEnv, type, ((ParsedParenthetic) parsedItem).getContent(), action);
		} else if (parsedItem instanceof ParsedArrayInitializer) {
			if (action)
				throw new QuickParseException("Array init operation cannot be an action");
			// TODO
		} else if (parsedItem instanceof ParsedAssignmentOperator) {
			if (!action)
				throw new QuickParseException("Assignment operator must be an action");
			// TODO
		} else if (parsedItem instanceof ParsedBinaryOp) {
			ParsedBinaryOp op = (ParsedBinaryOp) parsedItem;
			ObservableValue<?> arg1 = evaluate(parseEnv, TypeToken.of(Object.class), op.getOp1(), false);
			ObservableValue<?> arg2 = evaluate(parseEnv, TypeToken.of(Object.class), op.getOp2(), false);
			result = combineBinary(arg1, arg2, op, action);
			// TODO
		} else if (parsedItem instanceof ParsedCast) {
			if (action)
				throw new QuickParseException("Cast cannot be an action");
			// TODO
		} else if (parsedItem instanceof ParsedConditional) {
			if (action)
				throw new QuickParseException("Conditionals cannot be an action");
			ParsedConditional cond=(ParsedConditional) parsedItem;
			ObservableValue<?> condition = evaluate(parseEnv, TypeToken.of(Object.class), cond.getCondition(), false);
			if (!TypeToken.of(Boolean.class).isAssignableFrom(condition.getType().wrap()))
				throw new QuickParseException(
					"Condition in " + cond.getMatch().text + " evaluates to type " + condition.getType() + ", which is not boolean");
			ObservableValue<? extends T> affirm = evaluate(parseEnv, type, cond.getAffirmative(), false);
			ObservableValue<? extends T> neg = evaluate(parseEnv, type, cond.getNegative(), false);
			return ObservableValue.flatten(((ObservableValue<Boolean>) condition).mapV(v -> v ? affirm : neg));
			// TODO
		} else if (parsedItem instanceof ParsedConstructor) {
			if (action)
				throw new QuickParseException("Constructor cannot be an action");
			// TODO
		} else if (parsedItem instanceof ParsedInstanceofOp) {
			if (action)
				throw new QuickParseException("instanceof operation cannot be an action");
			// TODO
		} else if (parsedItem instanceof ParsedBoolean) {
			if (action)
				throw new QuickParseException("boolean literal cannot be an action");
			result = ObservableValue.constant(TypeToken.of(Boolean.TYPE), ((ParsedBoolean) parsedItem).getValue());
		} else if (parsedItem instanceof ParsedChar) {
			if (action)
				throw new QuickParseException("char literal cannot be an action");
			result = ObservableValue.constant(TypeToken.of(Character.TYPE), ((ParsedChar) parsedItem).getValue());
		} else if (parsedItem instanceof ParsedNull) {
			if (action)
				throw new QuickParseException("null literal cannot be an action");
			// TODO
		} else if (parsedItem instanceof ParsedNumber) {
			if (action)
				throw new QuickParseException("number literal cannot be an action");
			ParsedNumber num = (ParsedNumber) parsedItem;
			result = ObservableValue.constant((TypeToken<Number>) TypeToken.of(num.getValue().getClass()).unwrap(), num.getValue());
		} else if (parsedItem instanceof ParsedString) {
			if (action)
				throw new QuickParseException("String literal cannot be an action");
			result = ObservableValue.constant(TypeToken.of(String.class), ((ParsedString) parsedItem).getValue());
		} else if (parsedItem instanceof ParsedType) {
			if (action)
				throw new QuickParseException("Type cannot be an action");
			// TODO
		} else if (parsedItem instanceof ParsedUnaryOp) {
			if (action)
				throw new QuickParseException("Unary operation " + ((ParsedUnaryOp) parsedItem).getName() + " cannot be an action");
			// TODO
		} else if (parsedItem instanceof ParsedUnitValue) {
			if (action)
				throw new QuickParseException("Unit value cannot be an action");
			// TODO
		} else if (parsedItem instanceof ParsedPlaceHolder) {
			ExpressionResult<?> res = parseEnv.getContext().getVariable(((ParsedPlaceHolder) parsedItem).getName());
			if (res == null)
				throw new QuickParseException("Unrecognized placeholder: " + parsedItem.getMatch().text);
			if (res.type.isNull)
				return ObservableValue.constant(type, null);
			else if (res.type.isType)
				result = classWrap(res.type.type, res.type.type.getRawType());
			else
				result = res.value;
		} else
			throw new QuickParseException("Unrecognized parsed item type: " + parsedItem.getClass());

		if (!QuickUtils.isAssignableFrom(type, result.getType()))
			throw new QuickParseException(parsedItem.getMatch().text + " evaluates to type " + result.getType()
				+ ", which is not compatible with expected type " + type);
		return result;
	}

	private static <T> ObservableValue<Class<T>> classWrap(TypeToken<T> type, Class<?> clazz) {
		return ObservableValue.constant(new TypeToken<Class<T>>() {}.where(new TypeParameter<T>() {}, type), (Class<T>) clazz);
	}

	private static ObservableValue<?> combineBinary(ObservableValue<?> arg1, ObservableValue<?> arg2, ParsedBinaryOp op, boolean action)
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
				if (action)
					throw new QuickParseException("Binary operation " + op.getName() + " cannot be an action");
				return MathUtils.binaryMathOp(op.getName(), arg1, arg2);
			// Assignment operators
			case "=":
				if (!action)
					throw new QuickParseException("Binary operation " + op.getName() + " must be an action");
				if (!(arg1 instanceof SettableValue))
					throw new QuickParseException(op.getOp1().getMatch().text + " does not parse to a settable value");
				if (!QuickUtils.isAssignableFrom(arg1.getType(), arg2.getType()))
					throw new QuickParseException(
						op.getOp2().getMatch().text + ", type " + arg2.getType() + ", cannot be assigned to type " + arg1.getType());
				return ObservableValue.constant(TypeToken.of(ObservableAction.class), ((SettableValue<Object>) arg1).assignmentTo(arg2));
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
				if (!action)
					throw new QuickParseException("Binary operation " + op.getName() + " must be an action");
				if (!(arg1 instanceof SettableValue))
					throw new QuickParseException(op.getOp1().getMatch().text + " does not parse to a settable value");
				String mathOp = op.getName().substring(0, op.getName().length() - 1);
				ObservableValue<?> result = MathUtils.binaryMathOp(mathOp, arg1, arg2);
				if (!QuickUtils.isAssignableFrom(arg1.getType(), result.getType()))
					throw new QuickParseException(op.getOp1().getMatch().text + " " + mathOp + " " + op.getOp2().getMatch().text + ", type "
						+ arg2.getType() + ", cannot be assigned to type " + arg1.getType());
				return ObservableValue.constant(TypeToken.of(ObservableAction.class), ((SettableValue<Object>) arg1).assignmentTo(result));
			default:
				throw new QuickParseException("Unrecognized binary operator: " + op.getName());
			}
		} catch (IllegalArgumentException e) {
			throw new QuickParseException(e.getMessage(), e);
		}
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
