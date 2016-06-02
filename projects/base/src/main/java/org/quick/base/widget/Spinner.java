package org.quick.base.widget;

import org.observe.ObservableValue;
import org.observe.SettableValue;
import org.quick.base.model.SpinnerModel;
import org.quick.base.model.impl.SimpleSpinnerModel;
import org.quick.core.QuickTemplate;
import org.quick.core.model.ModelAttributes;
import org.quick.core.model.QuickActionListener;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.prop.QuickPropertyType;
import org.quick.core.tags.Template;

/** A text box with up and down arrows to increment or decrement the value */
@Template(location = "../../../../spinner.qck")
public class Spinner extends QuickTemplate {
	/** The increment attribute, specifying the action to perform when the user clicks the up arrow */
	public static final QuickAttribute<QuickActionListener> increment = new QuickAttribute<>("increment", ModelAttributes.actionType);
	/** The decrement attribute, specifying the action to perform when the user clicks the down arrow */
	public static final QuickAttribute<QuickActionListener> decrement = new QuickAttribute<>("decrement", ModelAttributes.actionType);

	/** Canned spinner types supported by this class */
	public static enum NumberSpinnerType {
		/** The type for an integer spinner */
		integer("int"),
		/** The type for a floating-point spinner */
		floating("float");

		private final String theDisplay;

		private NumberSpinnerType(String display) {
			theDisplay = display;
		}

		@Override
		public String toString() {
			return theDisplay;
		}
	}

	/** Used to specify a canned-type spinner */
	public static final QuickAttribute<NumberSpinnerType> type = new QuickAttribute<>("type", QuickPropertyType.forEnum(
		NumberSpinnerType.class));
	/** Specifies the minimum value for a canned-type spinner */
	public static final QuickAttribute<Integer> minI = new QuickAttribute<>("min", QuickPropertyType.integer);

	/** Specifies the maximum value for a canned-type spinner */
	public static final QuickAttribute<Integer> maxI = new QuickAttribute<>("max", QuickPropertyType.integer);

	/**
	 * Specifies the interval value (the amount by which the value changes when the user clicks one of the arrows) for a canned-type spinner
	 */
	public static final QuickAttribute<Integer> intervalI = new QuickAttribute<>("interval", QuickPropertyType.integer);

	/** Specifies the minimum value for a canned-type spinner */
	public static final QuickAttribute<Double> minF = new QuickAttribute<>("min", QuickPropertyType.floating);
	/** Specifies the maximum value for a canned-type spinner */
	public static final QuickAttribute<Double> maxF = new QuickAttribute<>("max", QuickPropertyType.floating);

	/**
	 * Specifies the interval value (the amount by which the value changes when the user clicks one of the arrows) for a canned-type spinner
	 */
	public static final QuickAttribute<Double> intervalF = new QuickAttribute<>("interval", QuickPropertyType.floating);

	/** Creates a spinner */
	public Spinner() {
		life().runWhen(() -> {
			Object accepter = new Object();

			atts().accept(accepter, ModelAttributes.model, type);

			// Validate the attribute combinations and values. Compose the model.
			atts()
				.getHolder(ModelAttributes.model)
				.tupleV(atts().getHolder(type))
				.value()
				.act(
					spinnerType -> {
						if(spinnerType.getValue1() != null) {
							atts().reject(accepter, ModelAttributes.value, increment, decrement, minI, maxI, intervalI, minF, maxF,
								intervalF);
							if(spinnerType.getValue2() != null) {
								msg().warn(
									"Both " + ModelAttributes.model.getName() + " and " + type.getName() + " attributes specified for "
										+ getTagName());
							}
							if(!(spinnerType.getValue1() instanceof SpinnerModel)) {
								msg().fatal(
									ModelAttributes.model.getName() + " value must be an instance of " + SpinnerModel.class.getName(),
									"value", spinnerType.getValue1());
								return;
							}
							setModel("model", spinnerType.getValue1());
						} else if(spinnerType.getValue2() != null) {
							atts().reject(accepter, increment, decrement);
							atts().require(accepter, ModelAttributes.value);
							switch (spinnerType.getValue2()) {
							case integer:
								atts().reject(accepter, minF, maxF, intervalF);
								atts().accept(accepter, minI, maxI, intervalI);

								SimpleSpinnerModel.LongModel intModel = new SimpleSpinnerModel.LongModel(0, 0, 1000, 1);
								setModel("model", intModel);
								((SettableValue<Long>) intModel.getValue()).link(ObservableValue.flatten(
									(ObservableValue<? extends ObservableValue<? extends Number>>) atts().getHolder(ModelAttributes.value))
									.mapV(num -> num.longValue()));
								intModel.getValue().act(valueEvent -> {
									SettableValue<?> modelValue = (SettableValue<?>) atts().get(ModelAttributes.value);
									((SettableValue<Object>) modelValue).set(valueEvent.getValue(), valueEvent);
								});
								atts().getHolder(minI).tupleV(atts().getHolder(maxI), atts().getHolder(intervalI)).value().act(tuple -> {
									Integer min = tuple.getValue1();
									Integer max = tuple.getValue2();
									Integer intvl = tuple.getValue3();
									if(min == null)
										min = 0;
									if(max == null)
										max = Integer.MAX_VALUE;
									if(intvl == null)
										intvl =  1;
									intModel.setConstraints(min.longValue(), max.longValue(), intvl.longValue());
								});
								break;
							case floating:
								atts().reject(accepter, minI, maxI, intervalI);
								atts().accept(accepter, minF, maxF, intervalF);

								SimpleSpinnerModel.DoubleModel floatModel = new SimpleSpinnerModel.DoubleModel(0, 0, 1000, 1);
								setModel("model", floatModel);
								((SettableValue<Double>) floatModel.getValue()).link(ObservableValue.flatten(
									(ObservableValue<? extends ObservableValue<? extends Number>>) atts().getHolder(ModelAttributes.value))
									.mapV(num -> num.doubleValue()));
								floatModel.getValue().act(valueEvent -> {
									SettableValue<?> modelValue = (SettableValue<?>) atts().get(ModelAttributes.value);
									((SettableValue<Object>) modelValue).set(valueEvent.getValue(), valueEvent);
								});
								atts().getHolder(minF).tupleV(atts().getHolder(maxF), atts().getHolder(intervalF)).value().act(tuple -> {
									Double min = tuple.getValue1();
									Double max = tuple.getValue2();
									Double intvl = tuple.getValue3();
									if(min == null)
										min = (double) 0;
									if(max == null)
										max = (double) Double.MAX_VALUE;
									if(intvl == null)
										intvl = (double) 1;
									floatModel.setConstraints(min.doubleValue(), max.doubleValue(), intvl.doubleValue());
								});
								break;
							}

						} else {
							atts().reject(accepter, minI, maxI, intervalI, minF, maxF, intervalF);
							atts().require(accepter, ModelAttributes.value, increment, decrement);

							atts()
								.getHolder(ModelAttributes.value)
								.tupleV(atts().getHolder(increment), atts().getHolder(decrement))
								.value()
								.act(
									tuple -> {
										setModel("model", new org.quick.base.model.impl.ComposedSpinnerModel<>(
											(ObservableValue<Object>) tuple.getValue1(), tuple.getValue2(), tuple.getValue3()));
									});
						}
					});
		}, org.quick.core.QuickConstants.CoreStage.INIT_SELF.toString(), 1);
	}
}
