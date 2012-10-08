package org.muis.core.style.sheet;

import java.util.Iterator;

import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleExpressionValue;

import prisms.lang.EvaluationException;
import prisms.lang.ParsedItem;

/** A style sheet whose values can be animated internally */
public class AnimatedStyleSheet extends AbstractStyleSheet implements Iterable<AnimatedStyleSheet.AnimatedVariable> {
	/** A variable whose value can change over time to affect style value expressions' evaluated values */
	public static final class AnimatedVariable implements Iterable<AnimationSegment> {
		private final String theName;

		private final double theStartValue;

		private final java.util.Collection<AnimationSegment> theAnimation;

		private final boolean isRepeating;

		private final long theDuration;

		private final double theEndValue;

		private double theCurrentValue;

		private int theStyleSheetCount;

		/**
		 * @param varName The name for the variable
		 * @param startValue The starting value for the variable
		 * @param animation The animations that the variable will go through over time
		 * @param repeat Whether the animation should cycle or just animate once and finish at the terminal value
		 */
		public AnimatedVariable(String varName, double startValue, AnimationSegment [] animation, boolean repeat) {
			theName = varName;
			theStartValue = startValue;
			theAnimation = java.util.Collections.unmodifiableCollection(java.util.Arrays.asList(animation));
			isRepeating = repeat;
			long duration = 0;
			double endValue = theStartValue;
			for(AnimationSegment segment : animation) {
				duration += segment.getDuration();
				endValue = segment.getEndValue();
			}
			theDuration = duration;
			theEndValue = endValue;
			theCurrentValue = theStartValue;
		}

