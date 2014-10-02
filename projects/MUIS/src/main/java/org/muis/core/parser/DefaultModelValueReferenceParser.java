package org.muis.core.parser;

import java.io.IOException;
import java.util.ArrayList;

import org.muis.core.MuisClassView;
import org.muis.core.MuisException;
import org.muis.core.eval.impl.ObservableEvaluator;
import org.muis.core.model.MuisValueReferenceParser;
import org.muis.core.rx.ObservableValue;

import prisms.lang.*;
import prisms.lang.eval.PrismsEvaluator;
import prisms.lang.eval.PrismsItemEvaluator;
import prisms.lang.types.ParsedType;

/** The default implementation of {@link MuisValueReferenceParser} */
public class DefaultModelValueReferenceParser implements MuisValueReferenceParser {
	private static boolean DEBUG = true;

	private static PrismsParser MVX_PARSER;
	private static ObservableEvaluator MVX_EVALUATOR;
	private static DefaultEvaluationEnvironment MVX_ENV;

	static {
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
		MVX_PARSER.validateConfig();

		MVX_EVALUATOR = new ObservableEvaluator();
		prisms.lang.eval.DefaultEvaluation.initializeBasics(MVX_EVALUATOR);
		prisms.lang.eval.DefaultEvaluation.initializeConstructors(MVX_EVALUATOR);
		MVX_EVALUATOR.seal();

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

	/** The base of all model parsers in MUIS */
	public static final DefaultModelValueReferenceParser BASE = new DefaultModelValueReferenceParser(null, null);

	private MuisValueReferenceParser theSuperParser;
	private PrismsParser theParser;
	private ObservableEvaluator theEvaluator;
	private EvaluationEnvironment theEnv;
	private final MuisClassView theClassView;

	/**
	 * @param superParser The parser to extend
	 * @param classView The class view to get mapped types from
	 */
	public DefaultModelValueReferenceParser(MuisValueReferenceParser superParser, MuisClassView classView) {
		theSuperParser = superParser;
		theParser = new WrappingPrismsParser(superParser != null ? superParser.getParser() : MVX_PARSER);
		theEvaluator = new WrappingObservableEvaluator(superParser != null ? superParser.getEvaluator() : MVX_EVALUATOR);
		theEnv = (superParser != null ? superParser.getEvaluationEnvironment() : MVX_ENV).scope(true);
		applyModification();
		theParser.validateConfig();
		theEvaluator.seal();
		theClassView = classView;
	}

	/** Allows subclasses to modify their parser, evaluator, and environment before being used */
	protected void applyModification() {
		if(theClassView != null) {
			// Add evaluation capability for namespace-qualified or unqualified tag types
			PrismsItemEvaluator<? super ParsedType> superEval = getEvaluator().getEvaluatorFor(ParsedType.class);
			getEvaluator().addEvaluator(ParsedType.class, new PrismsItemEvaluator<ParsedType>() {
				@Override
				public EvaluationResult evaluate(ParsedType item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType,
					boolean withValues) throws EvaluationException {
					ParseMatch namespace = item.getStored("namespace");
					String tagName = item.getName();
					String className = theClassView.getMappedClass(namespace == null ? null : namespace.text, tagName);
					if(className != null)
						try {
							return new EvaluationResult(new Type(theClassView.loadMappedClass(className, Object.class)));
						} catch(MuisException e) {
							// Need access to a message center?
							e.printStackTrace();
						}
					return superEval.evaluate(item, evaluator, env, asType, withValues);
				}
			});
		}
	}

	/** @return The parser that this parser inherits definitions from */
	public MuisValueReferenceParser getSuperParser() {
		return theSuperParser;
	}

	@Override
	public PrismsParser getParser() {
		return theParser;
	}

	@Override
	public ObservableEvaluator getEvaluator() {
		return theEvaluator;
	}

	@Override
	public EvaluationEnvironment getEvaluationEnvironment() {
		return theEnv;
	}

	@Override
	public ObservableValue<?> parse(String mvr, boolean asType) throws MuisParseException {
		ParsedItem item;
		try {
			ParseMatch [] matches = theParser.parseMatches(mvr);
			if(matches.length != 1)
				throw new MuisParseException("Exactly one expression expected for property value: " + mvr);
			item = theParser.parseStructures(new prisms.lang.ParseStructRoot(mvr), matches)[0];
		} catch(ParseException e) {
			throw new MuisParseException("Property parsing failed: " + mvr, e);
		}
		try {
			return theEvaluator.evaluateObservable(item, theEnv, asType);
		} catch(EvaluationException e) {
			throw new MuisParseException(e.getMessage(), e);
		}
	}
}
