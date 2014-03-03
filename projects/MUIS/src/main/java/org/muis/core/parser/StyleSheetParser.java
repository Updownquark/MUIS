package org.muis.core.parser;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import org.jdom2.Element;
import org.muis.core.*;
import org.muis.core.mgr.MuisState;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleDomain;
import org.muis.core.style.sheet.AnimatedStyleSheet;
import org.muis.core.style.sheet.ParsedStyleSheet;
import org.muis.core.style.sheet.StateGroupTypeExpression;
import org.muis.core.style.sheet.TemplateRole;
import org.muis.core.style.stateful.StateExpression;

import prisms.arch.PrismsConfig;
import prisms.lang.*;
import prisms.lang.types.ParsedStatementBlock;

/** Parses MUIS style sheets using a custom format */
public class StyleSheetParser {
	private static boolean DEBUG = true;

	/** Represents a namespace declaration in a style file */
	public static class ParsedNamespaceDeclaration extends ParsedItem {
		private String theName;

		private String theLocation;

		@Override
		public void setup(PrismsParser parser, ParsedItem parent, ParseMatch match) throws ParseException {
			super.setup(parser, parent, match);
			theName = getStored("name").text;
			theLocation = getStored("location").text.trim();
		}

		/** @return The namespace to map the toolkit to */
		public String getName() {
			return theName;
		}

		/** @return The location of the toolkit to map */
		public String getLocation() {
			return theLocation;
		}

		@Override
		public ParsedItem [] getDependents() {
			return new ParsedItem[0];
		}

		@Override
		public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
			throw new IllegalArgumentException("No such dependent " + dependent);
		}

		@Override
		public EvaluationResult evaluate(EvaluationEnvironment env, boolean asType, boolean withValues) throws EvaluationException {
			return null;
		}

