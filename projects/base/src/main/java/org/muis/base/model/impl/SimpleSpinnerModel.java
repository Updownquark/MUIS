package org.muis.base.model.impl;

import org.muis.base.model.SpinnerModel;
import org.muis.core.model.MuisActionEvent;
import org.muis.core.model.MuisActionListener;
import org.observe.*;

import prisms.lang.Type;

/**
 * A abstract implementation of spinner model that makes subclasses much easier to implement
 *
 * @param <T> The type of value the model manipulates
 */
public abstract class SimpleSpinnerModel<T> implements SpinnerModel<T> {
	private T theValue;

	private final ObservableValue<T> theObservableValue;

	private final Observer<ObservableValueEvent<T>> theValueController;

	private final SimpleSpinnerAction theIncrementer;
	private final SimpleSpinnerAction theDecrementer;

	/**
	 * @param type The type of value this model will manipulate
	 * @param value The initial value for the model
	 * @param settable Whether the model's value should be settable via typing
	 */
	protected SimpleSpinnerModel(Type type, T value, boolean settable) {
		if(settable) {
			theObservableValue = new org.observe.DefaultSettableValue<T>() {
				@Override
				public Type getType() {
					return type;
				}

				@Override
				public T get() {
					return theValue;
				}

				@Override
				public <V extends T> T set(V newValue, Object cause) throws IllegalArgumentException {
					String valMsg = isValid(newValue);
					if(valMsg != null)
						throw new IllegalArgumentException(valMsg);
					return SimpleSpinnerModel.this.setValue(newValue);
				}

				@Override
				public <V extends T> String isAcceptable(V newValue) {
					return isValid(newValue);
				}

				@Override
				public ObservableValue<Boolean> isEnabled() {
					return ObservableValue.constant(true);
				}
			};
		} else {
			theObservableValue = new DefaultObservableValue<T>() {
				@Override
				public Type getType() {
					return type;
				}

				@Override
				public T get() {
					return theValue;
				}
			};
		}
		theValueController = ((DefaultObservableValue<T>) theObservableValue).control(null);
		theIncrementer = new SimpleSpinnerAction() {
			@Override
			protected T getActionValue() {
				return getNextValue();
			}
		};
		theDecrementer = new SimpleSpinnerAction() {
			@Override
			protected T getActionValue() {
				return getPreviousValue();
			}
		};

		setValue(value);
	}

	/** @return The value that will replace the current value if the increment action executes */
	protected abstract T getNextValue();
	/** @return The value that will replace the current value if the decrement action executes */
	protected abstract T getPreviousValue();

	/**
	 * @param value The value to check
	 * @return Text describing why the given value is invalid in this model, or null if the value is valid
	 */
	protected abstract String isValid(T value);

	@Override
	public ObservableValue<? extends T> getValue() {
		return theObservableValue;
	}

	@Override
	public MuisActionListener getIncrement() {
		return theIncrementer;
	}

	@Override
	public MuisActionListener getDecrement() {
		return theDecrementer;
	}

	/**
	 * @param value The new value for this model
	 * @return The value that was previously set for this model
	 */
	protected T setValue(T value) {
		T oldValue = theValue;
		theValue = value;
		theValueController.onNext(theObservableValue.createChangeEvent(oldValue, theValue, null));
		checkActions();
		return oldValue;
	}

	/** Checks the increment and decrement actions' enabled status */
	protected void checkActions() {
		theIncrementer.check();
		theDecrementer.check();
	}

	/** A spinner model that manipulates an integer */
	public static class LongModel extends SimpleSpinnerModel<Long> {
		private long theMin;

		private long theMax;

		private long theInterval;

		/**
		 * @param value The initial value for the model
		 * @param min The minimum value
		 * @param max The maximum value
		 * @param interval The amount added or removed from the value when the increment/decrement actions are used
		 */
		public LongModel(long value, long min, long max, long interval) {
			super(new Type(Long.class), value, true);
			theMin = min;
			theMax = max;
			theInterval = interval;
		}

		/**
		 * @param min The minimum value
		 * @param max The maximum value
		 * @param interval The amount added or removed from the value when the increment/decrement actions are used
		 */
		public void setConstraints(long min, long max, long interval) {
			theMin = min;
			theMax = max;
			theInterval = interval;
			long value = getValue().get();
			if(value < theMin)
				setValue(theMin);
			else if(value > theMax)
				setValue(theMax);
			else
				checkActions();
		}

		@Override
		protected Long getNextValue() {
			return getValue().get() + theInterval;
		}

		@Override
		protected Long getPreviousValue() {
			return getValue().get() - theInterval;
		}

		@Override
		protected String isValid(Long value) {
			if(value < theMin)
				return "Value must be >= " + theMin;
			else if(value > theMax)
				return "Value must be <= " + theMax;
			return null;
		}
	}

	/** A spinner model that manipulates a double */
	public static class DoubleModel extends SimpleSpinnerModel<Double> {
		private double theMin;
		private double theMax;
		private double theInterval;

		/**
		 * @param value The initial value for the model
		 * @param min The minimum value
		 * @param max The maximum value
		 * @param interval The amount added or removed from the value when the increment/decrement actions are used
		 */
		public DoubleModel(double value, double min, double max, double interval) {
			super(new Type(Double.class), value, true);
			theMin = min;
			theMax = max;
			theInterval = interval;
		}

		/**
		 * @param min The minimum value
		 * @param max The maximum value
		 * @param interval The amount added or removed from the value when the increment/decrement actions are used
		 */
		public void setConstraints(double min, double max, double interval) {
			theMin = min;
			theMax = max;
			theInterval = interval;
			double value = getValue().get();
			if(value < theMin)
				setValue(theMin);
			else if(value > theMax)
				setValue(theMax);
			else
				checkActions();
		}

		@Override
		protected Double getNextValue() {
			return getValue().get() + theInterval;
		}

		@Override
		protected Double getPreviousValue() {
			return getValue().get() - theInterval;
		}

		@Override
		protected String isValid(Double value) {
			if(value < theMin)
				return "Value must be >= " + theMin;
			else if(value > theMax)
				return "Value must be <= " + theMax;
			return null;
		}
	}

	private abstract class SimpleSpinnerAction implements MuisActionListener {
		private boolean isEnabled;

		private final DefaultObservableValue<Boolean> theEnabledObservable;

		private final Observer<ObservableValueEvent<Boolean>> theEnabledController;

		{
			theEnabledObservable = new DefaultObservableValue<Boolean>() {
				@Override
				public Type getType() {
					return new Type(Boolean.class);
				}

				@Override
				public Boolean get() {
					return isEnabled;
				}
			};
			theEnabledController = theEnabledObservable.control(null);
		}

		@Override
		public ObservableValue<Boolean> isEnabled() {
			return theEnabledObservable;
		}

		@Override
		public void actionPerformed(MuisActionEvent event) {
			setValue(getActionValue());
		}

		protected abstract T getActionValue();

		private boolean isInstantEnabled() {
			T actionValue = getActionValue();
			if(actionValue == null || actionValue.equals(theValue))
				return false;
			return isValid(actionValue) == null;
		}

		void check() {
			boolean enabled = isInstantEnabled();
			if(enabled != isEnabled) {
				isEnabled = enabled;
				theEnabledController.onNext(theEnabledObservable.createChangeEvent(!enabled, enabled, null));
			}
		}
	}
}
