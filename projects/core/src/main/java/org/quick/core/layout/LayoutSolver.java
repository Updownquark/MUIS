package org.quick.core.layout;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;

import javax.swing.JFrame;

import org.qommons.ArrayUtils;
import org.qommons.QommonsUtils;
import org.qommons.Ternian;
import org.qommons.collect.BetterHashMap;
import org.qommons.collect.BetterList;
import org.qommons.collect.BetterMap;
import org.qommons.collect.CollectionElement;
import org.qommons.collect.ElementId;
import org.qommons.tree.BetterTreeSet;
import org.qommons.tree.SortedTreeList;
import org.quick.core.Point;
import org.quick.core.layout.LayoutSpringEvaluator.TensionAndSnap;
import org.quick.util.DebugPlotter;

public class LayoutSolver<L> {
	public interface EdgeDef {
		Orientation getOrientation();
	}

	public interface EdgeState extends EdgeDef {
		int getPosition();

		float getForce();
	}

	public interface SpringDef {
		EdgeDef getSource();

		EdgeDef getDest();
	}

	public interface SpringState extends SpringDef {
		@Override
		EdgeState getSource();

		@Override
		EdgeState getDest();

		default int getLength() {
			return getDest().getPosition() - getSource().getPosition();
		}

		float getTension();
	}

	public interface BoxDef {
		EdgeDef getLeft();

		EdgeDef getRight();

		EdgeDef getTop();

		EdgeDef getBottom();

		default EdgeDef getEdge(Orientation orientation, End end) {
			switch (orientation) {
			case horizontal:
				return end == End.leading ? getLeft() : getRight();
			default:
				return end == End.leading ? getTop() : getBottom();
			}
		}
	}

	public interface BoxState extends BoxDef {
		@Override
		EdgeState getLeft();

		@Override
		EdgeState getRight();

		@Override
		EdgeState getTop();

		@Override
		EdgeState getBottom();

		@Override
		default EdgeState getEdge(Orientation orientation, End end) {
			return (EdgeState) BoxDef.super.getEdge(orientation, end);
		}

		default Point getTopLeft() {
			return new Point(getLeft().getPosition(), getTop().getPosition());
		}

		default Point getCenter() {
			return new Point((getLeft().getPosition() + getRight().getPosition()) / 2, //
				(getTop().getPosition() + getBottom().getPosition()) / 2);
		}

		default int getWidth() {
			return getRight().getPosition() - getLeft().getPosition();
		}

		default int getHeight() {
			return getBottom().getPosition() - getTop().getPosition();
		}
	}

	public interface State<L> {
		LayoutSolver<L> getSolver();

		BoxState getBounds();

		EdgeState getEdge(L lineObj);

		EdgeState getEdge(EdgeDef line);

		SpringState getSpring(SpringDef spring);

		State<L> reset();

		State<L> layout(int width, int height);

		State<L> stretch(float hTension, float vTension);

		BoxState getBox(BoxDef box);

		boolean isEdgeMoving(EdgeState edge);

		int getWaveNumber();
	}

	public static final float SUPER_TENSION = 1_000_000;
	public static final float MAX_TENSION = 1_000;
	public static final float MAX_PREF_TENSION = 1;
	public static final int MAX_LAYOUT_SIZE = 1_000_000;

	public static LayoutSpringEvaluator forSizer(IntSupplier crossSize, SizeGuide sizer) {
		return new LayoutSpringEvaluator() {
			@Override
			public int getSize(float tension) {
				int cs = crossSize.getAsInt();
				if (tension == 0)
					return cap(sizer.getPreferred(cs, true));
				else if (tension < 0) {
					tension = -tension;
					if (tension == MAX_PREF_TENSION)
						return cap(sizer.getMinPreferred(cs, true));
					else if (tension < MAX_PREF_TENSION) {
						int pref = cap(sizer.getPreferred(cs, true));
						int minPref = cap(sizer.getMinPreferred(cs, true));
						return Math.round(minPref + (pref - minPref) * tension / MAX_PREF_TENSION);
					} else if (tension >= MAX_TENSION)
						return cap(sizer.getMin(cs, true));
					else {
						int minPref = cap(sizer.getMinPreferred(cs, true));
						int min = cap(sizer.getMin(cs, true));
						return Math.round(min + (minPref - min) * (tension - MAX_PREF_TENSION) / (MAX_TENSION - MAX_PREF_TENSION));
					}
				} else {
					if (tension == MAX_PREF_TENSION)
						return cap(sizer.getMaxPreferred(cs, true));
					else if (tension < MAX_PREF_TENSION) {
						int pref = cap(sizer.getPreferred(cs, true));
						int maxPref = cap(sizer.getMaxPreferred(cs, true));
						return Math.round(pref + (maxPref - pref) * tension / MAX_PREF_TENSION);
					} else if (tension >= MAX_TENSION)
						return cap(sizer.getMax(cs, true));
					else {
						int maxPref = cap(sizer.getMaxPreferred(cs, true));
						int max = cap(sizer.getMax(cs, true));
						return Math.round(maxPref + (max - maxPref) * (tension - MAX_PREF_TENSION) / (MAX_TENSION - MAX_PREF_TENSION));
					}
				}
			}

			@Override
			public TensionAndSnap getTension(int length) {
				length = cap(length);
				int cs = crossSize.getAsInt();
				int pref = cap(sizer.getPreferred(cs, true));
				if (length < pref) {
					int minPref = cap(sizer.getMinPreferred(cs, true));
					if (length >= minPref) {
						float tension = MAX_PREF_TENSION * (pref - length) / (pref - minPref);
						return new TensionAndSnap(tension, pref);
					} else {
						int min = cap(sizer.getMin(cs, true));
						if (length == min)
							return new TensionAndSnap(MAX_TENSION, minPref);
						else if (length < min) {
							if (length < 0)
								return new TensionAndSnap(SUPER_TENSION, min);
							else {
								float tension = MAX_TENSION + (SUPER_TENSION - MAX_TENSION) * (min - length) / min;
								return new TensionAndSnap(tension, min);
							}
						} else {
							float tension = MAX_PREF_TENSION + (MAX_TENSION - MAX_PREF_TENSION) * (minPref - length) / (minPref - min);
							return new TensionAndSnap(tension, minPref);
						}
					}
				} else if (length == pref) {
					return new TensionAndSnap(0, pref);
				} else {
					int maxPref = cap(sizer.getMaxPreferred(cs, true));
					if (length <= maxPref) {
						float tension = -MAX_PREF_TENSION * (length - pref) / (maxPref - pref);
						return new TensionAndSnap(tension, pref);
					} else {
						int max = cap(sizer.getMax(cs, true));
						if (length == max)
							return new TensionAndSnap(-MAX_TENSION, maxPref);
						else if (length > max) {
							float tension = -MAX_TENSION - (SUPER_TENSION - MAX_TENSION) * (length - max) / (MAX_LAYOUT_SIZE - max);
							return new TensionAndSnap(tension, max);
						} else {
							float tension = -MAX_PREF_TENSION - (MAX_TENSION - MAX_PREF_TENSION) * (length - maxPref) / (max - maxPref);
							return new TensionAndSnap(tension, maxPref);
						}
					}
				}
			}

			private int cap(int size) {
				if (size > MAX_LAYOUT_SIZE)
					return MAX_LAYOUT_SIZE;
				else
					return size;
			}
		};
	}

	private static final LayoutSpringEvaluator BOUNDS_TENSION = new LayoutSpringEvaluator() {
		@Override
		public int getSize(float tension) {
			throw new IllegalStateException("Placeholder! Should not actually be called");
		}

		@Override
		public TensionAndSnap getTension(int length) {
			throw new IllegalStateException("Placeholder! Should not actually be called");
		}
	};

	private static final int MAX_TRIES = 100;

	private final BoxImpl theBounds;

	private final BetterMap<L, EdgeImpl> theEdges;
	private int theSpringCount;
	private boolean isSealed;

	public LayoutSolver() {
		theEdges = BetterHashMap.build().unsafe().buildMap();
		theBounds = new BoxImpl(//
			new EdgeImpl((L) "Left Bound", 0, false, End.leading), new EdgeImpl((L) "Right Bound", 1, false, End.trailing), //
			new EdgeImpl((L) "Top Bound", 2, true, End.leading), new EdgeImpl((L) "Bottom Bound", 3, true, End.trailing)//
		);
		SpringImpl hTension = new SpringImpl(theBounds.left, theBounds.right, BOUNDS_TENSION);
		SpringImpl vTension = new SpringImpl(theBounds.top, theBounds.bottom, BOUNDS_TENSION);
		theBounds.left.constrain(hTension, true);
		theBounds.right.constrain(hTension, false);
		theBounds.top.constrain(vTension, true);
		theBounds.bottom.constrain(vTension, false);
	}

	public BoxDef getBounds() {
		return theBounds;
	}

	public BoxDef getOrCreateBox(L left, L right, L top, L bottom) {
		return new BoxImpl(//
			(EdgeImpl) getOrCreateEdge(left, false), (EdgeImpl) getOrCreateEdge(right, false), //
			(EdgeImpl) getOrCreateEdge(top, true), (EdgeImpl) getOrCreateEdge(bottom, true));
	}

	public EdgeDef getOrCreateEdge(L edgeObject, boolean vertical) {
		if (isSealed)
			throw new IllegalStateException("This solver cannot be altered");
		return theEdges.computeIfAbsent(edgeObject, lo -> new EdgeImpl(edgeObject, theEdges.size() + 4, vertical, null));
	}

	public EdgeDef getEdge(L edgeObject) {
		return theEdges.get(edgeObject);
	}

	public SpringDef createSpring(EdgeDef src, EdgeDef dest, LayoutSpringEvaluator eval) {
		if (isSealed)
			throw new IllegalStateException("This solver cannot be altered");
		if (src.getOrientation() != dest.getOrientation())
			throw new IllegalArgumentException("Springs can only be created between lines of the same orientation");
		EdgeImpl l1 = (EdgeImpl) src, l2 = (EdgeImpl) dest;
		SpringImpl spring = new SpringImpl(l1, l2, eval);
		l1.constrain(spring, true);
		l2.constrain(spring, false);
		return spring;
	}

