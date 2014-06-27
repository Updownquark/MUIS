package org.muis.core.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.muis.core.model.MuisValueReferenceParser;
import org.muis.core.model.MuisAppModel;
import org.muis.core.rx.ObservableValue;
import org.muis.core.rx.SettableValue;

import prisms.lang.*;
import prisms.lang.eval.MathUtils;
import prisms.lang.eval.PrismsEvaluator;
import prisms.lang.types.*;
import prisms.util.ProgramTracker;

/** The default implementation of {@link MuisValueReferenceParser} */
public class DefaultModelValueReferenceParser implements MuisValueReferenceParser {
	private static boolean DEBUG = false;

	private static final Pattern MVR_PATTERN;

	private static PrismsParser MVX_PARSER;
	private static DefaultEvaluationEnvironment MVX_ENV;

	static {
		Pattern p;
		try {
			p = Pattern.compile("\\$\\{.*\\}");
		} catch(RuntimeException e) {
			e.printStackTrace();
			p = null;
		}
		MVR_PATTERN = p;

		MVX_PARSER = new PrismsParser();
		try {
			MVX_PARSER.configure(StyleSheetParser.class.getResource("MVX.xml"));
		} catch(IOException e) {
			throw new IllegalStateException("Could not configure model value expression parser", e);
		}
		MVX_ENV = new DefaultEvaluationEnvironment();

		// Use the default prisms.lang Grammar.xml to implement some setup declarations to prepare the environment
		PrismsParser setupParser = new PrismsParser();
		try {
			setupParser.configure(PrismsParser.class.getResource("Grammar.xml"));
		} catch(IOException e) {
			throw new IllegalStateException("Could not configure style sheet setup parser", e);
		}
		prisms.lang.eval.PrismsEvaluator setupEvaluator = new prisms.lang.eval.PrismsEvaluator();
		prisms.lang.eval.DefaultEvaluation.initializeDefaults(setupEvaluator);
		if(DEBUG) {
			prisms.lang.debug.PrismsParserDebugGUI debugger = new prisms.lang.debug.PrismsParserDebugGUI();
			MVX_PARSER.setDebugger(debugger);
			setupParser.setDebugger(debugger);
			javax.swing.JFrame frame = prisms.lang.debug.PrismsParserDebugGUI.getDebuggerFrame(debugger);
			frame.pack();
			frame.setLocationRelativeTo(null);
		}

		ArrayList<String> commands = new ArrayList<>();
		// Add constants and functions like rgb(r, g, b) here
		commands.add("java.awt.Color rgb(int r, int g, int b){return " + org.muis.core.style.Colors.class.getName() + ".rgb(r, g, b);}");
		commands.add("java.awt.Color hsb(int h, int s, int b){return " + org.muis.core.style.Colors.class.getName() + ".hsb(h, s, b);}");
		commands.add("int round(double n){return (int) Math.round(n);}");
		commands.add("String toString(java.awt.Color c){return " + org.muis.core.style.Colors.class.getName() + ".toString(c);}");
		// TODO Add more constants and functions
		for(String command : commands) {
			try {
				setupEvaluator.evaluate(
					setupParser.parseStructures(new prisms.lang.ParseStructRoot(command), setupParser.parseMatches(command))[0], MVX_ENV,
					false, true);
			} catch(ParseException | EvaluationException e) {
				System.err.println("Could not execute XML stylesheet parser setup expression: " + command);
				e.printStackTrace();
			}
		}
	}

	private org.muis.core.MuisDocument theDocument;
	private EvaluationEnvironment theParseEnv;

