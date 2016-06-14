package org.quick.test.model;

import java.awt.Color;

/** A simple Quick model for the button test */
public class ButtonTestModel {
	public static Color getColor(int count) {
		switch (count % 3) {
		case 0:
			return org.quick.core.style.Colors.red;
		case 1:
			return org.quick.core.style.Colors.blue;
		case 2:
			return org.quick.core.style.Colors.green;
		}
		return org.quick.core.style.Colors.black;
	}
}