	public void linkSprings(SpringDef spring1, SpringDef spring2) {
		if (isSealed)
			throw new IllegalStateException("This solver cannot be altered");
		if (spring1.getSource().getOrientation() == spring2.getSource().getOrientation())
			throw new IllegalArgumentException("Linked springs must have different orientations");
		((SpringImpl) spring1).affects((SpringImpl) spring2);
	}

	public State<L> use() {
		isSealed = true;
		return new LayoutStateImpl();
	}

	private class EdgeImpl implements EdgeDef {
		final int id;
		private final L theValue;
		final boolean isVertical;
		final End borderEnd;
		private final List<SpringImpl> theOutgoingSprings;
		private final List<SpringImpl> theIncomingSprings;

		EdgeImpl(L value, int id, boolean vertical, End borderEnd) {
			theValue = value;
			this.id = id;
			isVertical = vertical;
			this.borderEnd = borderEnd;
			theOutgoingSprings = new LinkedList<>();
			theIncomingSprings = new LinkedList<>();
		}

		void constrain(SpringImpl spring, boolean outgoing) {
			if (borderEnd != null) {
				if (borderEnd == End.leading && !outgoing)
					throw new IllegalArgumentException("A leading border of the container may not be a destination spring endpoint");
				if (borderEnd == End.trailing && outgoing)
					throw new IllegalArgumentException("A trailing border of the container may not be a source spring endpoint");
			}
			(outgoing ? theOutgoingSprings : theIncomingSprings).add(spring);
		}

		@Override
		public Orientation getOrientation() {
			return Orientation.of(isVertical);
		}

		boolean isMobile(boolean stretchOp) {
			if (borderEnd == null)
				return true;
			else
				return stretchOp && borderEnd == End.trailing; // For stretch operations, the right and bottom edges can move
		}

		@Override
		public String toString() {
			return String.valueOf(theValue);
		}
	}

	private class SpringImpl implements SpringDef {
		final int id;
		private final EdgeImpl theSource;
		private final EdgeImpl theDest;
		final LayoutSpringEvaluator theEvaluator;
		List<SpringImpl> theLinkedSprings;

		SpringImpl(EdgeImpl source, EdgeImpl dest, LayoutSpringEvaluator evaluator) {
			id = theSpringCount;
			theSpringCount++;
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

		@Override
		public EdgeDef getSource() {
			return theSource;
		}

		@Override
		public EdgeDef getDest() {
			return theDest;
		}

		@Override
		public String toString() {
			return theSource.theValue + "->" + theDest.theValue;
		}
	}

	private class BoxImpl implements BoxDef {
		final EdgeImpl left;
		final EdgeImpl right;
		final EdgeImpl top;
		final EdgeImpl bottom;

		BoxImpl(EdgeImpl left, EdgeImpl right, EdgeImpl top, EdgeImpl bottom) {
			this.left = left;
			this.right = right;
			this.top = top;
			this.bottom = bottom;
		}

		@Override
		public EdgeImpl getLeft() {
			return left;
		}

		@Override
		public EdgeImpl getRight() {
			return right;
		}

		@Override
		public EdgeImpl getTop() {
			return top;
		}

		@Override
		public EdgeImpl getBottom() {
			return bottom;
		}
	}

	private static boolean DEBUG = true;
	private static boolean PRINT_SPEED = true;
	private static final int LINKED_SPRING_SLACK_FACTOR = 10;
	private static final int REGULAR_SLACK_FACTOR = 1;

	private static final Comparator<LayoutSolver<?>.EdgeStateImpl> POSITION_COMPARE = new Comparator<LayoutSolver<?>.EdgeStateImpl>() {
		@Override
		public int compare(LayoutSolver<?>.EdgeStateImpl div1, LayoutSolver<?>.EdgeStateImpl div2) {
			End end = div1.theEdge.borderEnd;
			if (end != null) {
				if (end == End.leading)
					return -1;
				else
					return 1;
			} else
				return div1.thePosition - div2.thePosition;
		}
	};

	private class LayoutStateImpl implements State<L> {
		private final IdentityHashMap<EdgeImpl, EdgeStateImpl> theEdgeStates;
		private final IdentityHashMap<SpringImpl, SpringStateImpl> theSpringStates;
		private final BetterList<EdgeStateImpl> theHEdgesByPosition;
		private final BetterList<EdgeStateImpl> theVEdgesByPosition;
		private final BetterList<EdgeStateImpl> theEdgesByForce;
		private final List<SpringStateImpl> theSpringStatesById = new ArrayList<>(theSpringCount);

		private final BoxStateImpl theBoundState;
		private boolean isStretching;
		private float theHTension;
		private float theVTension;

		private boolean isInitialized;
		private int theSlackenedSpringCount;
		private int[] theMovingEdges;
		private int theWaveNumber;

		LayoutStateImpl() {
			theEdgeStates = new IdentityHashMap<>();
			theSpringStates = new IdentityHashMap<>();
			BetterList<SpringStateImpl> springsByIndex = new BetterTreeSet<>(false, (s1, s2) -> s1.theSpring.id - s2.theSpring.id);
			theBoundState = new BoxStateImpl(this, //
				new EdgeStateImpl(this, theBounds.left, springsByIndex), new EdgeStateImpl(this, theBounds.right, springsByIndex), //
				new EdgeStateImpl(this, theBounds.top, springsByIndex), new EdgeStateImpl(this, theBounds.bottom, springsByIndex));
			for (EdgeImpl edge : theEdges.values())
				theEdgeStates.put(edge, new EdgeStateImpl(this, edge, springsByIndex));
			theBoundState.left.theIncoming.fillIncoming();
			theBoundState.right.theIncoming.fillIncoming();
			theBoundState.top.theIncoming.fillIncoming();
			theBoundState.bottom.theIncoming.fillIncoming();
			for (EdgeStateImpl line : theEdgeStates.values())
				line.theIncoming.fillIncoming();
			theEdgesByForce = new SortedTreeList<>(false,
				(edge1, edge2) -> -Float.compare(Math.abs(edge1.theTotalForce), Math.abs(edge2.theTotalForce)));
			theHEdgesByPosition = new SortedTreeList<>(false, POSITION_COMPARE);
			theVEdgesByPosition = new SortedTreeList<>(false, POSITION_COMPARE);
			theSpringStatesById.addAll(springsByIndex);
		}

		@Override
		public LayoutSolver<L> getSolver() {
			return LayoutSolver.this;
		}

		@Override
		public BoxState getBounds() {
			return theBoundState;
		}

		@Override
		public EdgeState getEdge(L edgeObj) {
			EdgeImpl divider = theEdges.get(edgeObj);
			return divider == null ? null : theEdgeStates.get(divider);
		}

		@Override
		public EdgeState getEdge(EdgeDef line) {
			return theEdgeStates.get(line);
		}

		@Override
		public SpringState getSpring(SpringDef spring) {
			return theSpringStates.get(spring);
		}

		@Override
		public BoxState getBox(BoxDef box) {
			return new BoxStateImpl(this, //
				theEdgeStates.get(((BoxImpl) box).left), theEdgeStates.get(((BoxImpl) box).right), //
				theEdgeStates.get(((BoxImpl) box).top), theEdgeStates.get(((BoxImpl) box).bottom));
		}

		@Override
		public boolean isEdgeMoving(EdgeState edge) {
			int[] moving = theMovingEdges;
			if (moving == null || moving.length == 0)
				return false;
			return Arrays.binarySearch(moving, ((EdgeStateImpl) edge).theEdge.id) >= 0;
		}

		@Override
		public int getWaveNumber() {
			return theWaveNumber;
		}

		@Override
		public State<L> reset() {
			isInitialized = false;
			theBoundState.left.reset();
			theBoundState.right.reset();
			theBoundState.top.reset();
			theBoundState.bottom.reset();
			for (EdgeStateImpl edge : theEdgeStates.values())
				edge.reset();
			theEdgesByForce.clear();
			theHEdgesByPosition.clear();
			theVEdgesByPosition.clear();
			return this;
		}

		@Override
		public State<L> layout(int width, int height) {
			theBoundState.right.setPosition(width);
			theBoundState.bottom.setPosition(height);
			theHTension = 0;
			theVTension = 0;
			return adjustViaWave(false);
		}

		@Override
		public State<L> stretch(float hTension, float vTension) {
			// Add a left->right and a top->bottom constraint on the bounds rectangle with the given tensions
			// The stretch tension needs to oppose the sum of all the external springs attached to the bounds
			theHTension = hTension * (theBounds.left.theOutgoingSprings.size() - 1 + theBounds.right.theIncomingSprings.size() - 1) / 2;
			theVTension = vTension * (theBounds.top.theOutgoingSprings.size() - 1 + theBounds.bottom.theIncomingSprings.size() - 1) / 2;
			// Then perform the normal layout operation with the additional degree of freedom
			// that the right and bottom bounds can be adjusted
			return adjustViaWave(true);
		}

