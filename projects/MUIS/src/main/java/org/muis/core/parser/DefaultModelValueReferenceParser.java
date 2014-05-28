package org.muis.core.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.muis.core.model.ModelValueReferenceParser;
import org.muis.core.model.MuisAppModel;
import org.muis.core.rx.ObservableValue;

import prisms.lang.*;
import prisms.lang.types.*;
import prisms.util.ProgramTracker;

/** The default implementation of {@link ModelValueReferenceParser} */
public class DefaultModelValueReferenceParser implements ModelValueReferenceParser {
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
				setupParser.parseStructures(new prisms.lang.ParseStructRoot(command), setupParser.parseMatches(command))[0].evaluate(
					MVX_ENV, false, true);
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
		theParseEnv=new EvaluationEnvironment(){
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
			public void load(InputStream in, PrismsParser parser) throws IOException {
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
		try {
			if(item instanceof ParsedMethod) {
			} else if(item instanceof ParsedIdentifier) {
				// TODO
			} else if(item instanceof ParsedConstructor) {
				// TODO
			} else if(item instanceof ParsedParenthetic) {
				// TODO
			} else if(item instanceof ParsedUnaryOp) {
				// TODO
			} else if(item instanceof ParsedBinaryOp) {
				// TODO
			} else if(item instanceof ParsedArrayIndex) {
				// TODO
			} else if(item instanceof ParsedConditional) {
				// TODO
			} else if(item instanceof ParsedType) {
				throw new MuisParseException("Parsed model value is not a value: " + item.toString());
			} else if(item instanceof ParsedCast) {
			} else if(item instanceof ParsedNumber || item instanceof ParsedString || item instanceof ParsedBoolean) {
				return ObservableValue.constant(item.evaluate(env, false, true));
			} else if(item instanceof ParsedNull) {
				return ObservableValue.constant(null);
			} else
				throw new MuisParseException("Unrecognized parsed item: " + item.getClass().getName());
		} catch(EvaluationException e) {
			throw new MuisParseException("Failed to parse model value", e);
		}
	}
}
