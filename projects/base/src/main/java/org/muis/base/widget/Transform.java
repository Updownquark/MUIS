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

	/** Creates a transform widget */
	public Transform() {
	}

	@Override
	public void doLayout() {
		super.doLayout();
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
		Double rotation = atts().get(rotate);
		return new MuisElementCapture(null, this, new TransformTransformer(atts().get(reflect), rotation == null ? 0 : rotation), x, y, z,
			w, h);
	}

	@Override
	public MuisElementCapture [] paintChildren(Graphics2D graphics, Rectangle area) {
		// TODO Auto-generated method stub
		return super.paintChildren(graphics, area);
	}

	/** Applies a transform to {@link MuisElementCapture}s */
	public static class TransformTransformer implements org.muis.core.MuisElementCapture.Transformer {
		private final Orientation theReflection;

		private final double theRotation;

		/**
		 * @param reflection The reflection for this transformer to apply. May be null.
		 * @param rotation The rotation, in clockwise degrees, for this transformer to apply
		 */
		public TransformTransformer(Orientation reflection, double rotation) {
			theReflection = reflection;
			theRotation = rotation;
		}

		/** @return The orientation that this transformer applies. May be null. */
		public Orientation getReflection() {
			return theReflection;
		}

		/** @return The rotation (in clockwise degrees) that this transformer applies */
		public double getRotation() {
			return theRotation;
		}

		@Override
		public Point getChildPosition(MuisElementCapture parent, MuisElementCapture child, Point pos) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Point getParentPosition(MuisElementCapture parent, MuisElementCapture child, Point pos) {
			// TODO Auto-generated method stub
			return null;
		}
	}
}
