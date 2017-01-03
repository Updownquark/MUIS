package org.quick.base.model;

import java.net.URL;
import java.time.Duration;
import java.util.*;

import org.observe.SimpleObservable;
import org.observe.SimpleSettableValue;
import org.quick.core.QuickParseEnv;
import org.quick.core.model.*;
import org.quick.core.parser.QuickParseException;
import org.quick.core.parser.QuickPropertyParser;
import org.quick.core.prop.QuickPropertyType;

public class SoundModel implements QuickAppModel {
	private final SimpleSettableValue<URL> theFile;
	private final SimpleSettableValue<Duration> theLength;

	private final SimpleSettableValue<Double> theRate;
	private final SimpleSettableValue<Boolean> isLooping;
	private final SimpleSettableValue<Boolean> isPlaying;
	/* TODO It would be better if this were just a getter method.  I'd have to spawn an animation just to update this value, which will
	 * not always be needed and the controls for how often the value is updated should be controlled by a CounterModel.
	 * Right now QuickAppModel returning a function is not supported.  I could just return an observable value that never fires events. */
	private final SimpleSettableValue<Duration> thePosition;
	private final SimpleObservable<Void> theEnd;

	private final Map<String, Object> theFields;

	public SoundModel() {
		theFile = new SimpleSettableValue<>(URL.class, true);
		theLength = new SimpleSettableValue<>(Duration.class, false);
		theLength.set(Duration.ZERO, null);

		theRate = new SimpleSettableValue<>(double.class, false);
		theRate.set(1.0, null);
		isLooping = new SimpleSettableValue<>(boolean.class, false);
		isLooping.set(false, null);
		isPlaying = new SimpleSettableValue<>(boolean.class, false);
		isPlaying.set(false, null);
		thePosition = new SimpleSettableValue<>(Duration.class, false);
		thePosition.set(Duration.ZERO, null);
		theEnd = new SimpleObservable<>();

		Map<String, Object> fields = new LinkedHashMap<>();
		fields.put("file", theFile);
		fields.put("playing", isPlaying);
		fields.put("rate", theRate);
		fields.put("position", thePosition);
		fields.put("end", theEnd);

		theFields = Collections.unmodifiableMap(fields);
	}

	@Override
	public Set<String> getFields() {
		return theFields.keySet();
	}

	@Override
	public Object getField(String name) {
		return theFields.get(name);
	}

	/** Builds a SoundModel */
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
				}).build();
		}

		@Override
		public QuickAppModel buildModel(QuickModelConfig config, QuickPropertyParser parser, QuickParseEnv parseEnv)
			throws QuickParseException {
			VALIDATOR.validate(config);
			int min = Integer.parseInt(config.getString("min", "0"));
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
			if (max <= min)
				throw new QuickParseException("max must be greater than min");
			if (maxFrequency < 10)
				throw new QuickParseException("max-frequency must be at least 10 milliseconds");
			// CounterModel counter = new CounterModel(min, max, rate, maxFrequency);
			if (start)
				counter.isRunning().set(true, null);
			return counter;
		}
	}
}