		@Override
		public String toString() {
			return theName + ":=" + theLocation;
		}
	}

	/** Represents a declared animation variable in a style file */
	public static class ParsedAnimation extends ParsedItem {
		private String theVariableName;

		private double theInitialValue;

		private double [] theNextValues;

		private double [] theStepSizes;

		private double [] theTimeSteps;

		private boolean isRepeating;

		@Override
		public void setup(PrismsParser parser, ParsedItem parent, ParseMatch match) throws ParseException {
			super.setup(parser, parent, match);
			theVariableName = getStored("name").text;
			theInitialValue = getNumber(getStored("initValue"), "initial value");
			ArrayList<Double> values = new ArrayList<>();
			ArrayList<Double> steps = new ArrayList<>();
			ArrayList<Double> times = new ArrayList<>();
			for(ParseMatch m : getAllStored("value", "step size", "time")) {
				if("value".equals(m.config.get("storeAs")))
					values.add(getNumber(m, "value"));
				else if("step".equals(m.config.get("storeAs"))) {
					while(steps.size() < values.size() - 1)
						steps.add(0d);
					steps.add(getNumber(m, "step size"));
				} else if("time".equals(m.config.get("storeAs")))
					times.add(getNumber(m, "time"));
			}
			theNextValues = new double[values.size()];
			for(int i = 0; i < theNextValues.length; i++)
				theNextValues[i] = values.get(i);
			theStepSizes = new double[steps.size()];
			for(int i = 0; i < theStepSizes.length; i++)
				theStepSizes[i] = steps.get(i);
			theTimeSteps = new double[times.size()];
			for(int i = 0; i < theTimeSteps.length; i++)
				theTimeSteps[i] = times.get(i);
			isRepeating = getStored("no-repeat") == null;
		}

		/** @return The name of the variable to animate */
		public String getVariableName() {
			return theVariableName;
		}

		/** @return The initial value for the variable */
		public double getInitialValue() {
			return theInitialValue;
		}

		/** @return The number of time steps that the variable goes through in a cycle */
		public int getTimeSteps() {
			return theNextValues.length;
		}

		/**
		 * @param index The index of the animated value to get
		 * @return The animated value for the given index
		 */
		public double getValue(int index) {
			return theNextValues[index];
		}

		/**
		 * @param index The index of the animated value step size to get
		 * @return The value step size for the given index
		 */
		public double getStepSize(int index) {
			return theStepSizes[index];
		}

		/**
		 * @param index The index to get the time step for
		 * @return The amount of time (in seconds) that the animation will take to go from the value at index-1 (or the initial value if
		 *         index==0) to the indexed value
		 */
		public double getTimeStep(int index) {
			return theTimeSteps[index];
		}

		/** @return Whether this animation will repeat or terminate after the first animation */
		public boolean isRepeating() {
			return isRepeating;
		}

		@Override
		public ParsedItem [] getDependents() {
			return new ParsedItem[0];
		}

		@Override
		public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
			throw new IllegalArgumentException("No such dependent " + dependent);
		}

		@Override
		public EvaluationResult evaluate(EvaluationEnvironment env, boolean asType, boolean withValues) throws EvaluationException {
			return null;
		}

		private double getNumber(ParseMatch match, String name) throws ParseException {
			ParsedItem initVal = getParser().parseStructures(this, match)[0];
			if(!(initVal instanceof prisms.lang.types.ParsedNumber))
				throw new ParseException("Number expected for animation " + name + ", not \"" + initVal.getMatch() + "\"", getRoot()
					.getFullCommand(), initVal.getMatch().index);
			return ((prisms.lang.types.ParsedNumber) initVal).getValue().doubleValue();
		}

		@Override
		public String toString() {
			String ret = ">>" + theVariableName + "=" + theInitialValue;
			for(int i = 0; i < theNextValues.length; i++) {
				ret += "->" + theNextValues[i];
				if(theStepSizes[i] > 0)
					ret += "(" + theStepSizes[i] + ")";
				ret += "@" + theTimeSteps[i];
			}
			if(!isRepeating)
				ret += "|";
			return ret;
		}
	}

	/** Represents a (potentially) namespace-qualified type */
	public static class ParsedType extends ParsedItem {
		private String theNamespace;

		private String theName;

		@Override
		public void setup(PrismsParser parser, ParsedItem parent, ParseMatch match) throws ParseException {
			super.setup(parser, parent, match);
			ParseMatch ns = getStored("namespace");
			theNamespace = ns == null ? null : ns.text;
			theName = getStored("name").text;
		}

		/** @return The namespace qualifying this type. May be null */
		public String getNamespace() {
			return theNamespace;
		}

		/** @return The mapped name of the type */
		public String getName() {
			return theName;
		}

		@Override
		public String toString() {
			return (theNamespace == null ? "" : theNamespace + ":") + theName;
		}

		@Override
		public ParsedItem [] getDependents() {
			return new ParsedItem[0];
		}

		@Override
		public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
			throw new IllegalArgumentException("No such dependent " + dependent);
		}

		@Override
		public EvaluationResult evaluate(EvaluationEnvironment env, boolean asType, boolean withValues) throws EvaluationException {
			return null;
		}
	}

	/** Represents a filter determining the conditions under which a set of style assignments will be applied */
	public static abstract class ParsedStyleFilter extends ParsedItem {
		@Override
		public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
			ParsedItem [] deps = getDependents();
			int idx = prisms.util.ArrayUtils.indexOf(deps, dependent);
			if(idx >= 0)
				deps[idx] = toReplace;
			else
				throw new IllegalArgumentException("No such dependent " + dependent);
		}

		@Override
		public EvaluationResult evaluate(EvaluationEnvironment env, boolean asType, boolean withValues) throws EvaluationException {
			return null;
		}
	}

	/** Represents a set of types that a set of styles will be applied to */
	public static class ParsedTypeSet extends ParsedStyleFilter {
		private ParsedType [] theTypes;

		@Override
		public void setup(PrismsParser parser, ParsedItem parent, ParseMatch match) throws ParseException {
			super.setup(parser, parent, match);
			ParsedItem [] types = parser.parseStructures(this, getAllStored("type"));
			theTypes = new ParsedType[types.length];
			System.arraycopy(types, 0, theTypes, 0, types.length);
		}

		/** @return The types that this filter represents */
		public ParsedType [] getTypes() {
			return theTypes;
		}

		@Override
		public ParsedItem [] getDependents() {
			return theTypes;
		}

		@Override
		public String toString() {
			String ret = "[";
			for(int i = 0; i < theTypes.length; i++) {
				if(i > 0)
					ret += ", ";
				ret += theTypes[i];
			}
			ret += "]";
			return ret;
		}
	}

	/** Represents a set of groups that a set of styles will be applied to */
	public static class ParsedGroupSet extends ParsedStyleFilter {
		private String [] theGroupNames;

		@Override
		public void setup(PrismsParser parser, ParsedItem parent, ParseMatch match) throws ParseException {
			super.setup(parser, parent, match);
			ArrayList<String> groupNames = new ArrayList<>();
			for(ParseMatch m : getAllStored("group"))
				groupNames.add(m.text);
			theGroupNames = groupNames.toArray(new String[groupNames.size()]);
		}

		/** @return The names of the groups that this filter represents */
		public String [] getGroupNames() {
			return theGroupNames;
		}

		@Override
		public ParsedItem [] getDependents() {
			return new ParsedItem[0];
		}

		@Override
		public String toString() {
			String ret = "(";
			for(int i = 0; i < theGroupNames.length; i++) {
				if(i > 0)
					ret += ", ";
				ret += theGroupNames[i];
			}
			ret += ")";
			return ret;
		}
	}

	/** Represents a state expression that a set of styles will be applied for */
	public static class ParsedState extends ParsedStyleFilter {
		private ParsedItem theState;

		@Override
		public void setup(PrismsParser parser, ParsedItem parent, ParseMatch match) throws ParseException {
			super.setup(parser, parent, match);
			theState = parser.parseStructures(this, getStored("state"))[0];
		}

		/** @return The style expression that this parsed state wraps */
		public ParsedItem getState() {
			return theState;
		}

		@Override
		public ParsedItem [] getDependents() {
			return new ParsedItem[] {theState};
		}

		@Override
		public String toString() {
			return "." + theState;
		}
	}

	/** Represents an attach point within a type of templated widget that a set of styles will be applied to */
	public static class ParsedAttachPoint extends ParsedStyleFilter {
		private String theAttachPoint;

		@Override
		public void setup(PrismsParser parser, ParsedItem parent, ParseMatch match) throws ParseException {
			super.setup(parser, parent, match);
			theAttachPoint = getStored("attachPoint").text;
		}

		/** @return The name of the attach point */
		public String getAttachPoint() {
			return theAttachPoint;
		}

		@Override
		public ParsedItem [] getDependents() {
			return new ParsedItem[0];
		}

		@Override
		public String toString() {
			return "#" + theAttachPoint;
		}
	}

	/** Represents a section of a style file that applies a set of styles to elements filtered by one or more style filters */
	public static class ParsedSection extends ParsedItem {
		private ParsedStyleFilter [] theFilters;

		private ParsedStatementBlock theContent;

		@Override
		public void setup(PrismsParser parser, ParsedItem parent, ParseMatch match) throws ParseException {
			super.setup(parser, parent, match);
			theContent = (ParsedStatementBlock) parser.parseStructures(this, getStored("content"))[0];
			ParsedItem [] filters = parser.parseStructures(this, getAllStored("filter"));
			theFilters = new ParsedStyleFilter[filters.length];
			System.arraycopy(filters, 0, theFilters, 0, filters.length);
		}

		/** @return The filters that determine the conditions under which the style assignments in the contents will be applied */
		public ParsedStyleFilter [] getFilters() {
			return theFilters;
		}

		/** @return The content to apply to elements matching this section's filters */
		public ParsedStatementBlock getContent() {
			return theContent;
		}

		@Override
		public ParsedItem [] getDependents() {
			return prisms.util.ArrayUtils.add(theFilters, theContent);
		}

		@Override
		public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
			int idx = prisms.util.ArrayUtils.indexOf(theFilters, dependent);
			if(idx >= 0) {
				if(toReplace instanceof ParsedStyleFilter)
					theFilters[idx] = (ParsedStyleFilter) toReplace;
				else
					throw new IllegalArgumentException("A style filter cannot be replaced with " + toReplace.getClass().getName());
			} else if(theContent == dependent) {
				if(toReplace instanceof ParsedStatementBlock)
					theContent = (ParsedStatementBlock) toReplace;
				else
					throw new IllegalArgumentException("A section's content block cannot be replaced with "
						+ toReplace.getClass().getName());
			} else
				throw new IllegalArgumentException("No such dependent " + dependent);
		}

		@Override
		public EvaluationResult evaluate(EvaluationEnvironment env, boolean asType, boolean withValues) throws EvaluationException {
			return null;
		}

		@Override
		public String toString() {
			StringBuilder ret = new StringBuilder();
			for(ParsedStyleFilter filter : theFilters)
				ret.append(filter);
			ret.append(theContent);
			return ret.toString();
		}
	}

	/** Represents a simple "domain.attribute=value" assignment */
	public static class ParsedAssignment extends ParsedItem {
		private ParsedType theDomain;

		private String theAttrName;

		private ParsedItem theValue;

		@Override
		public void setup(PrismsParser parser, ParsedItem parent, ParseMatch match) throws ParseException {
			super.setup(parser, parent, match);
			theDomain = (ParsedType) parser.parseStructures(this, getStored("domain"))[0];
			theAttrName = getStored("name").text;
			theValue = parser.parseStructures(this, getStored("value"))[0];
		}

		/** @return The domain of the attribute to be assigned */
		public ParsedType getDomain() {
			return theDomain;
		}

		/** @return The name of the attribute to be assigned */
		public String getAttrName() {
			return theAttrName;
		}

		/** @return The value to be assigned to the attribute */
		public ParsedItem getValue() {
			return theValue;
		}

		@Override
		public ParsedItem [] getDependents() {
			return new ParsedItem[] {theDomain, theValue};
		}

		@Override
		public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
			if(theDomain == dependent) {
				if(toReplace instanceof ParsedType)
					theDomain = (ParsedType) toReplace;
				else
					throw new IllegalArgumentException("Cannot replace the domain of an attribute assignment with "
						+ toReplace.getClass().getName());
			} else if(theValue == dependent)
				theValue = toReplace;
			else
				throw new IllegalArgumentException("No such dependent " + dependent);
		}

		@Override
		public EvaluationResult evaluate(EvaluationEnvironment env, boolean asType, boolean withValues) throws EvaluationException {
			return null;
		}

		@Override
		public String toString() {
			return theDomain + "." + theAttrName + "=" + theValue;
		}
	}

	/** Represents a bulk domain assignment like domain={attr1=value1; attr2=value2} */
	public static class ParsedDomainAssignment extends ParsedItem {
		private ParsedType theDomain;

		private DomainScopedBlock theValues;

		@Override
		public void setup(PrismsParser parser, ParsedItem parent, ParseMatch match) throws ParseException {
			super.setup(parser, parent, match);
			theDomain = (ParsedType) parser.parseStructures(this, getStored("domain"))[0];
			theValues = (DomainScopedBlock) parser.parseStructures(this, getStored("value"))[0];
		}

		/** @return The domain whose attributes will be assigned */
		public ParsedType getDomain() {
			return theDomain;
		}

		/** @return The domain-scoped assignments for the domain */
		public DomainScopedBlock getValues() {
			return theValues;
		}

		@Override
		public ParsedItem [] getDependents() {
			return new ParsedItem[] {theDomain, theValues};
		}

		@Override
		public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
			if(theDomain == dependent) {
				if(toReplace instanceof ParsedType)
					theDomain = (ParsedType) toReplace;
				else
					throw new IllegalArgumentException("Cannot replace the domain of an domain assignment with "
						+ toReplace.getClass().getName());
			} else if(theValues == dependent) {
				if(toReplace instanceof DomainScopedBlock)
					theValues = (DomainScopedBlock) toReplace;
				else
					throw new IllegalArgumentException("Cannot replace the value block of an domain assignment with "
						+ toReplace.getClass().getName());
			} else
				throw new IllegalArgumentException("No such dependent " + dependent);
		}

		@Override
		public EvaluationResult evaluate(EvaluationEnvironment env, boolean asType, boolean withValues) throws EvaluationException {
			return null;
		}

		@Override
		public String toString() {
			return theDomain + "=" + theValues;
		}
	}

	/** Represents a set of assignments where the domain is already available in context and will not be specified */
	public static class DomainScopedBlock extends ParsedItem {
		private DomainScopedAssignment [] theContents;

		@Override
		public void setup(PrismsParser parser, ParsedItem parent, ParseMatch match) throws ParseException {
			super.setup(parser, parent, match);
			ParsedItem [] contents = parser.parseStructures(this, getAllStored("content"));
			theContents = new DomainScopedAssignment[contents.length];
			System.arraycopy(contents, 0, theContents, 0, contents.length);
		}

		/** @return All domain-scoped assignments in this block */
		public DomainScopedAssignment [] getContents() {
			return theContents;
		}

		@Override
		public ParsedItem [] getDependents() {
			return theContents;
		}

		@Override
		public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
			int idx = prisms.util.ArrayUtils.indexOf(theContents, dependent);
			if(idx >= 0) {
				if(toReplace instanceof DomainScopedAssignment)
					theContents[idx] = (DomainScopedAssignment) toReplace;
				else
					throw new IllegalArgumentException("Cannot replace content in an domain-scoped assignment block with "
						+ toReplace.getClass().getName());
			} else
				throw new IllegalArgumentException("No such dependent " + dependent);
		}

		@Override
		public EvaluationResult evaluate(EvaluationEnvironment env, boolean asType, boolean withValues) throws EvaluationException {
			return null;
		}

		@Override
		public String toString() {
			StringBuilder ret = new StringBuilder('{');
			for(DomainScopedAssignment assn : theContents)
				ret.append(assn).append('\n');
			ret.append('}');
			return ret.toString();
		}
	}

	/** Represents a style attribute assignment where the domain is already available in context and will not be specified */
	public static class DomainScopedAssignment extends ParsedItem {
		private String theAttrName;

		private ParsedItem theValue;

		@Override
		public void setup(PrismsParser parser, ParsedItem parent, ParseMatch match) throws ParseException {
			super.setup(parser, parent, match);
			theAttrName = getStored("name").text;
			theValue = parser.parseStructures(this, getStored("value"))[0];
		}

		/** @return The name of the attribute to assign */
		public String getAttrName() {
			return theAttrName;
		}

		/** @return The value to assign to the attribute */
		public ParsedItem getValue() {
			return theValue;
		}

		@Override
		public ParsedItem [] getDependents() {
			return new ParsedItem[] {theValue};
		}

		@Override
		public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
			if(theValue == dependent)
				theValue = toReplace;
			else
				throw new IllegalArgumentException("No such dependent " + dependent);
		}

		@Override
		public EvaluationResult evaluate(EvaluationEnvironment env, boolean asType, boolean withValues) throws EvaluationException {
			return null;
		}

		@Override
		public String toString() {
			return theAttrName + "=" + theValue;
		}
	}

	private static class ExpressionContext {
		ArrayList<Class<? extends MuisElement>> theTypes;

		ArrayList<String> theGroups;

		StateExpression theState;

		TemplateRole theTemplatePath;

		ExpressionContext() {
			theTypes = new ArrayList<>();
			theGroups = new ArrayList<>();
		}

		boolean isEmpty() {
			return theTypes.isEmpty() && theGroups.isEmpty() && theState == null && theTemplatePath == null;
		}
	}

	private static class ExpressionContextStack implements Iterable<StateGroupTypeExpression<?>> {
		private ArrayList<ExpressionContext> theStack;

		ExpressionContextStack() {
			theStack = new ArrayList<>();
		}

		int size() {
			return theStack.size();
		}

		void push() {
			theStack.add(new ExpressionContext());
		}

		void pop() {
			theStack.remove(theStack.size() - 1);
		}

		ExpressionContext top() {
			return theStack.get(theStack.size() - 1);
		}

		void addType(Class<? extends MuisElement> type) throws MuisParseException {
			for(int i = theStack.size() - 1; i >= 0; i--) {
				if(!theStack.get(i).theTypes.isEmpty()) {
					boolean isSubType = false;
					for(Class<?> preType : theStack.get(i).theTypes) {
						if(preType.equals(type))
							throw new MuisParseException("Type " + type.getSimpleName() + " is already in this category");
						if(preType.isAssignableFrom(type)) {
							isSubType = true;
							break;
						}
					}
					if(!isSubType) {
						throw new MuisParseException("Type " + type.getSimpleName() + " is not a sub type of a type in a super-category");
					}
				}
			}
			top().theTypes.add(type);
		}

		void addGroup(String groupName) throws MuisParseException {
			for(int i = 0; i < theStack.size() - 1; i++) {
				if(theStack.get(i).theTemplatePath != null)
					break;
				if(!theStack.get(i).theGroups.isEmpty())
					throw new MuisParseException(
						"Groups are not hierarchical--a category with a group cannot contain a category with a group");
			}
			top().theGroups.add(groupName);
		}

		void setState(StateExpression state) {
			top().theState = state;
		}

		void addAttachPoint(String attachPoint, MuisEnvironment env) throws MuisParseException {
			Class<? extends MuisElement> type = null;
			for(int i = theStack.size() - 1; i >= 0; i--) {
				if(!theStack.get(i).theTypes.isEmpty()) {
					if(theStack.get(i).theTypes.size() > 1)
						throw new MuisParseException("Cannot attach-point specific styles to more than one type at once");
					type = theStack.get(i).theTypes.get(0);
					break;
				}
			}
			if(type == null)
				type = MuisElement.class;
			if(!(MuisTemplate.class.isAssignableFrom(type)))
				throw new MuisParseException("Element type " + type.getName() + " is not templated--cannot specify attach point styles");
			MuisTemplate.TemplateStructure templateStruct;
			try {
				templateStruct = MuisTemplate.TemplateStructure.getTemplateStructure(env, (Class<? extends MuisTemplate>) type);
			} catch(MuisException e) {
				throw new MuisParseException("Could not parse template structure for " + type.getName(), e);
			}
			MuisTemplate.AttachPoint ap = templateStruct.getAttachPoint(attachPoint);
			if(ap == null)
				throw new MuisParseException("Template " + type.getName() + " has no attach point named \"" + attachPoint + "\"");
			ArrayList<String> parentGroups = new ArrayList<>();
			for(int i = theStack.size() - 1; i >= 0; i--)
				if(!theStack.get(i).theGroups.isEmpty()) {
					parentGroups.addAll(theStack.get(i).theGroups);
					break;
				}
			top().theTemplatePath = new TemplateRole(ap, parentGroups, (Class<? extends MuisTemplate>) type, getTopTemplateRole());
			top().theTypes.add(ap.type);
		}

		Class<? extends MuisElement> [] getTopTypes() {
			for(int i = theStack.size() - 1; i >= 0; i--) {
				if(theStack.get(i).theTemplatePath != null)
					return new Class[0];
				if(!theStack.get(i).theTypes.isEmpty())
					return theStack.get(i).theTypes.toArray(new Class[theStack.get(i).theTypes.size()]);
			}
			return new Class[0];
		}

		TemplateRole getTopTemplateRole() {
			for(int i = theStack.size() - 1; i >= 0; i--) {
				if(theStack.get(i).theTemplatePath != null)
					return theStack.get(i).theTemplatePath;
			}
			return null;
		}

		@Override
		public java.util.Iterator<StateGroupTypeExpression<?>> iterator() {
			if(top().isEmpty()) {
				return new java.util.Iterator<StateGroupTypeExpression<?>>() {
					@Override
					public boolean hasNext() {
						return false;
					}

					@Override
					public StateGroupTypeExpression<?> next() {
						return null;
					}

					@Override
					public void remove() {
					}
				};
			}
			return new java.util.Iterator<StateGroupTypeExpression<?>>() {
				private Class<? extends MuisElement> [] theIterableTypes;

				private ArrayList<String> theIterableGroups;

				private StateExpression theOverallState;

				private TemplateRole theTemplatePath;

				private int theTypeIdx;

				private int theGroupIdx;

				private boolean hasCalledNext;

				{
					theIterableTypes = getTopTypes();
					theTemplatePath = getTopTemplateRole();
					theIterableGroups = new ArrayList<>();
					for(int i = theStack.size() - 1; i >= 0; i--) {
						if(theStack.get(i).theTemplatePath != null)
							break;
						if(theIterableGroups.isEmpty())
							theIterableGroups.addAll(theStack.get(i).theGroups);
					}
					for(ExpressionContext ctx : theStack) {
						if(ctx.theState != null) {
							if(theOverallState == null)
								theOverallState = ctx.theState;
							else
								theOverallState = theOverallState.and(ctx.theState);
						}
					}
				}

				@Override
				public boolean hasNext() {
					return !hasCalledNext || theTypeIdx < theIterableTypes.length || theGroupIdx < theIterableGroups.size();
				}

				@Override
				public StateGroupTypeExpression<?> next() {
					StateGroupTypeExpression<?> ret;
					if(theGroupIdx < theIterableGroups.size()) {
						if(theTypeIdx < theIterableTypes.length) {
							ret = new StateGroupTypeExpression<>(theOverallState, theIterableGroups.get(theGroupIdx),
								theIterableTypes[theTypeIdx++], theTemplatePath);
						} else {
							ret = new StateGroupTypeExpression<>(theOverallState, theIterableGroups.get(theGroupIdx), null, theTemplatePath);
						}
						if(theTypeIdx >= theIterableTypes.length) {
							theGroupIdx++;
							theTypeIdx = 0;
						}
					} else if(theIterableGroups.isEmpty()) {
						if(theTypeIdx < theIterableTypes.length) {
							ret = new StateGroupTypeExpression<>(theOverallState, null, theIterableTypes[theTypeIdx++], theTemplatePath);
						} else if(theIterableTypes.length == 0) {
							ret = new StateGroupTypeExpression<>(theOverallState, null, null, theTemplatePath);
						} else
							throw new java.util.NoSuchElementException();
					} else
						throw new java.util.NoSuchElementException();
					hasCalledNext = true;
					return ret;
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
	}

	private PrismsParser theParser;

	private EvaluationEnvironment theEnv;

	/** Creates a style sheet parser */
	public StyleSheetParser() {
		theParser = new PrismsParser();
		try {
			theParser.configure(getStyleSheetDefConfig());
		} catch(java.io.IOException | MuisParseException e) {
			throw new IllegalStateException("Could not configure style sheet expression parser", e);
		}
		theEnv = new DefaultEvaluationEnvironment();

		// Use the default prisms.lang Grammar.xml to implement some setup declarations to prepare the environment
		PrismsParser setupParser = new PrismsParser();
		try {
			setupParser.configure(getSetupConfig());
		} catch(IOException | MuisParseException e) {
			throw new IllegalStateException("Could not configure style sheet setup parser", e);
		}
		if(DEBUG) {
			prisms.lang.debug.PrismsParserDebugGUI debugger = new prisms.lang.debug.PrismsParserDebugGUI();
			theParser.setDebugger(debugger);
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
		// TODO Add more constants and functions
		for(String command : commands) {
			try {
				setupParser.parseStructures(new prisms.lang.ParseStructRoot(command), setupParser.parseMatches(command))[0].evaluate(
					theEnv, false, true);
			} catch(ParseException | EvaluationException e) {
				System.err.println("Could not execute XML stylesheet parser setup expression: " + command);
				e.printStackTrace();
			}
		}
	}

	private static PrismsConfig getStyleSheetDefConfig() throws IOException, MuisParseException {
		return getConfig(getStyleSheetDefXml());
	}

	private static PrismsConfig getSetupConfig() throws IOException, MuisParseException {
		return getConfig(getSetupConfigXml());
	}

	private static PrismsConfig getConfig(Element element) {
		return new PrismsConfig.DefaultPrismsConfig(element.getName(), element.getTextNormalize(), getSubConfigs(element));
	}

	private static PrismsConfig [] getSubConfigs(Element element) {
		ArrayList<PrismsConfig> ret = new ArrayList<>();
		for(org.jdom2.Attribute att : element.getAttributes()) {
			ret.add(new PrismsConfig.DefaultPrismsConfig(att.getName(), att.getValue(), new PrismsConfig[0]));
		}
		for(Element el : element.getChildren()) {
			ret.add(getConfig(el));
		}
		return ret.toArray(new PrismsConfig[ret.size()]);
	}

	private static Element getStyleSheetDefXml() throws IOException, MuisParseException {
		try {
			return new org.jdom2.input.SAXBuilder().build(
				new java.io.InputStreamReader(StyleSheetParser.class.getResourceAsStream("MSS.xml"))).getRootElement();
		} catch(org.jdom2.JDOMException e) {
			throw new MuisParseException("Could not parse MSS.xml", e);
		}
	}

	private static Element getSetupConfigXml() throws IOException, MuisParseException {
		try {
			return new org.jdom2.input.SAXBuilder().build(
				new java.io.InputStreamReader(PrismsParser.class.getResourceAsStream("Grammar.xml"))).getRootElement();
		} catch(org.jdom2.JDOMException e) {
			throw new MuisParseException("Could not parse Grammar.xml", e);
		}
	}

	/**
	 * Parses a style sheet from an XML document
	 *
	 * @param parseEnv The parsing environment
	 * @param location The location to get the document from
	 * @param env The MUIS environment that the style sheet is being loaded for
	 * @return The style sheet parsed from the XML resource
	 * @throws java.io.IOException If an error occurs reading the resource
	 * @throws MuisParseException If an error occurs parsing the XML or interpreting it as a style sheet
	 */
	public ParsedStyleSheet parse(MuisParseEnv parseEnv, URL location, MuisEnvironment env)
		throws java.io.IOException, MuisParseException {
		StringBuilder text = new StringBuilder();
		try (java.io.Reader reader = new java.io.BufferedReader(new java.io.InputStreamReader(location.openStream()))) {
			int read = reader.read();
			while(read >= 0) {
				text.append((char) read);
				read = reader.read();
			}
		}
		ParsedItem [] parsed;
		try {
			parsed = theParser.parseStructures(new prisms.lang.ParseStructRoot(text.toString()), theParser.parseMatches(text.toString()));
		} catch(ParseException e) {
			throw new MuisParseException("Could not parse " + location, e);
		}

		ParsedStyleSheet ret = new ParsedStyleSheet(theEnv.scope(true));
		ExpressionContextStack stack = new ExpressionContextStack();
		boolean hadValues = false;
		boolean hadAnimation = false;
		for(ParsedItem item : parsed) {
			if(item instanceof ParsedNamespaceDeclaration) {
				if(hadValues || hadAnimation) {
					int [] lc = getLineChar(text.toString(), item.getMatch().index);
					throw new MuisParseException(
						"Namespace declarations must occur before any style assignments or animation variables: \"" + item.getMatch().text
							+ "\" in style sheet " + location + " at line " + (lc[0] + 1) + ", char " + (lc[1] + 1));
				}

				ParsedNamespaceDeclaration ns = (ParsedNamespaceDeclaration) item;
				try {
					parseEnv.cv()
						.addNamespace(ns.getName(), env.getToolkit(org.muis.util.MuisUtils.resolveURL(location, ns.getLocation())));
				} catch(MuisException e) {
					throw new MuisParseException("Could not resolve toolkit for namespace \"" + ns.getName() + "\", location "
						+ ns.getLocation(), e);
				}
			} else if(item instanceof ParsedAnimation) {
				hadAnimation = true;
				if(hadValues) {
					int [] lc = getLineChar(text.toString(), item.getMatch().index);
					throw new MuisParseException("Animation variable declarations must occur before any style assignments: \""
						+ item.getMatch().text + "\" in style sheet " + location + " at line " + (lc[0] + 1) + ", char " + (lc[1] + 1));
				}

				ParsedAnimation anim = (ParsedAnimation) item;
				try {
					AnimatedStyleSheet.AnimationSegment[] segments = new AnimatedStyleSheet.AnimationSegment[anim.getTimeSteps()];
					for(int i = 0; i < segments.length; i++)
						segments[i] = new AnimatedStyleSheet.AnimationSegment(anim.getValue(i), anim.getStepSize(i), Math.round(anim
							.getTimeStep(i) * 1000));
					ret.addVariable(new AnimatedStyleSheet.AnimatedVariable(anim.getVariableName(), anim.getInitialValue(), segments, anim
						.isRepeating()));
				} catch(IllegalArgumentException e) {
					throw new MuisParseException(e.getMessage(), e);
				}
			} else if(item instanceof ParsedSection) {
				hadValues = true;
				applySection(parseEnv, ret, stack, (ParsedSection) item, location);
			} else if(item instanceof ParsedStatementBlock) {
				hadValues = true;
				applyBlock(parseEnv, ret, stack, (ParsedStatementBlock) item, location);
			} else if(item instanceof ParsedAssignment) {
				hadValues = true;
				applyAssignment(parseEnv, ret, stack, (ParsedAssignment) item, location);
			} else if(item instanceof ParsedDomainAssignment) {
				hadValues = true;
				applyDomainAssignment(parseEnv, ret, stack, (ParsedDomainAssignment) item, location);
			} else {
				int [] lc = getLineChar(text.toString(), item.getMatch().index);
				parseEnv.msg().error(
					"Unrecognized content in style sheet " + location + ": \"" + item.getMatch().text + "\" at line " + (lc[0] + 1)
					+ ", char " + (lc[1] + 1));
			}
		}
		return ret;
	}

	private static int [] getLineChar(String text, int index) {
		int line = 0, ch = 0;
		for(int c = 0; c < index; c++) {
			if(text.charAt(c) == '\n') {
				line++;
				ch = 0;
			} else if(text.charAt(c) >= ' ')
				ch++;
		}
		return new int[] {line, ch};
	}

	private void applySection(MuisParseEnv env, ParsedStyleSheet style, ExpressionContextStack stack, ParsedSection section, URL location) {
		stack.push();
		try {
			// Stack filters for coming block
			for(ParsedStyleFilter filter : section.getFilters()) {
				if(filter instanceof ParsedTypeSet) {
					for(ParsedType type : ((ParsedTypeSet) filter).getTypes()) {
						try {
							stack.addType(env.cv().loadMappedClass(type.getNamespace(), type.getName(), MuisElement.class));
						} catch(MuisException e) {
							int [] lc = getLineChar(section.getRoot().getFullCommand(), filter.getMatch().index);
							env.msg().error(
								"Could not load element type " + type + ": " + location + " at line " + (lc[0] + 1) + ", char "
								+ (lc[1] + 1), e);
							return;
						}
					}
				} else if(filter instanceof ParsedGroupSet) {
					for(String groupName : ((ParsedGroupSet) filter).getGroupNames())
						try {
							stack.addGroup(groupName);
						} catch(MuisParseException e) {
							int [] lc = getLineChar(section.getRoot().getFullCommand(), filter.getMatch().index);
							env.msg().error(e.getMessage() + ": " + location + " at line " + (lc[0] + 1) + ", char " + (lc[1] + 1), e);
							return;
						}
				} else if(filter instanceof ParsedState) {
					ParsedItem state = ((ParsedState) filter).getState();
					try {
						stack.setState(evaluateState(state, stack, location));
					} catch(MuisParseException e) {
						env.msg().error(e.getMessage(), e);
						return;
					}
				} else if(filter instanceof ParsedAttachPoint) {
					String attachPoint = ((ParsedAttachPoint) filter).getAttachPoint();
					try {
						stack.addAttachPoint(attachPoint, env.cv().getEnvironment());
					} catch(MuisParseException e) {
						env.msg().error(e.getMessage(), e);
					}
				} else {
					env.msg().error("Unrecognized style filter type: " + filter.getClass().getName());
					return;
				}
			}
			applyBlock(env, style, stack, section.getContent(), location);
		} finally {
			stack.pop();
		}
	}

	private StateExpression evaluateState(ParsedItem state, ExpressionContextStack stack, URL location) throws MuisParseException {
		if(state instanceof prisms.lang.types.ParsedBinaryOp) {
			prisms.lang.types.ParsedBinaryOp op = (prisms.lang.types.ParsedBinaryOp) state;
			switch (op.getName()) {
			case "&":
			case "&&":
				return evaluateState(op.getOp1(), stack, location).and(evaluateState(op.getOp2(), stack, location));
			case "|":
			case "||":
				return evaluateState(op.getOp1(), stack, location).or(evaluateState(op.getOp2(), stack, location));
			default:
				int [] lc = getLineChar(state.getRoot().getFullCommand(), state.getStored("name").index);
				throw new MuisParseException("Illegal operator in state expression: \"" + op.getName() + "\" at " + location + " at line "
					+ (lc[0] + 1) + ", char " + (lc[1] + 1));
			}
		} else if(state instanceof prisms.lang.types.ParsedUnaryOp) {
			prisms.lang.types.ParsedUnaryOp op = (prisms.lang.types.ParsedUnaryOp) state;
			switch (op.getName()) {
			case "!":
			case "-":
				return evaluateState(op.getOp(), stack, location).not();
			default:
				int [] lc = getLineChar(state.getRoot().getFullCommand(), state.getStored("name").index);
				throw new MuisParseException("Illegal operator in state expression: \"" + op.getName() + "\" at " + location + " at line "
					+ (lc[0] + 1) + ", char " + (lc[1] + 1));
			}
		} else if(state instanceof prisms.lang.types.ParsedParenthetic) {
			return evaluateState(((prisms.lang.types.ParsedParenthetic) state).getContent(), stack, location);
		} else if(state instanceof prisms.lang.types.ParsedIdentifier) {
			try {
				return new StateExpression.Simple(findState(((prisms.lang.types.ParsedIdentifier) state).getName(), stack));
			} catch(MuisParseException e) {
				int [] lc = getLineChar(state.getRoot().getFullCommand(), state.getMatch().index);
				throw new MuisParseException(e.getMessage() + " at " + location + " at line " + (lc[0] + 1) + ", char " + (lc[1] + 1));
			}
		} else {
			int [] lc = getLineChar(state.getRoot().getFullCommand(), state.getMatch().index);
			throw new MuisParseException("Unrecognized state expression type: " + state.getClass().getName() + " in " + location
				+ " at line " + (lc[0] + 1) + ", char " + (lc[1] + 1));
		}
	}

	private MuisState findState(String name, ExpressionContextStack stack) throws MuisParseException {
		Class<? extends MuisElement> [] types = stack.getTopTypes();
		if(types.length == 0)
			types = new Class[] {MuisElement.class};
		MuisState ret = null;
		for(int i = 0; i < types.length; i++) {
			boolean found = false;
			MuisState [] states = org.muis.util.MuisUtils.getStatesFor(types[i]);
			for(int j = 0; j < states.length; j++)
				if(states[j].getName().equals(name)) {
					found = true;
					if(ret == null)
						ret = states[j];
				}
			if(!found)
				throw new MuisParseException("Element type " + types[i].getName() + " does not support state \"" + name + "\"");
		}
		return ret;
	}

	private void applyBlock(MuisParseEnv env, ParsedStyleSheet style, ExpressionContextStack stack, ParsedStatementBlock block, URL location) {
		for(ParsedItem content : block.getContents()) {
			if(content instanceof ParsedSection) {
				applySection(env, style, stack, (ParsedSection) content, location);
			} else if(content instanceof ParsedStatementBlock) {
				applyBlock(env, style, stack, (ParsedStatementBlock) content, location);
			} else if(content instanceof ParsedAssignment) {
				applyAssignment(env, style, stack, (ParsedAssignment) content, location);
			} else if(content instanceof ParsedDomainAssignment) {
				applyDomainAssignment(env, style, stack, (ParsedDomainAssignment) content, location);
			} else {
				int [] lc = getLineChar(block.getRoot().getFullCommand(), content.getMatch().index);
				env.msg().error(
					"Unrecognized content in block of style sheet " + location + ": \"" + content.getMatch().text + "\" at line "
						+ (lc[0] + 1) + ", char " + (lc[1] + 1));
			}
		}
	}

	private void applyAssignment(MuisParseEnv env, ParsedStyleSheet style, ExpressionContextStack stack, ParsedAssignment assign,
		URL location) {
		StyleDomain domain;
		try {
			domain = org.muis.core.style.StyleParsingUtils.getStyleDomain(assign.getDomain().getNamespace(), assign.getDomain().getName(),
				env.cv());
		} catch(MuisException e) {
			env.msg().error(e.getMessage(), e);
			return;
		}
		for(StyleAttribute<?> attr : domain) {
			if(attr.getName().equals(assign.getAttrName())) {
				ParsedItem value;
				try {
					value = replaceIdentifiersAndStrings(assign.getValue(), style, attr, env);
				} catch(MuisException e) {
					env.msg().error("Could not interpret value " + assign.getValue() + " for attribute " + attr, e, "attribute", attr,
						"value",
						assign.getValue());
					return;
				}
				if(!checkValidity(style, attr, value, env, location))
					break;
				if(stack.size() > 0)
					for(StateGroupTypeExpression<?> expr : stack)
						style.setAnimatedValue(attr, expr, value);
				else
					style.setAnimatedValue(attr, new StateGroupTypeExpression<>(null, null, null, null), value);
				return;
			}
		}
		env.msg()
			.error(
				"No attribute " + assign.getAttrName() + " in domain "
			+ (assign.getDomain().getNamespace() == null ? "" : assign.getDomain().getNamespace() + ":") + assign.getDomain().getName());
	}

	private void applyDomainAssignment(MuisParseEnv env, ParsedStyleSheet style, ExpressionContextStack stack,
		ParsedDomainAssignment domainAssign, URL location) {
		StyleDomain domain;
		try {
			domain = org.muis.core.style.StyleParsingUtils.getStyleDomain(domainAssign.getDomain().getNamespace(), domainAssign.getDomain()
				.getName(), env.cv());
		} catch(MuisException e) {
			env.msg().error(e.getMessage(), e);
			return;
		}
		for(DomainScopedAssignment assign : domainAssign.getValues().getContents()) {
			boolean found = false;
			for(StyleAttribute<?> attr : domain) {
				if(attr.getName().equals(assign.getAttrName())) {
					found = true;
					ParsedItem value;
					try {
						value = replaceIdentifiersAndStrings(assign.getValue(), style, attr, env);
					} catch(MuisException e) {
						env.msg().error("Could not interpret value " + assign.getValue() + " for attribute " + attr, e, "attribute", attr,
							"value", assign.getValue());
						break;
					}
					if(!checkValidity(style, attr, value, env, location))
						break;
					if(stack.size() > 0)
						for(StateGroupTypeExpression<?> expr : stack)
							style.setAnimatedValue(attr, expr, value);
					else
						style.setAnimatedValue(attr, new StateGroupTypeExpression<>(null, null, null, null), value);
					break;
				}
			}
			if(!found)
				env.msg().error(
					"No attribute " + assign.getAttrName() + " in domain "
					+ (domainAssign.getDomain().getNamespace() == null ? "" : domainAssign.getDomain().getNamespace() + ":")
					+ domainAssign.getDomain().getName());
		}
	}

	private boolean checkValidity(ParsedStyleSheet style, StyleAttribute<?> attr, ParsedItem value, MuisParseEnv env, URL location) {
		EvaluationEnvironment evEnv = theEnv.scope(true);
		// EvaluationResult typeRes;
		try {
			for(AnimatedStyleSheet.AnimatedVariable var : style) {
				evEnv.declareVariable(var.getName(), new Type(Double.TYPE), false, value, 0);
			}
			// typeRes = value.evaluate(evEnv, false, false);
		} catch(EvaluationException e) {
			env.msg().error("Could not evaluate value " + value + " for attribute " + attr, e, "attribute", attr, "value", value);
			return false;
		}
		// This type checking is flawed because of the conversion ability in MUIS properties
		/*Type attrType = new Type(attr.getType().getType());
		if(!attrType.isAssignable(typeRes.getType())) {
			env.msg().error(
				"Value \"" + value + "\", resolving to type " + typeRes.getType() + " cannot be assigned to style attribute " + attr
					+ ", type " + attrType);
			return false;
		}*/
		return true;
	}

	ParsedItem replaceIdentifiersAndStrings(ParsedItem value, ParsedStyleSheet style, StyleAttribute<?> attr, MuisParseEnv env)
		throws MuisException {
		if(value instanceof prisms.lang.types.ParsedIdentifier) {
			String text = ((prisms.lang.types.ParsedIdentifier) value).getName();
			for(AnimatedStyleSheet.AnimatedVariable var : style) {
				if(var.getName().equals(text))
					return value;
			}
			return new org.muis.core.style.sheet.ConstantItem(attr.getType().getType(), attr.getType().parse(env, text));
		} else if(value instanceof prisms.lang.types.ParsedString) {
			String text = ((prisms.lang.types.ParsedString) value).getValue();
			return new org.muis.core.style.sheet.ConstantItem(attr.getType().getType(), attr.getType().parse(env, text));
		} else {
			for(ParsedItem depend : value.getDependents()) {
				ParsedItem replace = replaceIdentifiersAndStrings(depend, style, attr, env);
				if(replace != depend)
					value.replace(depend, replace);
			}
			return value;
		}
	}
}
