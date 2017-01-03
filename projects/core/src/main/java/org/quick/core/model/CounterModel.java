package org.quick.core.model;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.observe.SettableValue;
import org.observe.SimpleSettableValue;
import org.quick.core.QuickParseEnv;
import org.quick.core.parser.QuickParseException;
import org.quick.core.parser.QuickPropertyParser;
import org.quick.core.prop.QuickPropertyType;
import org.quick.motion.Animation;
import org.quick.motion.AnimationManager;

import com.google.common.reflect.TypeToken;

/** Increments a value on a timer */
public class CounterModel implements QuickAppModel {
	private final SimpleSettableValue<Long> theMin;
	private final SimpleSettableValue<Double> theRate;
	private final SimpleSettableValue<Long> theMax;
	private final long theMaxFrequency;

	private final SimpleSettableValue<Long> theValue;
	private final SimpleSettableValue<Boolean> isLooping;
	private final SimpleSettableValue<Boolean> isRunning;

	private final SettableValue<Long> theExposedMin;
	private final SettableValue<Double> theExposedRate;
	private final SettableValue<Long> theExposedMax;
	private final SettableValue<Long> theExposedValue;
	private final SettableValue<Boolean> theExposedRunning;

	private long theLastUpdateTime;
	private AtomicReference<CounterMotion> theMotion;
	private volatile boolean skipLoopStop;

	private final Map<String, Object> theModelValues;

	/**
	 * @param min The minimum (and initial) value for the counter
	 * @param max The maximum value for the counter. The counter will loop back to its initial value if this value is reached.
	 * @param rate The rate of increase, in counts/second
	 * @param maxFrequency The maximum frequency with which the counter will be incremented, in ms
	 */
	public CounterModel(long min, long max, double rate, long maxFrequency) {
		theMin = new SimpleSettableValue<>(long.class, false);
		theMin.set(min, null);
		theMax = new SimpleSettableValue<>(long.class, false);
		theMax.set(max, null);
		theRate = new SimpleSettableValue<>(double.class, false);
		theRate.set(rate, null);
		theMaxFrequency = maxFrequency;

		theValue = new SimpleSettableValue<>(TypeToken.of(long.class), false);
		theValue.set(min, null);
		isLooping = new SimpleSettableValue<>(TypeToken.of(boolean.class), false);
		isLooping.set(true, null);
		isRunning = new SimpleSettableValue<>(boolean.class, false);
		isRunning.set(false, null);

		theMotion = new AtomicReference<>();

		theMin.act(evt -> {
			if (theValue.get() < evt.getValue())
				theValue.set(evt.getValue(), evt);
		});
		theMax.act(evt -> {
			if (theValue.get() > evt.getValue())
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
		}).refresh(theMax);
		theExposedMax = theMax.filterAccept(v -> {
			if (v <= theMin.get())
				return "max must be greater than min";
			else
				return null;
		}).refresh(theMin);
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
		}).refresh(theMin).refresh(theMax).onSet(v -> {
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
		modelValues.put("looping", isLooping);
		modelValues.put("running", theExposedRunning);
		theModelValues = Collections.unmodifiableMap(modelValues);
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
	public SettableValue<Boolean> isLooping() {
		return isLooping;
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
		long steps = (long) (diff * theRate.get() / 1000);
		if (steps != 0) {
			boolean doSkipLoopStop = skipLoopStop;
			skipLoopStop = false;
			long newValue = theValue.get() + steps;
			long max = theMax.get();
			long min = theMin.get();
			if (newValue > max) {
				if (!doSkipLoopStop && !isLooping.get()) {
					stop();
					newValue = max;
				} else
					newValue = min + (newValue - min) % (max - min);
			} else if (newValue < min) {
				if (!doSkipLoopStop && !isLooping.get()) {
					stop();
					newValue = min;
				} else
					newValue = max - (min - newValue - 1) % (max - min);
			}
			theLastUpdateTime = time;
			theValue.set(newValue, null);
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
				}).forConfig("loop", b -> {
					b.withText(true).atMost(1);
				}).build();
		}

		@Override
		public QuickAppModel buildModel(QuickModelConfig config, QuickPropertyParser parser, QuickParseEnv parseEnv)
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
			CounterModel counter = new CounterModel(min, max, rate, maxFrequency);
			if (config.getString("loop") != null) {
				if ("true".equals(config.getString("loop")))
					counter.isLooping().set(true, null);
				else if ("false".equals(config.getString("loop")))
					counter.isLooping().set(false, null);
				else
					throw new QuickParseException("Invalid value for loop: " + config.getString("loop"));
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
