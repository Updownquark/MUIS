package org.quick.core.layout;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

import javax.swing.JFrame;

import org.qommons.Ternian;
import org.qommons.collect.BetterHashMap;
import org.qommons.collect.BetterMap;
import org.qommons.collect.CollectionElement;
import org.quick.core.Dimension;
import org.quick.core.Point;
import org.quick.util.DebugPlotter;

public class LayoutSolver<L> {
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

	public interface Box {
		Line getLeft();

		Line getRight();
		Line getTop();
		Line getBottom();

		default Point getTopLeft() {
			return new Point(getLeft().getPosition(), getTop().getPosition());
		}

		default int getWidth() {
			return getRight().getPosition() - getLeft().getPosition();
		}

		default int getHeight() {
			return getBottom().getPosition() - getTop().getPosition();
		}
	}

	public static final float MAX_TENSION = 10000;
	public static final float MAX_PREF_TENSION = 10;

	public static LayoutSpringEvaluator forSizer(IntSupplier crossSize, SizeGuide sizer) {
		return new LayoutSpringEvaluator() {
			@Override
			public int getSize(float tension) {
				int cs = crossSize.getAsInt();
				if (tension == 0)
					return sizer.getPreferred(cs, true);
				else if (tension < 0) {
					tension = -tension;
					if (tension == MAX_PREF_TENSION)
						return sizer.getMinPreferred(cs, true);
					else if (tension < MAX_PREF_TENSION) {
						int pref = sizer.getPreferred(cs, true);
						int minPref = sizer.getMinPreferred(cs, true);
						return Math.round(minPref + (pref - minPref) * tension / MAX_PREF_TENSION);
					} else if (tension >= MAX_TENSION)
						return sizer.getMin(cs, true);
					else {
						int minPref = sizer.getMinPreferred(cs, true);
						int min = sizer.getMin(cs, true);
						return Math.round(min + (minPref - min) * (tension - MAX_PREF_TENSION) / (MAX_TENSION - MAX_PREF_TENSION));
					}
				} else {
					if (tension == MAX_PREF_TENSION)
						return sizer.getMaxPreferred(cs, true);
					else if (tension < MAX_PREF_TENSION) {
						int pref = sizer.getPreferred(cs, true);
						int maxPref = sizer.getMaxPreferred(cs, true);
						return Math.round(pref + (maxPref - pref) * tension / MAX_PREF_TENSION);
					} else if (tension >= MAX_TENSION)
						return sizer.getMax(cs, true);
					else {
						int maxPref = sizer.getMaxPreferred(cs, true);
						int max = sizer.getMax(cs, true);
						return Math.round(maxPref + (max - maxPref) * (tension - MAX_PREF_TENSION) / (MAX_TENSION - MAX_PREF_TENSION));
					}
				}
			}

			@Override
			public TensionAndGradient getTension(int length) {
				int cs = crossSize.getAsInt();
				int pref = sizer.getPreferred(cs, true);
				if (length < pref) {
					int minPref = sizer.getMinPreferred(cs, true);
					if (length >= minPref) {
						float tension = -MAX_PREF_TENSION * (pref - length) / (pref - minPref);
						float gradient = (pref - minPref) / MAX_PREF_TENSION;
						return new TensionAndGradient(tension, gradient);
					} else {
						int min = sizer.getMin(cs, true);
						float gradient = (minPref - min) / (MAX_TENSION - MAX_PREF_TENSION);
						if (length <= min)
							return new TensionAndGradient(-MAX_TENSION, gradient);
						else {
							float tension = (MAX_TENSION - MAX_PREF_TENSION) * (length - min) / (minPref - min);
							return new TensionAndGradient(tension, gradient);
						}
					}
				} else {
					int maxPref = sizer.getMaxPreferred(cs, true);
					if (length <= maxPref) {
						float tension = MAX_PREF_TENSION * (length - pref) / (maxPref - pref);
						float gradient = (maxPref - pref) / MAX_PREF_TENSION;
						return new TensionAndGradient(tension, gradient);
					} else {
						int max = sizer.getMax(cs, true);
						float gradient = (max - maxPref) / (MAX_TENSION - MAX_PREF_TENSION);
						if (length >= max)
							return new TensionAndGradient(MAX_TENSION, gradient);
						else {
							float tension = (MAX_TENSION - MAX_PREF_TENSION) * (max - max) / (max - maxPref);
							return new TensionAndGradient(tension, gradient);
						}
					}
				}
			}
		};
	}

	private static final int MAX_TRIES = 4;

	private final BoxImpl theBounds = new BoxImpl(//
		new LineImpl(false, true), new LineImpl(false, true), //
			new LineImpl(true, true), new LineImpl(true, true)//
	);

	private final BetterMap<L, LineImpl> theLines;
	private final List<Collection<LineImpl>> theLineSets;
	private boolean isSealed;
	private int theIteration;
	private int theUnstableLines;

	public LayoutSolver() {
		theLines = BetterHashMap.build().unsafe().buildMap();
		theLineSets = new LinkedList<>();
	}

	public Box getBounds() {
		return theBounds;
	}

