package org.muis.base.widget;

import org.muis.base.model.SpinnerModel;
import org.muis.core.MuisAttribute;
import org.muis.core.MuisProperty;
import org.muis.core.MuisTemplate;
import org.muis.core.model.ModelAttributes;
import org.muis.core.model.MuisActionListener;
import org.muis.core.model.MuisAppModel;
import org.muis.core.rx.ObservableValue;
import org.muis.core.tags.Template;
import org.muis.util.BiTuple;

/** A text box with up and down arrows to increment or decrement the value */
@Template(location = "../../../../spinner.muis")
public class Spinner extends MuisTemplate {
	/** The increment attribute, specifying the action to perform when the user clicks the up arrow */
	public static final MuisAttribute<MuisActionListener> increment = new MuisAttribute<>("increment", ModelAttributes.actionType);
	/** The decrement attribute, specifying the action to perform when the user clicks the down arrow */
	public static final MuisAttribute<MuisActionListener> decrement = new MuisAttribute<>("decrement", ModelAttributes.actionType);

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
	public static final MuisAttribute<NumberSpinnerType> type = new MuisAttribute<>("type",
		new MuisProperty.MuisEnumProperty<NumberSpinnerType>(NumberSpinnerType.class));
	/** Specifies the minimum value for a canned-type spinner */
	public static final MuisAttribute<Double> min = new MuisAttribute<>("min", MuisProperty.floatAttr);
	/** Specifies the maximum value for a canned-type spinner */
	public static final MuisAttribute<Double> max = new MuisAttribute<>("max", MuisProperty.floatAttr);
	/** Specifies the interva value (the amount by which the value changes when the user clicks one of the arrows) for a canned-type spinner */
	public static final MuisAttribute<Double> interval = new MuisAttribute<>("interval", MuisProperty.floatAttr);

	private SpinnerModel<?> theModel;

	/** Creates a spinner */
	public Spinner() {
		life().runWhen(() -> {
			Object accepter = new Object();

			atts().accept(accepter, ModelAttributes.model);

			atts().accept(accepter, ModelAttributes.value);
			atts().accept(accepter, increment);
			atts().accept(accepter, decrement);

			atts().accept(accepter, type);
			atts().accept(accepter, min);
			atts().accept(accepter, max);
			atts().accept(accepter, interval);

			// TODO Validate the attribute combinations and values. Compose the model.
			atts()
				.getHolder(ModelAttributes.model)
				.tupleV(atts().getHolder(type))
				.act(
					event -> {
						BiTuple<MuisAppModel, NumberSpinnerType> spinnerType = event.getValue();
						if(spinnerType.getValue1() != null) {
							atts().reject(accepter, ModelAttributes.value);
							atts().reject(accepter, increment);
							atts().reject(accepter, decrement);
							atts().reject(accepter, min);
							atts().reject(accepter, max);
							atts().reject(accepter, interval);
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
							// TODO Set the model
						} else if(spinnerType.getValue2() != null) {
							atts().reject(accepter, increment);
							atts().reject(accepter, decrement);
							atts().require(accepter, ModelAttributes.value);
							atts().accept(accepter, min);
							atts().accept(accepter, max);
							atts().accept(accepter, interval);
						} else {
							atts().require(accepter, ModelAttributes.value);
							atts().require(accepter, increment);
							atts().require(accepter, decrement);
						}
					});
		}, org.muis.core.MuisConstants.CoreStage.INIT_SELF.toString(), 1);
	}

	@Override
	protected ObservableValue<SpinnerModel<?>> getModel() {
		return theModel;
	}
}
