package org.quick.base.model;

import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import org.observe.*;
import org.observe.Observable;
import org.observe.Observer;
import org.qommons.Causable;
import org.qommons.Transaction;
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
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
		theFile = new SimpleModelValue<>(lock, URL.class, true, name + ".file");
		theLength = new SimpleModelValue<>(lock, Duration.class, false, name + ".length");
		theLength.set(Duration.ZERO, null);
		theClip = new AtomicReference<>();

		theRate = new SimpleModelValue<>(lock, double.class, false, name + ".rate");
		theRate.set(1.0, null);
		isLooping = new SimpleModelValue<>(lock, boolean.class, false, name + ".looping");
		isLooping.set(false, null);
		isPlaying = new SimpleModelValue<Boolean>(lock, boolean.class, false, name + ".playing") {
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
				return theFile.map(file -> {
					if (file == null)
						return "No file set";
					else if (theClip.get() == null)
						return "Could not load audio file " + file;
					return null;
				});
			}
		};

		theExposedRate = theRate.filterAccept(value -> value == null ? "Rate must be non-zero" : null);

		lock.writeLock().lock(); // Lock for initialization
		isPlaying.set(false, null);
		theEnd = new SimpleObservable<>();

		theFile.changes().act(evt -> {
			isPlaying.set(false, evt);
			loadFile(evt.getNewValue(), evt);
		});
		isPlaying.changes().act(evt -> {
			if (evt.getNewValue())
				start();
			else
				stop();
		});
		// TODO Use the rate
		SimpleObservable<Object> internalPositionEvent = new SimpleObservable<>();
		class SoundModelPosition implements SettableValue<Duration> {
			final Observable<?> update = Observable.or(theFile.changes().noInit(), theLength.changes().noInit(),
				isPlaying.changes().noInit(), theEnd, internalPositionEvent);

			@Override
			public TypeToken<Duration> getType() {
				return TypeToken.of(Duration.class);
			}

			@Override
			public Duration get() {
				return getPositionValue();
			}

			@Override
			public Observable<ObservableValueEvent<Duration>> noInitChanges() {
				return new Changes();
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

			@Override
			public String toString() {
				return getName() + ".position";
			}

			class Changes implements Observable<ObservableValueEvent<Duration>> {
				@Override
				public Subscription subscribe(Observer<? super ObservableValueEvent<Duration>> observer) {
					Duration[] value = new Duration[] { get() };
					Subscription sub = update.act(x -> {
						Duration newPos = get();
						ObservableValueEvent<Duration> evt = createChangeEvent(value[0], newPos, x);
						try (Transaction t = Causable.use(evt)) {
							observer.onNext(evt);
						}
						value[0] = newPos;
					});
					return sub;
				}

				@Override
				public boolean isSafe() {
					return true;
				}

				@Override
				public Transaction lock() {
					return theFile.lock();
				}

				@Override
				public Transaction tryLock() {
					return theFile.tryLock();
				}
			}
		}
		thePosition = new SoundModelPosition();

		Map<String, Object> fields = new LinkedHashMap<>();
		fields.put("file", theFile);
		fields.put("length", theLength.unsettable());
		fields.put("playing", isPlaying);
		fields.put("rate", theExposedRate);
		fields.put("loop", isLooping);
		fields.put("position", thePosition);
		fields.put("end", theEnd.readOnly());

		theFields = Collections.unmodifiableMap(fields);
		lock.writeLock().unlock();
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