		/** @return The name of this variable */
		public String getName() {
			return theName;
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
					double p = (segment.getDuration() - time) * 1.0 / segment.getDuration();
					return start + p * (segment.getEndValue() - start);
				}
				start = segment.getEndValue();
			}
			throw new IllegalStateException("Logic error--should not get here");
		}

		/** @return This variable's current value within its style sheet */
		public double getCurrentValue() {
			return theCurrentValue;
		}
	}

	/** Represents the linear progression of a variable from one value to another over time */
	public static final class AnimationSegment {
		private final double theEndValue;

		private final long theDuration;

		/**
		 * @param endValue The value toward which the variable will progress
		 * @param duration The amount of time that will elapse between the beginning of this animation segment and the reaching of the end
		 *            value
		 */
		public AnimationSegment(double endValue, long duration) {
			theEndValue = endValue;
			theDuration = duration;
		}

		/** @return The value toward which the variable will progress */
		public double getEndValue() {
			return theEndValue;
		}

		/** @return The amount of time that will elapse between the beginning of this animation segment and the reaching of the end value */
		public long getDuration() {
			return theDuration;
		}
	}

	private final prisms.lang.EvaluationEnvironment theEnv;

	private java.util.LinkedHashSet<AnimatedVariable> theVariables;

	private volatile long theStartTime;

	private volatile boolean isPaused;

	private long theLastAnimationUpdate;

	/** @param env The evaluation environment for this style sheet to get constants, function definitions, and other information from */
	public AnimatedStyleSheet(prisms.lang.EvaluationEnvironment env) {
		theEnv = env;
		theVariables = new java.util.LinkedHashSet<>();
	}

	/** @return The evaluation environment that this style sheet uses */
	protected prisms.lang.EvaluationEnvironment getEvaluationEnvironment() {
		return theEnv;
	}

	/** @param var The animation variable to add to this style sheet */
	protected void addVariable(AnimatedVariable var) {
		if(var.theStyleSheetCount > 0)
			throw new IllegalStateException("Animated variables may not be shared between style sheets: " + var.getName());
		stopAnimation();
		if(!theVariables.add(var))
			throw new IllegalArgumentException("An animated variable named " + var.getName() + " already exists in this style sheet");
		var.theStyleSheetCount++;
		try {
			theEnv.declareVariable(var.getName(), new prisms.lang.Type(Double.TYPE), false, null, 0);
			theEnv.setVariable(var.getName(), var.getCurrentValue(), null, 0);
		} catch(EvaluationException | NullPointerException e) {
			throw new IllegalStateException("Could not set initial value of variable " + var.getName() + " for evaluation", e);
		}
	}

	/** @param var The variable to remove */
	protected void removeVariable(AnimatedVariable var) {
		stopAnimation();
		if(!theVariables.remove(var))
			return;
		var.theStyleSheetCount--;
		try {
			theEnv.dropVariable(var.getName(), null, 0);
		} catch(EvaluationException e) {
			throw new IllegalStateException("Could not drop variable " + var.getName() + " for evaluation", e);
		}
	}

	/** Begins animation on this style sheet */
	protected void startAnimation() {
		isPaused = false;
		if(theStartTime > 0)
			return; // Already animating
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
		theLastAnimationUpdate = time;
		for(AnimatedVariable var : theVariables) {
			var.theCurrentValue = var.getValueFor(time - theStartTime);
			try {
				theEnv.setVariable(var.getName(), var.getCurrentValue(), null, 0);
			} catch(EvaluationException | NullPointerException e) {
				throw new IllegalStateException("Could not set value of variable " + var.getName() + " for evaluation", e);
			}
		}
		// Iterate through local attributes and fire events for variable-dependent values
		for(StyleAttribute<?> attr : allLocal()) {
			for(StyleExpressionValue<StateGroupTypeExpression<?>, ParsedItem> sev : getLocalAnimatedValues(attr)) {
				if(hasVariable(sev.getValue()))
					styleChanged(attr, sev.getExpression(), this);
			}
		}
	}

	/** @return How long animation has been proceeding for this style sheet */
	public long getAnimationDuration() {
		return theLastAnimationUpdate - theStartTime;
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
	 * @return Whether the given expression is dependent on the value of an external variable
	 */
	public static boolean hasVariable(ParsedItem value) {
		if(value instanceof prisms.lang.types.ParsedIdentifier)
			return true;
		for(ParsedItem depend : value.getDependents())
			if(hasVariable(depend))
				return true;
		return false;
	}

	@Override
	public Iterator<AnimatedVariable> iterator() {
		return prisms.util.ArrayUtils.immutableIterator(theVariables.iterator());
	}

	/**
	 * @param attr The attribute to get the values of
	 * @return All expressions for values of the given attribute local to this style sheet
	 */
	public StyleExpressionValue<StateGroupTypeExpression<?>, ParsedItem> [] getLocalAnimatedValues(StyleAttribute<?> attr) {
		return (StyleExpressionValue<StateGroupTypeExpression<?>, ParsedItem> []) super.getLocalExpressions(attr);
	}

	/**
	 * @param attr The attribute to set the value for
	 * @param expr The conditional expression for which the given value will be applied
	 * @param value The value expression to be evaluated for the style attribute's value
	 */
	protected void setAnimatedValue(StyleAttribute<?> attr, StateGroupTypeExpression<?> expr, ParsedItem value) {
		super.set((StyleAttribute<Object>) attr, expr, value);
	}

	@Override
	public <T> StyleExpressionValue<StateGroupTypeExpression<?>, T> [] getLocalExpressions(StyleAttribute<T> attr) {
		StyleExpressionValue<StateGroupTypeExpression<?>, ParsedItem> [] exprs = getLocalAnimatedValues(attr);
		StyleExpressionValue<StateGroupTypeExpression<?>, T> [] ret = new StyleExpressionValue[exprs.length];
		for(int i = 0; i < exprs.length; i++) {
			ret[i] = new StyleExpressionValue<StateGroupTypeExpression<?>, T>(exprs[i].getExpression(), castAndValidate(attr,
				evaluate(attr, exprs[i].getValue())));
		}
		return ret;
	}

	/**
	 * Evaluates an expression to get an actual style value
	 *
	 * @param <T> The type of the style attribute
	 * @param attr The attribute to evaluate for
	 * @param item The expression to evaluate
	 * @return The value that the expression evaluates to
	 * @throws ClassCastException If the expression evaluates to a value that is not recognized by the attribute
	 * @throws IllegalArgumentException If the expression evaluates to an invalid value for the attribute
	 * @throws IllegalStateException If the evaluation of the expression fails
	 */
	protected <T> T evaluate(StyleAttribute<T> attr, ParsedItem item) throws ClassCastException, IllegalArgumentException,
		IllegalStateException {
		try {
			return super.castAndValidate(attr, (T) item.evaluate(theEnv, false, true).getValue());
		} catch(EvaluationException e) {
			throw new IllegalStateException("Animated expression " + item + " for style attribute " + attr + " failed to evaluate", e);
		}
	}

	@Override
	protected <T> T castAndValidate(StyleAttribute<T> attr, T value) throws ClassCastException {
		if(!(value instanceof ParsedItem))
			throw new IllegalStateException("Implementation error: " + ParsedItem.class.getSimpleName() + " expected");
		ParsedItem pi = (ParsedItem) value;
		T realValue = evaluate(attr, pi);
		if(realValue == null)
			return value;
		try {
			super.castAndValidate(attr, realValue);
		} catch(ClassCastException e) {
			throw new ClassCastException("Evaluation of animated expression " + pi + " produced an invalid result: " + e.getMessage());
		} catch(IllegalArgumentException e) {
			throw new IllegalArgumentException("Evaluation of animated expression " + pi + " produced an invalid resul", e);
		}
		return value;
	}

	@Override
	protected <T> void set(StyleAttribute<T> attr, StateGroupTypeExpression<?> exp, T value) throws ClassCastException,
		IllegalArgumentException {
		super.set(attr, exp, (T) new ConstantItem(attr.getType().getType(), value));
	}
}
