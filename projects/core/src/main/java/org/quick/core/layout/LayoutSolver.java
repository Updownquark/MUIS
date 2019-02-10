package org.quick.core.layout;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Line2D;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

import javax.swing.JFrame;

import org.qommons.ArrayUtils;
import org.qommons.Ternian;
import org.qommons.collect.BetterHashMap;
import org.qommons.collect.BetterList;
import org.qommons.collect.BetterMap;
import org.qommons.collect.CollectionElement;
import org.qommons.collect.ElementId;
import org.qommons.tree.SortedTreeList;
import org.quick.core.Dimension;
import org.quick.core.Point;
import org.quick.core.layout.LayoutSpringEvaluator.TensionAndSnap;
import org.quick.util.DebugPlotter;

public class LayoutSolver<L> {
	public interface Divider {
		Orientation getOrientation();

		int getPosition();

		float getForce();
	}

	public interface Spring {
		Divider getSource();

		Divider getDest();

		default int getLength() {
			return getDest().getPosition() - getSource().getPosition();
		}

		float getTension();
	}

	public interface Box {
		Divider getLeft();

		Divider getRight();

		Divider getTop();

		Divider getBottom();

		default Divider get(Orientation orientation, End end) {
			switch (orientation) {
			case horizontal:
				return end == End.leading ? getLeft() : getRight();
			default:
				return end == End.leading ? getTop() : getBottom();
			}
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
			public TensionAndSnap getTension(int length) {
				int cs = crossSize.getAsInt();
				int pref = sizer.getPreferred(cs, true);
				if (length < pref) {
					int minPref = sizer.getMinPreferred(cs, true);
					if (length >= minPref) {
						float tension = MAX_PREF_TENSION * (pref - length) / (pref - minPref);
						return new TensionAndSnap(tension, pref);
					} else {
						int min = sizer.getMin(cs, true);
						if (length == min)
							return new TensionAndSnap(MAX_TENSION, minPref);
						else if (length < min)
							return new TensionAndSnap(MAX_TENSION, min);
						else {
							float tension = MAX_PREF_TENSION + (MAX_TENSION - MAX_PREF_TENSION) * (minPref - length) / (minPref - min);
							return new TensionAndSnap(tension, minPref);
						}
					}
				} else if (length == pref) {
					return new TensionAndSnap(0, pref);
				} else {
					int maxPref = sizer.getMaxPreferred(cs, true);
					if (length <= maxPref) {
						float tension = -MAX_PREF_TENSION * (length - pref) / (maxPref - pref);
						return new TensionAndSnap(tension, pref);
					} else {
						int max = sizer.getMax(cs, true);
						if (length == max)
							return new TensionAndSnap(-MAX_TENSION, maxPref);
						else if (length > max)
							return new TensionAndSnap(-MAX_TENSION, max);
						else {
							float tension = -MAX_PREF_TENSION - (MAX_TENSION - MAX_PREF_TENSION) * (length - maxPref) / (max - maxPref);
							return new TensionAndSnap(tension, maxPref);
						}
					}
				}
			}
		};
	}

	private final int MAX_TRIES = 100; // TODO Should be static when not debugging

	private final BoxImpl theBounds = new BoxImpl(//
		new DivImpl((L) "Left Bound", false, End.leading), new DivImpl((L) "Right Bound", false, End.trailing), //
		new DivImpl((L) "Top Bound", true, End.leading), new DivImpl((L) "Bottom Bound", true, End.trailing)//
	);

	private final BetterMap<L, DivImpl> theLines;
	private final List<Collection<DivImpl>> theLineSets;
	private final BetterList<DivImpl> theLinesByForce;
	private boolean isSealed;
	private int theIteration;

	public LayoutSolver() {
		theLines = BetterHashMap.build().unsafe().buildMap();
		theLineSets = new LinkedList<>();
		theLinesByForce = new SortedTreeList<>(false,
			(div1, div2) -> -Float.compare(Math.abs(div1.theTotalForce), Math.abs(div2.theTotalForce)));
	}

