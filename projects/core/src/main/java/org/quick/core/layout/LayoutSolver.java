package org.quick.core.layout;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.qommons.Ternian;
import org.qommons.collect.BetterHashMap;
import org.qommons.collect.BetterMap;
import org.qommons.collect.CollectionElement;
import org.quick.core.Dimension;

public class LayoutSolver<L> implements LayoutSpringEvaluator {
	public interface Line {
		Orientation getOrientation();

		int getPosition();

		float getForce();
	}

	public interface Spring {
		Line getSource();

		Line getDest();

		float getTension();
	}

	private static final int MAX_TRIES = 4;

	private final LineImpl[][] theBounds = new LayoutSolver.LineImpl[][] { //
		new LayoutSolver.LineImpl[] { //
			new LineImpl(false, true), new LineImpl(false, true)//
		}, new LayoutSolver.LineImpl[] { //
			new LineImpl(true, true), new LineImpl(true, true)//
		} };

	private final BetterMap<L, LineImpl> theLines;
	private final List<Collection<LineImpl>> theLineSets;
	private boolean isSealed;
	private int theIteration;
	private int theUnstableLines;

	public LayoutSolver() {
		theLines = BetterHashMap.build().unsafe().buildMap();
		theLineSets = new LinkedList<>();
	}

	public Line getLeft() {
		return theBounds[0][0];
	}

	public Line getRight() {
		return theBounds[0][1];
	}

	public Line getTop() {
		return theBounds[1][0];
	}

	public Line getBottom() {
		return theBounds[1][1];
	}

