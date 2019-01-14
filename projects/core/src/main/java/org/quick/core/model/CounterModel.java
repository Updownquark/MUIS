package org.quick.core.model;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.observe.SettableValue;
import org.observe.SimpleObservable;
import org.observe.util.TypeTokens;
import org.quick.core.QuickParseEnv;
import org.quick.core.parser.QuickParseException;
import org.quick.core.parser.QuickPropertyParser;
import org.quick.core.prop.QuickPropertyType;
import org.quick.motion.Animation;
import org.quick.motion.AnimationManager;

/** Increments a value on a timer */
public class CounterModel implements QuickAppModel {
	/**
	 * The types of actions that a counter model can execute when its value passes its min or max constraints as a result of normal running
	 */
	public static enum FinishAction {
		/** The counter will stop running and its value will remain at the constraint it just passed */
		stop,
		/** The counter will stop running and its value will be reset to the opposite constraint of the one it just passed */
		reset,
		/** The counter's value will be reset to the opposite constraint of the one it just passed and the counter will resume running */
		loop
	};
	private final String theName;
	private final ReentrantReadWriteLock theLock;
	private final SimpleModelValue<Long> theMin;
	private final SimpleModelValue<Double> theRate;
	private final SimpleModelValue<Long> theMax;
	private final long theMaxFrequency;

	private final SimpleModelValue<Long> theValue;
	private final SimpleModelValue<FinishAction> onFinish;
	private final SimpleModelValue<Boolean> isRunning;
	private final SimpleObservable<Void> theLoop;

	private final SettableValue<Long> theExposedMin;
	private final SettableValue<Double> theExposedRate;
	private final SettableValue<Long> theExposedMax;
	private final SettableValue<Long> theExposedValue;
	private final SettableValue<Boolean> theExposedRunning;

	private long theLastUpdateTime;
	private double theReference;
	private AtomicReference<CounterMotion> theMotion;
	private volatile boolean skipLoopStop;

	private final Map<String, Object> theModelValues;

	/**
	 * @param min The minimum (and initial) value for the counter
	 * @param max The maximum value for the counter. The counter will loop back to its initial value if this value is reached.
	 * @param rate The rate of increase, in counts/second
	 * @param maxFrequency The maximum frequency with which the counter will be incremented, in ms
	 * @param name The name of this model
	 */
	public CounterModel(long min, long max, double rate, long maxFrequency, String name) {
		theName = name;
		theLock = new ReentrantReadWriteLock();
		theMin = new SimpleModelValue<>(theLock, long.class, false, name + ".min");
		theMin.set(min, null);
		theMax = new SimpleModelValue<>(theLock, long.class, false, name + ".max");
		theMax.set(max, null);
		theRate = new SimpleModelValue<>(theLock, double.class, false, name + ".rate");
		theRate.set(rate, null);
		theMaxFrequency = maxFrequency;

		theValue = new SimpleModelValue<>(theLock, TypeTokens.get().of(long.class), false, name + ".value");
		theValue.set(min, null);
		onFinish = new SimpleModelValue<>(theLock, FinishAction.class, false, name + ".onFinish");
		onFinish.set(FinishAction.loop, null);
		isRunning = new SimpleModelValue<>(theLock, boolean.class, false, name + ".running");
		isRunning.set(false, null);
		theLoop=new SimpleObservable<>();

		theMotion = new AtomicReference<>();

		theMin.changes().act(evt -> {
			if (theValue.get() < evt.getNewValue())
				theValue.set(evt.getNewValue(), evt);
		});
		theMax.changes().act(evt -> {
			if (theValue.get() > evt.getNewValue())
				theValue.set(theMin.get(), evt);
		});
		isRunning.value().act(running -> {
			if (running) {
				skipLoopStop = true;
				start();
			} else
				stop();
		});

		theExposedMin = theMin.filterAccept(v -> {
			if (v >= theMax.get())
				return "min must be less than max";
			else
				return null;
		}).refresh(theMax.changes());
		theExposedMax = theMax.filterAccept(v -> {
			if (v <= theMin.get())
				return "max must be greater than min";
			else
				return null;
		}).refresh(theMin.changes());
		theExposedRate = theRate.filterAccept(v -> {
			if (Double.isNaN(v))
				return "rate must be a number";
			else if (v == 0)
				return "rate must be non-zero";
			else if (Double.isInfinite(v))
				return "rate must not be infinite";
			else
				return null;
		});
		theExposedValue = theValue.filterAccept(v -> {
			if (v < theMin.get())
				return "value may not be less than min";
			else if (v > theMax.get())
				return "value may not be greater than max";
			else
				return null;
		}).refresh(theMin.changes()).refresh(theMax.changes()).onSet(v -> {
			theReference = v;
			// Allow the counter to keep counting backwards from min if the value is reset to min externally while going backward
			if (isRunning.get() && v == theMin.get())
				skipLoopStop = true;
		});
		theExposedRunning = isRunning.filterAccept(running -> {
			if (running == isRunning.get())
				return "Already " + (running ? "running" : "stopped");
			else
				return null;
		});

		Map<String, Object> modelValues = new LinkedHashMap<>();
		modelValues.put("min", theExposedMin);
		modelValues.put("max", theExposedMax);
		modelValues.put("rate", theExposedRate);
		modelValues.put("value", theExposedValue);
		modelValues.put("onFinish", onFinish);
		modelValues.put("running", theExposedRunning);
		modelValues.put("loop", theLoop.readOnly());
		theModelValues = Collections.unmodifiableMap(modelValues);
	}

