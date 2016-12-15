package org.quick.test.model;

import java.awt.Color;

/** The model backing the ModelTest.qck */
public class ModelTestModel {
	private static final int RED = 0;
	private static final int BLUE = 1;
	private static final int GREEN = 2;

	public static String getBgImage(int colorIndex) {
		switch (colorIndex) {
		case RED:
			return "fire";
		case BLUE:
			return "waterfall";
		case GREEN:
			return "plant";
		default:
			return "fire";
		}
	}

	public static String getGroupName(int colorIndex) {
		switch (colorIndex) {
		case RED:
			return "bg-red";
		case BLUE:
			return "bg-blue";
		case GREEN:
			return "bg-green";
		default:
			return "bg-red";
		}
	}

	public static Color getFgColor(int colorIndex) {
		return Color.black;
	}

	public static String getText(int colorIndex) {
		switch (colorIndex) {
		case RED:
			return "red";
		case BLUE:
			return "blue";
		case GREEN:
			return "green";
		default:
			return "red";
		}
	}
}