	/** @param doc The document to get models from */
	public DefaultModelValueReferenceParser(org.muis.core.MuisDocument doc) {
		theDocument = doc;
		theParseEnv = new EvaluationEnvironment() {
			@Override
			public boolean usePublicOnly() {
				return true;
			}

			@Override
			public ClassGetter getClassGetter() {
				return MVX_ENV.getClassGetter();
			}

			@Override
			public Type getVariableType(String name) {
				MuisAppModel mv = theDocument.getHead().getModel(name);
				if(mv != null)
					return new Type(MuisAppModel.class);
				Type ret = MVX_ENV.getVariableType(name);
				if(ret != null)
					return ret;
				return null;
			}

			@Override
			public Object getVariable(String name, ParsedItem struct, int index) throws EvaluationException {
				MuisAppModel model = theDocument.getHead().getModel(name);
				if(model != null)
					return model;
				return MVX_ENV.getVariable(name, struct, index);
			}

			@Override
			public void declareVariable(String name, Type type, boolean isFinal, ParsedItem struct, int index) throws EvaluationException {
				throw new EvaluationException("Variables cannot be declared in model value expressions", struct, index);
			}

			@Override
			public void setVariable(String name, Object value, ParsedItem struct, int index) throws EvaluationException {
				throw new EvaluationException("Variables cannot be assigned in model value expressions", struct, index);
			}

			@Override
			public void dropVariable(String name, ParsedItem struct, int index) throws EvaluationException {
				throw new EvaluationException("Variables cannot be dropped in model value expressions", struct, index);
			}

			@Override
			public Variable [] getDeclaredVariables() {
				return MVX_ENV.getDeclaredVariables();
			}

			@Override
			public Variable getDeclaredVariable(String name) {
				MuisAppModel model = theDocument.getHead().getModel(name);
				if(model != null)
					return new Variable(new Type(MuisAppModel.class), name, true);
				return MVX_ENV.getDeclaredVariable(name);
			}

			@Override
			public void declareFunction(ParsedFunctionDeclaration function) {
				throw new IllegalStateException("Functions cannot be declared in model value expressions");
			}

			@Override
			public ParsedFunctionDeclaration [] getDeclaredFunctions() {
				return MVX_ENV.getDeclaredFunctions();
			}

			@Override
			public void dropFunction(ParsedFunctionDeclaration function, ParsedItem struct, int index) throws EvaluationException {
				throw new EvaluationException("Functions cannot be dropped in model value expressions", struct, index);
			}

			@Override
			public void setReturnType(Type type) {
				throw new IllegalStateException("Scope is not supported");
			}

			@Override
			public Type getReturnType() {
				throw new IllegalStateException("Scope is not supported");
			}

			@Override
			public void setHandledExceptionTypes(Type [] types) {
				throw new IllegalStateException("Scope is not supported");
			}

			@Override
			public boolean canHandle(Type exType) {
				return true;
			}

			@Override
			public int getHistoryCount() {
				return 0;
			}

			@Override
			public Type getHistoryType(int index) {
				throw new IllegalStateException("History is not supported");
			}

			@Override
			public Object getHistory(int index) {
				throw new IllegalStateException("History is not supported");
			}

			@Override
			public void addHistory(Type type, Object result) {
				throw new IllegalStateException("History is not supported");
			}

			@Override
			public void clearHistory() {
			}

			@Override
			public void addImportType(Class<?> type) {
				throw new IllegalStateException("Imports are not supported");
			}

			@Override
			public void addImportPackage(String packageName) {
				throw new IllegalStateException("Imports are not supported");
			}

			@Override
			public String [] getImportPackages() {
				return new String[0];
			}

			@Override
			public void clearImportPackages() {
			}

			@Override
			public Class<?> getImportType(String name) {
				return MVX_ENV.getImportType(name);
			}

			@Override
			public Class<?> [] getImportTypes() {
				return MVX_ENV.getImportTypes();
			}

			@Override
			public void clearImportTypes() {
			}

			@Override
			public void addImportMethod(Class<?> type, String method) {
				throw new IllegalStateException("Imports are not supported");
			}

			@Override
			public ImportMethod [] getImportMethods() {
				return MVX_ENV.getImportMethods();
			}

			@Override
			public Class<?> getImportMethodType(String methodName) {
				return MVX_ENV.getImportMethodType(methodName);
			}

			@Override
			public void clearImportMethods() {
			}

			@Override
			public EvaluationEnvironment scope(boolean dependent) {
				throw new IllegalStateException("Scope is not supported");
			}

			@Override
			public EvaluationEnvironment transact() {
				throw new IllegalStateException("Transactions are not supported");
			}

			@Override
			public void cancel() {
				throw new IllegalStateException("Cancel is not supported");
			}

			@Override
			public boolean isCanceled() {
				return false;
			}

			@Override
			public void uncancel() {
				throw new IllegalStateException("Cancel is not supported");
			}

			@Override
			public ProgramTracker getTracker() {
				return null;
			}

			@Override
			public Variable [] save(OutputStream out) throws IOException {
				throw new IllegalStateException("Save is not supported");
			}

			@Override
			public void load(InputStream in, PrismsParser parser, PrismsEvaluator eval) throws IOException {
				throw new IllegalStateException("Load is not supported");
			}
		};
	}

