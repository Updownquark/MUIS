package org.muis.base.widget;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import org.muis.core.*;
import org.muis.core.layout.Orientation;
import org.muis.core.layout.SizeGuide;
import org.muis.core.tags.Template;

/** A widget with the capability to rotate and/or reflect its contents */
@Template(location = "../../../../simple-container.muis")
public class Transform extends MuisTemplate {
	/** The attribute allowing the user to reflect this widget's contents across either the x or the y axis */
	public static final MuisAttribute<Orientation> reflect = new MuisAttribute<>("reflect", new MuisProperty.MuisEnumProperty<>(
		Orientation.class));

	/** The attribute allowing the user to rotate this widget's contents. In clockwise degrees. */
	public static final MuisAttribute<Double> rotate = new MuisAttribute<>("rotate", MuisProperty.floatAttr);

	public Transform() {
	}

	@Override
	public void doLayout() {
	}

	@Override
	public SizeGuide getWSizer() {
		// TODO Auto-generated method stub
		return super.getWSizer();
	}

	@Override
	public SizeGuide getHSizer() {
		// TODO Auto-generated method stub
		return super.getHSizer();
	}

	@Override
	protected MuisElementCapture createCapture(int x, int y, int z, int w, int h) {
		// TODO Auto-generated method stub
		return super.createCapture(x, y, z, w, h);
	}

	@Override
	public MuisElementCapture [] paintChildren(Graphics2D graphics, Rectangle area) {
		// TODO Auto-generated method stub
		return super.paintChildren(graphics, area);
	}

	public static class TransformElementCapture extends MuisElementCapture {
		private final Orientation theReflection;

		private final double theRotation;

		public TransformElementCapture(MuisElementCapture p, MuisElement el, int xPos, int yPos, int zIndex, int w, int h,
			Orientation reflect, double rotate) {
			super(p, el, xPos, yPos, zIndex, w, h);
			theReflection = reflect;
			theRotation = rotate;
		}

		@Override
		public Point getDocLocation() {
			// TODO Auto-generated method stub
			return super.getDocLocation();
		}

		@Override
		protected TransformElementCapture clone() {
			return (TransformElementCapture) super.clone();
		}
	}

	public static final class TransformEventPositionCapture extends MuisEventPositionCapture {
		private final Orientation theReflection;

		private final double theRotation;

	}
}
