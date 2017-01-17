package org.quick.base.model;

import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import org.observe.*;
import org.observe.Observable;
import org.observe.Observer;
import org.quick.core.QuickParseEnv;
import org.quick.core.model.*;
import org.quick.core.parser.QuickParseException;
import org.quick.core.parser.QuickPropertyParser;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.prop.QuickPropertyType;

import com.google.common.reflect.TypeToken;

public class SoundModel implements QuickAppModel {
	private final String theName;
	private final SimpleModelValue<URL> theFile;
	private final SimpleModelValue<Duration> theLength;

	private final SimpleModelValue<Boolean> isPlaying;
	private final SimpleModelValue<Double> theRate;
	private final SimpleModelValue<Boolean> isLooping;
	private final SettableValue<Duration> thePosition;
	private final SimpleObservable<Object> theEnd;

	private final SettableValue<Double> theExposedRate;

	private final Map<String, Object> theFields;

	private final AtomicReference<Clip> theClip;

	public SoundModel(String name) {
		theName = name;
		theFile = new SimpleModelValue<>(URL.class, true, name + ".file");
		theLength = new SimpleModelValue<>(Duration.class, false, name + ".length");
		theLength.set(Duration.ZERO, null);
		theClip = new AtomicReference<>();

		theRate = new SimpleModelValue<>(double.class, false, name + ".rate");
		theRate.set(1.0, null);
		isLooping = new SimpleModelValue<>(boolean.class, false, name + ".looping");
		isLooping.set(false, null);
		isPlaying = new SimpleModelValue<Boolean>(boolean.class, false, name + ".playing") {
			@Override
			public String isAcceptable(Boolean value) {
				String msg = super.isAcceptable(value);
				if (msg != null)
					return msg;
				if (value) {
					Clip clip = theClip.get();
					if (clip == null)
						return "No file set";
				}
				return null;
			}

			@Override
			public ObservableValue<String> isEnabled() {
				return theFile.mapV(file -> {
					if (file == null)
						return "No file set";
					else if (theClip.get() == null)
						return "Could not load audio file " + file;
					return null;
				});
			}
		};

		theExposedRate = theRate.filterAccept(value -> value == null ? "Rate must be non-zero" : null);

		isPlaying.set(false, null);
		theEnd = new SimpleObservable<>();

		theFile.act(evt -> {
			isPlaying.set(false, evt);
			loadFile(evt.getValue(), evt);
		});
		isPlaying.act(evt -> {
			if (evt.getValue())
				start();
			else
				stop();
		});
		// TODO Use the rate
		SimpleObservable<Object> internalPositionEvent = new SimpleObservable<>();
		thePosition = new SettableValue<Duration>() {
			@Override
			public TypeToken<Duration> getType() {
				return TypeToken.of(Duration.class);
			}

			@Override
			public boolean isSafe() {
				return true;
			}

			@Override
			public Duration get() {
				return getPositionValue();
			}

			@Override
			public Subscription subscribe(Observer<? super ObservableValueEvent<Duration>> observer) {
				return Observable.or(theFile.noInit(), theLength.noInit(), isPlaying.noInit(), theEnd, internalPositionEvent).act(evt -> {
					Duration newPos = get();
					Observer.onNextAndFinish(observer, createChangeEvent(newPos, newPos, evt));
				});
			}

			@Override
			public Duration set(Duration value, Object cause) throws IllegalArgumentException {
				Clip clip = theClip.get();
				if (clip == null)
					throw new IllegalArgumentException("No clip to set position for");
				Duration len = Duration.of(clip.getMicrosecondLength(), ChronoUnit.MICROS);
				if (value.compareTo(len) >= 0)
					throw new IllegalArgumentException("Position " + value + " must be less than the length (" + len + ")");
				Duration oldPos = Duration.of(clip.getMicrosecondPosition(), ChronoUnit.MICROS);
				clip.setMicrosecondPosition(value.toNanos() / 1000);
				internalPositionEvent.onNext(cause);
				return oldPos;
			}

			@Override
			public String isAcceptable(Duration value) {
				Clip clip = theClip.get();
				if (clip == null)
					return "No clip to set position for";
				Duration len = Duration.of(clip.getMicrosecondLength(), ChronoUnit.MICROS);
				if (value.compareTo(len) >= 0)
					return "Position " + value + " must be less than the length (" + len + ")";
				return null;
			}

			@Override
			public ObservableValue<String> isEnabled() {
				return isPlaying.isEnabled();
			}
		};

		Map<String, Object> fields = new LinkedHashMap<>();
		fields.put("file", theFile);
		fields.put("length", theLength.unsettable());
		fields.put("playing", isPlaying);
		fields.put("rate", theExposedRate);
		fields.put("loop", isLooping);
		fields.put("position", thePosition);
		fields.put("end", theEnd.readOnly());

		theFields = Collections.unmodifiableMap(fields);
	}