	public Line getOrCreateLine(L lineObject, boolean vertical) {
		if (isSealed)
			throw new IllegalStateException("This solver cannot be altered");
		return theLines.computeIfAbsent(lineObject, lo -> new LineImpl(vertical, false));
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

	public LayoutSolver solve(Dimension size) {
		theBounds[0][1].thePosition = size.width;
		theBounds[1][1].thePosition = size.height;
		List<SpringImpl> dirtySprings = new LinkedList<>();
		if (!isSealed) {
			isSealed = true;
			init(dirtySprings);
		}
		for (int i = 0; theUnstableLines > 0 && i < MAX_TRIES; i++)
			adjust(dirtySprings);
		return this;
	}

	@Override
	public int getSize(float tension) {
		// TODO Auto-generated method stub
	}

	@Override
	public TensionAndGradient getTension(int length) {
		// TODO Auto-generated method stub
	}

	private void init(List<SpringImpl> dirtySprings) {
		theIteration++;
		theLineSets.clear();
		if (theLines.isEmpty())
			return;
		List<LineImpl> lines = new LinkedList<>();
		theLineSets.add(lines);
		CollectionElement<LineImpl> el = theLines.values().getTerminalElement(true);
		if (!el.get().isBorder)
			el.get().setPosition(0, theIteration);
		initConstraints(el.get(), Ternian.NONE, false, lines, dirtySprings); // By NONE, I mean both
		el = theLines.values().getAdjacentElement(el.getElementId(), true);
		while (el != null) {
			if (el.get().lastChecked != theIteration) {
				if (!el.get().isBorder)
					el.get().setPosition(0, theIteration);
				lines = new LinkedList<>();
				theLineSets.add(lines);
				initConstraints(el.get(), Ternian.NONE, false, lines, dirtySprings);
			}
			el = theLines.values().getAdjacentElement(el.getElementId(), true);
		}
		for (SpringImpl spring : dirtySprings)
			spring.recalculate();
		dirtySprings.clear();
		return;
	}

	private void initConstraints(LineImpl line, Ternian outgoing, boolean secondary, List<LineImpl> lines, List<SpringImpl> dirtySprings) {
		if (outgoing != Ternian.FALSE && line.theOutgoingSprings != null) {
			for (SpringImpl spring : line.theOutgoingSprings) {
				LineImpl dest = spring.theDest;
				if (dest.isBorder) {
					if (spring.recalculate())
						dirtySprings.add(spring);
				} else if (dest.lastChecked != theIteration) {
					lines.add(dest);
					if (dest.setPosition(line.thePosition + spring.theEvaluator.getSize(0), theIteration)) {
						initConstraints(dest, Ternian.TRUE, secondary, lines, dirtySprings);
					}
				} else
					dirtySprings.add(spring);
				if (!secondary && spring.theLinkedSprings != null) {
					for (SpringImpl linked : spring.theLinkedSprings) {
						if (linked.theSource.isBorder) {
						} else if (linked.theSource.lastChecked != theIteration) {
							lines.add(linked.theSource);
							linked.theSource.setPosition(0, theIteration);
							initConstraints(linked.theSource, Ternian.FALSE, true, lines, dirtySprings);
						}
						if (linked.theDest.isBorder) {
						} else if (linked.theDest.lastChecked != theIteration) {
							lines.add(linked.theDest);
							linked.theSource.setPosition(linked.theSource.thePosition + linked.theEvaluator.getSize(0), theIteration);
							initConstraints(linked.theDest, Ternian.TRUE, true, lines, dirtySprings);
						}
					}
				}
			}
		}
		if (outgoing != Ternian.TRUE && line.theIncomingSprings != null) {
			for (SpringImpl spring : line.theIncomingSprings) {
				LineImpl src = spring.theSource;
				if (src.isBorder) {
					if (spring.recalculate())
						dirtySprings.add(spring);
				} else if (src.lastChecked != theIteration) {
					lines.add(src);
					if (src.setPosition(line.thePosition - spring.theEvaluator.getSize(0), theIteration)) {
						initConstraints(src, Ternian.FALSE, secondary, lines, dirtySprings);
					}
				} else
					dirtySprings.add(spring);
				if (!secondary && spring.theLinkedSprings != null) {
					for (SpringImpl linked : spring.theLinkedSprings) {
						if (linked.theSource.isBorder) {
						} else if (linked.theSource.lastChecked != theIteration) {
							lines.add(linked.theSource);
							linked.theSource.setPosition(0, theIteration);
							initConstraints(linked.theSource, Ternian.FALSE, true, lines, dirtySprings);
						}
						if (linked.theDest.isBorder) {
						} else if (linked.theDest.lastChecked != theIteration) {
							lines.add(linked.theDest);
							linked.theSource.setPosition(linked.theSource.thePosition + linked.theEvaluator.getSize(0), theIteration);
							initConstraints(linked.theDest, Ternian.TRUE, true, lines, dirtySprings);
						}
					}
				}
			}
		}
	}

	private void adjust(List<SpringImpl> dirtySprings) {
		theIteration++;
		if (theLineSets.isEmpty())
			return;
		for (Collection<LineImpl> lineSet : theLineSets) {
			for (LineImpl line : lineSet) {
				if (line.isBorder || line.lastChecked == theIteration)
					continue;
				adjustLine(line, dirtySprings);
			}
		}
		for (SpringImpl spring : dirtySprings)
			spring.recalculate();
		dirtySprings.clear();
		return;
	}

	private void adjustLine(LineImpl line, List<SpringImpl> dirtySprings) {
		if (line.theForce == 0 || line.theOutgoingSprings == null) {
			line.lastChecked = theIteration;
			return;
		}
		float maxGradient = line.theForce > 0 ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
		int signum = (int) Math.signum(line.theForce);
		for (SpringImpl spring : line.theOutgoingSprings) {
			if (signum != (int) Math.signum(spring.theGradient))
				continue;
			if (signum > 0) {
				if (spring.theGradient > maxGradient)
					maxGradient = spring.theGradient;
			} else {
				if (spring.theGradient < maxGradient)
					maxGradient = spring.theGradient;
			}
		}
		int pixDiff = Math.round(Math.abs(maxGradient) * line.theForce);
		if (line.setPosition(line.thePosition + pixDiff, theIteration)) {
			if (line.theOutgoingSprings != null) {
				for (SpringImpl spring : line.theOutgoingSprings) {
					if (spring.theDest.lastChecked != theIteration)
						adjustLine(spring.theDest, dirtySprings);
					dirtySprings.add(spring);
				}
			}
		}
	}

	private class LineImpl implements Line {
		private final boolean isVertical;
		final boolean isBorder;
		private List<SpringImpl> theOutgoingSprings;
		private List<SpringImpl> theIncomingSprings;
		int thePosition;
		float theForce;
		int lastChecked;

		LineImpl(boolean vertical, boolean border) {
			isVertical = vertical;
			isBorder = border;
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

		void setForce(float force) {
			if (theForce == 0)
				theUnstableLines++;
			theForce = force;
			if (theForce == 0)
				theUnstableLines--;
		}
	}

	private class SpringImpl implements Spring {
		private final LineImpl theSource;
		private final LineImpl theDest;
		private final LayoutSpringEvaluator theEvaluator;
		private List<SpringImpl> theLinkedSprings;
		float theTension;
		float theGradient;

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
			LayoutSpringEvaluator.TensionAndGradient tag = theEvaluator.getTension(theDest.thePosition - theSource.thePosition);
			if (theTension == tag.tension)
				return false;
			float tensionDiff = tag.tension - theTension;
			theSource.setForce(theSource.theForce - tensionDiff);
			theDest.setForce(theDest.theForce + tensionDiff);
			theGradient = tag.gradient;
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