		// This method adjusts the layout by moving high-priority (absolute force) adjacent sets of edges
		// in the direction of their net force to achieve equilibrium until all edges are in their optimum position.
		// Empirically, this seems to work well for simple layouts but not when there are any linked springs.
		private State<L> adjustViaMultiMove(boolean stretch) {
			if (!isInitialized) {
				isStretching = stretch;
				isInitialized = true;
				long start = DEBUG ? System.currentTimeMillis() : 0;
				init();
				if (DEBUG)
					System.out.println("Initialized in " + QommonsUtils.printTimeLength(System.currentTimeMillis() - start));
			} else if (isStretching != stretch) {
				isStretching = stretch;
				theBoundState.right.forceChanged(true);
				theBoundState.bottom.forceChanged(true);
			}
			long start = PRINT_SPEED ? System.currentTimeMillis() : 0;
			int maxMoves = MAX_TRIES * theEdges.size();
			EdgeStateImpl toMove = theEdgesByForce.peekFirst();
			while (toMove == null && theSlackenedSpringCount > 0) {
				for (SpringStateImpl spring : theSpringStatesById)
					spring.pullInSlack();
				toMove = theEdgesByForce.peekFirst();
			}
			int moves;
			for (moves = 0; toMove != null && moves < maxMoves; moves++) {
				// Rather than move just one line at a time, we can try to move a larger set of adjacent lines
				// This is much more efficient in general.
				boolean signum = toMove.theTotalForce > 0;
				BetterList<EdgeStateImpl> linesByPosition = toMove.theEdge.isVertical ? theVEdgesByPosition : theHEdgesByPosition;
				// Find the min line to move
				ElementId minBound = toMove.thePositionSortedElement;
				CollectionElement<EdgeStateImpl> el = linesByPosition.getAdjacentElement(toMove.thePositionSortedElement, false);
				while (el != null) {
					End borderEnd = el.get().theEdge.borderEnd;
					if (borderEnd == null || (stretch && borderEnd == End.trailing)) { // Exclude bounds if they are not flexible
						float force = el.get().theTotalForce;
						if (Math.abs(force) < MAX_PREF_TENSION || (force > 0) == signum)
							minBound = el.getElementId();
						else
							break;
					}
					el = linesByPosition.getAdjacentElement(el.getElementId(), false);
				}
				// Find the max line to move
				ElementId maxBound = toMove.thePositionSortedElement;
				el = linesByPosition.getAdjacentElement(toMove.thePositionSortedElement, true);
				while (el != null) {
					End borderEnd = el.get().theEdge.borderEnd;
					if (borderEnd == null || (stretch && borderEnd == End.trailing)) { // Exclude bounds if they are not flexible
						float force = el.get().theTotalForce;
						if (Math.abs(force) < MAX_PREF_TENSION || (force > 0) == signum)
							maxBound = el.getElementId();
						else
							break;
					}
					el = linesByPosition.getAdjacentElement(el.getElementId(), true);
				}
				// Try all possible combinations of (min..toMove)x(toMove..max), largest combinations first
				boolean moved = false;
				outer: for (ElementId min = minBound; min != null && min.compareTo(toMove.thePositionSortedElement) <= 0; //
					min = CollectionElement.getElementId(linesByPosition.getAdjacentElement(min, true))) {
					for (ElementId max = maxBound; max != null && max.compareTo(toMove.thePositionSortedElement) >= 0; //
						max = CollectionElement.getElementId(linesByPosition.getAdjacentElement(max, false))) {
						theMovingEdges = null;
						if (adjustEdgeSet(toMove.theEdge.isVertical, min, max, stretch, null)) {
							moved = true;
							break outer;
						}
					}
				}
				if (!moved)
					toMove.stabilized();
				theMovingEdges = null;
				toMove = theEdgesByForce.peekFirst();
				while (toMove == null && theSlackenedSpringCount > 0) {
					for (SpringStateImpl spring : theSpringStatesById) {
						if (spring.pullInSlack()) {
							spring.recalculate(stretch);
						}
					}
					toMove = theEdgesByForce.peekFirst();
				}
			}
			if (PRINT_SPEED)
				System.out.println(moves + " moves in " + QommonsUtils.printTimeLength(System.currentTimeMillis() - start));
			return this;
		}

		private State<L> adjustViaWave(boolean stretch) {
			if (!isInitialized) {
				isStretching = stretch;
				isInitialized = true;
				long start = DEBUG ? System.currentTimeMillis() : 0;
				init();
				if (DEBUG)
					System.out.println("Initialized in " + QommonsUtils.printTimeLength(System.currentTimeMillis() - start));
			} else if (isStretching != stretch) {
				isStretching = stretch;
				theBoundState.right.forceChanged(true);
				theBoundState.bottom.forceChanged(true);
			}
			long start = PRINT_SPEED ? System.currentTimeMillis() : 0;
			int maxWaves = MAX_TRIES * theEdges.size();
			EdgeStateImpl waveSource = theEdgesByForce.peekFirst();
			while (waveSource == null && theSlackenedSpringCount > 0) {
				for (SpringStateImpl spring : theSpringStatesById)
					spring.pullInSlack();
				waveSource = theEdgesByForce.peekFirst();
			}
			int waves;
			int moves = 0;
			WaveSpringQueue springsToVisit = new WaveSpringQueue(theSpringCount);
			for (waves = 0; waveSource != null && waves < maxWaves; waves++) {
				if (propagateWave(waveSource, true, true, springsToVisit, stretch)) {
					// Pop all springs from the queue.
					IndexAndDirection propagation = springsToVisit.pop();
					while (propagation != null) {
						SpringStateImpl spring = theSpringStatesById.get(propagation.index);
						if (propagation.incoming && propagateWave(spring.theSource.theEdge, true, false, springsToVisit, stretch))
							moves++;
						if (propagation.outgoing && propagateWave(spring.theDest.theEdge, false, true, springsToVisit, stretch))
							moves++;
						propagation = springsToVisit.pop();
					}
					springsToVisit.clearContent();
				} else
					waveSource.stabilized();
				waveSource = theEdgesByForce.peekFirst();
				while (waveSource == null && theSlackenedSpringCount > 0) {
					for (SpringStateImpl spring : theSpringStatesById) {
						if (spring.pullInSlack()) {
							spring.recalculate(stretch);
						}
					}
					waveSource = theEdgesByForce.peekFirst();
				}
			}
			if (PRINT_SPEED)
				System.out
					.println(waves + " waves (" + moves + " moves, " + theSlackenedSpringCount + " slackened springs) in "
						+ QommonsUtils.printTimeLength(System.currentTimeMillis() - start));
			return this;
		}

		// Can't be static because it refers to fields in this (non-static) class
		private final Comparator<IndexAndDirection> BY_TARGET_FORCE = new Comparator<IndexAndDirection>() {
			@Override
			public int compare(IndexAndDirection i1, IndexAndDirection i2) {
				SpringStateImpl spring1 = theSpringStatesById.get(i1.index);
				SpringStateImpl spring2 = theSpringStatesById.get(i2.index);
				float maxForce1, maxForce2;
				if (i1.outgoing) {
					maxForce1 = Math.abs(spring1.theDest.theEdge.theTotalForce);
					if (i1.incoming)
						maxForce1 = Math.max(Math.abs(spring1.theSource.theEdge.theTotalForce), maxForce1);
				} else
					maxForce1 = Math.abs(spring1.theSource.theEdge.theTotalForce);
				if (i2.outgoing) {
					maxForce2 = Math.abs(spring2.theDest.theEdge.theTotalForce);
					if (i2.incoming)
						maxForce2 = Math.max(Math.abs(spring2.theSource.theEdge.theTotalForce), maxForce1);
				} else
					maxForce2 = Math.abs(spring2.theSource.theEdge.theTotalForce);
				return -Float.compare(maxForce1, maxForce2); // Largest absolute net force first
			}
		};

		// Inner variable so I don't have to re-create it all the time
		private final List<IndexAndDirection> propagationTargets = new ArrayList<>();

		private boolean propagateWave(EdgeStateImpl edge, boolean incoming, boolean outgoing, WaveSpringQueue waveQueue, boolean stretch) {
			if (!edge.theEdge.isMobile(stretch) || edge.theTotalForce == 0)
				return false;
			boolean moved = adjustEdgeSet(edge.theEdge.isVertical, edge.thePositionSortedElement, edge.thePositionSortedElement, stretch,
				idx -> waveQueue.hasContained(idx));
			if (moved) {
				// Add all of the edge's springs that have NOT yet been visited to the springsToVisit with the according direction
				// Add them to a temporary list first, then sort (prioritize) them by target net force before adding to the wave queue
				if(incoming){
					for(SpringStateImpl spring : edge.theIncoming.theSprings){
						if (waveQueue.reserve(spring.theSpring.id)) {
							propagationTargets.add(new IndexAndDirection(spring.theSpring.id, true, false));
							// For springs with linked springs, add those too with both directions
							for (SpringStateImpl linked : spring.theLinkedSprings) {
								if (waveQueue.reserve(linked.theSpring.id))
									propagationTargets.add(new IndexAndDirection(linked.theSpring.id, true, true));
							}
						}
					}

				}
				if(outgoing){
					for(SpringStateImpl spring : edge.theOutgoing.theSprings){
						if (waveQueue.reserve(spring.theSpring.id)) {
							propagationTargets.add(new IndexAndDirection(spring.theSpring.id, false, true));
							// For springs with linked springs, add those too with both directions
							for (SpringStateImpl linked : spring.theLinkedSprings) {
								if (waveQueue.reserve(linked.theSpring.id))
									propagationTargets.add(new IndexAndDirection(linked.theSpring.id, true, true));
							}
						}
					}
				}
				// Prioritize by target net force
				Collections.sort(propagationTargets, BY_TARGET_FORCE);
				for (IndexAndDirection prop : propagationTargets)
					waveQueue.push(prop.index, prop.incoming, prop.outgoing);
				propagationTargets.clear();
			}
			theMovingEdges = null;
			return moved;
		}

		private void init() {
			List<SpringStateImpl> dirtySprings = new LinkedList<>();
			if (theEdges.isEmpty())
				return;
			theBoundState.left.setPosition(0);
			theBoundState.top.setPosition(0);
			if (theBounds.left.theOutgoingSprings != null)
				initConstraints(theBoundState.left, Ternian.TRUE, dirtySprings);
			if (theBounds.top.theOutgoingSprings != null)
				initConstraints(theBoundState.top, Ternian.TRUE, dirtySprings);
			for (EdgeStateImpl line : theEdgeStates.values()) {
				if (!line.isPositioned()) {
					line.setPosition(0);
					initConstraints(line, Ternian.NONE, dirtySprings);
				}
			}
			if (theBounds.right.theIncomingSprings != null)
				initConstraints(theBoundState.right, Ternian.FALSE, dirtySprings);
			if (theBounds.bottom.theIncomingSprings != null)
				initConstraints(theBoundState.bottom, Ternian.FALSE, dirtySprings);
			boolean stretching = theHTension != 0 || theVTension != 0;
			for (SpringStateImpl spring : dirtySprings)
				spring.recalculate(stretching);
			return;
		}

