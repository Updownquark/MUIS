package org.quick.core.layout;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

import javax.swing.JFrame;

import org.qommons.ArrayUtils;
import org.qommons.IntList;
import org.qommons.QommonsUtils;
import org.qommons.Ternian;
import org.qommons.collect.BetterHashMap;
import org.qommons.collect.BetterList;
import org.qommons.collect.BetterMap;
import org.qommons.collect.CollectionElement;
import org.qommons.collect.ElementId;
import org.qommons.tree.SortedTreeList;
import org.quick.core.Point;
import org.quick.core.layout.LayoutSpringEvaluator.TensionAndSnap;
import org.quick.util.DebugPlotter;

public class LayoutSolver<L> {
	public interface DividerDef {
		Orientation getOrientation();
	}

	public interface DividerState extends DividerDef {
		int getPosition();

		float getForce();
	}

	public interface SpringDef {
		DividerDef getSource();

		DividerDef getDest();
	}

	public interface SpringState extends SpringDef {
		@Override
		DividerState getSource();

		@Override
		DividerState getDest();

		default int getLength() {
			return getDest().getPosition() - getSource().getPosition();
		}

		float getTension();
	}

	public interface BoxDef {
		DividerDef getLeft();

		DividerDef getRight();

		DividerDef getTop();

		DividerDef getBottom();