	@Override
	public String getName() {
		return theName;
	}

	@Override
	public Set<String> getFields() {
		return theModelValues.keySet();
	}

	@Override
	public Object getField(String name) {
		return theModelValues.get(name);
	}

	/** @return A settable value that exposes and controls the initial value (and lower bound) of this counter's value */
	public SettableValue<Long> getMin() {
		return theExposedMin;
	}

	/** @return A settable value that exposes and controls the maximum for this counter's value */
	public SettableValue<Long> getMax() {
		return theExposedMax;
	}

	/** @return A settable value that exposes and controls the rate of increase for this counter's value, in counts per second */
	public SettableValue<Double> getRate() {
		return theExposedRate;
	}

	/** @return A settable value that exposes and controls this counter's value directly */
	public SettableValue<Long> getValue() {
		return theExposedValue;
	}

	/**
	 * @return A settable value that exposes and controls whether this counter loops back around and keeps running or stops at its bounds
	 */
	public SettableValue<FinishAction> onFinish() {
		return onFinish;
	}

	/** @return A settable value that exposes and controls whether this counter is currently increasing its value */
	public SettableValue<Boolean> isRunning() {
		return theExposedRunning;
	}

	long getMaxFrequency() {
		return theMaxFrequency;
	}

	void update(long time) {
		if (!isRunning.get())
			return;
		long diff = time - theLastUpdateTime;
		double newRef = theReference + (diff * theRate.get() / 1000);
		long newValue = Math.round(newRef);
		boolean looped = false;
		long current = theValue.get();
		if (newValue != current) {
			boolean doSkipLoopStop = skipLoopStop;
			skipLoopStop = false;
			long max = theMax.get();
			long min = theMin.get();
			if (newValue > max) {
				if (doSkipLoopStop)
					newValue = min + (newValue - min) % (max - min);
				else {
					looped = true;
					switch (onFinish.get()) {
					case stop:
						stop();
						newValue = max;
						break;
					case reset:
						stop();
						newValue = min;
						break;
					case loop:
						newValue = min + (newValue - min) % (max - min);
						break;
					}
				}
			} else if (newValue < min) {
				if (doSkipLoopStop)
					newValue = max - (min - newValue - 1) % (max - min);
				else {
					looped = true;
					switch (onFinish.get()) {
					case stop:
						stop();
						newValue = min;
						break;
					case reset:
						stop();
						newValue = max;
						break;
					case loop:
						newValue = max - (min - newValue - 1) % (max - min);
						break;
					}
				}
			}
			theLastUpdateTime = time;
			theReference = newRef;
			theValue.set(newValue, null);
			if (looped)
				theLoop.onNext(null);
		}
	}

