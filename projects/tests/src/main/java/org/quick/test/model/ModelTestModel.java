package org.quick.test.model;

import java.awt.Color;

import org.observe.SimpleSettableValue;
import org.quick.core.QuickException;
import org.quick.core.model.DefaultQuickModel;
import org.quick.core.model.SettableValueLinker;
import org.quick.core.style.Colors;

/** The model backing the ModelTest.qck */
public class ModelTestModel extends DefaultQuickModel {
	private DefaultQuickModel theBgModel;

	private SimpleSettableValue<Color> theBgValue;
	private org.quick.base.model.QuickButtonGroup theBgGroup;

	private SimpleSettableValue<String> theBgGroupName;

	private SimpleSettableValue<String> theBgImage;

	private DefaultQuickModel theFgModel;

	private SimpleSettableValue<Color> theFgValue;

	private SimpleSettableValue<String> theFgText;

	/** Creates the model */
	public ModelTestModel() {
		theBgModel = new DefaultQuickModel();
		theBgValue = new SimpleSettableValue<>(Color.class, false);
		theBgGroup = new org.quick.base.model.QuickButtonGroup();
		theBgGroupName = new SimpleSettableValue<>(String.class, true);
		theBgImage = new SimpleSettableValue<>(String.class, false);

		theFgModel = new DefaultQuickModel();
		theFgValue = new SimpleSettableValue<>(Color.class, false);
		theFgText = new SimpleSettableValue<>(String.class, true);

		subModels().put("bg", theBgModel);
		theBgModel.subModels().put("buttons", theBgGroup);
		theBgModel.values().put("value", theBgValue);
		theBgModel.values().put("groupName", theBgGroupName);
		theBgModel.values().put("image", theBgImage);
		theBgModel.seal();

		subModels().put("fg", theFgModel);
		theFgModel.values().put("value", theFgValue);
		theFgModel.values().put("text", theFgText);
		theFgModel.seal();
		seal();

		theBgGroup.getValue("red", Boolean.class);
		theBgGroup.getValue("green", Boolean.class);
		theBgGroup.getValue("blue", Boolean.class);

		new SettableValueLinker<>(null, theBgGroup, theBgValue).setLeftToRight(value -> {
			try {
				return Colors.parseColor(value);
			} catch(QuickException e) {
				e.printStackTrace();
				return Color.white;
			}
		}).setRightToLeft(value -> {
			return Colors.toString(value);
		}).link();
		new SettableValueLinker<>(null, theBgGroup, theBgGroupName).setLeftToRight(value -> {
			return "bg-" + value;
		}).setRightToLeft(value -> {
			throw new IllegalStateException("Should never get called");
		}).link();
		new SettableValueLinker<>(null, theBgGroup, theBgImage).setLeftToRight(value -> {
			switch (value) {
			case "red":
				return "fire";
			case "blue":
				return "waterfall";
			case "green":
				return "plant";
			default:
				return "fire";
			}
		}).setRightToLeft(value -> {
			throw new IllegalStateException("Should never get called");
		}).link();

		new SettableValueLinker<>(null, theFgValue, theFgText).setLeftToRight(value -> {
			return Colors.toString(value);
		}).setRightToLeft(value -> {
			try {
				return Colors.parseColor(value);
			} catch(QuickException e) {
				// e.printStackTrace(); //No need to print this
			return Color.black;
			}
		}).link();

		theBgValue.set(Color.blue, null);
		theFgValue.set(Color.black, null);
	}
}