	@Override
	public String getName() {
		return theName;
	}

	@Override
	public Set<String> getFields() {
		return theFields.keySet();
	}

	@Override
	public Object getField(String name) {
		return theFields.get(name);
	}

	public SettableValue<URL> getFile() {
		return theFile;
	}

	public SettableValue<Double> getRate() {
		return theExposedRate;
	}

	public SettableValue<Boolean> isLooping() {
		return isLooping;
	}

	public Duration getPositionValue() {
		Clip clip = theClip.get();
		return Duration.of(clip.getMicrosecondPosition(), ChronoUnit.MICROS);
	}

	private void loadFile(URL file, Object cause) {
		Clip newClip;
		if (file == null)
			newClip = null;
		else {
			try {
				newClip = AudioSystem.getClip();
				newClip.open(AudioSystem.getAudioInputStream(file));
			} catch (Exception e) {
				throw new IllegalStateException("Could not load audio file " + file, e);
			}
		}
		Clip old = theClip.getAndSet(newClip);
		if (old != null)
			old.close();

		theLength.set(newClip == null ? Duration.ZERO : Duration.of(newClip.getMicrosecondLength(), ChronoUnit.MICROS), cause);
		//TODO add a listener to be notified when the clip reaches the end
		// newClip.addLineListener(lineEvent->{
		// lineEvent.
		// });
	}

	private void start() {
		Clip clip = theClip.get();
		clip.setFramePosition(0);
		if (clip != null)
			clip.start();
	}

	private void stop() {
		Clip clip = theClip.get();
		if (clip != null)
			clip.stop();
	}

	@Override
	public String toString() {
		return theName;
	}

	/** Builds a SoundModel */
	public static class Builder implements QuickModelBuilder {
		private static QuickModelConfigValidator VALIDATOR;
		static {
			VALIDATOR = QuickModelConfigValidator.build()//
				.forConfig("file", b -> {
					b.withText(true).atMost(1);
				}).forConfig("rate", b -> {
					b.withText(true).atMost(1);
				}).forConfig("loop", b -> {
					b.withText(true).required();
				}).build();
		}

		@Override
		public QuickAppModel buildModel(String name, QuickModelConfig config, QuickPropertyParser parser, QuickParseEnv parseEnv)
			throws QuickParseException {
			VALIDATOR.validate(config);
			SoundModel model = new SoundModel(name);
			if (config.getString("file") != null) {
				QuickAttribute<URL> fileProp = QuickAttribute.build("file", QuickPropertyType.resource).build();
				model.getFile().link(parser.parseProperty(fileProp, parseEnv, config.getString("file")));
			}
			if (config.getString("rate") != null) {
				QuickAttribute<Double> rateProp = QuickAttribute.build("rate", QuickPropertyType.floating).build();
				model.getRate().link(parser.parseProperty(rateProp, parseEnv, config.getString("rate")));
			}
			if (config.getString("loop") != null) {
				QuickAttribute<Boolean> loopProp = QuickAttribute.build("loop", QuickPropertyType.boole).build();
				model.isLooping().link(parser.parseProperty(loopProp, parseEnv, config.getString("loop")));
			}
			return model;
		}
	}
}
