package org.muis.core.style.sheet;

import java.util.Iterator;

import org.muis.core.eval.impl.ObservableEvaluator;
import org.muis.core.eval.impl.ObservableItemEvaluator;
import org.muis.core.model.MuisValueReferenceParser;
import org.muis.core.parser.MuisParseException;
import org.muis.core.rx.ObservableValue;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleExpressionValue;

import prisms.lang.*;
import prisms.lang.eval.PrismsEvaluator;
import prisms.lang.types.ParsedIdentifier;

/** A style sheet whose values can be animated internally */
public class AnimatedStyleSheet extends AbstractStyleSheet implements Iterable<AnimatedStyleSheet.AnimatedVariable> {
	/** A variable whose value can change over time to affect style value expressions' evaluated values */
	public static final class AnimatedVariable extends org.muis.core.rx.DefaultObservableValue<Double> implements Variable,
		Iterable<AnimationSegment> {
		private final String theName;

		private final double theStartValue;

		private final java.util.Collection<AnimationSegment> theAnimation;

		private final boolean isRepeating;

		private final long theDuration;

		private final double theEndValue;

		private double theCurrentValue;

		private int theStyleSheetCount;

		private org.muis.core.rx.Observer<org.muis.core.rx.ObservableValueEvent<Double>> theController;

		/**
		 * @param varName The name for the variable
		 * @param startValue The starting value for the variable
		 * @param animation The animations that the variable will go through over time
		 * @param repeat Whether the animation should cycle or just animate once and finish at the terminal value
		 */
		public AnimatedVariable(String varName, double startValue, AnimationSegment [] animation, boolean repeat) {
			theController = control(null);
			theName = varName;
			theStartValue = startValue;
			theAnimation = java.util.Collections.unmodifiableCollection(java.util.Arrays.asList(animation));
			isRepeating = repeat;
			long duration = 0;
			double endValue = theStartValue;
			double start = theStartValue;
			for(int s = 0; s < animation.length; s++) {
				AnimationSegment segment = animation[s];
				duration += segment.getDuration();
				endValue = segment.getEndValue();
				if(segment.getStepSize() > Math.abs(endValue - start))
					throw new IllegalArgumentException("Step size (" + segment.getStepSize() + ") of segment " + s
						+ " of animated variable " + theName + " is greater than its value change (" + Math.abs(endValue - start) + ")");
				start = endValue;
			}
			theDuration = duration;
			theEndValue = endValue;
			theCurrentValue = theStartValue;
		}

		@Override
		public String getName() {
			return theName;
		}

		@Override
		public boolean isFinal() {
			return true; // Not modifiable through attribute values
		}

		@Override
		public boolean isInitialized() {
			return true;
		}

		@Override
		public Object getValue() {
			return theCurrentValue;
		}

		/** @return The initial value for this variable */
		public double getStartValue() {
			return theStartValue;
		}

		@Override
		public Iterator<AnimationSegment> iterator() {
			return theAnimation.iterator();
		}

		/** @return Whether this variable's animation repeats or terminates on its final value after one round */
		public boolean isRepeating() {
			return isRepeating;
		}

		/** @return The duration of one round of animation on this variable */
		public long getDuration() {
			return theDuration;
		}

		/** @return The final value for this variable's animation */
		public double getEndValue() {
			return theEndValue;
		}

		/**
		 * @param time The amount of time elapsed since animation began
		 * @return The value that this variable would have for the given animation time
		 */
		public double getValueFor(long time) {
			if(theDuration == 0 || (!isRepeating && time >= theDuration))
				return theEndValue;
			time %= theDuration;
			double start = theStartValue;
			for(AnimationSegment segment : theAnimation) {
				if(time <= segment.getDuration()) {
					double p = time * 1.0 / segment.getDuration();
					double valueDiff = segment.getEndValue() - start;
					double valueP = p * valueDiff;
					if(segment.getStepSize() > 0) {
						// make sure that valueP is at an even increment of the step size
						valueP = Math.round(valueP / segment.getStepSize()) * segment.getStepSize();
					}
					return start + valueP;
				}
				time -= segment.getDuration();
				start = segment.getEndValue();
			}
			throw new IllegalStateException("Logic error--should not get here");
		}

		/** @return This variable's current value within its style sheet */
		@Override
		public Double get() {
			return theCurrentValue;
		}

		@Override
		public Type getType() {
			return new Type(Double.class);
		}

		private void setTime(long time) {
			double oldValue = theCurrentValue;
			theCurrentValue = getValueFor(time);
			if(oldValue == theCurrentValue)
				return;
			theController.onNext(new org.muis.core.rx.ObservableValueEvent<>(this, oldValue, theCurrentValue, null));
		}
	}

	/** Represents the linear progression of a variable from one value to another over time */
	public static final class AnimationSegment {
		private final double theEndValue;

		private final double theStepSize;

		private final long theDuration;

