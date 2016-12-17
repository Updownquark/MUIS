package org.quick.core.model;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.observe.ObservableValue;
import org.observe.SimpleSettableValue;
import org.quick.core.QuickParseEnv;
import org.quick.core.parser.QuickParseException;
import org.quick.core.parser.QuickPropertyParser;
import org.quick.core.prop.QuickPropertyType;
import org.quick.motion.Animation;
import org.quick.motion.AnimationManager;

import com.google.common.reflect.TypeToken;

public class CounterModel implements QuickAppModel {
	private final SimpleSettableValue<Float> theInit;
	private final SimpleSettableValue<Float> theRate;
	private final SimpleSettableValue<Float> theMax;
	private final SimpleSettableValue<Float> theStep;
	private final long theMaxFrequency;

	private final SimpleSettableValue<Float> theValue;
	private final SimpleSettableValue<Boolean> isRunning;
	private final SimpleSettableValue<Boolean> isPaused;

	private long theLastUpdateTime;
	private AtomicReference<CounterMotion> theMotion;

	private final Map<String, Object> theModelValues;

	public CounterModel(float init, float max, float rate, float step, long maxFrequency) {
		theInit = new SimpleSettableValue<>(float.class, false);
		theInit.set(init, null);
		theMax = new SimpleSettableValue<>(float.class, false);
		theMax.set(max, null);
		theRate = new SimpleSettableValue<>(float.class, false);
		theRate.set(rate, null);
		theStep = new SimpleSettableValue<>(float.class, false);
		theStep.set(step, null);
		theMaxFrequency = maxFrequency;

		theValue = new SimpleSettableValue<>(TypeToken.of(float.class), false);
		theValue.set(init, null);
		isRunning = new SimpleSettableValue<>(boolean.class, false);
		isRunning.set(false, null);
		isPaused = new SimpleSettableValue<>(boolean.class, false);
		isPaused.set(false, null);

		theMotion = new AtomicReference<>();

		Map<String, Object> modelValues = new LinkedHashMap<>();
		modelValues.put("value", theValue.unsettable());
		modelValues.put("init", theInit.filterAccept(v -> {
			if (Float.isNaN(v))
				return "init must be a number";
			else if (Float.isInfinite(v))
				return "init must not be infinite";
			else if (v >= theMax.get())
				return "init must be less than max";
			else if (v > theMax.get() - theRate.get())
				return "init must be at most max - step";
			else if ((theMax.get() - v) / theRate.get() <= theMaxFrequency)
				return "init must be at most max - " + theMaxFrequency + "*rate";
			else
				return null;
		}).refresh(theStep).refresh(theMax));
		modelValues.put("step", theStep.filterAccept(v -> {
			if (Float.isNaN(v))
				return "step must be a number";
			else if (v < 0)
				return "step must not be negative";
			else if (Float.isInfinite(v))
				return "step must not be infinite";
			else if (v > theMax.get() - theInit.get())
				return "step must be at most max - init";
			else
				return null;
		}).refresh(theInit).refresh(theMax));
		modelValues.put("max", theMax.filterAccept(v -> {
			if (Float.isNaN(v))
				return "max must be a number";
			else if (v < theInit.get())
				return "max must be greater than init";
			else if (v < theInit.get() + theStep.get())
				return "max must be at least init + step";
			else if ((v - theInit.get()) / theRate.get() <= theMaxFrequency)
				return "max must be at least init+" + theMaxFrequency + "*rate";
			else
				return null;
		}).refresh(theInit).refresh(theStep));
		modelValues.put("rate", theRate.filterAccept(v -> {
			if (Float.isNaN(v))
				return "rate must be a number";
			else if (v <= 0)
				return "rate must be a positive number";
			else if (Float.isInfinite(v))
				return "rate must not be infinite";
			else if ((theMax.get() - theInit.get()) / v <= theMaxFrequency)
				return "rate must be at most (max - init)/" + theMaxFrequency;
			else
				return null;
		}));
		modelValues.put("running", isRunning.unsettable());
		modelValues.put("paused", isPaused);
		modelValues.put("start", new org.observe.ObservableAction<Boolean>() {
			@Override
			public TypeToken<Boolean> getType() {
				return isRunning.getType();
			}

			@Override
			public Boolean act(Object cause) throws IllegalStateException {
				return start();
			}

			@Override
			public ObservableValue<String> isEnabled() {
				return isRunning.mapV(running -> running ? "Already started" : null);
			}
		});
		modelValues.put("stop", new org.observe.ObservableAction<Boolean>() {
			@Override
			public TypeToken<Boolean> getType() {
				return isRunning.getType();
			}

			@Override
			public Boolean act(Object cause) throws IllegalStateException {
				return stop();
			}

			@Override
			public ObservableValue<String> isEnabled() {
				return isRunning.mapV(running -> running ? null : "Already stopped");
			}
		});
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

	long getMaxFrequency() {
		return theMaxFrequency;
	}

	void update(long time) {
		long diff = time - theLastUpdateTime;

		if (theStep.get() > 0) {
			float step = theStep.get();
			int steps = (int) (diff * theRate.get() / step);
			if (steps > 0) {
				theLastUpdateTime = time;
				theValue.set(theValue.get() + steps * step, null);
			}
		} else {
			float valueDiff = (diff * theRate.get() / 1000);
			theLastUpdateTime = time;
			float newValue = theValue.get() + valueDiff;
			float range = theMax.get() - theInit.get();
			if (newValue > theMax.get())
				newValue -= range * (int) ((newValue - theMax.get()) / range);
			theValue.set(newValue, null);
		}
	}

	public boolean start() {
		CounterMotion motion = new CounterMotion(this);
		if (theMotion.compareAndSet(null, motion)) {
			isRunning.set(true, null);
			AnimationManager.get().start(motion);
			return true;
		}
		return false;
	}

	public boolean stop() {
		CounterMotion motion = theMotion.getAndSet(null);
		if (motion != null) {
			theLastUpdateTime = 0;
			motion.stop();
			isRunning.set(false, null);
			return true;
		}
		return false;
	}

	public boolean isRunning() {
		return theMotion.get() != null;
	}

	public CounterModel reset() {
		theValue.set(theInit.get(), null);
		return this;
	}

	public CounterModel pause() {
		isPaused.set(true, null);
		return this;
	}

	public CounterModel resume() {
		isPaused.set(false, null);
		return this;
	}

	public boolean isPaused() {
		return isPaused.get();
	}

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
				}).forConfig("step", b -> {
					b.withText(true).atMost(1);
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
			float init = Float.parseFloat(config.getString("init", "0"));
			float max = Float.parseFloat(config.getString("max", "Infinity"));
			float rate = 1000f
				/ QuickPropertyType.duration.getSelfParser().parse(parser, parseEnv, config.getString("rate")).get().toMillis();
			float step = Float.parseFloat(config.getString("step", "0"));
			long maxFrequency = QuickPropertyType.duration.getSelfParser()
				.parse(parser, parseEnv, config.getString("max-frequency", "10mi")).get().toMillis();
			String startStr = config.getString("start", "true");
			if (!"true".equals(startStr) && !"false".equals(startStr))
				throw new QuickParseException("Invalid value for start: " + startStr);
			if (Float.isNaN(init) || Float.isNaN(max) || Float.isNaN(rate) || Float.isNaN(step))
				throw new QuickParseException("NaN values not accepted for init, max, rate, or step");
			if (Float.isInfinite(init) || Float.isInfinite(rate) || Float.isInfinite(step))
				throw new QuickParseException("init, rate, and step must not be infinite");
			if (rate <= 0)
				throw new QuickParseException("rate must be positive");
			if (step < 0)
				throw new QuickParseException("step must not be negative");
			if (max <= init)
				throw new QuickParseException("max must be greater than init");
			if (init + step < max)
				throw new QuickParseException("max must be at least init+step");
			if (maxFrequency < 10)
				throw new QuickParseException("max-frequency must be at least 10 milliseconds");
			if (max - init < maxFrequency * rate)
				throw new QuickParseException("max - init must be at least max-frequency*rate");
			return new CounterModel(init, max, rate, step, maxFrequency);
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
			if (!model.isPaused())
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