	@Override
	public int getNextMVR(String value, int start) {
		return getNextModelValueReference(value, start);
	}

	@Override
	public String extractMVR(String value, int start) throws MuisParseException {
		return extractModelValueReference(value, start);
	}

	@Override
	public ObservableValue<?> parseMVR(String mvr) throws MuisParseException {
		return parseModelValueReference(mvr, theParseEnv);
	}

	/**
	 * @param value The value to inspect
	 * @param start The starting index to search
	 * @return The index in the value of the start of the next model value reference
	 */
	public static int getNextModelValueReference(String value, int start) {
		Matcher matcher = MVR_PATTERN.matcher(value);
		if(!matcher.find(start))
			return -1;
		return matcher.start();
	}

	/**
	 * @param value The value to parse
	 * @param start The location of the start of a model value reference
	 * @return The extracted model value reference
	 * @throws MuisParseException If an error occurs extracting the reference
	 */
	public static String extractModelValueReference(String value, int start) throws MuisParseException {
		Matcher matcher = MVR_PATTERN.matcher(value);
		if(!matcher.find(start) || matcher.start() != start)
			throw new MuisParseException("No MUIS model detected: " + value.substring(start));
		return matcher.group();
	}

	/**
	 * @param mvr The value to parse
	 * @param env The evaluation environment to use to evaluate the reference
	 * @return The parsed model value reference
	 * @throws MuisParseException If an error occurs parsing the reference
	 */
	public static ObservableValue<?> parseModelValueReference(String mvr, EvaluationEnvironment env) throws MuisParseException {
		Matcher matcher = MVR_PATTERN.matcher(mvr);
		if(!matcher.matches())
			throw new MuisParseException(mvr + " is not a recognized model value reference");
		String content = matcher.group(1);
		prisms.lang.ParsedItem [] items;
		try {
			prisms.lang.ParseMatch [] matches = MVX_PARSER.parseMatches(content);
			items = MVX_PARSER.parseStructures(new prisms.lang.ParseStructRoot(content), matches);
		} catch(ParseException e) {
			throw new MuisParseException("Could not parse model value expression", e);
		}
		if(items.length == 0)
			throw new MuisParseException("No model value: \"" + content + "\"");
		if(items.length > 1)
			throw new MuisParseException("Multiple model values specified: \"" + content + "\"");
		return evaluate(items[0], env);
	}