		/**
		 * @param endValue The value toward which the variable will progress
		 * @param valueStepSize The value step size by which the variable's value will be incremented
		 * @param duration The amount of time that will elapse between the beginning of this animation segment and the reaching of the end
		 *            value
		 */
		public AnimationSegment(double endValue, double valueStepSize, long duration) {
			if(valueStepSize < 0)
				throw new IllegalArgumentException("Negative value size for animation segment");
			theEndValue = endValue;
			theStepSize = valueStepSize;
			theDuration = duration;
		}

		/** @return The value toward which the variable will progress */
		public double getEndValue() {
			return theEndValue;
		}

		/** @return The value step size by which the variable's value will be incremented */
		public double getStepSize() {
			return theStepSize;
		}

		/** @return The amount of time that will elapse between the beginning of this animation segment and the reaching of the end value */
		public long getDuration() {
			return theDuration;
		}
	}

	private final MuisValueReferenceParser theModelParser;

	private java.util.LinkedHashMap<String, AnimatedVariable> theVariables;

	private volatile long theStartTime;

	private volatile boolean isPaused;

	/** @param modelParser The model parser for this style sheet to get constants, function definitions, and other information from */
	public AnimatedStyleSheet(MuisValueReferenceParser modelParser) {
		theModelParser = new org.muis.core.parser.DefaultModelValueReferenceParser(modelParser, null) {
			@Override
			protected void applyModification() {
				super.applyModification();
				if(getEvaluationEnvironment() instanceof DefaultEvaluationEnvironment)
					((DefaultEvaluationEnvironment) getEvaluationEnvironment()).addVariableSource(new VariableSource() {
						@Override
						public Variable [] getDeclaredVariables() {
							return theVariables.values().toArray(new Variable[theVariables.size()]);
						}

						@Override
						public Variable getDeclaredVariable(String name) {
							return theVariables.get(name);
						}
					});

				getEvaluator().addEvaluator(ParsedIdentifier.class, new ObservableItemEvaluator<ParsedIdentifier>() {
					private final ObservableItemEvaluator<? super ParsedIdentifier> superEval = getEvaluator().getObservableEvaluatorFor(
						ParsedIdentifier.class);

					@Override
					public ObservableValue<?> evaluateObservable(ParsedIdentifier item, ObservableEvaluator evaluator,
						EvaluationEnvironment env, boolean asType) throws EvaluationException {
						AnimatedVariable var = theVariables.get(item.getName());
						if(var != null)
							return var;
						else if(superEval != null)
							return superEval.evaluateObservable(item, evaluator, env, asType);
						else
							return null;
					}
				});
				getEvaluator().addEvaluator(ConstantItem.class, new prisms.lang.eval.PrismsItemEvaluator<ConstantItem>() {
					@Override
					public EvaluationResult evaluate(ConstantItem item, PrismsEvaluator evaluator, EvaluationEnvironment env2,
						boolean asType, boolean withValues) throws EvaluationException {
						return item.get();
					}
				});
			}
		};
		theVariables = new java.util.LinkedHashMap<>();
	}

	/** @return The model parser that this style sheet uses */
	protected MuisValueReferenceParser getModelParser() {
		return theModelParser;
	}

	/** @param var The animation variable to add to this style sheet */
	protected void addVariable(AnimatedVariable var) {
		if(var.theStyleSheetCount > 0)
			throw new IllegalStateException("Animated variables may not be shared between style sheets: " + var.getName());
		stopAnimation();
		if(theVariables.containsKey(var.getName()))
			throw new IllegalArgumentException("An animated variable named " + var.getName() + " already exists in this style sheet");
		theVariables.put(var.getName(), var);
		var.theStyleSheetCount++;
	}

	/** @param var The variable to remove */
	protected void removeVariable(AnimatedVariable var) {
		stopAnimation();
		if(theVariables.remove(var.getName()) != null)
			return;
		var.theStyleSheetCount--;
	}

	/** Begins animation on this style sheet */
	protected void startAnimation() {
		isPaused = false;
		if(theStartTime > 0)
			return; // Already animating
		if(theVariables.isEmpty())
			return; // Nothing to animate
		theStartTime = System.currentTimeMillis();
		org.muis.motion.AnimationManager.get().start(new org.muis.motion.Animation() {
			@Override
			public boolean update(long time) {
				if(theStartTime == 0) {
					theStartTime = 0;
					setAnimationTime(0);
					return true;
				} else if(isPaused)
					return false;
				setAnimationTime(time);
				return false;
			}

			@Override
			public long getMaxFrequency() {
				return 20;
			}
		});
	}

	/** Stops animation on this style sheet, causing all variables to revert to their original values (this happens asynchronously) */
	protected void stopAnimation() {
		isPaused = false;
		theStartTime = 0;
	}