		private void initConstraints(EdgeStateImpl line, Ternian outgoing, List<SpringStateImpl> dirtySprings) {
			if (outgoing != Ternian.FALSE) {
				for (SpringStateImpl spring : line.theOutgoing.theSprings) {
					LayoutSpringEvaluator eval = spring.theSpring.theEvaluator;
					EdgeStateImpl dest = spring.theDest.theEdge;
					if (eval != BOUNDS_TENSION && !dest.isPositioned()) {
						dest.setPosition(line.thePosition + eval.getSize(0));
						initConstraints(dest, Ternian.TRUE, dirtySprings);
					} else
						dirtySprings.add(spring);
				}
			}
			if (outgoing != Ternian.TRUE) {
				for (SpringStateImpl spring : line.theIncoming.theSprings) {
					LayoutSpringEvaluator eval = spring.theSpring.theEvaluator;
					EdgeStateImpl src = spring.theSource.theEdge;
					if (eval != BOUNDS_TENSION && !src.isPositioned()) {
						src.setPosition(line.thePosition - eval.getSize(0));
						initConstraints(src, Ternian.FALSE, dirtySprings);
					} else
						dirtySprings.add(spring);
				}
			}
		}

		// These are basically local variables that I don't want to waste performance initializing repeatedly
		// This state class is NOT thread-safe, so this should be fine within good usage as long as I clear them after usage
		private final List<AdjustingSpring> incomingSprings = new ArrayList<>();
		private final List<AdjustingSpring> outgoingSprings = new ArrayList<>();

		private int lastCalculatedForces;
		private static final int MAX_BOUND_VALUE = 1000000;

		private class AdjustingSpring {
			final SpringStateImpl spring;
			final boolean isOutgoing;
			final int anchorOffset;
			TensionAndSnap tension;
			final List<LinkedSpringAdjustment> linkedSpringAdjustments;

			AdjustingSpring(SpringStateImpl spring, boolean outgoing, int initPosition) {
				this.spring = spring;
				isOutgoing = outgoing;
				anchorOffset = (outgoing ? spring.theSource : spring.theDest).theEdge.thePosition - initPosition;
				tension = spring.theTension;
				linkedSpringAdjustments = new ArrayList<>(spring.theLinkedSprings.size());
				for (SpringStateImpl linked : spring.theLinkedSprings)
					linkedSpringAdjustments.add(new LinkedSpringAdjustment(linked));
			}

			float recalculate(int systemPosition) {
				int anchorPosition = systemPosition + anchorOffset;
				if (!linkedSpringAdjustments.isEmpty()) {
					// This here is a bit of a hack.
					// The implementations behind linked springs inspect the position state of the cross edges,
					// so the only way to communicate the test case to the linked spring is to actually do the adjustment temporarily
					int oldPos;
					if (isOutgoing) {
						oldPos = spring.theSource.theEdge.thePosition;
						spring.theSource.theEdge.thePosition = anchorPosition;
					} else {
						oldPos = spring.theDest.theEdge.thePosition;
						spring.theDest.theEdge.thePosition = anchorPosition;
					}
					try {
						for (LinkedSpringAdjustment linked : linkedSpringAdjustments)
							linked.adjust();
					} finally {
						if (isOutgoing)
							spring.theSource.theEdge.thePosition = oldPos;
						else
							spring.theDest.theEdge.thePosition = oldPos;
					}
				}
				LayoutSpringEvaluator eval = spring.theSpring.theEvaluator;
				TensionAndSnap newTension;
				if (eval == BOUNDS_TENSION) {
					float tensionF = spring.getSource().getOrientation().isVertical() ? theVTension : theHTension;
					newTension = new TensionAndSnap(tensionF, tensionF > 0 ? 10000000 : 0);
				} else if (isOutgoing)
					newTension = eval.getTension(spring.theDest.theEdge.thePosition - anchorPosition);
				else
					newTension = eval.getTension(anchorPosition - spring.theSource.theEdge.thePosition);
				tension = newTension;
				return spring.slacken(newTension.tension);
			}

			void updateForce(boolean stretching) {
				spring.theTension = tension;
				spring.pullInSlack();
				for (LinkedSpringAdjustment linked : linkedSpringAdjustments)
					linked.apply(stretching);
				if (isOutgoing) {
					if (spring.theDest.forceChanged())
						spring.theDest.theEdge.forceChanged(stretching);
				} else {
					if (spring.theSource.forceChanged())
						spring.theSource.theEdge.forceChanged(stretching);
				}
			}
		}

		private class LinkedSpringAdjustment {
			final SpringStateImpl theSpring;
			int sourcePosition;
			int destPosition;
			TensionAndSnap tension;

			LinkedSpringAdjustment(SpringStateImpl linked) {
				theSpring = linked;
				sourcePosition = theSpring.theSource.theEdge.thePosition;
				destPosition = theSpring.theDest.theEdge.thePosition;
				tension = theSpring.theTension;
			}

			void adjust() {
				int todo = todo;
				// TODO Figure out the optimum placement for both the source and dest given the dynamic current tension
			}

			void apply(boolean stretching) {
				if (!theSpring.theTension.equals(tension)) {
					theSpring.theTension = tension;
				}
				if (sourcePosition != theSpring.theSource.theEdge.thePosition) {
					theSpring.theSource.theEdge.setPosition(sourcePosition);
					for (SpringStateImpl spring : theSpring.theSource.theEdge.theIncoming.theSprings)
						spring.checkTension();
					for (SpringStateImpl spring : theSpring.theSource.theEdge.theOutgoing.theSprings) {
						if (spring != theSpring)
							spring.checkTension();
					}
				}
				if (destPosition != theSpring.theDest.theEdge.thePosition) {
					theSpring.theDest.theEdge.setPosition(destPosition);
					for (SpringStateImpl spring : theSpring.theDest.theEdge.theOutgoing.theSprings)
						spring.checkTension();
					for (SpringStateImpl spring : theSpring.theDest.theEdge.theIncoming.theSprings) {
						if (spring != theSpring)
							spring.checkTension();
					}
				}
				if (theSpring.theSource.forceChanged())
					theSpring.theSource.theEdge.forceChanged(stretching);
				if (theSpring.theDest.forceChanged())
					theSpring.theDest.theEdge.forceChanged(stretching);
			}
		}

