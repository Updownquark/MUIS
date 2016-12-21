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
	private final SimpleSettableValue<Integer> theInit;
	private final SimpleSettableValue<Float> theRate;
	private final SimpleSettableValue<Integer> theMax;
	private final long theMaxFrequency;

	private final SimpleSettableValue<Integer> theValue;
	private final SettableValue<Boolean> isLooping;
	private final SimpleSettableValue<Boolean> isRunning;

	private final SettableValue<Integer> theExposedInit;
	private final SettableValue<Float> theExposedRate;
	private final SettableValue<Integer> theExposedMax;
	private final SettableValue<Integer> theExposedValue;
	private final SettableValue<Boolean> theExposedRunning;

	private long theLastUpdateTime;
	private AtomicReference<CounterMotion> theMotion;

	private final Map<String, Object> theModelValues;

	/**
	 * @param init The initial value for the counter
	 * @param max The maximum value for the counter. The counter will loop back to its initial value if this value is reached.
	 * @param rate The rate of increase, in counts/second
	 * @param maxFrequency The maximum frequency with which the counter will be incremented, in ms
	 */
	public CounterModel(int init, int max, float rate, long maxFrequency) {
		theInit = new SimpleSettableValue<>(int.class, false);
		theInit.set(init, null);
		theMax = new SimpleSettableValue<>(int.class, false);
		theMax.set(max, null);
		theRate = new SimpleSettableValue<>(float.class, false);
		theRate.set(rate, null);
		theMaxFrequency = maxFrequency;

		theValue = new SimpleSettableValue<>(TypeToken.of(int.class), false);
		theValue.set(init, null);
		isLooping = new SimpleSettableValue<>(TypeToken.of(boolean.class), false);
		isLooping.set(true, null);
		isRunning = new SimpleSettableValue<>(boolean.class, false);
		isRunning.set(false, null);

		theMotion = new AtomicReference<>();

		theInit.act(evt -> {
			if (theValue.get() < evt.getValue())
				theValue.set(evt.getValue(), evt);
		});
		theMax.act(evt -> {
			if (theValue.get() > evt.getValue())
				theValue.set(theInit.get(), evt);
		});
		isRunning.value().act(running -> {
			if (running)
				start();
			else
				stop();
		});

		theExposedInit = theInit.filterAccept(v -> {
			if (v >= theMax.get())
				return "init must be less than max";
			else
				return null;
		}).refresh(theMax);
		theExposedMax = theMax.filterAccept(v -> {
			if (v <= theInit.get())
				return "max must be greater than init";
			else
				return null;
		}).refresh(theInit);
		theExposedRate = theRate.filterAccept(v -> {
			if (Float.isNaN(v))
				return "rate must be a number";
			else if (v == 0)
				return "rate must be non-zero";
			else if (Float.isInfinite(v))
				return "rate must not be infinite";
			else
				return null;
		});
		theExposedValue = theValue.filterAccept(v -> {
			if (v < theInit.get())
				return "value may not be less than init";
			else if (v > theMax.get())
				return "value may not be greater than max";
			else
				return null;
		}).refresh(theInit).refresh(theMax);
		theExposedRunning = isRunning.filterAccept(running -> {
			if (running == isRunning.get())
				return "Already " + (running ? "running" : "stopped");
			else
				return null;
		});

		Map<String, Object> modelValues = new LinkedHashMap<>();
		modelValues.put("init", theExposedInit);
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
	public SettableValue<Integer> getInit() {
		return theExposedInit;
	}

	/** @return A settable value that exposes and controls the maximum for this counter's value */
	public SettableValue<Integer> getMax() {
		return theExposedMax;
	}

	/** @return A settable value that exposes and controls the rate of increase for this counter's value */
	public SettableValue<Float> getRate() {
		return theExposedRate;
	}

	/** @return A settable value that exposes and controls this counter's value directly */
	public SettableValue<Integer> getValue() {
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
		long diff = time - theLastUpdateTime;
		int steps = (int) (diff * theRate.get() / 1000);
		if (steps != 0) {
			int newValue = theValue.get() + steps;
			int max = theMax.get();
			int init = theInit.get();
			if (newValue > max) {
				if (!isLooping.get()) {
					stop();
					newValue = max;
				} else
					newValue = init + (newValue - init) % (max - init);
			} else if (newValue < init) {
				if (!isLooping.get()) {
					stop();
					newValue = init;
				} else
					newValue = max - (init - newValue) % (max - init);
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
			theLastUpdateTime = 0;
			motion.stop();
			isRunning.set(false, null);
			return true;
		}
		return false;
	}

	/** Builds a CounterModel */
	public static class Builder implements QuickModelBuilder {
		private static QuickModelConfigValidator VALIDATOR;
		static {
			VALIDATOR = QuickModelConfigValidator.build()//
				.forConfig("init", b -> {
					b.withText(true).atMost(1);
				}).forConfig("max", b -> {
					b.withText(true).atMost(1);
				}).forConfig("rate", b -> {
					b.withText(true).required();
				}).forConfig("max-frequency", b -> {
					b.withText(true).atMost(1);
				}).forConfig("start", b -> {
					b.withText(true).atMost(1);
				}).build();
		}

		@Override
		public QuickAppModel buildModel(QuickModelConfig config, QuickPropertyParser parser, QuickParseEnv parseEnv)
			throws QuickParseException {
			VALIDATOR.validate(config);
			int init = Integer.parseInt(config.getString("init", "0"));
			int max = Integer.parseInt(config.getString("max", "" + Integer.MAX_VALUE));
			float rate = 1000f
				/ QuickPropertyType.duration.getSelfParser().parse(parser, parseEnv, config.getString("rate")).get().toMillis();
			long maxFrequency = QuickPropertyType.duration.getSelfParser()
				.parse(parser, parseEnv, config.getString("max-frequency", "10mi")).get().toMillis();
			String startStr = config.getString("start", "true");
			if (!"true".equals(startStr) && !"false".equals(startStr))
				throw new QuickParseException("Invalid value for start: " + startStr);
			boolean start = "true".equals(startStr);
			if (Float.isNaN(rate))
				throw new QuickParseException("NaN not accepted for rate");
			if (Float.isInfinite(rate))
				throw new QuickParseException("rate must not be infinite");
			if (rate <= 0)
				throw new QuickParseException("rate must be positive");
			if (max <= init)
				throw new QuickParseException("max must be greater than init");
			if (maxFrequency < 10)
				throw new QuickParseException("max-frequency must be at least 10 milliseconds");
			if (max - init < maxFrequency * rate / 1000)
				throw new QuickParseException("max - init must be at least max-frequency*rate");
			CounterModel counter = new CounterModel(init, max, rate, maxFrequency);
			if (start)
				counter.start();
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
