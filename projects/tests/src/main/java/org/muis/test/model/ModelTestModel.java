package org.muis.test.model;

import java.awt.Color;

import org.muis.core.MuisException;
import org.muis.core.model.DefaultMuisModel;
import org.muis.core.model.DefaultMuisModelValue;
import org.muis.core.model.ModelValueLinker;
import org.muis.core.style.Colors;

/** The model backing the ModelTest.muis */
public class ModelTestModel extends DefaultMuisModel {
	private DefaultMuisModel theBgModel;

	private DefaultMuisModelValue<Color> theBgValue;
	private org.muis.base.model.MuisButtonGroup theBgGroup;

	private DefaultMuisModelValue<String> theBgGroupName;

	private DefaultMuisModelValue<String> theBgImage;

	private DefaultMuisModel theFgModel;
	private DefaultMuisModelValue<Color> theFgValue;
	private DefaultMuisModelValue<String> theFgText;

	/** Creates the model */
	public ModelTestModel() {
		theBgModel = new DefaultMuisModel();
		theBgValue = new DefaultMuisModelValue<>(Color.class);
		theBgGroup = new org.muis.base.model.MuisButtonGroup();
		theBgGroupName = new DefaultMuisModelValue<>(String.class);
		theBgImage = new DefaultMuisModelValue<>(String.class);

		theFgModel = new DefaultMuisModel();
		theFgValue = new DefaultMuisModelValue<>(Color.class);
		theFgText = new DefaultMuisModelValue<>(String.class);

		subModels().put("bg", theBgModel);
		theBgModel.subModels().put("buttons", theBgGroup);
		theBgModel.values().put("value", theBgValue);
		theBgModel.values().put("group-name", theBgGroupName);
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

		new ModelValueLinker<>(null, theBgGroup, theBgValue).setLeftToRight(value -> {
			try {
				return Colors.parseColor(value);
			} catch(MuisException e) {
				e.printStackTrace();
				return Color.white;
			}
		}).setRightToLeft(value -> {
			return Colors.toString(value);
		}).link();
		new ModelValueLinker<>(null, theBgGroup, theBgGroupName).setLeftToRight(value -> {
			return "bg-" + value;
		}).setRightToLeft(value -> {
			throw new IllegalStateException("Should never get called");
		}).link();
		new ModelValueLinker<>(null, theBgGroup, theBgImage).setLeftToRight(value -> {
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

		new ModelValueLinker<>(null, theFgValue, theFgText).setLeftToRight(value -> {
			return Colors.toString(value);
		}).setRightToLeft(value -> {
			try {
				return Colors.parseColor(value);
			} catch(MuisException e) {
				// e.printStackTrace(); //No need to print this
			return Color.black;
			}
		}).link();

		theBgValue.set(Color.blue, null);
		theFgValue.set(Color.black, null);
	}
}