		private boolean adjustEdgeSet(boolean vertical, ElementId minBound, ElementId maxBound, boolean stretching,
			IntPredicate preQueuedSprings) {
			BetterList<EdgeStateImpl> linesByPosition = vertical ? theVEdgesByPosition : theHEdgesByPosition;
			// Determine the set of springs that act upon the given set of lines as a whole,
			// i.e. the springs that have one end on a line in the set and the other end on a line not in the set
			float force = 0;
			int initPosition;
			// TODO At least in the case of wave=true, we don't want to find the absolute equilibrium for the target edge.
			// If there are other (mobile) edges connected to the target by springs which also have a force on them
			// that would (if moved) relax the overall force on the target, we want to allow that line to contribute as well.
			// Otherwise, we could be doing extra work, plus we could get stuck moving back and forth
			// between two unstable states on either side of the true equilibrium for the system
			CollectionElement<EdgeStateImpl> el = linesByPosition.getElement(minBound);
			initPosition = el.get().thePosition;
			while (true) {
				EdgeStateImpl line = el.get();

				for (SpringStateImpl spring : line.theIncoming.theSprings) {
					if (spring.theSource.theEdge.thePositionSortedElement.compareTo(minBound) < 0//
						|| spring.theSource.theEdge.thePositionSortedElement.compareTo(maxBound) > 0) {
						incomingSprings.add(new AdjustingSpring(spring, false, initPosition));
						force += spring.getTension();
					}
				}
				for (SpringStateImpl spring : line.theOutgoing.theSprings) {
					if (spring.theDest.theEdge.thePositionSortedElement.compareTo(minBound) < 0//
						|| spring.theDest.theEdge.thePositionSortedElement.compareTo(maxBound) > 0) {
						outgoingSprings.add(new AdjustingSpring(spring, true, initPosition));
						force -= spring.getTension();
					}
				}
				if (el.getElementId().equals(maxBound))
					break;
				else
					el = linesByPosition.getAdjacentElement(el.getElementId(), true);
			}
			boolean halfWay = false;
			if (preQueuedSprings != null) {
				for (AdjustingSpring spring : incomingSprings) {
					if (preQueuedSprings.test(spring.spring.theSpring.id) || !spring.spring.theSource.theEdge.theEdge.isMobile(stretching))
						continue;
					float extForce = spring.spring.theSource.theEdge.theTotalForce;
					if (extForce != 0 && (extForce > 0) != (force > 0)) {
						// This connected edge would, if allowed to move, also serve to relax the force on the edge set we're moving now
						// Give it a chance
						halfWay = true;
						break;
					}
				}
				if (!halfWay) {
					for (AdjustingSpring spring : outgoingSprings) {
						if (preQueuedSprings.test(spring.spring.theSpring.id)
							|| !spring.spring.theDest.theEdge.theEdge.isMobile(stretching))
							continue;
						float extForce = spring.spring.theDest.theEdge.theTotalForce;
						if (extForce != 0 && (extForce > 0) != (force > 0)) {
							// This connected edge would, if moved, also serve to relax the force on the edge set we're moving now
							// Give it a chance
							halfWay = true;
							break;
						}
					}
				}
			}
			if (DEBUG)
				System.out.println(
					"Moving " + minBound + (minBound.equals(maxBound) ? "" : "..." + maxBound) + " (" + force + ")");
			int[] relPositions = new int[linesByPosition.getElementsBefore(maxBound) - linesByPosition.getElementsBefore(minBound) + 1];
			int[] moving = new int[relPositions.length];
			el = linesByPosition.getElement(minBound);
			initPosition = el.get().thePosition;
			moving[0] = el.get().theEdge.id;
			for (int i = 1; i < relPositions.length; i++) {
				el = linesByPosition.getAdjacentElement(el.getElementId(), true);
				relPositions[i] = el.get().thePosition - initPosition;
				moving[i] = el.get().theEdge.id;
			}
			theMovingEdges = moving;
			theWaveNumber++;
			lastCalculatedForces = initPosition;

			boolean hasLeadingForce, hasTrailingForce;
			int leadingMost, trailingMost;
			float leadingForce, trailingForce;
			if (force > 0) {
				leadingMost = initPosition;
				leadingForce = force;
				hasLeadingForce = true;
				if (stretching)
					trailingMost = MAX_BOUND_VALUE;
				else
					trailingMost = theBoundState.getEdge(Orientation.of(vertical), End.trailing).getPosition()
						- relPositions[relPositions.length - 1];
				trailingForce = 0;
				hasTrailingForce = false;
			} else {
				leadingMost = 0;
				leadingForce = 0;
				hasLeadingForce = false;
				trailingMost = initPosition;
				trailingForce = force;
				hasTrailingForce = true;
			}
			int position = initPosition;
			stepping: while (leadingMost < trailingMost - 1) {
				if (DEBUG)
					System.out.println("\tStep");
				int nextSnap = force > 0 ? trailingMost : leadingMost;
				int signum = (int) Math.signum(force);
				for (AdjustingSpring spring : incomingSprings) {
					int springSign = (int) Math.signum(spring.tension.tension);
					if (signum == springSign) {
						int snap = spring.spring.theSource.theEdge.getPosition() + spring.tension.snap - spring.anchorOffset;
						if (signum > 0) {
							if (snap < nextSnap) {
								if (DEBUG)
									System.out.println("\t\tSnap to " + snap + " for " + spring + " " + spring.tension);
								nextSnap = snap;
								if (nextSnap <= leadingMost)
									break stepping;
							}
						} else {
							if (snap > nextSnap) {
								if (DEBUG)
									System.out.println("\t\tSnap to " + snap + " for " + spring + " " + spring.tension);
								nextSnap = snap;
								if (nextSnap >= trailingMost)
									break stepping;
							}
						}
					}
				}
				for (AdjustingSpring spring : outgoingSprings) {
					int springSign = (int) Math.signum(spring.tension.tension);
					if (springSign != 0 && signum != springSign) {
						int snap = spring.spring.theDest.theEdge.getPosition() - spring.tension.snap - spring.anchorOffset;
						if (signum > 0) {
							if (snap < nextSnap) {
								if (DEBUG)
									System.out.println("\t\tSnap to " + snap + " for " + spring + " " + spring.tension);
								nextSnap = snap;
								if (nextSnap <= leadingMost)
									break stepping;
							}
						} else {
							if (snap > nextSnap) {
								if (DEBUG)
									System.out.println("\t\tSnap to " + snap + " for " + spring + " " + spring.tension);
								nextSnap = snap;
								if (nextSnap >= trailingMost)
									break stepping;
							}
						}
					}
				}
				if (nextSnap == trailingMost || nextSnap == leadingMost)
					break;
				// Re-compute forces
				position = nextSnap;
				force = computeAdjustedForce(nextSnap);
				if (DEBUG)
					System.out.println("\t\t\tforce=" + force);
				if (force == 0) {
					leadingMost = trailingMost = position;
					leadingForce = trailingForce = force;
					hasLeadingForce = hasTrailingForce = true;
					break;
				} else if (force < 0) {
					trailingMost = position;
					trailingForce = force;
					hasTrailingForce = true;
				} else {
					leadingMost = position;
					leadingForce = force;
					hasLeadingForce = true;
				}
			}
			if (!hasLeadingForce)
				leadingForce = computeAdjustedForce(leadingMost);
			if (!hasTrailingForce)
				trailingForce = computeAdjustedForce(trailingMost);
			if (trailingMost - leadingMost > 1) {
				// Do a linear interpolation step to try to narrow the range
				int interpPos = interpolate(leadingMost, leadingForce, trailingMost, trailingForce);
				if (interpPos != leadingMost && interpPos != trailingMost) {
					float interpForce = computeAdjustedForce(interpPos);
					if (DEBUG)
						System.out.println("\tinterpolated " + leadingMost + "(" + leadingForce + ")..." + trailingMost + "("
							+ trailingForce + ")=" + interpPos + " (" + interpForce + ")");
					if (interpForce > 0) {
						leadingMost = interpPos;
						leadingForce = interpForce;
					} else {
						trailingMost = interpPos;
						trailingForce = interpForce;
					}
					if (Math.abs(interpForce) < Math.abs(force)) {
						position = interpPos;
						force = interpForce;
					}
				}
			}
			if (trailingMost == leadingMost) {
				position = leadingMost;
			} else if (trailingMost == leadingMost + 1) {
				if (Math.abs(leadingForce) < Math.abs(trailingForce))
					position = leadingMost;
				else
					position = trailingMost;
			} else {
				// Steps can't help us at this point. Use binary search.
				float[] f = new float[1];
				int lead = leadingMost, trail = trailingMost;
				if (DEBUG)
					System.out.println(
						"\tbsearch between " + leadingMost + " (" + leadingForce + ") and " + trailingMost + " (" + trailingForce + ")");
				position = ArrayUtils.binarySearch(leadingMost, trailingMost, pos -> {
					if (pos == lead)
						return 1;
					else if (pos == trail)
						return -1;
					f[0] = computeAdjustedForce(pos);
					if (DEBUG)
						System.out.println("\t\t" + pos + ": " + f[0]);
					if (f[0] == 0)
						return 0;
					else
						return f[0] > 0 ? 1 : -1;
				});
				if (position < 0)
					position = -position - 1;
				force = f[0];
				if (Math.abs(leadingForce) < Math.abs(force)) {
					position = leadingMost;
					force = leadingForce;
				}
				if (Math.abs(trailingForce) < Math.abs(force)) {
					position = trailingMost;
					force = trailingForce;
				}
			}
			boolean modified = position != initPosition;
			if (modified) {
				if (halfWay) {
					int halfwayPos = (position + initPosition) / 2;
					if (halfwayPos != initPosition){
						position = halfwayPos;
						if (DEBUG)
							System.out.println("Halved move to " + position);
					}
				}

				if (DEBUG)
					System.out.println("\tMoving to " + position);
				if (lastCalculatedForces != position)
					computeAdjustedForce(position);
				// From here on out, we can't just use the backing position-sorted list to iterate,
				// because setting the positions here will affect the list's content
				// So we need to create a copy
				List<EdgeStateImpl> includedLines = new ArrayList<>(relPositions.length);
				el = linesByPosition.getElement(minBound);
				for (int i = 0; i < relPositions.length; i++) {
					includedLines.add(el.get());
					el = linesByPosition.getAdjacentElement(el.getElementId(), true);
				}
				// Set the positions
				for (int i = 0; i < includedLines.size(); i++)
					includedLines.get(i).setPosition(position + relPositions[i]);
				// Update spring tensions and forces on the lines outside the set
				for (AdjustingSpring spring : incomingSprings)
					spring.updateForce(stretching);
				for (AdjustingSpring spring : outgoingSprings)
					spring.updateForce(stretching);

				// Set the forces on the lines in the set
				for (EdgeStateImpl line : includedLines) {
					// Don't just use || here, because then if incoming force is changed, the outgoing forceChanged() won't be called
					boolean forceChanged = line.theIncoming.forceChanged();
					if (line.theOutgoing.forceChanged())
						forceChanged = true;
					if (forceChanged)
						line.forceChanged(stretching);
				}
			} else {
				if (DEBUG)
					System.out.println("\tStable");
			}
			// Reset "local variables" for the next adjustment
			incomingSprings.clear();
			outgoingSprings.clear();
			lastCalculatedForces = Integer.MIN_VALUE;
			return modified;
		}

		private float computeAdjustedForce(int position) {
			lastCalculatedForces = position;
			float force = 0;
			for (AdjustingSpring spring : incomingSprings)
				force += spring.recalculate(position);
			for (AdjustingSpring spring : outgoingSprings)
				force -= spring.recalculate(position);
			return force;
		}

		private int interpolate(int leadingMost, float leadingForce, int trailingMost, float trailingForce) {
			int interpPos = Math.round(leadingMost + leadingForce * (trailingMost - leadingMost) / (leadingForce - trailingForce));
			// Increment to try to enclose the real result more often
			if ((interpPos - leadingMost) < (trailingMost - interpPos))
				interpPos++;
			else
				interpPos--;
			return interpPos;
		}
	}

	/** This is a super-specialized queue to keep track of springs visited during a breadth-first traversal of the edge graph (a wave) */
	/** This is a really super-specialized, high-efficiency integer queue with a fixed capacity */
	private static class WaveSpringQueue implements Iterable<IndexAndDirection> {
		private final int[] theValues;
		private final long[] theDirections;
		private final long[] theContent;
		private final IndexAndDirection thePopped;
		private int theStart;
		private int theEnd;
		private boolean isFull;

		WaveSpringQueue(int capacity) {
			theValues = new int[capacity];
			int words = wordIndex(capacity - 1) + 1;
			theContent = new long[words];
			theDirections = new long[words * 2];
			thePopped = new IndexAndDirection();
		}

		/**
		 * @param springIndex The spring to reserve a place for in the queue
		 * @return Whether there is a place for the spring (i.e. false if the spring has already been in the queue during the current wave)
		 */
		boolean reserve(int springIndex) {
			return !setBit(theContent, springIndex);
		}

		boolean hasContained(int springIndex) {
			return getBit(theContent, springIndex);
		}

		IndexAndDirection pop() {
			if (theStart == theEnd && !isFull)
				return null;
			thePopped.index = theValues[theStart];
			thePopped.incoming = getBit(theDirections, theStart * 2);
			thePopped.outgoing = getBit(theDirections, theStart * 2 + 1);
			// Specifically commenting this out because we want to keep a record of all content visited during the entire wave
			// clearBit(theContent, thePopped.index);
			theStart = increment(theStart);
			isFull = false;
			return thePopped;
		}

		void push(int springIndex, boolean incoming, boolean outgoing) {
			if (isFull)
				throw new IllegalStateException("Queue is at capacity");
			theValues[theEnd] = springIndex;
			if (incoming)
				setBit(theDirections, theEnd * 2);
			else
				clearBit(theDirections, theEnd * 2);
			if (outgoing)
				setBit(theDirections, theEnd * 2 + 1);
			else
				clearBit(theDirections, theEnd * 2 + 1);
			theEnd = increment(theEnd);
			if (theStart == theEnd)
				isFull = true;
		}

		void clearContent() {
			Arrays.fill(theContent, 0);
		}

		private int increment(int index) {
			index++;
			if (index == theValues.length)
				index = 0;
			return index;
		}

