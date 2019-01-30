package org.quick.core.layout;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.qommons.collect.BetterHashMap;
import org.qommons.collect.BetterMap;
import org.qommons.collect.CollectionElement;

public class LayoutSolver<L> {
	public interface Line {
		Orientation getOrientation();

		int getPosition();

		float getForce();
	}

	public interface LayoutSpringEvaluator {
		int getSize(float tension);

		float getTension(int length);
	}

	public interface Spring {
		Line getSource();

		Line getDest();

		float getTension();
	}

	private static final int MAX_TRIES = 3;

	private final BetterMap<L, LineImpl> theLines;
	private final List<Collection<LineImpl>> theLineSets;
	private boolean isSealed;
	private int theIteration;

	public LayoutSolver() {
		theLines = BetterHashMap.build().unsafe().buildMap();
		theLineSets = new LinkedList<>();
	}

	public LayoutSolver solve() {
		isSealed = true;
		List<SpringImpl> dirtySprings = new LinkedList<>();
		if (!init(dirtySprings)) {
			for (int i = 0; i < MAX_TRIES; i++)
				if (adjust(dirtySprings))
					break;
		}
		return this;
	}

	public Line getOrCreateLine(L lineObject, boolean vertical) {
		if (isSealed)
			throw new IllegalStateException("This solver cannot be altered");
		return theLines.computeIfAbsent(lineObject, lo -> new LineImpl(vertical));
	}

	public Line getLine(L lineObject) {
		return theLines.get(lineObject);
	}

	public Spring createSpring(Line src, Line dest, LayoutSpringEvaluator eval) {
		if (isSealed)
			throw new IllegalStateException("This solver cannot be altered");
		if (src.getOrientation() != dest.getOrientation())
			throw new IllegalArgumentException("Springs can only be created between lines of the same orientation");
		LineImpl l1 = (LineImpl) src, l2 = (LineImpl) dest;
		SpringImpl spring = new SpringImpl(l1, l2, eval);
		l1.constrain(spring, true);
		l2.constrain(spring, false);
		return spring;
	}

	public void linkSprings(Spring spring1, Spring spring2) {
		if (isSealed)
			throw new IllegalStateException("This solver cannot be altered");
		if (spring1.getSource().getOrientation() == spring2.getSource().getOrientation())
			throw new IllegalArgumentException("Linked springs must have different orientations");
		((SpringImpl) spring1).affects((SpringImpl) spring2);
	}

	private boolean init(List<SpringImpl> dirtySprings) {
		theIteration++;
		theLineSets.clear();
		if (theLines.isEmpty())
			return true;
		List<LineImpl> lines = new LinkedList<>();
		theLineSets.add(lines);
		CollectionElement<LineImpl> el = theLines.values().getTerminalElement(true);
		el.get().setPosition(0, theIteration);
		initConstraints(el.get(), lines, dirtySprings);
		el = theLines.values().getAdjacentElement(el.getElementId(), true);
		while (el != null) {
			if (el.get().lastChecked != theIteration) {
				el.get().setPosition(0, theIteration);
				lines = new LinkedList<>();
				theLineSets.add(lines);
				initConstraints(el.get(), lines, dirtySprings);
			}
			el = theLines.values().getAdjacentElement(el.getElementId(), true);
		}
		if (dirtySprings.isEmpty())
			return true;
		for (SpringImpl spring : dirtySprings)
			spring.recalculate();
		dirtySprings.clear();
		return false;
	}

	private void initConstraints(LineImpl line, List<LineImpl> lines, List<SpringImpl> dirtySprings) {
		if (line.theOutgoingSprings != null) {
			for (SpringImpl spring : line.theOutgoingSprings) {
				LineImpl dest = spring.theDest;
				if (dest.lastChecked != theIteration) {
					lines.add(dest);
					if (dest.setPosition(line.thePosition + spring.theEvaluator.getSize(0), theIteration)) {
						initConstraints(dest, lines, dirtySprings);
					}
				} else
					dirtySprings.add(spring);
				if (spring.theLinkedSprings != null) {
					for (SpringImpl linked : spring.theLinkedSprings) {
						// TODO What to do here?
					}
				}
			}
		}
		if (line.theIncomingSprings != null) {
			for (SpringImpl spring : line.theIncomingSprings) {
				LineImpl src = spring.theSource;
				if (src.lastChecked != theIteration) {
					lines.add(src);
					if (src.setPosition(line.thePosition - spring.theEvaluator.getSize(0), theIteration)) {
						initConstraints(src, lines, dirtySprings);
					}
				} else
					dirtySprings.add(spring);
				if (spring.theLinkedSprings != null) {
					for (SpringImpl linked : spring.theLinkedSprings) {
						// TODO What to do here?
					}
				}
			}
		}
	}

	private boolean adjust(List<SpringImpl> dirtySprings) {
		// TODO
	}

	private static class LineImpl implements Line {
		private final boolean isVertical;
		private List<SpringImpl> theOutgoingSprings;
		private List<SpringImpl> theIncomingSprings;
		int thePosition;
		float theForce;
		int lastChecked;

		LineImpl(boolean vertical) {
			isVertical = vertical;
		}

		void constrain(SpringImpl spring, boolean outgoing) {
			List<SpringImpl> springs;
			if (outgoing) {
				if (theOutgoingSprings == null)
					theOutgoingSprings = new LinkedList<>();
				springs = theOutgoingSprings;
			} else {
				if (theIncomingSprings == null)
					theIncomingSprings = new LinkedList<>();
				springs = theIncomingSprings;
			}
			springs.add(spring);
		}

		boolean setPosition(int position, int iteration) {
			lastChecked = iteration;
			if (thePosition == position)
				return false;
			thePosition = position;
			return true;
		}

		@Override
		public Orientation getOrientation() {
			return Orientation.of(isVertical);
		}

		@Override
		public int getPosition() {
			return thePosition;
		}

		@Override
		public float getForce() {
			return theForce;
		}
	}

	private static class SpringImpl implements Spring {
		private final LineImpl theSource;
		private final LineImpl theDest;
		private final LayoutSpringEvaluator theEvaluator;
		private List<SpringImpl> theLinkedSprings;
		float theTension;

		SpringImpl(LineImpl source, LineImpl dest, LayoutSpringEvaluator evaluator) {
			theSource = source;
			theDest = dest;
			theEvaluator = evaluator;
		}

		void affects(SpringImpl other) {
			if (theLinkedSprings == null)
				theLinkedSprings = new LinkedList<>();
			theLinkedSprings.add(other);
			if (other.theLinkedSprings == null)
				other.theLinkedSprings = new LinkedList<>();
			other.theLinkedSprings.add(this);
		}

		boolean recalculate() {
			float tension = theEvaluator.getTension(theDest.thePosition - theSource.thePosition);
			if (theTension == tension)
				return false;
			float tensionDiff = tension - theTension;
			theSource.theForce -= tensionDiff;
			theDest.theForce += tensionDiff;
			return true;
		}

		@Override
		public Line getSource() {
			return theSource;
		}

		@Override
		public Line getDest() {
			return theDest;
		}

		@Override
		public float getTension() {
			return theTension;
		}
	}
}