	public Box getBounds() {
		return theBounds;
	}

	public Box getOrCreateBox(L left, L right, L top, L bottom) {
		return new BoxImpl(//
			(DivImpl) getOrCreateLine(left, false), (DivImpl) getOrCreateLine(right, false), //
			(DivImpl) getOrCreateLine(top, true), (DivImpl) getOrCreateLine(bottom, true));
	}

	public Divider getOrCreateLine(L lineObject, boolean vertical) {
		if (isSealed)
			throw new IllegalStateException("This solver cannot be altered");
		return theLines.computeIfAbsent(lineObject, lo -> new DivImpl(lineObject, vertical, null));
	}

	public Divider getLine(L lineObject) {
		return theLines.get(lineObject);
	}

	public Spring createSpring(Divider src, Divider dest, LayoutSpringEvaluator eval) {
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

	public void linkSprings(Spring spring1, Spring spring2) {
		if (isSealed)
			throw new IllegalStateException("This solver cannot be altered");
		if (spring1.getSource().getOrientation() == spring2.getSource().getOrientation())
			throw new IllegalArgumentException("Linked springs must have different orientations");
		((SpringImpl) spring1).affects((SpringImpl) spring2);
	}

	public LayoutSolver reset() {
		theBounds.left.reset();
		theBounds.right.reset();
		theBounds.top.reset();
		theBounds.bottom.reset();
		for (DivImpl line : theLines.values())
			line.reset();
		theLineSets.clear();
		theLinesByForce.clear();
		theIteration = 0;
		isSealed = false;
		return this;
	}

	public LayoutSolver solve(Dimension size) {
		theBounds.right.thePosition = size.width;
		theBounds.bottom.thePosition = size.height;
		List<SpringImpl> dirtySprings = new LinkedList<>();
		if (!isSealed) {
			isSealed = true;
			init(dirtySprings);
		}
		int moves = MAX_TRIES * theLines.size();
		DivImpl toMove = theLinesByForce.peekFirst();
		for (int i = 0; toMove != null && i < moves; i++) {
			adjust(toMove);
			toMove = theLinesByForce.peekFirst();
		}
		return this;
	}

	private void init(List<SpringImpl> dirtySprings) {
		theIteration++;
		theLineSets.clear();
		if (theLines.isEmpty())
			return;
		List<DivImpl> lines = new LinkedList<>();
		if (theBounds.left.theOutgoingSprings != null) {
			initConstraints(theBounds.left, Ternian.TRUE, false, lines, dirtySprings);
			if (!lines.isEmpty()) {
				theLineSets.add(lines);
				lines = new LinkedList<>();
			}
		}
		if (theBounds.top.theOutgoingSprings != null) {
			initConstraints(theBounds.top, Ternian.TRUE, false, lines, dirtySprings);
			if (!lines.isEmpty()) {
				theLineSets.add(lines);
				lines = new LinkedList<>();
			}
		}
		for (DivImpl line : theLines.values()) {
			if (line.lastChecked != theIteration) {
				line.setPosition(0);
				initConstraints(line, Ternian.NONE, false, lines, dirtySprings);
				theLineSets.add(lines);
				lines = new LinkedList<>();
			}
		}
		if (theBounds.right.theIncomingSprings != null) {
			initConstraints(theBounds.right, Ternian.FALSE, false, lines, dirtySprings);
			if (!lines.isEmpty()) {
				theLineSets.add(lines);
				lines = new LinkedList<>();
			}
		}
		if (theBounds.bottom.theIncomingSprings != null) {
			initConstraints(theBounds.bottom, Ternian.FALSE, false, lines, dirtySprings);
			if (!lines.isEmpty()) {
				theLineSets.add(lines);
				lines = new LinkedList<>();
			}
		}
		for (SpringImpl spring : dirtySprings)
			spring.recalculate();
		dirtySprings.clear();
		return;
	}

	private void initConstraints(DivImpl line, Ternian outgoing, boolean secondary, List<DivImpl> lines, List<SpringImpl> dirtySprings) {
		if (line.lastChecked == theIteration)
			return;
		line.lastChecked = theIteration;
		lines.add(line);
		if (outgoing != Ternian.FALSE && line.theOutgoingSprings != null) {
			for (SpringImpl spring : line.theOutgoingSprings) {
				DivImpl dest = spring.theDest;
				if (dest.lastChecked != theIteration) {
					if (dest.borderEnd != null) {
						dest.lastChecked = theIteration;
						lines.add(dest);
						dirtySprings.add(spring);
					} else if (dest.setPosition(line.thePosition + spring.theEvaluator.getSize(0)))
						initConstraints(dest, Ternian.TRUE, secondary, lines, dirtySprings);
				} else
					dirtySprings.add(spring);
				if (!secondary && spring.theLinkedSprings != null) {
					for (SpringImpl linked : spring.theLinkedSprings) {
						if (linked.theSource.borderEnd != null) {
						} else if (linked.theSource.lastChecked != theIteration) {
							lines.add(linked.theSource);
							linked.theSource.setPosition(0);
							initConstraints(linked.theSource, Ternian.FALSE, true, lines, dirtySprings);
						}
						if (linked.theDest.borderEnd != null) {
						} else if (linked.theDest.lastChecked != theIteration) {
							lines.add(linked.theDest);
							linked.theSource.setPosition(linked.theSource.thePosition + linked.theEvaluator.getSize(0));
							initConstraints(linked.theDest, Ternian.TRUE, true, lines, dirtySprings);
						}
					}
				}
			}
		}
		if (outgoing != Ternian.TRUE && line.theIncomingSprings != null) {
			for (SpringImpl spring : line.theIncomingSprings) {
				DivImpl src = spring.theSource;
				if (src.lastChecked != theIteration) {
					if (src.borderEnd != null) {
						src.lastChecked = theIteration;
						lines.add(src);
						dirtySprings.add(spring);
					} else if (src.setPosition(line.thePosition - spring.theEvaluator.getSize(0)))
						initConstraints(src, Ternian.FALSE, secondary, lines, dirtySprings);
				} else
					dirtySprings.add(spring);
				if (!secondary && spring.theLinkedSprings != null) {
					for (SpringImpl linked : spring.theLinkedSprings) {
						if (linked.theSource.borderEnd != null) {
						} else if (linked.theSource.lastChecked != theIteration) {
							lines.add(linked.theSource);
							linked.theSource.setPosition(0);
							initConstraints(linked.theSource, Ternian.FALSE, true, lines, dirtySprings);
						}
						if (linked.theDest.borderEnd != null) {
						} else if (linked.theDest.lastChecked != theIteration) {
							lines.add(linked.theDest);
							linked.theSource.setPosition(linked.theSource.thePosition + linked.theEvaluator.getSize(0));
							initConstraints(linked.theDest, Ternian.TRUE, true, lines, dirtySprings);
						}
					}
				}
			}
		}
	}

	private void adjust(DivImpl toMove) {
		float cutoffForce = theLinesByForce.size() == 1 ? 0 : Math.abs(theLinesByForce.get(1).theTotalForce);
		int leadingMost, trailingMost;
		float leadingForce, trailingForce;
		if (toMove.theTotalForce > 0) {
			leadingMost = toMove.thePosition;
			leadingForce = toMove.theTotalForce;
			trailingMost = theBounds.get(toMove.getOrientation(), End.trailing).getPosition();
			trailingForce = 0;
		} else {
			leadingMost = 0;
			leadingForce = 0;
			trailingMost = toMove.thePosition;
			trailingForce = -toMove.theTotalForce;
		}
		float force = toMove.theTotalForce;
		TensionAndSnap[] outgoingTensions, incomingTensions;
		if (toMove.theOutgoingSprings != null) {
			outgoingTensions = new TensionAndSnap[toMove.theOutgoingSprings.size()];
			int i = 0;
			for (SpringImpl spring : toMove.theOutgoingSprings)
				outgoingTensions[i++] = spring.theTension;
		} else
			outgoingTensions = null;
		if (toMove.theIncomingSprings != null) {
			incomingTensions = new TensionAndSnap[toMove.theIncomingSprings.size()];
			int i = 0;
			for (SpringImpl spring : toMove.theIncomingSprings)
				incomingTensions[i++] = spring.theTension;
		} else
			incomingTensions = null;
		boolean stepped = false;
		stepping: while (leadingMost < trailingMost - 1 && force != 0 && Math.abs(force) >= cutoffForce) {
			int nextSnap = force > 0 ? trailingMost : leadingMost;
			int signum = (int) Math.signum(force);
			if (outgoingTensions != null) {
				int i = 0;
				for (SpringImpl spring : toMove.theOutgoingSprings) {
					int springSign = (int) Math.signum(outgoingTensions[i].tension);
					if (springSign != 0 && signum != springSign) {
						int snap = spring.theDest.getPosition() - outgoingTensions[i].snap;
						if (signum > 0) {
							if (snap < nextSnap) {
								nextSnap = snap;
								if (nextSnap <= leadingMost)
									break stepping;
							}
						} else {
							if (snap > nextSnap) {
								nextSnap = snap;
								if (nextSnap >= trailingMost)
									break stepping;
							}
						}
					}
					i++;
				}
			}
			if (incomingTensions != null) {
				int i = 0;
				for (SpringImpl spring : toMove.theIncomingSprings) {
					int springSign = (int) Math.signum(incomingTensions[i].tension);
					if (signum == springSign) {
						int snap = spring.theSource.getPosition() + incomingTensions[i].snap;
						if (signum > 0) {
							if (snap < nextSnap) {
								nextSnap = snap;
								if (nextSnap <= leadingMost)
									break stepping;
							}
						} else {
							if (snap > nextSnap) {
								nextSnap = snap;
								if (nextSnap >= trailingMost)
									break stepping;
							}
						}
					}
					i++;
				}
			}
			if (nextSnap == trailingMost || nextSnap == leadingMost)
				break;
			// Re-compute forces
			force = 0;
			if (outgoingTensions != null) {
				int i = 0;
				for (SpringImpl spring : toMove.theOutgoingSprings) {
					outgoingTensions[i] = spring.theEvaluator.getTension(spring.theDest.thePosition - nextSnap);
					force -= outgoingTensions[i].tension;
					i++;
				}
			}
			if (incomingTensions != null) {
				int i = 0;
				for (SpringImpl spring : toMove.theIncomingSprings) {
					incomingTensions[i] = spring.theEvaluator.getTension(nextSnap - spring.theSource.thePosition);
					force += incomingTensions[i].tension;
					i++;
				}
			}
			if (force < 0) {
				trailingMost = nextSnap;
				trailingForce = force;
			} else {
				leadingMost = nextSnap;
				leadingForce = force;
			}
		}
		int position;
		if (force == 0 || Math.abs(force) < cutoffForce) {
			position = leadingMost;
		} else if (!stepped && leadingMost < trailingMost - 1) {
			// Steps can't help us at this point. Use binary search.
			float[] f = new float[1];
			int lead = leadingMost, trail = trailingMost;
			position = ArrayUtils.binarySearch(leadingMost, trailingMost, pos -> {
				if (pos == lead)
					return 1;
				else if (pos == trail)
					return -1;
				f[0] = 0;
				if (outgoingTensions != null) {
					int i = 0;
					for (SpringImpl spring : toMove.theOutgoingSprings) {
						outgoingTensions[i] = spring.theEvaluator.getTension(spring.theDest.thePosition - pos);
						f[0] -= outgoingTensions[i].tension;
						i++;
					}
				}
				if (incomingTensions != null) {
					int i = 0;
					for (SpringImpl spring : toMove.theIncomingSprings) {
						incomingTensions[i] = spring.theEvaluator.getTension(pos - spring.theSource.thePosition);
						f[0] += incomingTensions[i].tension;
						i++;
					}
				}
				if (f[0] == 0 || Math.abs(f[0]) <= cutoffForce)
					return 0;
				else
					return f[0] > 0 ? -1 : 1;
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
		} else if (leadingForce < trailingForce) {
			position = leadingMost;
			force = leadingForce;
		} else {
			position = trailingMost;
			force = trailingForce;
		}
		if (toMove.setPosition(position)) {
			toMove.setForce(force);
			if (outgoingTensions != null) {
				int i = 0;
				for (SpringImpl spring : toMove.theOutgoingSprings) {
					TensionAndSnap newTension = outgoingTensions[i];
					float diff = newTension.tension - spring.theTension.tension;
					spring.theTension = newTension;
					spring.theDest.setForce(spring.theDest.theTotalForce + diff);
					i++;
				}
			}
			if (incomingTensions != null) {
				int i = 0;
				for (SpringImpl spring : toMove.theIncomingSprings) {
					TensionAndSnap newTension = incomingTensions[i];
					float diff = newTension.tension - spring.theTension.tension;
					spring.theTension = newTension;
					spring.theSource.setForce(spring.theSource.theTotalForce - diff);
					i++;
				}
			}
		} else {
			toMove.stabilized();
		}
	}

	private class SpringSet implements Iterable<SpringImpl> {
		private List<SpringImpl> theSprings;
		float theForce;

		SpringSet() {}

		void add(SpringImpl spring) {
			if (theSprings == null)
				theSprings = new LinkedList<>();
			theSprings.add(spring);
		}

		void adjustForce(float diff) {
			theForce += diff;
		}

		void reset() {
			theForce = 0;
			if (theSprings != null) {
				for (SpringImpl spring : theSprings)
					spring.reset();
			}
		}

		@Override
		public Iterator<SpringImpl> iterator() {
			if (theSprings == null)
				return Collections.emptyIterator();
			else
				return theSprings.iterator();
		}
	}

	private class DivImpl implements Divider {
		private final L theValue;
		private final boolean isVertical;
		final End borderEnd;
		private final SpringSet theOutgoingSprings;
		private final SpringSet theIncomingSprings;
		private ElementId theForceSortedElement;
		int thePosition;
		float theTotalForce;
		int lastChecked;

		DivImpl(L value, boolean vertical, End borderEnd) {
			theValue = value;
			isVertical = vertical;
			this.borderEnd = borderEnd;
			theOutgoingSprings = new SpringSet();
			theIncomingSprings = new SpringSet();
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

		void reset() {
			lastChecked = 0;
			thePosition = 0;
			theTotalForce = 0;
			theForceSortedElement = null;
			theOutgoingSprings.reset();
			theIncomingSprings.theForce = 0;
		}

		boolean setPosition(int position) {
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
			return theTotalForce;
		}

		void adjustForce(boolean outgoing, float forceDiff) {
			if (outgoing) {
				theOutgoingSprings.adjustForce(forceDiff);
				theTotalForce -= forceDiff;
			} else {
				theIncomingSprings.adjustForce(-forceDiff);
				theTotalForce += forceDiff;
			}
			theTotalForce = forceDiff;
			if (theForceSortedElement != null) {
				if (forceDiff == 0) {
					theLinesByForce.mutableElement(theForceSortedElement).remove();
					theForceSortedElement = null;
				} else {
					boolean belongs = true;
					CollectionElement<DivImpl> adj = theLinesByForce.getAdjacentElement(theForceSortedElement, false);
					if (adj != null && Math.abs(adj.get().theTotalForce) < Math.abs(forceDiff))
						belongs = false;
					if (belongs) {
						adj = theLinesByForce.getAdjacentElement(theForceSortedElement, true);
						if (adj != null && Math.abs(adj.get().theTotalForce) > Math.abs(forceDiff))
							belongs = false;
					}
					if (!belongs) {
						theLinesByForce.mutableElement(theForceSortedElement).remove();
						theForceSortedElement = theLinesByForce.addElement(this, false).getElementId();
					}
				}
			} else if (borderEnd == null && forceDiff != 0) {
				theForceSortedElement = theLinesByForce.addElement(this, false).getElementId();
			}
		}

		void stabilized() {
			if (theForceSortedElement != null) {
				theLinesByForce.mutableElement(theForceSortedElement).remove();
				theForceSortedElement = null;
			}
		}

		@Override
		public String toString() {
			return String.valueOf(theValue) + ": " + thePosition + " (" + theTotalForce + ")";
		}
	}

	private class SpringImpl implements Spring {
		private final DivImpl theSource;
		private final DivImpl theDest;
		private final LayoutSpringEvaluator theEvaluator;
		private List<SpringImpl> theLinkedSprings;
		TensionAndSnap theTension;

		SpringImpl(DivImpl source, DivImpl dest, LayoutSpringEvaluator evaluator) {
			theSource = source;
			theDest = dest;
			theEvaluator = evaluator;
			theTension = TensionAndSnap.ZERO;
		}

		void affects(SpringImpl other) {
			if (theLinkedSprings == null)
				theLinkedSprings = new LinkedList<>();
			theLinkedSprings.add(other);
			if (other.theLinkedSprings == null)
				other.theLinkedSprings = new LinkedList<>();
			other.theLinkedSprings.add(this);
		}

		void reset() {
			theTension = TensionAndSnap.ZERO;
		}

		boolean recalculate() {
			LayoutSpringEvaluator.TensionAndSnap tas = theEvaluator.getTension(theDest.thePosition - theSource.thePosition);
			if (theTension.equals(tas.tension))
				return false;
			float tensionDiff = tas.tension - theTension.tension;
			theSource.setForce(theSource.theTotalForce - tensionDiff);
			theDest.setForce(theDest.theTotalForce + tensionDiff);
			theTension = tas;
			return true;
		}

		@Override
		public Divider getSource() {
			return theSource;
		}

		@Override
		public Divider getDest() {
			return theDest;
		}

		@Override
		public float getTension() {
			return theTension.tension;
		}

		@Override
		public String toString() {
			return theSource.theValue + "->" + theDest.theValue + ": " + theTension;
		}
	}

	private class BoxImpl implements Box {
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

	public static void main(String[] args) {
		DebugPlotter plotter = new DebugPlotter();
		Map<Box, Line2D[]> boxes = new HashMap<>(); // Each entry is 4 lines--left, right, top, bottom
		Map<Spring, SpringHolder> springs = new HashMap<>();

		// First, a simple system with 2 boxes
		LayoutSolver<String> solver = new LayoutSolver<>();
		Box box1 = solver.getOrCreateBox("b1 left", "b1 right", "b1 top", "b1 bottom");
		Box box2 = solver.getOrCreateBox("b2 left", "b2 right", "b2 top", "b2 bottom");
		addBox(plotter, boxes, solver.getBounds(), "bounds");
		addBox(plotter, boxes, box1, "box1");
		addBox(plotter, boxes, box2, "box2");
		// Margins
		Spring leftMargin = solver.createSpring(solver.getBounds().getLeft(), box1.getLeft(),
			forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, Integer.MAX_VALUE)));
		addSpring(plotter, springs, leftMargin, "left margin", () -> box1.getCenter().y);
		Spring rightMargin = solver.createSpring(box2.getRight(), solver.getBounds().getRight(),
			forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, Integer.MAX_VALUE)));
		addSpring(plotter, springs, rightMargin, "right margin", () -> box2.getCenter().y);
		Spring topMargin1 = solver.createSpring(solver.getBounds().getTop(), box1.getTop(),
			forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, Integer.MAX_VALUE)));
		addSpring(plotter, springs, topMargin1, "top margin 1", () -> box1.getCenter().x);
		Spring topMargin2 = solver.createSpring(solver.getBounds().getTop(), box2.getTop(),
			forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, Integer.MAX_VALUE)));
		addSpring(plotter, springs, topMargin2, "top margin 2", () -> box2.getCenter().x);
		Spring bottomMargin1 = solver.createSpring(box1.getBottom(), solver.getBounds().getBottom(),
			forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, Integer.MAX_VALUE)));
		addSpring(plotter, springs, bottomMargin1, "bottom margin 1", () -> box1.getCenter().x);
		Spring bottomMargin2 = solver.createSpring(box2.getBottom(), solver.getBounds().getBottom(),
			forSizer(() -> 0, new SimpleSizeGuide(0, 3, 3, 3, Integer.MAX_VALUE)));
		addSpring(plotter, springs, bottomMargin2, "bottom margin 2", () -> box2.getCenter().x);
		// Between the boxes
		Spring padding = solver.createSpring(box1.getRight(), box2.getLeft(), //
			forSizer(() -> 0, new SimpleSizeGuide(0, 5, 10, 15, Integer.MAX_VALUE)));
		addSpring(plotter, springs, padding, "padding", () -> box1.getCenter().y);
		// Now the box dimensions
		Spring w1 = solver.createSpring(box1.getLeft(), box1.getRight(), //
			forSizer(() -> 0, new SimpleSizeGuide(10, 50, 100, 150, 500)));
		addSpring(plotter, springs, w1, "box 1 width", () -> box1.getCenter().y);
		Spring h1 = solver.createSpring(box1.getTop(), box1.getBottom(), //
			forSizer(() -> 0, new SimpleSizeGuide(10, 40, 80, 125, 400)));
		addSpring(plotter, springs, h1, "box 1 height", () -> box1.getCenter().x);
		Spring w2 = solver.createSpring(box2.getLeft(), box2.getRight(), //
			forSizer(() -> 0, new SimpleSizeGuide(10, 30, 75, 100, 300)));
		addSpring(plotter, springs, w2, "box 2 width", () -> box2.getCenter().y);
		Spring h2 = solver.createSpring(box2.getTop(), box2.getBottom(), //
			forSizer(() -> 0, new SimpleSizeGuide(10, 40, 80, 125, 400)));
		addSpring(plotter, springs, h2, "box 2 height", () -> box2.getCenter().x);

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
					// solver.reset();
					solver.solve(//
						Dimension.fromAWT(plotter.getSize()));
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
		plotter.setAction(() -> printAll(boxes, springs));
		solver.solve(Dimension.fromAWT(plotter.getSize()));
		updateShapes(boxes, springs);
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

	private static void addBox(DebugPlotter plotter, Map<Box, Line2D[]> boxes, Box box, String boxName) {
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

	private static void addSpring(DebugPlotter plotter, Map<Spring, SpringHolder> springs, Spring spring, String springName,
		IntSupplier location) {
		Line2D line = new Line2D.Float();
		springs.put(spring, new SpringHolder(line, location));
		plotter.add(line).setColor(Color.blue).setText(() -> springName + ": L " + spring.getLength() + " T " + spring.getTension());
	}

	private static boolean updateShapes(Map<Box, Line2D[]> boxes, Map<Spring, SpringHolder> springs) {
		boolean updated = false;
		for (Map.Entry<Box, Line2D[]> box : boxes.entrySet()) {
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
			for (Map.Entry<Spring, SpringHolder> spring : springs.entrySet()) {
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

	private static void printAll(Map<Box, Line2D[]> boxes, Map<Spring, SpringHolder> springs) {
		for (Box box : boxes.keySet()) {
			System.out.println(box.getLeft());
			System.out.println(box.getRight());
			System.out.println(box.getTop());
			System.out.println(box.getBottom());
		}
		for (Spring spring : springs.keySet())
			System.out.println(spring);
	}
}