		@Override
		public Iterator<IndexAndDirection> iterator() {
			return new Iterator<IndexAndDirection>() {
				private final IndexAndDirection value = new IndexAndDirection();
				private int theIndex = theStart;
				private boolean hasNexed;

				@Override
				public boolean hasNext() {
					if (theIndex != theEnd)
						return true;
					else if (isFull && !hasNexed)
						return true;
					else
						return false;
				}

				@Override
				public IndexAndDirection next() {
					value.index = theValues[theIndex];
					value.incoming = getBit(theDirections, theIndex * 2);
					value.outgoing = getBit(theDirections, theIndex * 2 + 1);
					hasNexed = true;
					theIndex = increment(theIndex);
					return value;
				}
			};
		}

		@Override
		public String toString() {
			StringBuilder str = new StringBuilder("[");
			boolean first = true;
			for (IndexAndDirection iad : this) {
				if (!first)
					str.append(", ");
				first = false;
				str.append(iad);
			}
			str.append(']');
			return str.toString();
		}

		private final static int ADDRESS_BITS_PER_WORD = 6;

		private static int wordIndex(int bitIndex) {
			return bitIndex >> ADDRESS_BITS_PER_WORD;
		}

		private static boolean getBit(long[] words, int bitIndex) {
			int wordIndex = wordIndex(bitIndex);
			return (words[wordIndex] & (1L << bitIndex)) != 0;
		}

		/**
		 * Sets a bit in the given word set to true
		 *
		 * @param words The words to test
		 * @param bitIndex The index of the bit to test
		 * @return The previous state of the bit
		 */
		private static boolean setBit(long[] words, int bitIndex) {
			int wordIndex = wordIndex(bitIndex);
			if ((words[wordIndex] & (1L << bitIndex)) != 0)
				return true;
			words[wordIndex] |= (1L << bitIndex); // Restores invariants
			return false;
		}

		private static void clearBit(long[] words, int bitIndex) {
			int wordIndex = wordIndex(bitIndex);
			words[wordIndex] &= ~(1L << bitIndex);
		}
	}

	private static class IndexAndDirection {
		int index;
		boolean incoming;
		boolean outgoing;

		IndexAndDirection() {
		}

		IndexAndDirection(int index, boolean incoming, boolean outgoing) {
			this.index = index;
			this.incoming = incoming;
			this.outgoing = outgoing;
		}

		@Override
		public String toString() {
			StringBuilder str = new StringBuilder();
			str.append(index).append('(');
			if (incoming) {
				if (outgoing)
					str.append("both");
				else
					str.append("incoming");
			} else
				str.append("outgoing");
			str.append(')');
			return str.toString();
		}
	}

	private class EdgeStateImpl implements EdgeState {
		final LayoutStateImpl theLayoutState;
		final EdgeImpl theEdge;
		final SpringSet theIncoming;
		final SpringSet theOutgoing;

		int thePosition;
		float theTotalForce;

		private ElementId thePositionSortedElement;
		private ElementId theForceSortedElement;

		EdgeStateImpl(LayoutStateImpl layoutState, EdgeImpl divider, BetterList<SpringStateImpl> springsByIndex) {
			theLayoutState = layoutState;
			theEdge = divider;
			theIncoming = new SpringSet(this, true, springsByIndex);
			theOutgoing = new SpringSet(this, false, springsByIndex);
		}

		void reset() {
			thePosition = 0;
			theTotalForce = 0;
			thePositionSortedElement = null;
			theForceSortedElement = null;
			theOutgoing.reset();
			theIncoming.theForce = 0;
		}

		void forceChanged(boolean stretching) {
			theTotalForce = theIncoming.theForce + theOutgoing.theForce;
			boolean shouldRegisterForce;
			if (theTotalForce == 0)
				shouldRegisterForce = false;
			else
				shouldRegisterForce = theEdge.isMobile(stretching);
			if (theForceSortedElement != null) {
				if (!shouldRegisterForce) {
					theLayoutState.theEdgesByForce.mutableElement(theForceSortedElement).remove();
					theForceSortedElement = null;
				} else {
					boolean belongs = true;
					CollectionElement<EdgeStateImpl> adj = theLayoutState.theEdgesByForce.getAdjacentElement(theForceSortedElement, false);
					if (adj != null && Math.abs(adj.get().theTotalForce) < Math.abs(theTotalForce))
						belongs = false;
					if (belongs) {
						adj = theLayoutState.theEdgesByForce.getAdjacentElement(theForceSortedElement, true);
						if (adj != null && Math.abs(adj.get().theTotalForce) > Math.abs(theTotalForce))
							belongs = false;
					}
					if (!belongs) {
						theLayoutState.theEdgesByForce.mutableElement(theForceSortedElement).remove();
						theForceSortedElement = theLayoutState.theEdgesByForce.addElement(this, false).getElementId();
					}
				}
			} else if (shouldRegisterForce)
				theForceSortedElement = theLayoutState.theEdgesByForce.addElement(this, false).getElementId();
		}

		void stabilized() {
			if (theForceSortedElement != null) {
				theLayoutState.theEdgesByForce.mutableElement(theForceSortedElement).remove();
				theForceSortedElement = null;
			}
		}

		boolean isPositioned() {
			return thePositionSortedElement != null;
		}

		boolean setPosition(int position) {
			if (thePosition == position && thePositionSortedElement != null)
				return false;
			thePosition = position;
			BetterList<EdgeStateImpl> linesByPosition = theEdge.isVertical ? theLayoutState.theVEdgesByPosition
				: theLayoutState.theHEdgesByPosition;
			if (thePositionSortedElement != null) {
				boolean belongs = true;
				CollectionElement<EdgeStateImpl> adj = linesByPosition.getAdjacentElement(thePositionSortedElement, false);
				if (adj != null && POSITION_COMPARE.compare(adj.get(), this) > 0)
					belongs = false;
				if (belongs) {
					adj = linesByPosition.getAdjacentElement(thePositionSortedElement, true);
					if (adj != null && POSITION_COMPARE.compare(adj.get(), this) < 0)
						belongs = false;
				}
				if (!belongs) {
					linesByPosition.mutableElement(thePositionSortedElement).remove();
					thePositionSortedElement = linesByPosition.addElement(this, false).getElementId();
				}
			} else
				thePositionSortedElement = linesByPosition.addElement(this, false).getElementId();
			return true;
		}

		@Override
		public Orientation getOrientation() {
			return theEdge.getOrientation();
		}

		@Override
		public int getPosition() {
			return thePosition;
		}

		@Override
		public float getForce() {
			return theTotalForce;
		}

		@Override
		public String toString() {
			return theEdge + ": " + thePosition + " (" + theTotalForce + ")";
		}
	}

	private class SpringSet {
		final EdgeStateImpl theEdge;
		private final boolean isIncoming;
		final List<SpringStateImpl> theSprings;
		float theForce;

		SpringSet(EdgeStateImpl divider, boolean incoming, BetterList<SpringStateImpl> springsByIndex) {
			theEdge = divider;
			isIncoming = incoming;
			theSprings = new ArrayList<>((incoming ? divider.theEdge.theIncomingSprings : divider.theEdge.theOutgoingSprings).size());
			if (!incoming) {
				for (SpringImpl spring : divider.theEdge.theOutgoingSprings) {
					SpringStateImpl springState = new SpringStateImpl(this, spring);
					theEdge.theLayoutState.theSpringStates.put(spring, springState);
					theSprings.add(springState);
					springsByIndex.add(springState);
				}
			}
		}

		void fillIncoming() {
			for (SpringImpl spring : theEdge.theEdge.theIncomingSprings) {
				SpringStateImpl springState = theEdge.theLayoutState.theSpringStates.get(spring);
				springState.theDest = this;
				theSprings.add(springState);
				springState.findLinked();
			}
		}

		boolean forceChanged() {
			float totalForce = 0;
			for (SpringStateImpl springState : theSprings)
				totalForce += springState.getTension();
			if (!isIncoming)
				totalForce = -totalForce;
			if (totalForce == theForce)
				return false;
			theForce = totalForce;
			return true;
		}

		void reset() {
			theForce = 0;
			for (SpringStateImpl spring : theSprings)
				spring.reset();
		}
	}

	private class SpringStateImpl implements SpringState {
		final SpringSet theSource;
		SpringSet theDest;
		final SpringImpl theSpring;
		private final List<SpringStateImpl> theLinkedSprings;
		TensionAndSnap theTension;

		private final int theMaxSlack;
		private int theSlack;
		private float theSlackMultiplier;

		SpringStateImpl(SpringSet source, SpringImpl spring) {
			theSource = source;
			theSpring = spring;
			theLinkedSprings = new ArrayList<>(spring.theLinkedSprings == null ? 0 : spring.theLinkedSprings.size());
			theMaxSlack = spring.theLinkedSprings == null ? REGULAR_SLACK_FACTOR
				: ((spring.theLinkedSprings.size() + 1) * LINKED_SPRING_SLACK_FACTOR);
			theTension = TensionAndSnap.ZERO;
		}

		void findLinked() {
			if (theSpring.theLinkedSprings != null) {
				for (SpringImpl spring : theSpring.theLinkedSprings)
					theLinkedSprings.add(theSource.theEdge.theLayoutState.theSpringStates.get(spring));
			}
			theSlack = theMaxSlack;
			if (theSlack > 1)
				theSource.theEdge.theLayoutState.theSlackenedSpringCount++;
			theSlackMultiplier = calcSlackMultiplier(theSlack, theMaxSlack);
		}

		void reset() {
			theTension = TensionAndSnap.ZERO;
			boolean wasSlackened = theSlack > 1;
			theSlack = theMaxSlack;
			if (theSlack > 1) {
				if (!wasSlackened)
					theSource.theEdge.theLayoutState.theSlackenedSpringCount++;
				theSlackMultiplier = calcSlackMultiplier(theSlack, theMaxSlack);
			}
		}

		void updateLinkedSprings(boolean stretching) {
			pullInSlack();
			for (SpringStateImpl spring : theLinkedSprings)
				spring.recalculate(stretching);
		}

