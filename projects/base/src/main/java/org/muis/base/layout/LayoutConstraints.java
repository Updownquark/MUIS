package org.muis.base.layout;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;

import org.muis.base.layout.GeneticLayout.LayoutConstraint;
import org.muis.base.layout.GeneticLayout.LayoutScratchPad;
import org.muis.core.MuisElement;

public class LayoutConstraints {
	public static abstract class ExpressionConstraint implements LayoutConstraint, Iterable<LayoutConstraint> {
		private LayoutConstraint [] theComponentConstraints;

		public ExpressionConstraint(LayoutConstraint... constraints) {
			theComponentConstraints = constraints;
		}

		@Override
		public Iterator<LayoutConstraint> iterator() {
			return prisms.util.ArrayUtils.iterator(theComponentConstraints, true);
		}

		public ExpressionConstraint not() {
			return new NotExpression(this);
		}

		public AndConstraint and(LayoutConstraint constraint) {
			return new AndConstraint(this, constraint);
		}

		public OrConstraint or(LayoutConstraint constraint) {
			return new OrConstraint(this, constraint);
		}
	}

	public static class NotExpression extends ExpressionConstraint {
		public NotExpression(LayoutConstraint constraint) {
			super(constraint);
		}

		public LayoutConstraint getComponent() {
			return iterator().next();
		}

		@Override
		public boolean isViolated(MuisElement container, LayoutScratchPad layout) {
			return !getComponent().isViolated(container, layout);
		}

		@Override
		public float getViolation(MuisElement container, LayoutScratchPad layout) {
			return 1 / getComponent().getViolation(container, layout);
		}
	}

	public static class OrConstraint extends ExpressionConstraint {
		public OrConstraint(LayoutConstraint... constraints) {
			super(constraints);
		}

		@Override
		public boolean isViolated(MuisElement container, LayoutScratchPad layout) {
			for(LayoutConstraint constraint : this)
				if(constraint.isViolated(container, layout))
					return true;
			return false;
		}

		@Override
		public float getViolation(MuisElement container, LayoutScratchPad layout) {
			float max = 0;
			boolean hasConstraint = false;
			for(LayoutConstraint constraint : this) {
				hasConstraint = true;
				float violation = constraint.getViolation(container, layout);
				if(violation > max)
					max = violation;
			}
			if(!hasConstraint)
				return 0;
			return max;
		}
	}

	public static class AndConstraint extends ExpressionConstraint {
		public AndConstraint(LayoutConstraint... constraints) {
			super(constraints);
		}

		@Override
		public boolean isViolated(MuisElement container, LayoutScratchPad layout) {
			for(LayoutConstraint constraint : this)
				if(!constraint.isViolated(container, layout))
					return false;
			return true;
		}

		@Override
		public float getViolation(MuisElement container, LayoutScratchPad layout) {
			float min = Float.POSITIVE_INFINITY;
			boolean hasConstraint = false;
			for(LayoutConstraint constraint : this) {
				hasConstraint = true;
				float violation = constraint.getViolation(container, layout);
				if(violation < min)
					min = violation;
			}
			if(!hasConstraint)
				return 0;
			return min;
		}
	}

	/**
	 * A layout constraint preventing elements from being layed out on top of one another. The variable cost is the total area of
	 * intersection of elements.
	 */
	public static final LayoutConstraint interference = new LayoutConstraint() {
		@Override
		public boolean isViolated(MuisElement container, LayoutScratchPad layout) {
			ArrayList<Rectangle> bounds = new ArrayList<>();
			for(MuisElement element : layout) {
				Rectangle bound = layout.get(element);
				for(Rectangle b : bounds) {
					if(b.intersects(bound))
						return true;
				}
				bounds.add(bound);
			}
			return false;
		}

		@Override
		public float getViolation(MuisElement container, LayoutScratchPad layout) {
			float ret = 0;
			ArrayList<Rectangle> bounds = new ArrayList<>();
			for(MuisElement element : layout) {
				Rectangle bound = layout.get(element);
				for(Rectangle b : bounds) {
					if(b.intersects(bound)) {
						Rectangle intersection = b.intersection(bound);
						ret += intersection.width * intersection.height;
					}
				}
				bounds.add(bound);
			}
			return ret;
		}
	};

	/** A layout constraint encouraging sizes within the preferred range */
	public static final LayoutConstraint prefSize = new LayoutConstraint() {
	};

	/** A layout constraint causing components to stay within the container's bounds */
	public static final LayoutConstraint containerBounds = new LayoutConstraint() {
	};

	/**
	 * A layout constraint enforcing the {@link org.muis.core.style.LayoutStyles#margin} style. This constraint also enforces the same rules
	 * as {@link #containerBounds}.
	 */
	public static final LayoutConstraint margin = new LayoutConstraint() {
	};
}