	private static ObservableValue<?> evaluate(ParsedItem item, EvaluationEnvironment env) throws MuisParseException {
		// TODO should we allow assignments for model values?
		try {
			if(item instanceof ParsedMethod) {
				// TODO
			} else if(item instanceof ParsedIdentifier) {
				// TODO
			} else if(item instanceof ParsedConstructor) {
				ParsedConstructor constructor = (ParsedConstructor) item;
				ObservableValue<?> [] args = new ObservableValue[constructor.getArguments().length];
				Type [] argTypes = new Type[args.length];
				for(int i = 0; i < args.length; i++) {
					args[i] = evaluate(constructor.getArguments()[i], env);
					argTypes[i] = args[i].getType();
				}
				Type type = constructor.getType().evaluate(env, true, false).getType();
				Class<?> base = type.getBaseType();
				StringBuilder constructorString = new StringBuilder();
				constructorString.append(type.getBaseType().getName()).append('(');
				for(int i = 0; i < argTypes.length; i++) {
					if(i > 0)
						constructorString.append(", ");
					constructorString.append(argTypes[i]);
				}
				java.lang.reflect.Constructor<?> toInvoke;
				try {
					toInvoke = constructor.getTarget(type, argTypes, true);
				} catch(EvaluationException e) {
					throw new MuisParseException("No such constructor " + constructorString, e);
				}

				if(args.length == 0) {
					org.muis.core.rx.DefaultObservableValue<?> ret = new org.muis.core.rx.DefaultObservableValue<Object>() {
						@Override
						public Type getType() {
							return type;
						}

						@Override
						public Object get() {
							try {
								return toInvoke.newInstance();
							} catch(InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
								throw new IllegalStateException("Could not instantiate " + type, e);
							}
						}
					};
					ret.control(null);
					return ret;
				}
				return new org.muis.core.rx.ComposedObservableValue<>(type, constructorArgs -> {
					try {
						return toInvoke.newInstance(constructorArgs);
					} catch(Exception e) {
						throw new IllegalArgumentException("Could not invoke constructor " + constructorString, e);
					}
				}, args);
			} else if(item instanceof ParsedParenthetic) {
				return evaluate(((ParsedParenthetic) item).getContent(), env);
			} else if(item instanceof ParsedUnaryOp) {
				ParsedUnaryOp op = (ParsedUnaryOp) item;
				ObservableValue<?> operand = evaluate(op.getOp(), env);
				return evaluateUnary(op.getName(), operand);
			} else if(item instanceof ParsedBinaryOp) {
				ParsedBinaryOp op = (ParsedBinaryOp) item;
				ObservableValue<?> left = evaluate(op.getOp1(), env);
				ObservableValue<?> right = evaluate(op.getOp2(), env);
				return evaluateBinary(op.getName(), left, right);
			} else if(item instanceof ParsedArrayIndex) {
				ParsedArrayIndex ai = (ParsedArrayIndex) item;
				ObservableValue<?> array = evaluate(ai.getArray(), env);
				ObservableValue<?> index = evaluate(ai.getIndex(), env);
				if(!array.getType().isArray() && !new Type(java.util.List.class).isAssignable(array.getType()))
					throw new MuisParseException("Array index cannot be applied to " + array.getType());
				if(!new Type(Integer.TYPE).isAssignable(index.getType()))
					throw new MuisParseException(index.getType() + " cannot be used as an array index");
				Type elType;
				if(array.getType().isArray())
					elType = array.getType().getComponentType();
				else if(array.getType().getParamTypes().length == 1)
					elType = array.getType().getParamTypes()[0];
				else
					elType = new Type(Object.class);
				return array.composeV(elType, (Object arr, Integer idx) -> {
					if(arr.getClass().isArray())
						return java.lang.reflect.Array.get(arr, idx);
					else
						return ((java.util.List<?>) arr).get(idx);
				}, (ObservableValue<Integer>) index);
			} else if(item instanceof ParsedCast) {
				ParsedCast cast = (ParsedCast) item;
				Type castType = cast.getType().evaluate(env, true, false).getType();
				ObservableValue<?> value = evaluate(cast.getValue(), env);
				if(castType.getCommonType(value.getType()) == null)
					throw new MuisParseException("Cannot cast from " + value.getType() + " to " + castType);
				return value.mapV(castType, v -> {
					return v;
				});
			} else if(item instanceof ParsedConditional) {
				ParsedConditional cond = (ParsedConditional) item;
				ObservableValue<Boolean> condition = (ObservableValue<Boolean>) evaluate(cond.getCondition(), env);
				if(!new Type(Boolean.TYPE).isAssignable(condition.getType()))
					throw new MuisParseException("A conditional must start with a boolean expression");
				ObservableValue<?> affirm = evaluate(cond.getAffirmative(), env);
				ObservableValue<?> negate = evaluate(cond.getNegative(), env);
				Type commonType = affirm.getType().getCommonType(negate.getType());
				return condition.composeV(commonType, (Boolean b, Object aff, Object neg) -> {
					return b ? aff : neg;
				}, affirm, negate);
			} else if(item instanceof ParsedType) {
				throw new MuisParseException("Parsed model value is not a value: " + item.toString());
			} else if(item instanceof ParsedCast) {
				Type castType = ((ParsedCast) item).getType().evaluate(env, true, false).getType();
				ObservableValue<?> value = evaluate(((ParsedCast) item).getValue(), env);
				if(castType.getCommonType(value.getType()) == null)
					throw new MuisParseException("Cannot cast " + value.getType() + " to " + castType);
				return value.mapV(castType, val -> {
					return val;
				});
			} else if(item instanceof ParsedNumber || item instanceof ParsedString || item instanceof ParsedChar
				|| item instanceof ParsedBoolean) {
				return ObservableValue.constant(item.evaluate(env, false, true));
			} else if(item instanceof ParsedNull) {
				return ObservableValue.constant(null);
			} else
				throw new MuisParseException("Unrecognized parsed item: " + item.getClass().getName());
		} catch(EvaluationException e) {
			throw new MuisParseException("Failed to parse model value", e);
		}
	}