		boolean pullInSlack() {
			if (theSlack > 1) {
				theSlack--;
				if (theSlack == 1)
					theSource.theEdge.theLayoutState.theSlackenedSpringCount--;
				float preMult = DEBUG ? theSlackMultiplier : 0;
				float preTension = DEBUG ? getTension() : 0;
				theSlackMultiplier = calcSlackMultiplier(theSlack, theMaxSlack);
				if (DEBUG)
					System.out.println("Unslackened " + this + " from " + preTension + "(" + preMult + ") to " + getTension() + "("
						+ theSlackMultiplier + ")");
				return true;
			} else
				return false;
		}

		float slacken(float tension) {
			if (theSlack == 1)
				return tension;
			else if (theSlack == LINKED_SPRING_SLACK_FACTOR)
				return 0; // Initially, just let the rest of the layout do what it wants. We'll assert ourselves later.
			else
				return tension * theSlackMultiplier;
		}

		boolean checkTension() {
			TensionAndSnap tas;
			if (theSpring.theEvaluator == BOUNDS_TENSION) {
				float tension = theSpring.theSource.getOrientation().isVertical() ? theSource.theEdge.theLayoutState.theVTension
					: theSource.theEdge.theLayoutState.theHTension;
				tas = new TensionAndSnap(tension, tension > 0 ? 10000000 : 0);
			} else
				tas = theSpring.theEvaluator.getTension(theDest.theEdge.thePosition - theSource.theEdge.thePosition);
			if (theTension.equals(tas.tension))
				return false;
			theTension = tas;
			return true;
		}

		boolean recalculate(boolean stretching) {
			if (!checkTension())
				return false;
			if (theSource.forceChanged())
				theSource.theEdge.forceChanged(stretching);
			if (theDest.forceChanged())
				theDest.theEdge.forceChanged(stretching);
			return true;
		}

		@Override
		public EdgeState getSource() {
			return theSource.theEdge;
		}

		@Override
		public EdgeState getDest() {
			return theDest.theEdge;
		}

		@Override
		public float getTension() {
			return slacken(theTension.tension);
		}

		@Override
		public String toString() {
			String str = theSpring.toString() + ": ";
			if (theSpring.theEvaluator == BOUNDS_TENSION)
				str += "stretch";
			else
				str += theTension;
			return str;
		}
	}

	private static float calcSlackMultiplier(int slack, int maxSlack) {
		if (slack == 1)
			return 1;
		else if (slack == LINKED_SPRING_SLACK_FACTOR)
			return 0;
		else {
			// Exponential
			slack--;
			float mult = 1.0f;
			while (slack > 31) {
				mult /= 1 << 31;
				slack -= 31;
			}
			mult /= (1 << slack);
			return mult;
		}
	}

	private class BoxStateImpl implements BoxState {
		final EdgeStateImpl left;
		final EdgeStateImpl right;
		final EdgeStateImpl top;
		final EdgeStateImpl bottom;

		BoxStateImpl(LayoutStateImpl layoutState, EdgeStateImpl left, EdgeStateImpl right, EdgeStateImpl top, EdgeStateImpl bottom) {
			this.left = left;
			this.right = right;
			this.top = top;
			this.bottom = bottom;
		}

		@Override
		public EdgeState getLeft() {
			return left;
		}

		@Override
		public EdgeState getRight() {
			return right;
		}

		@Override
		public EdgeState getTop() {
			return top;
		}

		@Override
		public EdgeState getBottom() {
			return bottom;
		}
	}

