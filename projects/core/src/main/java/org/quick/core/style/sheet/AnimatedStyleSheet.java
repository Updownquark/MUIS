package org.quick.core.style.sheet;

import java.util.Iterator;

import org.observe.ObservableValue;
import org.quick.core.style.StyleAttribute;

import prisms.lang.Type;
import prisms.lang.Variable;

/** A style sheet whose values can be animated internally */
public class AnimatedStyleSheet extends AbstractStyleSheet implements Iterable<AnimatedStyleSheet.AnimatedVariable> {
	/** A variable whose value can change over time to affect style value expressions' evaluated values */
	public static final class AnimatedVariable extends org.observe.DefaultObservableValue<Double> implements Variable,
		Iterable<AnimationSegment> {
		private final String theName;

		private final double theStartValue;

		private final java.util.Collection<AnimationSegment> theAnimation;

		private final boolean isRepeating;

		private final long theDuration;

		private final double theEndValue;

		private double theCurrentValue;

		private int theStyleSheetCount;

		private org.observe.Observer<org.observe.ObservableValueEvent<Double>> theController;

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
			theController.onNext(createChangeEvent(oldValue, theCurrentValue, null));
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

	private java.util.LinkedHashMap<String, AnimatedVariable> theVariables;

	private volatile long theStartTime;

	private volatile boolean isPaused;

	/**
	 * Creates the style sheet
	 *
	 * @param depends The style sheets that this style sheet inherits style information from
	 */
	public AnimatedStyleSheet(org.observe.collect.ObservableList<StyleSheet> depends) {
		super(depends);
		theVariables = new java.util.LinkedHashMap<>();
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
		org.quick.motion.AnimationManager.get().start(new org.quick.motion.Animation() {
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
		for(AnimatedVariable var : theVariables.values())
			var.setTime(time);
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

	@Override
	public Iterator<AnimatedVariable> iterator() {
		return org.qommons.ArrayUtils.immutableIterator(theVariables.values().iterator());
	}

	/**
	 * @param <T> The type of the attribute to set the value for
	 * @param attr The attribute to set the value for
	 * @param expr The expression which must be true for the given value to apply
	 * @param value The observable value for the attribute
	 * @return This style, for chaining
	 * @throws ClassCastException If the value's type is not compatible with the attribute
	 */
	protected <T> AnimatedStyleSheet setAnimatedValue(StyleAttribute<T> attr, StateGroupTypeExpression<?> expr,
		ObservableValue<? extends T> value) {
		if(!attr.getType().canCast(value.getType()))
			throw new ClassCastException("Value's type is incompatible with style attribute " + attr);
		set(attr, expr, value);
		return this;
	}

	@Override
	protected <T> AnimatedStyleSheet set(StyleAttribute<T> attr, StateGroupTypeExpression<?> exp, T value) throws ClassCastException,
		IllegalArgumentException {
		super.set(attr, exp, (T) new ConstantItem(attr.getType().getType(), value));
		return this;
	}
}