	private static <T> ObservableValue<?> evaluateUnary(String opName, ObservableValue<T> operand) throws MuisParseException {
		switch (opName) {
		case "+":
			if(!operand.getType().isMathable())
				throw new MuisParseException("Operator " + opName + " cannot be applied to a value of type " + operand.getType());
			return operand; // No-op
		case "-":
			if(!operand.getType().isMathable())
				throw new MuisParseException("Operator " + opName + " cannot be applied to a value of type " + operand.getType());
			if(operand instanceof SettableValue)
				return ((SettableValue<T>) operand).mapV(value -> {
					return MathUtils.negate(value);
				}, value -> {
					return (T) operand.getType().cast(MathUtils.negate(value));
				});
			else
				return operand.mapV(operand.getType(), value -> {
					return MathUtils.negate(value);
				});
		case "!":
			if(!new Type(Boolean.TYPE).isAssignable(operand.getType()))
				throw new MuisParseException("Operator " + opName + " cannot be applied to a value of type " + operand.getType());
			Function<Boolean, Boolean> not = value -> {
				return !value;
			};
			if(operand instanceof SettableValue)
				return ((SettableValue<Boolean>) operand).mapV(not, not);
			else
				return ((ObservableValue<Boolean>) operand).mapV(not);
		case "~":
			if(!operand.getType().isIntMathable())
				throw new MuisParseException("Operator " + opName + " cannot be applied to a value of type " + operand.getType());
			Class<?> prim = Type.getPrimitiveType(operand.getType().getBaseType());
			if(prim == Double.TYPE || prim == Float.TYPE)
				throw new MuisParseException("Operator " + opName + " cannot be applied to a floating point type: " + operand.getType());
			if(operand instanceof SettableValue)
				return ((SettableValue<T>) operand).mapV(value -> {
					return MathUtils.complement(value);
				}, value -> {
					return (T) operand.getType().cast(MathUtils.complement(value));
				});
			else
				return operand.mapV(operand.getType(), value -> {
					return MathUtils.complement(value);
				});
		default:
			throw new MuisParseException("Unrecognized unary operator: " + opName);
		}
	}