	/**
	 * Debugs this class graphically for a layout scenario or two
	 *
	 * @param args Command-line arguments. The first argument may be:
	 *        <ul>
	 *        <li>2box: Creates a simple system with 2 boxes (also the default when no args are specified)</li>
	 *        <li>2boxAndText: Creates a system with 2 boxes and a simulated text block</li>
	 *        </ul>
	 */
	public static void main(String[] args) {
		DebugPlotter plotter = new DebugPlotter();
		Map<BoxState, BoxHolder> boxes = new LinkedHashMap<>();
		Map<SpringState, SpringHolder> springs = new LinkedHashMap<>();
		LayoutSolver<String> solver = new LayoutSolver<>();
		State<String> solverState;

		String scenario;
		if (args.length == 0)
			scenario = "2box";
		else {
			scenario = args[0].toLowerCase();
			switch (scenario) {
			case "2box":
			case "2boxandtext":
				break;
			default:
				scenario = "2box";
				break;
			}
		}
		if (scenario.equals("2box")) {
			// First, a simple system with 2 boxes
			BoxDef box1 = solver.getOrCreateBox("b1 left", "b1 right", "b1 top", "b1 bottom");
			BoxDef box2 = solver.getOrCreateBox("b2 left", "b2 right", "b2 top", "b2 bottom");
			// Margins
			SpringDef leftMargin = solver.createSpring(solver.getBounds().getLeft(), box1.getLeft(),
				forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, 1000)));
			SpringDef rightMargin = solver.createSpring(box2.getRight(), solver.getBounds().getRight(),
				forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, 1000)));
			SpringDef topMargin1 = solver.createSpring(solver.getBounds().getTop(), box1.getTop(),
				forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, 1000)));
			SpringDef topMargin2 = solver.createSpring(solver.getBounds().getTop(), box2.getTop(),
				forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, 1000)));
			SpringDef bottomMargin1 = solver.createSpring(box1.getBottom(), solver.getBounds().getBottom(),
				forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, 1000)));
			SpringDef bottomMargin2 = solver.createSpring(box2.getBottom(), solver.getBounds().getBottom(),
				forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, 1000)));
			// Between the boxes
			SpringDef padding = solver.createSpring(box1.getRight(), box2.getLeft(), //
				forSizer(() -> 0, new SimpleSizeGuide(0, 5, 10, 15, 1000)));
			// Now the box dimensions
			SpringDef w1 = solver.createSpring(box1.getLeft(), box1.getRight(), //
				forSizer(() -> 0, new SimpleSizeGuide(10, 50, 100, 150, 500)));
			SpringDef h1 = solver.createSpring(box1.getTop(), box1.getBottom(), //
				forSizer(() -> 0, new SimpleSizeGuide(10, 40, 80, 125, 400)));
			SpringDef w2 = solver.createSpring(box2.getLeft(), box2.getRight(), //
				forSizer(() -> 0, new SimpleSizeGuide(10, 30, 75, 100, 300)));
			SpringDef h2 = solver.createSpring(box2.getTop(), box2.getBottom(), //
				forSizer(() -> 0, new SimpleSizeGuide(10, 40, 80, 125, 400)));

			solverState = solver.use();
			BoxState box1State = solverState.getBox(box1);
			BoxState box2State = solverState.getBox(box2);

			addBox(plotter, solverState, boxes, solverState.getBounds(), "bounds");
			addBox(plotter, solverState, boxes, box1State, "box1");
			addBox(plotter, solverState, boxes, box2State, "box2");

			addSpring(plotter, springs, solverState.getSpring(leftMargin), "left margin", () -> box1State.getCenter().y);
			addSpring(plotter, springs, solverState.getSpring(rightMargin), "right margin", () -> box2State.getCenter().y);
			addSpring(plotter, springs, solverState.getSpring(topMargin1), "top margin 1", () -> box1State.getCenter().x);
			addSpring(plotter, springs, solverState.getSpring(topMargin2), "top margin 2", () -> box2State.getCenter().x);
			addSpring(plotter, springs, solverState.getSpring(bottomMargin1), "bottom margin 1", () -> box1State.getCenter().x);
			addSpring(plotter, springs, solverState.getSpring(bottomMargin2), "bottom margin 2", () -> box2State.getCenter().x);
			addSpring(plotter, springs, solverState.getSpring(padding), "padding", () -> box1State.getCenter().y);
			addSpring(plotter, springs, solverState.getSpring(w1), "box 1 width", () -> box1State.getCenter().y);
			addSpring(plotter, springs, solverState.getSpring(h1), "box 1 height", () -> box1State.getCenter().x);
			addSpring(plotter, springs, solverState.getSpring(w2), "box 2 width", () -> box2State.getCenter().y);
			addSpring(plotter, springs, solverState.getSpring(h2), "box 2 height", () -> box2State.getCenter().x);
		} else {
			BoxDef box1 = solver.getOrCreateBox("b1 left", "b1 right", "b1 top", "b1 bottom");
			BoxDef textBox = solver.getOrCreateBox("text left", "text right", "text top", "text bottom");
			BoxDef box2 = solver.getOrCreateBox("b2 left", "b2 right", "b2 top", "b2 bottom");
			BoxState[] textBoxState = new BoxState[1];

			// Margins
			SpringDef leftMargin = solver.createSpring(solver.getBounds().getLeft(), box1.getLeft(),
				forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, 1000)));
			SpringDef rightMargin = solver.createSpring(box2.getRight(), solver.getBounds().getRight(),
				forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, 1000)));
			SpringDef topMargin1 = solver.createSpring(solver.getBounds().getTop(), box1.getTop(),
				forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, 1000)));
			SpringDef topMarginText = solver.createSpring(solver.getBounds().getTop(), textBox.getTop(),
				forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, 1000)));
			SpringDef topMargin2 = solver.createSpring(solver.getBounds().getTop(), box2.getTop(),
				forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, 1000)));
			SpringDef bottomMargin1 = solver.createSpring(box1.getBottom(), solver.getBounds().getBottom(),
				forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, 1000)));
			SpringDef bottomMarginText = solver.createSpring(textBox.getBottom(), solver.getBounds().getBottom(),
				forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, 1000)));
			SpringDef bottomMargin2 = solver.createSpring(box2.getBottom(), solver.getBounds().getBottom(),
				forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, 1000)));
			// Between the boxes
			SpringDef padding1 = solver.createSpring(box1.getRight(), textBox.getLeft(), //
				forSizer(() -> 0, new SimpleSizeGuide(0, 5, 10, 15, 1000)));
			SpringDef padding2 = solver.createSpring(textBox.getRight(), box2.getLeft(), //
				forSizer(() -> 0, new SimpleSizeGuide(0, 5, 10, 15, 1000)));
			// Now the box dimensions
			SpringDef w1 = solver.createSpring(box1.getLeft(), box1.getRight(), //
				forSizer(() -> 0, new SimpleSizeGuide(10, 50, 100, 150, 500)));
			SpringDef h1 = solver.createSpring(box1.getTop(), box1.getBottom(), //
				forSizer(() -> 0, new SimpleSizeGuide(10, 40, 80, 125, 400)));
			SpringDef wText = solver.createSpring(textBox.getLeft(), textBox.getRight(), //
				forSizer(() -> textBoxState[0].getHeight(), new ConstAreaSizer(25000)));
			SpringDef hText = solver.createSpring(textBox.getTop(), textBox.getBottom(), //
				forSizer(() -> textBoxState[0].getWidth(), new ConstAreaSizer(25000)));
			solver.linkSprings(wText, hText);
			SpringDef w2 = solver.createSpring(box2.getLeft(), box2.getRight(), //
				forSizer(() -> 0, new SimpleSizeGuide(10, 30, 75, 100, 300)));
			SpringDef h2 = solver.createSpring(box2.getTop(), box2.getBottom(), //
				forSizer(() -> 0, new SimpleSizeGuide(10, 40, 80, 125, 400)));

			solverState = solver.use();
			BoxState box1State = solverState.getBox(box1);
			textBoxState[0] = solverState.getBox(textBox);
			BoxState box2State = solverState.getBox(box2);

			addBox(plotter, solverState, boxes, solverState.getBounds(), "bounds");
			addBox(plotter, solverState, boxes, box1State, "box1");
			addBox(plotter, solverState, boxes, textBoxState[0], "textBox");
			addBox(plotter, solverState, boxes, box2State, "box2");

			addSpring(plotter, springs, solverState.getSpring(leftMargin), "left margin", () -> box1State.getCenter().y);
			addSpring(plotter, springs, solverState.getSpring(rightMargin), "right margin", () -> box2State.getCenter().y);
			addSpring(plotter, springs, solverState.getSpring(topMargin1), "top margin 1", () -> box1State.getCenter().x);
			addSpring(plotter, springs, solverState.getSpring(topMarginText), "top margin text", () -> textBoxState[0].getCenter().x);
			addSpring(plotter, springs, solverState.getSpring(topMargin2), "top margin 2", () -> box2State.getCenter().x);
			addSpring(plotter, springs, solverState.getSpring(bottomMargin1), "bottom margin 1", () -> box1State.getCenter().x);
			addSpring(plotter, springs, solverState.getSpring(bottomMarginText), "bottom margin text", () -> textBoxState[0].getCenter().x);
			addSpring(plotter, springs, solverState.getSpring(bottomMargin2), "bottom margin 2", () -> box2State.getCenter().x);
			addSpring(plotter, springs, solverState.getSpring(padding1), "padding1", () -> box1State.getCenter().y);
			addSpring(plotter, springs, solverState.getSpring(padding2), "padding2", () -> box2State.getCenter().y);
			addSpring(plotter, springs, solverState.getSpring(w1), "box 1 width", () -> box1State.getCenter().y);
			addSpring(plotter, springs, solverState.getSpring(h1), "box 1 height", () -> box1State.getCenter().x);
			addSpring(plotter, springs, solverState.getSpring(wText), "text box width", () -> textBoxState[0].getCenter().y);
			addSpring(plotter, springs, solverState.getSpring(hText), "text box height", () -> textBoxState[0].getCenter().x);
			addSpring(plotter, springs, solverState.getSpring(w2), "box 2 width", () -> box2State.getCenter().y);
			addSpring(plotter, springs, solverState.getSpring(h2), "box 2 height", () -> box2State.getCenter().x);
		}

		JFrame plotterFrame = plotter.showFrame("Layout Solver", null);
		plotterFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		boolean[] reSolve = new boolean[1];
		plotter.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				reSolve[0] = true;
			}
		});
		Thread reSolveThread = new Thread(() -> {
			while (true) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
				if (reSolve[0]) {
					reSolve[0] = false;
					reSolve(solverState, plotter.getWidth(), plotter.getHeight());
				}
			}
		}, "Re-solver");
		Thread updateRenderThread = new Thread(() -> {
			int waveNumber = solverState.getWaveNumber();
			while (true) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
				if (updateShapes(boxes, springs) || waveNumber != solverState.getWaveNumber()) {
					waveNumber = solverState.getWaveNumber();
					EventQueue.invokeLater(() -> plotter.repaint());
				}
			}
		}, "Plot updater");
		plotter.setDoubleClick(() -> printAll(boxes, springs));
		plotter.setRightClick(() -> reSolve[0] = true);
		reSolve[0] = true;
		reSolveThread.start();
		updateRenderThread.start();
	}

	private static void reSolve(State<String> solverState, int width, int height) {
		solverState.reset();
		// solverState.stretch(-MAX_TENSION, -MAX_TENSION);
		// solverState.stretch(-MAX_PREF_TENSION, -MAX_PREF_TENSION);
		// solverState.stretch(0, 0);
		// solverState.stretch(MAX_PREF_TENSION, MAX_PREF_TENSION);
		// solverState.stretch(MAX_TENSION, MAX_TENSION);
		solverState.layout(width, height);
	}

	private static class BoxHolder {
		private final BoxState theBox;
		private final State<String> theState;

		private final Line2D theLeft;
		private final Line2D theRight;
		private final Line2D theTop;
		private final Line2D theBottom;

		private final DebugPlotter.ShapeHolder leftRender;
		private final DebugPlotter.ShapeHolder rightRender;
		private final DebugPlotter.ShapeHolder topRender;
		private final DebugPlotter.ShapeHolder bottomRender;

		BoxHolder(DebugPlotter plotter, State<String> state, BoxState box, String boxName) {
			theBox = box;
			theState = state;

			theLeft = new Line2D.Float();
			theRight = new Line2D.Float();
			theTop = new Line2D.Float();
			theBottom = new Line2D.Float();

			leftRender = plotter.add(theLeft).setColor(() -> state.isEdgeMoving(box.getLeft()) ? Color.green : Color.black)
				.setText(() -> boxName + " left: P " + box.getLeft().getPosition() + " F " + box.getLeft().getForce());
			rightRender = plotter.add(theRight).setColor(() -> state.isEdgeMoving(box.getRight()) ? Color.green : Color.black)
				.setText(() -> boxName + " right: P" + box.getRight().getPosition() + " F " + box.getRight().getForce());
			topRender = plotter.add(theTop).setColor(() -> state.isEdgeMoving(box.getTop()) ? Color.green : Color.black)
				.setText(() -> boxName + " top: P " + box.getTop().getPosition() + " F " + box.getTop().getForce());
			bottomRender = plotter.add(theBottom).setColor(() -> state.isEdgeMoving(box.getBottom()) ? Color.green : Color.black)
				.setText(() -> boxName + " bottom: P " + box.getBottom().getPosition() + " F " + box.getBottom().getForce());
		}

		boolean update() {
			double left = theBox.getLeft().getPosition();
			double right = theBox.getRight().getPosition();
			double top = theBox.getTop().getPosition();
			double bottom = theBox.getBottom().getPosition();
			if (theLeft.getX1() != left || theLeft.getY1() != top || theLeft.getY2() != bottom || theRight.getX1() != right) {
				theLeft.setLine(left, top, left, bottom);
				theRight.setLine(right, top, right, bottom);
				theTop.setLine(left, top, right, top);
				theBottom.setLine(left, bottom, right, bottom);
				return true;
			} else
				return false;
		}
	}

	private static class SpringHolder {
		final Line2D line;
		final IntSupplier location;

		public SpringHolder(Line2D line, IntSupplier location) {
			super();
			this.line = line;
			this.location = location;
		}
	}

	private static void addBox(DebugPlotter plotter, State<String> state, Map<BoxState, BoxHolder> boxes, BoxState box, String boxName) {
		boxes.put(box, new BoxHolder(plotter, state, box, boxName));
	}

	private static void addSpring(DebugPlotter plotter, Map<SpringState, SpringHolder> springs, SpringState spring, String springName,
		IntSupplier location) {
		Line2D line = new Line2D.Float();
		springs.put(spring, new SpringHolder(line, location));
		plotter.add(line).setColor(Color.blue).setText(() -> springName + ": L " + spring.getLength() + " T " + spring.getTension());
	}

	private static boolean updateShapes(Map<BoxState, BoxHolder> boxes, Map<SpringState, SpringHolder> springs) {
		boolean updated = false;
		for (BoxHolder box : boxes.values()) {
			if (box.update())
				updated = true;
		}
		if (updated) {
			for (Map.Entry<SpringState, SpringHolder> spring : springs.entrySet()) {
				int pos = spring.getValue().location.getAsInt();
				if (spring.getKey().getSource().getOrientation().isVertical()) {
					spring.getValue().line.setLine(pos, spring.getKey().getSource().getPosition(), //
						pos, spring.getKey().getDest().getPosition());
				} else {
					spring.getValue().line.setLine(spring.getKey().getSource().getPosition(), pos, //
						spring.getKey().getDest().getPosition(), pos);
				}
			}
		}
		return updated;
	}

	private static void printAll(Map<BoxState, ?> boxes, Map<SpringState, SpringHolder> springs) {
		for (BoxDef box : boxes.keySet()) {
			System.out.println(box.getLeft());
			System.out.println(box.getRight());
			System.out.println(box.getTop());
			System.out.println(box.getBottom());
		}
		for (SpringDef spring : springs.keySet())
			System.out.println(spring);
	}

	private static class ConstAreaSizer extends AbstractSizeGuide {
		private int theArea;

		ConstAreaSizer(int area) {
			theArea = area;
		}

		@Override
		public int getMin(int crossSize, boolean csMax) {
			if (csMax)
				return cap(theArea / cap(crossSize));
			return getPreferred(crossSize, csMax);
		}

		@Override
		public int getMinPreferred(int crossSize, boolean csMax) {
			return getMin(crossSize, csMax);
		}

		@Override
		public int getPreferred(int crossSize, boolean csMax) {
			return cap(theArea / cap(crossSize));
		}

		@Override
		public int getMaxPreferred(int crossSize, boolean csMax) {
			return getPreferred(crossSize, csMax);
		}

		@Override
		public int getMax(int crossSize, boolean csMax) {
			return getPreferred(crossSize, csMax);
		}

		@Override
		public int getBaseline(int size) {
			return 0;
		}

		private int cap(int size) {
			if (size < 30)
				return 30;
			return size;
		}
	}
}