	/**
	 * The engine method for animation on this style sheet. Updates the values of all the variables so that the values reflected by this
	 * style sheet are updated.
	 *
	 * @param time The animation time to update with
	 */
	protected void setAnimationTime(long time) {
		java.util.HashSet<String> changedVars = new java.util.HashSet<>();
		for(AnimatedVariable var : theVariables.values()) {
			var.setTime(time);
		}
		// Iterate through local attributes and fire events for variable-dependent values
		// TODO This may not be necessary when styles are all rx-ified since things should be listening to the observable variables
		for(StyleAttribute<?> attr : allLocal()) {
			for(StyleExpressionEvalValue<StateGroupTypeExpression<?>, ?> sev : getLocalExpressions(attr)) {
				if(hasVariable(sev.getValueExpression(), changedVars))
					styleChanged(attr, sev.getExpression(), this);
			}
		}
	}

	/**
	 * Pauses animation of the values for this style sheet
	 *
	 * @throws IllegalStateException If animation has not been started on this style sheet
	 */
	public void pause() throws IllegalStateException {
		if(theStartTime == 0)
			throw new IllegalStateException("Animation is not running");
		isPaused = true;
	}

	/** @return Whether animation has started on this style sheet */
	public boolean isAnimating() {
		return theStartTime > 0;
	}

	/** @return Whether animation on this style sheet is currently paused */
	public boolean isPaused() {
		return isPaused;
	}

	/**
	 * @param value The expression to check
	 * @param vars A set of variable names to check for. May be null to check for any variable.
	 * @return Whether the given expression is dependent on the value of an external variable
	 */
	public static boolean hasVariable(ParsedItem value, java.util.Set<String> vars) {
		if(value instanceof prisms.lang.types.ParsedIdentifier)
			return vars == null || vars.contains(((prisms.lang.types.ParsedIdentifier) value).getName());
		for(ParsedItem depend : value.getDependents())
			if(hasVariable(depend, vars))
				return true;
		return false;
	}

	@Override
	public Iterator<AnimatedVariable> iterator() {
		return prisms.util.ArrayUtils.immutableIterator(theVariables.values().iterator());
	}

	@Override
	protected <T> StyleExpressionValue<StateGroupTypeExpression<?>, T> createStyleExpressionValue(StyleAttribute<T> attr,
		StateGroupTypeExpression<?> exp, T value) {
		if(value == null)
			value = (T) new ConstantItem(attr.getType().getType(), null);
		else if(!(value instanceof ParsedItem))
			value = (T) new ConstantItem(new prisms.lang.Type(value.getClass()), value);
		return new StyleExpressionEvalValue<>(this, attr, exp, (ParsedItem) value);
	}

	/**
	 * @param attr The attribute to set the value for
	 * @param expr The conditional expression for which the given value will be applied
	 * @param value The value expression to be evaluated for the style attribute's value
	 */
	protected <T> void setAnimatedValue(StyleAttribute<T> attr, StateGroupTypeExpression<?> expr, ObservableValue<? extends T> value) {
		super.set((StyleAttribute<Object>) attr, expr, value);
	}

	/**
	 * @param attr The attribute to set the value for
	 * @param expr The expression which must be true for the given value to apply
	 * @param parseableValue The string to parse to get the value for the attribute
	 * @throws MuisParseException If an error occurs parsing the attribute
	 */
	protected void setAnimatedValue(StyleAttribute<?> attr, StateGroupTypeExpression<?> expr, String parseableValue)
		throws MuisParseException {
		ObservableValue<?> value = theModelParser.parse(parseableValue, false);
		if(!attr.getType().getType().isAssignable(value.getType()))
			throw new MuisParseException("Parsed value's type is incompatible with style attribute " + attr + ": " + parseableValue);
		setAnimatedValue((StyleAttribute<Object>) attr, expr, value);
	}

	@Override
	public <T> StyleExpressionEvalValue<StateGroupTypeExpression<?>, T> [] getLocalExpressions(StyleAttribute<T> attr) {
		StyleExpressionValue<StateGroupTypeExpression<?>, T> [] array = super.getLocalExpressions(attr);
		StyleExpressionEvalValue<StateGroupTypeExpression<?>, T> [] ret = new StyleExpressionEvalValue[array.length];
		System.arraycopy(array, 0, ret, 0, array.length);
		return ret;
	}

	@Override
	public <T> StyleExpressionEvalValue<StateGroupTypeExpression<?>, T> [] getExpressions(StyleAttribute<T> attr) {
		StyleExpressionValue<StateGroupTypeExpression<?>, T> [] array = super.getExpressions(attr);
		StyleExpressionEvalValue<StateGroupTypeExpression<?>, T> [] ret = new StyleExpressionEvalValue[array.length];
		System.arraycopy(array, 0, ret, 0, array.length);
		return ret;
	}

	@Override
	protected <T> void set(StyleAttribute<T> attr, StateGroupTypeExpression<?> exp, T value) throws ClassCastException,
		IllegalArgumentException {
		super.set(attr, exp, (T) new ConstantItem(attr.getType().getType(), value));
	}
}