		default DividerDef get(Orientation orientation, End end) {
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
		DividerState getLeft();

		@Override
		DividerState getRight();

		@Override
		DividerState getTop();

		@Override
		DividerState getBottom();

		@Override
		default DividerState get(Orientation orientation, End end) {
			return (DividerState) BoxDef.super.get(orientation, end);
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

		DividerState getLine(L lineObj);

		DividerState getLine(DividerDef line);

		SpringState getSpring(SpringDef spring);

		State<L> reset();

		State<L> layout(int width, int height);

		State<L> stretch(float hTension, float vTension);

		BoxState getBox(BoxDef box);
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
							float tension = MAX_TENSION + (SUPER_TENSION - MAX_TENSION) * (min - length) / min;
							return new TensionAndSnap(tension, minPref);
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
				if (size < 0)
					return 0;
				else if (size > MAX_LAYOUT_SIZE)
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

	private static final int MAX_TRIES = 100; // TODO Should be static when not debugging

	private final BoxImpl theBounds;

	private final BetterMap<L, DivImpl> theLines;
	private boolean isSealed;

	public LayoutSolver() {
		theLines = BetterHashMap.build().unsafe().buildMap();
		theBounds = new BoxImpl(//
			new DivImpl((L) "Left Bound", false, End.leading), new DivImpl((L) "Right Bound", false, End.trailing), //
			new DivImpl((L) "Top Bound", true, End.leading), new DivImpl((L) "Bottom Bound", true, End.trailing)//
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
			(DivImpl) getOrCreateLine(left, false), (DivImpl) getOrCreateLine(right, false), //
			(DivImpl) getOrCreateLine(top, true), (DivImpl) getOrCreateLine(bottom, true));
	}

	public DividerDef getOrCreateLine(L lineObject, boolean vertical) {
		if (isSealed)
			throw new IllegalStateException("This solver cannot be altered");
		return theLines.computeIfAbsent(lineObject, lo -> new DivImpl(lineObject, vertical, null));
	}

	public DividerDef getLine(L lineObject) {
		return theLines.get(lineObject);
	}

	public SpringDef createSpring(DividerDef src, DividerDef dest, LayoutSpringEvaluator eval) {
		if (isSealed)
			throw new IllegalStateException("This solver cannot be altered");
		if (src.getOrientation() != dest.getOrientation())
			throw new IllegalArgumentException("Springs can only be created between lines of the same orientation");
		DivImpl l1 = (DivImpl) src, l2 = (DivImpl) dest;
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

	public State use() {
		isSealed = true;
		return new LayoutStateImpl();
	}

	private class DivImpl implements DividerDef {
		private final L theValue;
		final boolean isVertical;
		final End borderEnd;
		private final List<SpringImpl> theOutgoingSprings;
		private final List<SpringImpl> theIncomingSprings;

		DivImpl(L value, boolean vertical, End borderEnd) {
			theValue = value;
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

		@Override
		public String toString() {
			return String.valueOf(theValue);
		}
	}

	private class SpringImpl implements SpringDef {
		private final DivImpl theSource;
		private final DivImpl theDest;
		final LayoutSpringEvaluator theEvaluator;
		List<SpringImpl> theLinkedSprings;

		SpringImpl(DivImpl source, DivImpl dest, LayoutSpringEvaluator evaluator) {
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
		public DividerDef getSource() {
			return theSource;
		}

		@Override
		public DividerDef getDest() {
			return theDest;
		}

		@Override
		public String toString() {
			return theSource.theValue + "->" + theDest.theValue;
		}
	}

	private class BoxImpl implements BoxDef {
		final DivImpl left;
		final DivImpl right;
		final DivImpl top;
		final DivImpl bottom;

		BoxImpl(DivImpl left, DivImpl right, DivImpl top, DivImpl bottom) {
			this.left = left;
			this.right = right;
			this.top = top;
			this.bottom = bottom;
		}

		@Override
		public DivImpl getLeft() {
			return left;
		}

		@Override
		public DivImpl getRight() {
			return right;
		}

		@Override
		public DivImpl getTop() {
			return top;
		}

		@Override
		public DivImpl getBottom() {
			return bottom;
		}
	}

	private static boolean DEBUG = true;
	private static boolean PRINT_SPEED = true;

	private class LayoutStateImpl implements State<L> {
		private final IdentityHashMap<DivImpl, DivStateImpl> theLineStates;
		private final IdentityHashMap<SpringImpl, SpringStateImpl> theSpringStates;
		private final BetterList<DivStateImpl> theHLinesByPosition;
		private final BetterList<DivStateImpl> theVLinesByPosition;
		private final BetterList<DivStateImpl> theLinesByForce;

		private final BoxStateImpl theBoundState;
		private boolean isStretching;
		private float theHTension;
		private float theVTension;

		private boolean isInitialized;

		LayoutStateImpl() {
			theLineStates = new IdentityHashMap<>();
			theSpringStates = new IdentityHashMap<>();
			theBoundState = new BoxStateImpl(this, //
				new DivStateImpl(this, theBounds.left), new DivStateImpl(this, theBounds.right), //
				new DivStateImpl(this, theBounds.top), new DivStateImpl(this, theBounds.bottom));
			for (DivImpl line : theLines.values())
				theLineStates.put(line, new DivStateImpl(this, line));
			theBoundState.left.theIncoming.fillIncoming();
			theBoundState.right.theIncoming.fillIncoming();
			theBoundState.top.theIncoming.fillIncoming();
			theBoundState.bottom.theIncoming.fillIncoming();
			for (DivStateImpl line : theLineStates.values())
				line.theIncoming.fillIncoming();
			theLinesByForce = new SortedTreeList<>(false,
				(div1, div2) -> -Float.compare(Math.abs(div1.theTotalForce), Math.abs(div2.theTotalForce)));
			theHLinesByPosition = new SortedTreeList<>(false, (div1, div2) -> div1.thePosition - div2.thePosition);
			theVLinesByPosition = new SortedTreeList<>(false, (div1, div2) -> div1.thePosition - div2.thePosition);
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
		public DividerState getLine(L lineObj) {
			DivImpl divider = theLines.get(lineObj);
			return divider == null ? null : theLineStates.get(divider);
		}

		@Override
		public DividerState getLine(DividerDef line) {
			return theLineStates.get(line);
		}

		@Override
		public SpringState getSpring(SpringDef spring) {
			return theSpringStates.get(spring);
		}

		@Override
		public BoxState getBox(BoxDef box) {
			return new BoxStateImpl(this, //
				theLineStates.get(((BoxImpl) box).left), theLineStates.get(((BoxImpl) box).right), //
				theLineStates.get(((BoxImpl) box).top), theLineStates.get(((BoxImpl) box).bottom));
		}

		@Override
		public State<L> reset() {
			isInitialized = false;
			theBoundState.left.reset();
			theBoundState.right.reset();
			theBoundState.top.reset();
			theBoundState.bottom.reset();
			for (DivStateImpl line : theLineStates.values())
				line.reset();
			theLinesByForce.clear();
			theHLinesByPosition.clear();
			theVLinesByPosition.clear();
			return this;
		}

		@Override
		public State<L> layout(int width, int height) {
			theBoundState.right.setPosition(width);
			theBoundState.bottom.setPosition(height);
			theHTension = 0;
			theVTension = 0;
			return adjustLayout(false);
		}

		@Override
		public State<L> stretch(float hTension, float vTension) {
			// Add a left->right and a top->bottom constraint on the bounds rectangle with the given tensions
			// The stretch tension needs to oppose the sum of all the external springs attached to the bounds
			theHTension = hTension * (theBounds.left.theOutgoingSprings.size() - 1 + theBounds.right.theIncomingSprings.size() - 1) / 2;
			theVTension = vTension * (theBounds.top.theOutgoingSprings.size() - 1 + theBounds.bottom.theIncomingSprings.size() - 1) / 2;
			// Then perform the normal layout operation with the additional degree of freedom
			// that the right and bottom bounds can be adjusted
			return adjustLayout(true);
		}

		private State<L> adjustLayout(boolean stretch) {
			if (!isInitialized) {
				isStretching = stretch;
				isInitialized = true;
				long start = DEBUG ? System.currentTimeMillis() : 0;
				init();
				if (DEBUG)
					System.out.println("Initialized in " + QommonsUtils.printTimeLength(System.currentTimeMillis() - start));
			} else if (isStretching != stretch) {
				isStretching = stretch;
				theBoundState.right.forceChanged();
				theBoundState.bottom.forceChanged();
			}
			long start = PRINT_SPEED ? System.currentTimeMillis() : 0;
			int maxMoves = MAX_TRIES * theLines.size();
			DivStateImpl toMove = theLinesByForce.peekFirst();
			int moves = 0;
			for (int i = 0; toMove != null && i < maxMoves; i++) {
				moves++;
				// Rather than move just one line at a time, we can try to move a larger set of adjacent lines
				// This is much more efficient in general.
				boolean signum = toMove.theTotalForce > 0;
				BetterList<DivStateImpl> linesByPosition = toMove.theDivider.isVertical ? theVLinesByPosition : theHLinesByPosition;
				// Find the min line to move
				ElementId minBound = toMove.thePositionSortedElement;
				CollectionElement<DivStateImpl> el = linesByPosition.getAdjacentElement(toMove.thePositionSortedElement, false);
				while (el != null) {
					End borderEnd = el.get().theDivider.borderEnd;
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
					End borderEnd = el.get().theDivider.borderEnd;
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
						if (adjustEdgeSet(toMove.theDivider.isVertical, min, max, stretch)) {
							moved = true;
							break outer;
						}
					}
				}
				if (!moved)
					toMove.stabilized();
				toMove = theLinesByForce.peekFirst();
			}
			if (PRINT_SPEED)
				System.out.println(moves + " moves in " + QommonsUtils.printTimeLength(System.currentTimeMillis() - start));
			return this;
		}

		private void init() {
			List<SpringStateImpl> dirtySprings = new LinkedList<>();
			if (theLines.isEmpty())
				return;
			theBoundState.left.setPosition(0);
			theBoundState.top.setPosition(0);
			if (theBounds.left.theOutgoingSprings != null)
				initConstraints(theBoundState.left, Ternian.TRUE, dirtySprings);
			if (theBounds.top.theOutgoingSprings != null)
				initConstraints(theBoundState.top, Ternian.TRUE, dirtySprings);
			for (DivStateImpl line : theLineStates.values()) {
				if (!line.isPositioned()) {
					line.setPosition(0);
					initConstraints(line, Ternian.NONE, dirtySprings);
				}
			}
			if (theBounds.right.theIncomingSprings != null)
				initConstraints(theBoundState.right, Ternian.FALSE, dirtySprings);
			if (theBounds.bottom.theIncomingSprings != null)
				initConstraints(theBoundState.bottom, Ternian.FALSE, dirtySprings);
			for (SpringStateImpl spring : dirtySprings)
				spring.recalculate();
			return;
		}

		private void initConstraints(DivStateImpl line, Ternian outgoing, List<SpringStateImpl> dirtySprings) {
			if (outgoing != Ternian.FALSE) {
				for (SpringStateImpl spring : line.theOutgoing.theSprings) {
					LayoutSpringEvaluator eval = spring.theSpring.theEvaluator;
					DivStateImpl dest = spring.theDest.theDivider;
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
					DivStateImpl src = spring.theSource.theDivider;
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
		private final List<SpringStateImpl> incomingSprings = new ArrayList<>();
		private final List<SpringStateImpl> outgoingSprings = new ArrayList<>();
		private final IntList incomingSpringDestOffsets = new IntList();
		private final IntList outgoingSpringSrcOffsets = new IntList();
		private final List<TensionAndSnap> incomingTensions = new ArrayList<>();
		private final List<TensionAndSnap> outgoingTensions = new ArrayList<>();
		private int lastCalculatedForces;
		private static final int MAX_BOUND_VALUE = 1000000;
		private static final boolean WITH_CUTOFF = false;

		private boolean adjustEdgeSet(boolean vertical, ElementId minBound, ElementId maxBound, boolean flexBounds) {
			BetterList<DivStateImpl> linesByPosition = vertical ? theVLinesByPosition : theHLinesByPosition;
			// Determine the set of springs that act upon the given set of lines as a whole,
			// i.e. the springs that have one end on a line in the set and the other end on a line not in the set
			float force = 0;
			int initPosition;
			CollectionElement<DivStateImpl> el = linesByPosition.getElement(minBound);
			initPosition = el.get().thePosition;
			while (true) {
				DivStateImpl line = el.get();
				for (SpringStateImpl spring : line.theIncoming.theSprings) {
					if (spring.theSource.theDivider.thePositionSortedElement.compareTo(minBound) < 0//
						|| spring.theSource.theDivider.thePositionSortedElement.compareTo(maxBound) > 0) {
						incomingSprings.add(spring);
						incomingSpringDestOffsets.add(spring.theDest.theDivider.thePosition - initPosition);
						incomingTensions.add(spring.theTension);
						force += spring.theTension.tension;
					}
				}
				for (SpringStateImpl spring : line.theOutgoing.theSprings) {
					if (spring.theDest.theDivider.thePositionSortedElement.compareTo(minBound) < 0//
						|| spring.theDest.theDivider.thePositionSortedElement.compareTo(maxBound) > 0) {
						outgoingSprings.add(spring);
						outgoingSpringSrcOffsets.add(spring.theSource.theDivider.thePosition - initPosition);
						outgoingTensions.add(spring.theTension);
						force -= spring.theTension.tension;
					}
				}
				if (el.getElementId().equals(maxBound))
					break;
				else
					el = linesByPosition.getAdjacentElement(el.getElementId(), true);
			}
			// Instead of finding the absolute best equilibrium for the line set, we'll stop doing work when the force on the line set
			// drops so that a different line set has more force and is therefore higher-priority
			float cutoffForce;
			if (WITH_CUTOFF) {
				float cf = 0;
				for (DivStateImpl line : theLinesByForce) {
					if (line.theDivider.isVertical != vertical || line.thePositionSortedElement.compareTo(minBound) < 0
						|| line.thePositionSortedElement.compareTo(maxBound) > 0) {
						cf = Math.abs(line.theTotalForce);
						break;
					}
				}
				cutoffForce = cf;
			} else
				cutoffForce = 0;
			if (DEBUG)
				System.out.println(
					"Moving " + minBound + (minBound.equals(maxBound) ? "" : "..." + maxBound) + " (" + force + "), cutoff=" + cutoffForce);
			int[] relPositions = new int[linesByPosition.getElementsBefore(maxBound) - linesByPosition.getElementsBefore(minBound) + 1];
			el = linesByPosition.getElement(minBound);
			initPosition = el.get().thePosition;
			for (int i = 1; i < relPositions.length; i++) {
				el = linesByPosition.getAdjacentElement(el.getElementId(), true);
				relPositions[i] = el.get().thePosition - initPosition;
			}
			lastCalculatedForces = initPosition;

			boolean hasLeadingForce, hasTrailingForce;
			int leadingMost, trailingMost;
			float leadingForce, trailingForce;
			if (force > 0) {
				leadingMost = initPosition;
				leadingForce = force;
				hasLeadingForce = true;
				if (flexBounds)
					trailingMost = MAX_BOUND_VALUE;
				else
					trailingMost = theBoundState.get(Orientation.of(vertical), End.trailing).getPosition()
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
				for (int i = 0; i < incomingSprings.size(); i++) {
					SpringStateImpl spring = incomingSprings.get(i);
					int springSign = (int) Math.signum(incomingTensions.get(i).tension);
					if (signum == springSign) {
						int snap = spring.theSource.theDivider.getPosition() + incomingTensions.get(i).snap
							- incomingSpringDestOffsets.get(i);
						if (signum > 0) {
							if (snap < nextSnap) {
								if (DEBUG)
									System.out.println("\t\tSnap to " + snap + " for " + spring + " " + incomingTensions.get(i));
								nextSnap = snap;
								if (nextSnap <= leadingMost)
									break stepping;
							}
						} else {
							if (snap > nextSnap) {
								if (DEBUG)
									System.out.println("\t\tSnap to " + snap + " for " + spring + " " + incomingTensions.get(i));
								nextSnap = snap;
								if (nextSnap >= trailingMost)
									break stepping;
							}
						}
					}
				}
				for (int i = 0; i < outgoingSprings.size(); i++) {
					SpringStateImpl spring = outgoingSprings.get(i);
					int springSign = (int) Math.signum(outgoingTensions.get(i).tension);
					if (springSign != 0 && signum != springSign) {
						int snap = spring.theDest.theDivider.getPosition() - outgoingTensions.get(i).snap - outgoingSpringSrcOffsets.get(i);
						if (signum > 0) {
							if (snap < nextSnap) {
								if (DEBUG)
									System.out.println("\t\tSnap to " + snap + " for " + spring + " " + outgoingTensions.get(i));
								nextSnap = snap;
								if (nextSnap <= leadingMost)
									break stepping;
							}
						} else {
							if (snap > nextSnap) {
								if (DEBUG)
									System.out.println("\t\tSnap to " + snap + " for " + spring + " " + outgoingTensions.get(i));
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
				if (Math.abs(force) < cutoffForce)
					break;
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
			if (trailingMost - leadingMost <= 1) {
				if (Math.abs(leadingForce) < Math.abs(trailingForce))
					position = leadingMost;
				else
					position = trailingMost;
			} else if (Math.abs(force) < cutoffForce) {
				if (DEBUG)
					System.out.println("\tcutoff (" + cutoffForce + ")");
				if (Math.abs(leadingForce) < Math.abs(trailingForce))
					position = leadingMost;
				else
					position = trailingMost;
				// Cutoff--there's another line with higher priority
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
					else if (Math.abs(f[0]) <= cutoffForce) {
						if (DEBUG)
							System.out.println("\t\tcutoff(" + cutoffForce + ")");
						return 0;
					} else
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
				if (DEBUG)
					System.out.println("\tMoving to " + position);
				if (lastCalculatedForces != position)
					computeAdjustedForce(position);
				// From here on out, we can't just use the backing position-sorted list to iterate,
				// because setting the positions here will affect the list's content
				// So we need to create a copy
				List<DivStateImpl> includedLines = new ArrayList<>(relPositions.length);
				el = linesByPosition.getElement(minBound);
				for (int i = 0; i < relPositions.length; i++) {
					includedLines.add(el.get());
					el = linesByPosition.getAdjacentElement(el.getElementId(), true);
				}
				// Set the positions
				for (int i = 0; i < includedLines.size(); i++)
					includedLines.get(i).setPosition(position + relPositions[i]);
				// Update spring tensions and forces on the lines outside the set
				for (int i = 0; i < incomingSprings.size(); i++) {
					SpringStateImpl spring = incomingSprings.get(i);
					TensionAndSnap newTension = incomingTensions.get(i);
					spring.theTension = newTension;
					if (spring.theSource.forceChanged())
						spring.theSource.theDivider.forceChanged();
				}
				for (int i = 0; i < outgoingSprings.size(); i++) {
					SpringStateImpl spring = outgoingSprings.get(i);
					TensionAndSnap newTension = outgoingTensions.get(i);
					spring.theTension = newTension;
					if (spring.theDest.forceChanged())
						spring.theDest.theDivider.forceChanged();
				}

				// Set the forces on the lines in the set
				for (DivStateImpl line : includedLines) {
					// Don't just use || here, because then if incoming force is changed, the outgoing forceChanged() won't be called
					boolean forceChanged = line.theIncoming.forceChanged();
					if (line.theOutgoing.forceChanged())
						forceChanged = true;
					if (forceChanged)
						line.forceChanged();
				}
			} else {
				if (DEBUG)
					System.out.println("\tStable");
			}
			// Reset "local variables" for the next adjustment
			incomingSprings.clear();
			outgoingSprings.clear();
			incomingSpringDestOffsets.clear();
			outgoingSpringSrcOffsets.clear();
			incomingTensions.clear();
			outgoingTensions.clear();
			lastCalculatedForces = -1;
			return modified;
		}

		private float computeAdjustedForce(int position) {
			lastCalculatedForces = position;
			float force = 0;
			for (int i = 0; i < incomingSprings.size(); i++) {
				SpringStateImpl spring = incomingSprings.get(i);
				int springDestPosition = position + incomingSpringDestOffsets.get(i);
				LayoutSpringEvaluator eval = spring.theSpring.theEvaluator;
				TensionAndSnap newTension;
				if (eval == BOUNDS_TENSION) {
					float tension = spring.getSource().getOrientation().isVertical() ? theVTension : theHTension;
					newTension = new TensionAndSnap(tension, tension > 0 ? 10000000 : 0);
				} else
					newTension = eval.getTension(springDestPosition - spring.theSource.theDivider.thePosition);
				incomingTensions.set(i, newTension);
				force += incomingTensions.get(i).tension;
			}
			for (int i = 0; i < outgoingSprings.size(); i++) {
				SpringStateImpl spring = outgoingSprings.get(i);
				int springSrcPosition = position + outgoingSpringSrcOffsets.get(i);
				LayoutSpringEvaluator eval = spring.theSpring.theEvaluator;
				TensionAndSnap newTension;
				if (eval == BOUNDS_TENSION) {
					float tension = spring.getSource().getOrientation().isVertical() ? theVTension : theHTension;
					newTension = new TensionAndSnap(tension, tension > 0 ? 10000000 : 0);
				} else
					newTension = eval.getTension(spring.theDest.theDivider.thePosition - springSrcPosition);
				outgoingTensions.set(i, newTension);
				force -= outgoingTensions.get(i).tension;
			}
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

	private class DivStateImpl implements DividerState {
		final LayoutStateImpl theLayoutState;
		final DivImpl theDivider;
		final SpringSet theIncoming;
		final SpringSet theOutgoing;

		int thePosition;
		float theTotalForce;

		private ElementId thePositionSortedElement;
		private ElementId theForceSortedElement;

		DivStateImpl(LayoutStateImpl layoutState, DivImpl divider) {
			theLayoutState = layoutState;
			theDivider = divider;
			theIncoming = new SpringSet(this, true);
			theOutgoing = new SpringSet(this, false);
		}

		void reset() {
			thePosition = 0;
			theTotalForce = 0;
			thePositionSortedElement = null;
			theForceSortedElement = null;
			theOutgoing.reset();
			theIncoming.theForce = 0;
		}

		void forceChanged() {
			theTotalForce = theIncoming.theForce + theOutgoing.theForce;
			boolean shouldRegisterForce;
			if (theTotalForce == 0)
				shouldRegisterForce = false;
			else if (theDivider.borderEnd == null)
				shouldRegisterForce = true;
			else if (theLayoutState.isStretching && theDivider.borderEnd == End.trailing)
				shouldRegisterForce = true;
			else
				shouldRegisterForce = false;
			if (theForceSortedElement != null) {
				if (!shouldRegisterForce) {
					theLayoutState.theLinesByForce.mutableElement(theForceSortedElement).remove();
					theForceSortedElement = null;
				} else {
					boolean belongs = true;
					CollectionElement<DivStateImpl> adj = theLayoutState.theLinesByForce.getAdjacentElement(theForceSortedElement, false);
					if (adj != null && Math.abs(adj.get().theTotalForce) < Math.abs(theTotalForce))
						belongs = false;
					if (belongs) {
						adj = theLayoutState.theLinesByForce.getAdjacentElement(theForceSortedElement, true);
						if (adj != null && Math.abs(adj.get().theTotalForce) > Math.abs(theTotalForce))
							belongs = false;
					}
					if (!belongs) {
						theLayoutState.theLinesByForce.mutableElement(theForceSortedElement).remove();
						theForceSortedElement = theLayoutState.theLinesByForce.addElement(this, false).getElementId();
					}
				}
			} else if (shouldRegisterForce)
				theForceSortedElement = theLayoutState.theLinesByForce.addElement(this, false).getElementId();
		}

		void stabilized() {
			if (theForceSortedElement != null) {
				theLayoutState.theLinesByForce.mutableElement(theForceSortedElement).remove();
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
			BetterList<DivStateImpl> linesByPosition = theDivider.isVertical ? theLayoutState.theVLinesByPosition
				: theLayoutState.theHLinesByPosition;
			if (thePositionSortedElement != null) {
				boolean belongs = true;
				CollectionElement<DivStateImpl> adj = linesByPosition.getAdjacentElement(thePositionSortedElement, false);
				if (adj != null && adj.get().thePosition > thePosition)
					belongs = false;
				if (belongs) {
					adj = linesByPosition.getAdjacentElement(thePositionSortedElement, true);
					if (adj != null && adj.get().thePosition < thePosition)
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
			return theDivider.getOrientation();
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
			return theDivider + ": " + thePosition + " (" + theTotalForce + ")";
		}
	}

	private class SpringSet {
		final DivStateImpl theDivider;
		private final boolean isIncoming;
		final List<SpringStateImpl> theSprings;
		float theForce;

		SpringSet(DivStateImpl divider, boolean incoming) {
			theDivider = divider;
			isIncoming = incoming;
			theSprings = new ArrayList<>((incoming ? divider.theDivider.theIncomingSprings : divider.theDivider.theOutgoingSprings).size());
			if (!incoming) {
				for (SpringImpl spring : divider.theDivider.theOutgoingSprings) {
					SpringStateImpl springState = new SpringStateImpl(this, spring);
					theDivider.theLayoutState.theSpringStates.put(spring, springState);
					theSprings.add(springState);
				}
			}
		}

		void fillIncoming() {
			for (SpringImpl spring : theDivider.theDivider.theIncomingSprings) {
				SpringStateImpl springState = theDivider.theLayoutState.theSpringStates.get(spring);
				springState.theDest = this;
				theSprings.add(springState);
			}
		}

		boolean forceChanged() {
			float totalForce = 0;
			for (SpringStateImpl springState : theSprings)
				totalForce += springState.theTension.tension;
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
		TensionAndSnap theTension;

		SpringStateImpl(SpringSet source, SpringImpl spring) {
			theSource = source;
			theSpring = spring;
			theTension = TensionAndSnap.ZERO;
		}

		void reset() {
			theTension = TensionAndSnap.ZERO;
		}

		boolean recalculate() {
			TensionAndSnap tas;
			if (theSpring.theEvaluator == BOUNDS_TENSION) {
				float tension = theSpring.theSource.getOrientation().isVertical() ? theSource.theDivider.theLayoutState.theVTension
					: theSource.theDivider.theLayoutState.theHTension;
				tas = new TensionAndSnap(tension, tension > 0 ? 10000000 : 0);
			} else
				tas = theSpring.theEvaluator
				.getTension(theDest.theDivider.thePosition - theSource.theDivider.thePosition);
			if (theTension.equals(tas.tension))
				return false;
			theTension = tas;
			if (theSource.forceChanged())
				theSource.theDivider.forceChanged();
			if (theDest.forceChanged())
				theDest.theDivider.forceChanged();
			return true;
		}

		@Override
		public DividerState getSource() {
			return theSource.theDivider;
		}

		@Override
		public DividerState getDest() {
			return theDest.theDivider;
		}

		@Override
		public float getTension() {
			return theTension.tension;
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

	private class BoxStateImpl implements BoxState {
		final DivStateImpl left;
		final DivStateImpl right;
		final DivStateImpl top;
		final DivStateImpl bottom;

		BoxStateImpl(LayoutStateImpl layoutState, DivStateImpl left, DivStateImpl right, DivStateImpl top, DivStateImpl bottom) {
			this.left = left;
			this.right = right;
			this.top = top;
			this.bottom = bottom;
		}

		@Override
		public DividerState getLeft() {
			return left;
		}

		@Override
		public DividerState getRight() {
			return right;
		}

		@Override
		public DividerState getTop() {
			return top;
		}

		@Override
		public DividerState getBottom() {
			return bottom;
		}
	}

	/**
	 * Debugs this class graphically for a layout scenario or two
	 *
	 * @param args Command-line arguments, ignored
	 */
	public static void main(String[] args) {
		DebugPlotter plotter = new DebugPlotter();
		Map<BoxState, Line2D[]> boxes = new HashMap<>(); // Each entry is 4 lines--left, right, top, bottom
		Map<SpringState, SpringHolder> springs = new HashMap<>();

		// First, a simple system with 2 boxes
		LayoutSolver<String> solver = new LayoutSolver<>();
		BoxDef box1 = solver.getOrCreateBox("b1 left", "b1 right", "b1 top", "b1 bottom");
		BoxDef box2 = solver.getOrCreateBox("b2 left", "b2 right", "b2 top", "b2 bottom");
		// Margins
		SpringDef leftMargin = solver.createSpring(solver.getBounds().getLeft(), box1.getLeft(),
			forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, 50)));
		SpringDef rightMargin = solver.createSpring(box2.getRight(), solver.getBounds().getRight(),
			forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, 50)));
		SpringDef topMargin1 = solver.createSpring(solver.getBounds().getTop(), box1.getTop(),
			forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, 50)));
		SpringDef topMargin2 = solver.createSpring(solver.getBounds().getTop(), box2.getTop(),
			forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, 50)));
		SpringDef bottomMargin1 = solver.createSpring(box1.getBottom(), solver.getBounds().getBottom(),
			forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, 50)));
		SpringDef bottomMargin2 = solver.createSpring(box2.getBottom(), solver.getBounds().getBottom(),
			forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, 50)));
		// Between the boxes
		SpringDef padding = solver.createSpring(box1.getRight(), box2.getLeft(), //
			forSizer(() -> 0, new SimpleSizeGuide(0, 5, 10, 15, 25)));
		// Now the box dimensions
		SpringDef w1 = solver.createSpring(box1.getLeft(), box1.getRight(), //
			forSizer(() -> 0, new SimpleSizeGuide(10, 50, 100, 150, 500)));
		SpringDef h1 = solver.createSpring(box1.getTop(), box1.getBottom(), //
			forSizer(() -> 0, new SimpleSizeGuide(10, 40, 80, 125, 400)));
		SpringDef w2 = solver.createSpring(box2.getLeft(), box2.getRight(), //
			forSizer(() -> 0, new SimpleSizeGuide(10, 30, 75, 100, 300)));
		SpringDef h2 = solver.createSpring(box2.getTop(), box2.getBottom(), //
			forSizer(() -> 0, new SimpleSizeGuide(10, 40, 80, 125, 400)));

		LayoutSolver.State<String> solverState = solver.use();
		BoxState box1State = solverState.getBox(box1);
		BoxState box2State = solverState.getBox(box2);

		addBox(plotter, boxes, solverState.getBounds(), "bounds");
		addBox(plotter, boxes, box1State, "box1");
		addBox(plotter, boxes, box2State, "box2");

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
					reSolve(solverState);
				}
			}
		}, "Re-solver");
		Thread updateRenderThread = new Thread(() -> {
			while (true) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
				if (updateShapes(boxes, springs))
					EventQueue.invokeLater(() -> plotter.repaint());
			}
		}, "Plot updater");
		updateRenderThread.start();
		reSolveThread.start();
		plotter.setDoubleClick(() -> printAll(boxes, springs));
		plotter.setRightClick(() -> reSolve[0] = true);
		reSolve[0] = true;
		updateShapes(boxes, springs);
	}

	private static void reSolve(State<String> solverState) {
		solverState.reset();
		// solverState.stretch(-MAX_TENSION, -MAX_TENSION);
		// solverState.stretch(-MAX_PREF_TENSION, -MAX_PREF_TENSION);
		// solverState.stretch(0, 0);
		// solverState.stretch(MAX_PREF_TENSION, MAX_PREF_TENSION);
		solverState.stretch(MAX_TENSION, MAX_TENSION);
		// solverState.stretch(plotter.getWidth(), plotter.getHeight());
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

	private static void addBox(DebugPlotter plotter, Map<BoxState, Line2D[]> boxes, BoxState box, String boxName) {
		Line2D[] lines = new Line2D[4];
		lines[0] = new Line2D.Float();
		lines[1] = new Line2D.Float();
		lines[2] = new Line2D.Float();
		lines[3] = new Line2D.Float();
		boxes.put(box, lines);
		plotter.add(lines[0]).setColor(Color.black)
			.setText(() -> boxName + " left: P " + box.getLeft().getPosition() + " F " + box.getLeft().getForce());
		plotter.add(lines[1]).setColor(Color.black)
			.setText(() -> boxName + " right: P" + box.getRight().getPosition() + " F " + box.getRight().getForce());
		plotter.add(lines[2]).setColor(Color.black)
			.setText(() -> boxName + " top: P " + box.getTop().getPosition() + " F " + box.getTop().getForce());
		plotter.add(lines[3]).setColor(Color.black)
			.setText(() -> boxName + " bottom: P " + box.getBottom().getPosition() + " F " + box.getBottom().getForce());
	}

	private static void addSpring(DebugPlotter plotter, Map<SpringState, SpringHolder> springs, SpringState spring, String springName,
		IntSupplier location) {
		Line2D line = new Line2D.Float();
		springs.put(spring, new SpringHolder(line, location));
		plotter.add(line).setColor(Color.blue).setText(() -> springName + ": L " + spring.getLength() + " T " + spring.getTension());
	}

	private static boolean updateShapes(Map<BoxState, Line2D[]> boxes, Map<SpringState, SpringHolder> springs) {
		boolean updated = false;
		for (Map.Entry<BoxState, Line2D[]> box : boxes.entrySet()) {
			double left = box.getKey().getLeft().getPosition();
			double right = box.getKey().getRight().getPosition();
			double top = box.getKey().getTop().getPosition();
			double bottom = box.getKey().getBottom().getPosition();
			if (box.getValue()[0].getX1() != left || box.getValue()[0].getY1() != top || box.getValue()[0].getY2() != bottom
				|| box.getValue()[1].getX1() != right) {
				updated = true;
				box.getValue()[0].setLine(left, top, left, bottom);
				box.getValue()[1].setLine(right, top, right, bottom);
				box.getValue()[2].setLine(left, top, right, top);
				box.getValue()[3].setLine(left, bottom, right, bottom);
			}
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

	private static void printAll(Map<BoxState, Line2D[]> boxes, Map<SpringState, SpringHolder> springs) {
		for (BoxDef box : boxes.keySet()) {
			System.out.println(box.getLeft());
			System.out.println(box.getRight());
			System.out.println(box.getTop());
			System.out.println(box.getBottom());
		}
		for (SpringDef spring : springs.keySet())
			System.out.println(spring);
	}
}
