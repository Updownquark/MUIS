package org.quick.base.layout;

import org.qommons.FloatList;
import org.qommons.collect.DefaultGraph;
import org.qommons.collect.Graph;
import org.quick.core.layout.End;
import org.quick.core.layout.Orientation;

public interface SpringBox {
	public static float OUTER_PREF_TENSION = 1000;
	public static float NOT_PREF_TENSION_THRESH = 100000;
	public static float OUTER_SIZE_TENSION = 1000000;
	public static float MAX_TENSION = 1E9f;
	public static final FloatList TENSIONS = new FloatList(new float[] { //
		MAX_TENSION, OUTER_SIZE_TENSION, NOT_PREF_TENSION_THRESH, OUTER_PREF_TENSION, 0, -OUTER_PREF_TENSION, -NOT_PREF_TENSION_THRESH,
		-OUTER_SIZE_TENSION, -MAX_TENSION });

	LayoutSpring getSizeGuide(Orientation orient);

	SpringBox setSize(int width, int height);
	SpringBox setTension(float horiz, float vertical);

	class LocatedSpringBox {
		public final SpringBox box;
		private int theX;
		private int theY;

		public LocatedSpringBox(SpringBox box) {
			this.box = box;
		}

		public int getX() {
			return theX;
		}

		public int getY() {
			return theY;
		}

		public int getPosition(Orientation orient) {
			return orient.isVertical() ? theY : theX;
		}

		public void setPosition(Orientation orient, int pos) {
			if (orient.isVertical())
				theY = pos;
			else
				theX = pos;
		}
	}

	class Constraint {
		public final LayoutSpring spring;
		public final Orientation orientation;
		public final End startTarget;
		public final End endTarget;

		public Constraint(LayoutSpring spring, Orientation orientation, End startTarget, End endTarget) {
			this.spring = spring;
			this.orientation = orientation;
			this.startTarget = startTarget;
			this.endTarget = endTarget;
		}
	}

	class ConstrainedSpringBox extends DefaultGraph<LocatedSpringBox, Constraint> implements SpringBox {
		private final ConstrainedLayoutSpring theHorizontal;
		private final ConstrainedLayoutSpring theVertical;
		private final LocatedSpringBox theBorder;

		public ConstrainedSpringBox() {
			theHorizontal = new ConstrainedLayoutSpring();
			theVertical = new ConstrainedLayoutSpring();
			theBorder = new LocatedSpringBox(this);
		}

		public LocatedSpringBox getBorder() {
			return theBorder;
		}

		@Override
		public LayoutSpring getSizeGuide(Orientation orient) {
			return orient.isVertical() ? theVertical : theHorizontal;
		}

		@Override
		public SpringBox setSize(int width, int height) {
			theHorizontal.theSize = width;
			theVertical.theSize = height;
			doSetTension(LayoutSpring.getTension(theHorizontal, width, height), LayoutSpring.getTension(theVertical, height, width), false);
			return this;
		}

		@Override
		public SpringBox setTension(float horiz, float vertical) {
			doSetTension(horiz, vertical, true);
			return this;
		}

		private void doSetTension(float horiz, float vertical, boolean withSize) {
			theHorizontal.theTension = horiz;
			theVertical.theTension = vertical;

			// Set the tension for all springs
			for(Edge<LocatedSpringBox, Constraint> edge : getEdges()){
				Constraint constraint=edge.getValue();
				if(constraint.orientation.isVertical())
					constraint.spring.setTension(vertical);
				else
					constraint.spring.setTension(horiz);
			}
			for(Node<LocatedSpringBox, Constraint> node : getNodes()){
				node.getValue().box.setTension(horiz, vertical);
			}

			/* Need to resolve cycles.  Make a heuristic that:
			 * * Walks the graph over and over attempting to balance out all the tensions in all the cycles
			 * * When it adjusts any constraint, that constraint keeps statistics on its tension history
			 * 		* It will use the mean tension given it by the heuristic.
			 * 		* After a certain number of adjustments, it will reject tension adjustments within a range defined by its mean and deviation
			 * 		* When an adjustment is rejected, the algorithm will turn around
			 * * The algorithm will continue until no more adjustments are accepted
			 */
			if (withSize) {
				// Figure out the total size of this box
				int[] width = new int[1];
				int[] height = new int[1];
				Graph.Walker<LocatedSpringBox, Constraint> walker = new Graph.Walker<>((from, path, to) -> {
					Constraint constraint = path.getValue();
					int add;
					if (constraint.startTarget == constraint.endTarget)
						add = constraint.spring.getSize();
					else
						add = -constraint.spring.getSize(); // TODO Is this right?
					if (constraint.orientation.isVertical())
						height[0] += add;
					else
						width[0] += add;

					return true;
				});
			}

			// TODO Auto-generated method stub

		}

		private class ConstrainedLayoutSpring implements LayoutSpring {
			int theSize = -1;
			float theTension = Float.NaN;

			@Override
			public int getSize() {
				return theSize;
			}

			@Override
			public LayoutSpring setSize(int size) {
				throw new UnsupportedOperationException("Use SpringBox.setSize(int width, int height)");
			}

			@Override
			public float getTension() {
				return theTension;
			}

			@Override
			public LayoutSpring setTension(float tension) {
				throw new UnsupportedOperationException("Use SpringBox.setTension(float horiz, float vertical)");
			}
		}
	}
}