	private static <L, R> ObservableValue<?> evaluateBinary(String opName, ObservableValue<L> left, ObservableValue<R> right)
		throws MuisParseException {
		switch (opName) {
		case "+":
			if(!left.getType().isMathable() || !right.getType().isMathable())
				throw new MuisParseException("Binary operator " + opName + " cannot be applied to " + left.getType() + " and "
					+ right.getType());
			if(left instanceof SettableValue) {
				return ((SettableValue<L>) left).composeV(left.getType().getCommonType(right.getType()), (L l, R r) -> {
					return MathUtils.add(left.getType(), l, right.getType(), r);
				}, right, (Number l, R r) -> {
					return (L) left.getType().cast(MathUtils.subtract(left.getType(), l, right.getType(), r));
				});
			} else
				return left.composeV(left.getType().getCommonType(right.getType()), (Object l, Object r) -> {
					return MathUtils.add(left.getType(), l, right.getType(), r);
				}, right);
		case "-":
			if(!left.getType().isMathable() || !right.getType().isMathable())
				throw new MuisParseException("Binary operator " + opName + " cannot be applied to " + left.getType() + " and "
					+ right.getType());
			if(left instanceof SettableValue) {
				return ((SettableValue<L>) left).composeV(left.getType().getCommonType(right.getType()), (L l, R r) -> {
					return MathUtils.subtract(left.getType(), l, right.getType(), r);
				}, right, (Number l, R r) -> {
					return (L) left.getType().cast(MathUtils.add(left.getType(), l, right.getType(), r));
				});
			} else
				return left.composeV(left.getType().getCommonType(right.getType()), (Object l, Object r) -> {
					return MathUtils.subtract(left.getType(), l, right.getType(), r);
				}, right);
		case "*":
			if(!left.getType().isMathable() || !right.getType().isMathable())
				throw new MuisParseException("Binary operator " + opName + " cannot be applied to " + left.getType() + " and "
					+ right.getType());
			if(left instanceof SettableValue) {
				return ((SettableValue<L>) left).composeV(left.getType().getCommonType(right.getType()), (L l, R r) -> {
					return MathUtils.multiply(left.getType(), l, right.getType(), r);
				}, right, (Number l, R r) -> {
					return (L) left.getType().cast(MathUtils.divide(left.getType(), l, right.getType(), r));
				});
			} else
				return left.composeV(left.getType().getCommonType(right.getType()), (Object l, Object r) -> {
					return MathUtils.multiply(left.getType(), l, right.getType(), r);
				}, right);
		case "/":
			if(!left.getType().isMathable() || !right.getType().isMathable())
				throw new MuisParseException("Binary operator " + opName + " cannot be applied to " + left.getType() + " and "
					+ right.getType());
			if(left instanceof SettableValue) {
				return ((SettableValue<L>) left).composeV(left.getType().getCommonType(right.getType()), (L l, R r) -> {
					return MathUtils.divide(left.getType(), l, right.getType(), r);
				}, right, (Number l, R r) -> {
					return (L) left.getType().cast(MathUtils.multiply(left.getType(), l, right.getType(), r));
				});
			} else
				return left.composeV(left.getType().getCommonType(right.getType()), (Object l, Object r) -> {
					return MathUtils.divide(left.getType(), l, right.getType(), r);
				}, right);
		case "%":
			if(!left.getType().isMathable() || !right.getType().isMathable())
				throw new MuisParseException("Binary operator " + opName + " cannot be applied to " + left.getType() + " and "
					+ right.getType());
			return left.composeV(left.getType().getCommonType(right.getType()), (Object l, Object r) -> {
				return MathUtils.modulo(left.getType(), l, right.getType(), r);
			}, right);
		case "==":
			ObservableValue<Integer> comp = compare(left, right, opName, true);
			if(comp instanceof SettableValue)
				return ((SettableValue<Integer>) comp).mapV(val -> {
					return val == 0;
				}, val -> {
					return val ? 0 : 1;
				});
			else
				return comp.mapV(val -> {
					return val == 0;
				});
		case "!=":
			comp = compare(left, right, opName, true);
			if(comp instanceof SettableValue)
				return ((SettableValue<Integer>) comp).mapV(val -> {
					return val == 0;
				}, val -> {
					return val ? 1 : 0;
				});
			else
				return comp.mapV(val -> {
					return val != 0;
				});
		case ">":
			return compare(left, right, opName, true).mapV(val -> {
				return val > 0;
			});
		case ">=":
			return compare(left, right, opName, true).mapV(val -> {
				return val >= 0;
			});
		case "<":
			return compare(left, right, opName, true).mapV(val -> {
				return val < 0;
			});
		case "<=":
			return compare(left, right, opName, true).mapV(val -> {
				return val <= 0;
			});
		case "&&":
			if(!left.getType().canAssignTo(Boolean.TYPE) || !right.getType().canAssignTo(Boolean.TYPE))
				throw new MuisParseException("Binary operator " + opName + " cannot be applied to " + left.getType() + " and "
					+ right.getType());
			return left.composeV(left.getType().getCommonType(right.getType()), (Object l, Object r) -> {
				return ((Boolean) l).booleanValue() && ((Boolean) r).booleanValue();
			}, right);
		case "||":
			if(!left.getType().canAssignTo(Boolean.TYPE) || !right.getType().canAssignTo(Boolean.TYPE))
				throw new MuisParseException("Binary operator " + opName + " cannot be applied to " + left.getType() + " and "
					+ right.getType());
			return left.composeV(left.getType().getCommonType(right.getType()), (Object l, Object r) -> {
				return ((Boolean) l).booleanValue() || ((Boolean) r).booleanValue();
			}, right);
		case "<<":
			if(!left.getType().isIntMathable() || !right.getType().isIntMathable())
				throw new MuisParseException("Binary operator " + opName + " cannot be applied to " + left.getType() + " and "
					+ right.getType());
			return left.composeV(left.getType().getCommonType(right.getType()), (Object l, Object r) -> {
				return MathUtils.leftShift(left.getType(), l, right.getType(), r);
			}, right);
		case ">>":
			if(!left.getType().isIntMathable() || !right.getType().isIntMathable())
				throw new MuisParseException("Binary operator " + opName + " cannot be applied to " + left.getType() + " and "
					+ right.getType());
			return left.composeV(left.getType().getCommonType(right.getType()), (Object l, Object r) -> {
				return MathUtils.rightShift(left.getType(), l, right.getType(), r);
			}, right);
		case ">>>":
			if(!left.getType().isIntMathable() || !right.getType().isIntMathable())
				throw new MuisParseException("Binary operator " + opName + " cannot be applied to " + left.getType() + " and "
					+ right.getType());
			return left.composeV(left.getType().getCommonType(right.getType()), (Object l, Object r) -> {
				return MathUtils.rightShiftUnsigned(left.getType(), l, right.getType(), r);
			}, right);
		case "&":
			boolean isBool = left.getType().canAssignTo(Boolean.TYPE) && right.getType().canAssignTo(Boolean.TYPE);
			if(!isBool && (!left.getType().isIntMathable() || !right.getType().isIntMathable()))
				throw new MuisParseException("Binary operator " + opName + " cannot be applied to " + left.getType() + " and "
					+ right.getType());
			return left.composeV(left.getType().getCommonType(right.getType()), (Object l, Object r) -> {
				if(isBool) {
					return ((Boolean) l).booleanValue() & ((Boolean) r).booleanValue();
				}
				if(left.getType().canAssignTo(Integer.TYPE) && right.getType().canAssignTo(Integer.TYPE)) {
					Type intType = new Type(Integer.TYPE);
					return ((Number) intType.cast(l)).intValue() & ((Number) intType.cast(r)).intValue();
				} else {
					Type longType = new Type(Long.TYPE);
					return ((Number) longType.cast(l)).longValue() & ((Number) longType.cast(r)).longValue();
				}
			}, right);
		case "|":
			isBool = left.getType().canAssignTo(Boolean.TYPE) && right.getType().canAssignTo(Boolean.TYPE);
			if(!isBool && (!left.getType().isIntMathable() || !right.getType().isIntMathable()))
				throw new MuisParseException("Binary operator " + opName + " cannot be applied to " + left.getType() + " and "
					+ right.getType());
			return left.composeV(left.getType().getCommonType(right.getType()), (Object l, Object r) -> {
				if(isBool) {
					return ((Boolean) l).booleanValue() | ((Boolean) r).booleanValue();
				}
				if(left.getType().canAssignTo(Integer.TYPE) && right.getType().canAssignTo(Integer.TYPE)) {
					Type intType = new Type(Integer.TYPE);
					return ((Number) intType.cast(l)).intValue() | ((Number) intType.cast(r)).intValue();
				} else {
					Type longType = new Type(Long.TYPE);
					return ((Number) longType.cast(l)).longValue() | ((Number) longType.cast(r)).longValue();
				}
			}, right);
		case "^":
			isBool = left.getType().canAssignTo(Boolean.TYPE) && right.getType().canAssignTo(Boolean.TYPE);
			if(!isBool && (!left.getType().isIntMathable() || !right.getType().isIntMathable()))
				throw new MuisParseException("Binary operator " + opName + " cannot be applied to " + left.getType() + " and "
					+ right.getType());
			return left.composeV(left.getType().getCommonType(right.getType()), (Object l, Object r) -> {
				if(isBool) {
					return ((Boolean) l).booleanValue() ^ ((Boolean) r).booleanValue();
				}
				if(left.getType().canAssignTo(Integer.TYPE) && right.getType().canAssignTo(Integer.TYPE)) {
					Type intType = new Type(Integer.TYPE);
					return ((Number) intType.cast(l)).intValue() ^ ((Number) intType.cast(r)).intValue();
				} else {
					Type longType = new Type(Long.TYPE);
					return ((Number) longType.cast(l)).longValue() ^ ((Number) longType.cast(r)).longValue();
				}
			}, right);
		default:
			throw new MuisParseException("Unrecognized binary operator " + opName);
		}
	}