	/**
	 * Starts the counter
	 *
	 * @return Whether the counter was stopped
	 */
	private boolean start() {
		CounterMotion motion = new CounterMotion(this);
		if (theMotion.compareAndSet(null, motion)) {
			theLastUpdateTime = 0;
			theReference = theValue.get();
			if (!isRunning.get())
				isRunning.set(true, null);
			AnimationManager.get().start(motion);
			return true;
		}
		return false;
	}

	/**
	 * Stops the counter
	 *
	 * @return Whether the counter was running
	 */
	private boolean stop() {
		CounterMotion motion = theMotion.getAndSet(null);
		if (motion != null) {
			if (isRunning.get())
				isRunning.set(false, null);
			theLastUpdateTime = 0;
			motion.stop();
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return theName;
	}

	/** Builds a CounterModel */
	public static class Builder implements QuickModelBuilder {
		private static QuickModelConfigValidator VALIDATOR;
		static {
			VALIDATOR = QuickModelConfigValidator.build()//
				.forConfig("min", b -> {
					b.withText(true).atMost(1);
				}).forConfig("max", b -> {
					b.withText(true).atMost(1);
				}).forConfig("rate", b -> {
					b.withText(true).required();
				}).forConfig("max-frequency", b -> {
					b.withText(true).atMost(1);
				}).forConfig("start", b -> {
					b.withText(true).atMost(1);
				}).forConfig("on-finish", b -> {
					b.withText(true).atMost(1);
				}).build();
		}

		@Override
		public QuickAppModel buildModel(String name, QuickModelConfig config, QuickPropertyParser parser, QuickParseEnv parseEnv)
			throws QuickParseException {
			VALIDATOR.validate(config);
			long min = Long.parseLong(config.getString("min", "0"));
			long max = Long.parseLong(config.getString("max", "1000000000000"));
			double rate = 1000.0
				/ QuickPropertyType.duration.getSelfParser().parse(parser, parseEnv, config.getString("rate")).get().toMillis();
			long maxFrequency = QuickPropertyType.duration.getSelfParser()
				.parse(parser, parseEnv, config.getString("max-frequency", "10mi")).get().toMillis();
			String startStr = config.getString("start", "true");
			if (!"true".equals(startStr) && !"false".equals(startStr))
				throw new QuickParseException("Invalid value for start: " + startStr);
			boolean start = "true".equals(startStr);
			if (Double.isNaN(rate))
				throw new QuickParseException("NaN not accepted for rate");
			if (Double.isInfinite(rate))
				throw new QuickParseException("rate must not be infinite");
			if (rate <= 0)
				throw new QuickParseException("rate must be positive");
			if (max <= min)
				throw new QuickParseException("max must be greater than min");
			if (maxFrequency < 10)
				throw new QuickParseException("max-frequency must be at least 10 milliseconds");
			CounterModel counter = new CounterModel(min, max, rate, maxFrequency, name);
			if (config.getString("on-finish") != null) {
				try {
					counter.onFinish().set(FinishAction.valueOf(config.getString("on-finish")), null);
				} catch (IllegalArgumentException e) {
					throw new QuickParseException("Invalid value for on-finish: " + config.getString("on-finish"));
				}
			}
			if (start)
				counter.isRunning().set(true, null);
			return counter;
		}
	}

	private static class CounterMotion implements Animation {
		private final Reference<CounterModel> theModel;
		private volatile boolean isStopped;

		CounterMotion(CounterModel model) {
			theModel = new WeakReference<>(model);
		}

		void stop() {
			isStopped = true;
		}

		@Override
		public boolean update(long time) {
			if (isStopped)
				return true;
			CounterModel model = theModel.get();
			if (model == null)
				return true;
			model.update(time);
			return false;
		}

		@Override
		public long getMaxFrequency() {
			CounterModel model = theModel.get();
			if (model == null)
				return 1;
			return model.getMaxFrequency();
		}
	}
}