	public Box getOrCreateBox(L left, L right, L top, L bottom) {
		return new BoxImpl(//
			(LineImpl) getOrCreateLine(left, false), (LineImpl) getOrCreateLine(right, false), //
			(LineImpl) getOrCreateLine(top, true), (LineImpl) getOrCreateLine(right, true));
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
		theBounds.right.thePosition = size.width;
		theBounds.bottom.thePosition = size.height;
		List<SpringImpl> dirtySprings = new LinkedList<>();
		if (!isSealed) {
			isSealed = true;
			init(dirtySprings);
		}
		for (int i = 0; theUnstableLines > 0 && i < MAX_TRIES; i++)
			adjust(dirtySprings);
		return this;
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

	private class BoxImpl implements Box {
		final LineImpl left;
		final LineImpl right;
		final LineImpl top;
		final LineImpl bottom;

		BoxImpl(LineImpl left, LineImpl right, LineImpl top, LineImpl bottom) {
			this.left = left;
			this.right = right;
			this.top = top;
			this.bottom = bottom;
		}

		@Override
		public LineImpl getLeft() {
			return left;
		}

		@Override
		public LineImpl getRight() {
			return right;
		}

		@Override
		public LineImpl getTop() {
			return top;
		}

		@Override
		public LineImpl getBottom() {
			return bottom;
		}
	}

	public static void main(String[] args) {
		DebugPlotter plotter = new DebugPlotter();
		Map<Box, Line2D[]> boxes = new HashMap<>(); // Each entry is 4 lines--left, right, top, bottom

		// First, a simple system with 2 boxes
		LayoutSolver<String> solver = new LayoutSolver<>();
		Box box1 = solver.getOrCreateBox("b1 l", "b1 r", "b1 t", "b1 b");
		Box box2 = solver.getOrCreateBox("b2 l", "b2 r", "b2 t", "b2 b");
		addBox(plotter, boxes, solver.getBounds(), "bounds");
		addBox(plotter, boxes, box1, "box1");
		addBox(plotter, boxes, box2, "box2");
		//Margins
		Spring leftMargin = solver.createSpring(box1.getLeft(), solver.getBounds().getLeft(),
			forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, Integer.MAX_VALUE)));
		Spring rightMargin = solver.createSpring(solver.getBounds().getRight(), box2.getRight(),
			forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, Integer.MAX_VALUE)));
		Spring topMargin1 = solver.createSpring(solver.getBounds().getTop(), box1.getTop(),
			forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, Integer.MAX_VALUE)));
		Spring topMargin2 = solver.createSpring(solver.getBounds().getTop(), box2.getTop(),
			forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, Integer.MAX_VALUE)));
		Spring bottomMargin1 = solver.createSpring(box1.getBottom(), solver.getBounds().getBottom(),
			forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, Integer.MAX_VALUE)));
		Spring bottomMargin2 = solver.createSpring(box2.getBottom(), solver.getBounds().getBottom(),
			forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, Integer.MAX_VALUE)));
		// Between the boxes
		Spring padding = solver.createSpring(box1.getRight(), box2.getLeft(), //
			forSizer(() -> 0, new SimpleSizeGuide(0, 5, 10, 15, Integer.MAX_VALUE)));
		// Now the box dimensions
		Spring w1 = solver.createSpring(box1.getLeft(), box1.getRight(), //
			forSizer(() -> 0, new SimpleSizeGuide(10, 50, 100, 150, 500)));
		Spring h1 = solver.createSpring(box1.getTop(), box1.getBottom(), //
			forSizer(() -> 0, new SimpleSizeGuide(10, 40, 80, 125, 400)));
		Spring w2 = solver.createSpring(box2.getLeft(), box2.getRight(), //
			forSizer(() -> 0, new SimpleSizeGuide(10, 30, 75, 100, 300)));
		Spring h2 = solver.createSpring(box2.getTop(), box2.getBottom(), //
			forSizer(() -> 0, new SimpleSizeGuide(10, 40, 80, 125, 400)));

		JFrame plotterFrame = plotter.showFrame("Layout Solver", null);
		updateBox(boxes);
		// TODO Now show the debugger and call the solver using the debugger's size
		// Set breakpoints to watch it work
		// Need a hook or some way to update the shapes when the solver moves or recalculates anything
	}

	private static void addBox(DebugPlotter plotter, Map<Box, Line2D[]> boxes, Box box, String boxName) {
		Line2D[] lines = new Line2D[4];
		lines[0] = new Line2D.Float(0, 0, 0, 0);
		lines[1] = new Line2D.Float(0, 0, 0, 0);
		lines[2] = new Line2D.Float(0, 0, 0, 0);
		lines[3] = new Line2D.Float(0, 0, 0, 0);
		boxes.put(box, lines);
		plotter.add(lines[0]).setColor(Color.black).setText(boxName + " left");
		plotter.add(lines[1]).setColor(Color.black).setText(boxName + " right");
		plotter.add(lines[2]).setColor(Color.black).setText(boxName + " top");
		plotter.add(lines[3]).setColor(Color.black).setText(boxName + " bottom");
	}

	private static void updateBox(Map<Box, Line2D[]> boxes) {
		for (Map.Entry<Box, Line2D[]> box : boxes.entrySet()) {
			box.getValue()[0].setLine(//
				box.getKey().getLeft().getPosition(), box.getKey().getTop().getPosition(), box.getKey().getLeft().getPosition(),
				box.getKey().getBottom().getPosition());
			box.getValue()[1].setLine(//
				box.getKey().getRight().getPosition(), box.getKey().getTop().getPosition(), box.getKey().getRight().getPosition(),
				box.getKey().getBottom().getPosition());
			box.getValue()[2].setLine(//
				box.getKey().getLeft().getPosition(), box.getKey().getTop().getPosition(), box.getKey().getRight().getPosition(),
				box.getKey().getTop().getPosition());
			box.getValue()[3].setLine(//
				box.getKey().getLeft().getPosition(), box.getKey().getBottom().getPosition(), box.getKey().getRight().getPosition(),
				box.getKey().getBottom().getPosition());
		}
	}
}