	private static <L, R> ObservableValue<Integer> compare(ObservableValue<L> left, ObservableValue<R> right, String opName,
		boolean equality) throws MuisParseException {
		Type commonType = left.getType().getCommonType(right.getType());
		if(commonType == null)
			throw new MuisParseException("Binary operator " + opName + " cannot be applied to " + left.getType() + " and "
				+ right.getType());
		boolean comparable;
		if(left.getType().canAssignTo(Comparable.class) && left.getType().canAssignTo(Comparable.class)) {
			Type leftParam = left.getType().getParamTypes().length == 1 ? left.getType().getParamTypes()[0] : null;
			Type rightParam = right.getType().getParamTypes().length == 1 ? right.getType().getParamTypes()[0] : null;
			if(leftParam == null || rightParam == null) {
				comparable = false;
				if(!equality)
					throw new MuisParseException("Binary operator " + opName + " cannot be applied to " + left.getType() + " and "
						+ right.getType());
			} else if(!leftParam.isAssignable(rightParam)) {
				comparable = false;
				if(!equality)
					throw new MuisParseException("Binary operator " + opName + " cannot be applied to " + left.getType() + " and "
						+ right.getType());
			} else
				comparable = true;
		} else
			comparable = false;
		if(!comparable && !equality) {
			if(!left.getType().isMathable() || !right.getType().isMathable())
				throw new MuisParseException("Binary operator " + opName + " cannot be applied to " + left.getType() + " and "
					+ right.getType());
		}
		BiFunction<L, R, Integer> function = (L l, R r) -> {
			if(left.getType().canAssignTo(Integer.TYPE) && right.getType().canAssignTo(Integer.TYPE)) {
				Type intType = new Type(Integer.TYPE);
				int leftInt = ((Number) intType.cast(l)).intValue();
				int rightInt = ((Number) intType.cast(r)).intValue();
				return leftInt > rightInt ? 1 : (leftInt < rightInt ? -1 : 0);
			} else if(left.getType().canAssignTo(Long.TYPE) && right.getType().canAssignTo(Long.TYPE)) {
				Type longType = new Type(Long.TYPE);
				long leftLong = ((Number) longType.cast(l)).longValue();
				long rightLong = ((Number) longType.cast(r)).longValue();
				return leftLong > rightLong ? 1 : (leftLong < rightLong ? -1 : 0);
			} else if(left.getType().canAssignTo(Float.TYPE) && right.getType().canAssignTo(Float.TYPE)) {
				Type floatType = new Type(Float.TYPE);
				int leftFloat = ((Number) floatType.cast(l)).intValue();
				int rightFloat = ((Number) floatType.cast(r)).intValue();
				return leftFloat > rightFloat ? 1 : (leftFloat < rightFloat ? -1 : 0);
			} else if(left.getType().canAssignTo(Double.TYPE) && right.getType().canAssignTo(Double.TYPE)) {
				Type doubleType = new Type(Double.TYPE);
				int leftDouble = ((Number) doubleType.cast(l)).intValue();
				int rightDouble = ((Number) doubleType.cast(r)).intValue();
				return leftDouble > rightDouble ? 1 : (leftDouble < rightDouble ? -1 : 0);
			} else if(comparable)
				return ((Comparable<Object>) l).compareTo(r);
			else
				return l == r ? 0 : 1;
		};
		if(equality && left instanceof SettableValue && left.getType().isAssignable(right.getType())) {
			return ((SettableValue<L>) left).composeV(commonType, function, right, (Integer eq, R r) -> {
				return eq.intValue() == 0 ? null : "No method available to assert inequality";
			}, (Integer eq, R r) -> {
				return (L) left.getType().cast(r);
			});
		} else
			return left.composeV(commonType, function, right);
	}
}
