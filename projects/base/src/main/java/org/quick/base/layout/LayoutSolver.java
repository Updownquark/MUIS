package org.quick.base.layout;

import java.awt.Dimension;
import java.util.*;

import org.qommons.BiTuple;
import org.qommons.collect.DefaultGraph;
import org.qommons.collect.Graph;
import org.qommons.collect.MutableGraph;
import org.quick.core.QuickElement;
import org.quick.core.layout.*;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * <p>
 * LayoutSolver is a utility that can be used to build an abstract version of a layout configuration with edges and constraints and solve
 * for the ideal (or close to ideal) position of the edges.
 * </p>
 *
 * <p>
 * This class may be used to back any layout which can be described as a set of edges related by springs, each defining a size for each
 * {@link LayoutGuideType size type}. Implementations of such layouts can be made trivial by use of this class.
 * </p>
 *
 * To initialize this class, edges and boxes (sets of 4 bounding edges) are added to the solver via:
 * <ul>
 * <li>{@link #edgeFor(String, Orientation)},</li>
 * <li>{@link #boxFor(Object)},</li>
 * <li>and {@link #boxForElement(QuickElement)}.</li>
 * </ul>
 * Then information is added to the solver to describe how the edges are associated with each other via:
 * <ul>
 * <li>{@link #constrain(Edge, Edge, LayoutEdgeLink)}</li>
 * <li>and {@link #depends(Edge, Edge, Edge, Edge)}</li>
 * </ul>
 * <p>
 * To obtain the {@link LayoutGuideType#min min}, {@link LayoutGuideType#minPref min-preferred}, {@link LayoutGuideType#pref preferred},
 * {@link LayoutGuideType#maxPref max-preferred}, or {@link LayoutGuideType#max max} size for the layout, the
 * {@link #forSizeType(LayoutGuideType)} method is used.
 * </p>
 *
 * <p>
 * To obtain the optimal edge position configuration for a particular layout size, the {@link #forLayoutSize(Dimension)} method is used.
 * </p>
 *
 * <p>
 * Either of these methods produces a {@link LayoutSystemSet} which can be {@link LayoutSystemSet#solve() solved} to produce a
 * {@link LayoutSolution}.
 * </p>
 *
 * <p>
 * The {@link LayoutSolution} can be queried via {@link LayoutSolution#getPosition(Edge)} for the desired results.
 * </p>
 *
 * TODO
 * <ul>
 * <li>Extract independent sub-graphs of constraints and replace with a single combination constraint (e.g. parallel or series spring)</li>
 * <li>How to initialize each LayoutSystem's edge positions?</li>
 * <li>Create outer algorithm for solving each system</li>
 * <li>Create inner algorithm for balancing edge forces</li>
 * <li>UNIT TESTS!!!</li>
 * </ul>
 */
public class LayoutSolver extends EdgeBox.SimpleEdgeBox {
	/**
	 * <p>
	 * Represents a set of edge positions for a layout.
	 * </p>
	 *
	 * <p>
	 * During computation for the layout, {@link #isInitialized(Edge)} should be used in conjunction with queries to determine if the edge
	 * has been initialized yet. After the solution is returned by {@link LayoutSystemSet#solve()}, all edges will have been initialized.
	 * </p>
	 */
	public interface LayoutSolution extends EdgeBox {
		boolean isInitialized(Edge edge);

		int getPosition(Edge edge);
	}

	public static class NamedEdge extends Edge.SimpleEdge {
		private final String theName;

		public NamedEdge(String name, Orientation orient) {
			super(orient);
			theName = name;
		}

		@Override
		public String toString() {
			return theName + " (" + getOrientation() + ")";
		}
	}

	/**
	 * Represents a constraint on the distance between 2 edges. The constraint may depend on the positions of other edges in the layout
	 * (these should be declared via {@link LayoutSolver#depends(Edge, Edge, Edge, Edge)}. This is not enforced, but may result in non-ideal
	 * solutions otherwise).
	 */
	public interface LayoutEdgeLink {
		void recalculate(LayoutSolution current);

		LayoutSpring getConstraint();

		LayoutEdgeLink copy();

		public static class SizeGuideEdgeLink implements LayoutEdgeLink {
			public final SizeGuide guide;
			public final BiTuple<Edge, Edge> crossEdges;
			private LayoutSpring.SizeGuideSpring theSpring;

			public SizeGuideEdgeLink(SizeGuide guide, Edge crossEdge1, Edge crossEdge2) {
				this.guide = guide;
				crossEdges = new BiTuple<>(crossEdge1, crossEdge2);
			}

			@Override
			public void recalculate(LayoutSolution current) {
				if (theSpring == null)
					theSpring = new LayoutSpring.SizeGuideSpring(guide);
				Orientation orient = crossEdges.getValue1().getOrientation().opposite();
				int layoutLength = current.getPosition(current.getBoundary(orient, End.trailing));
				int crossSize;
				boolean csAvailable = !current.isInitialized(crossEdges.getValue1());
				if (csAvailable)
					crossSize = current.getPosition(current.getBoundary(orient.opposite(), End.trailing));
				else
					crossSize = current.getPosition(crossEdges.getValue2()) - current.getPosition(crossEdges.getValue1());
				theSpring.recalculate(layoutLength, crossSize, csAvailable);
			}

			@Override
			public LayoutSpring getConstraint() {
				return theSpring;
			}

			@Override
			public SizeGuideEdgeLink copy() {
				return new SizeGuideEdgeLink(guide, crossEdges.getValue1(), crossEdges.getValue2());
			}
		}

		public static class SimpleSpringEdgeLink implements LayoutEdgeLink {
			public final LayoutSpring spring;

			public SimpleSpringEdgeLink(LayoutSpring spring) {
				this.spring = spring;
			}

			@Override
			public void recalculate(LayoutSolution current) {}

			@Override
			public LayoutSpring getConstraint() {
				return spring;
			}

			@Override
			public SimpleSpringEdgeLink copy() {
				return this; // Stateless
			}
		}
	}

	private static class EdgePair {
		final Edge edge1;
		final Edge edge2;

		EdgePair(Edge edge1, Edge edge2) {
			super();
			this.edge1 = edge1;
			this.edge2 = edge2;
		}

		@Override
		public int hashCode() {
			return edge1.hashCode() + edge2.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof EdgePair))
				return false;
			EdgePair other = (EdgePair) obj;
			if (edge1 == other.edge1 && edge2 == other.edge2)
				return true;
			else if (edge1 == other.edge2 && edge2.equals(other.edge1))
				return true;
			else
				return false;
		}

		@Override
		public String toString() {
			return edge1 + ", " + edge2;
		}
	}

	public static class LayoutSystem {
		private static abstract class LayoutEdgeState implements LayoutEdgeLink {
			float force;

			@Override
			public abstract LayoutEdgeState copy();
		}

		private static class SimpleEdgeState extends LayoutEdgeState {
			private final LayoutEdgeLink theLink;

			SimpleEdgeState(LayoutEdgeLink link) {
				theLink = link;
			}

			@Override
			public void recalculate(LayoutSolution current) {
				theLink.recalculate(current);
			}

			@Override
			public LayoutSpring getConstraint() {
				return theLink.getConstraint();
			}

			@Override
			public LayoutEdgeState copy() {
				return new SimpleEdgeState(theLink.copy());
			}
		}

		private static class SeriesEdgeState extends LayoutEdgeState {
			final Edge[] theEdges;
			final LayoutEdgeState[] theSeries;
			LayoutSpring evaluated;

			SeriesEdgeState(Edge[] edges, LayoutEdgeState[] series) {
				theEdges = edges;
				theSeries = series;
			}

			@Override
			public void recalculate(LayoutSolution current) {
				evaluated = null;
				for (LayoutEdgeState link : theSeries)
					link.recalculate(current);
			}

			@Override
			public LayoutSpring getConstraint() {
				if (evaluated == null) {
					LayoutSpring[] springs = new LayoutSpring[theSeries.length];
					for (int i = 0; i < springs.length; i++)
						springs[i] = theSeries[i].getConstraint();
					evaluated = new LayoutSpring.CachingSpring(new LayoutSpring.SeriesSpring(springs));
				}
				return evaluated;
			}

			@Override
			public SeriesEdgeState copy() {
				SeriesEdgeState ret = new SeriesEdgeState(theEdges, theSeries.clone());
				for (int i = 0; i < ret.theSeries.length; i++)
					ret.theSeries[i] = ret.theSeries[i].copy();
				return ret;
			}
		}

		private final LayoutSystemSet theSystemSet;
		private final Graph<Edge, LayoutEdgeState> theEdges;
		private final Map<Edge, float[]> theForces;
		private int theSolveCount;

		LayoutSystem(LayoutSystemSet systemSet, MutableGraph<Edge, LayoutEdgeLink> edges) {
			theSystemSet = systemSet;
			MutableGraph<Edge, LayoutEdgeState> edgeStates = new DefaultGraph<>();
			for (Graph.Node<Edge, LayoutEdgeLink> node : edges.getNodes())
				edgeStates.addNode(node.getValue());
			for (Graph.Edge<Edge, LayoutEdgeLink> edge : edges.getEdges())
				edgeStates.addEdge(edgeStates.nodeFor(edge.getStart().getValue()), edgeStates.nodeFor(edge.getEnd().getValue()),
					edge.isDirected(), new SimpleEdgeState(edge.getValue()));
			boolean modified;
			do {
				modified = false;

				// Search for series
				Iterator<? extends Graph.Node<Edge, LayoutEdgeState>> nodeIter = new ArrayList<>(edgeStates.getNodes()).iterator();
				while (nodeIter.hasNext()) {
					Graph.Node<Edge, LayoutEdgeState> node = nodeIter.next();
					Graph.Edge<Edge, LayoutEdgeState>[] nodeEdges = checkSeries(node);
					if (nodeEdges != null) {
						// Replace the edge-node-edge combination with a single series edge
						modified = true;
						nodeIter.remove();
						boolean series0 = nodeEdges[0].getValue() instanceof SeriesEdgeState;
						boolean series1 = nodeEdges[1].getValue() instanceof SeriesEdgeState;
						int componentLength = 0;
						componentLength += series0 ? ((SeriesEdgeState) nodeEdges[0].getValue()).theSeries.length : 1;
						componentLength += series1 ? ((SeriesEdgeState) nodeEdges[1].getValue()).theSeries.length : 1;
						LayoutEdgeState[] seriesStates = new LayoutEdgeState[componentLength];
						Edge[] seriesEdges = new Edge[componentLength - 1];
						componentLength = 0;
						if (series0) {
							SeriesEdgeState ses = (SeriesEdgeState) nodeEdges[0].getValue();
							int count = ses.theSeries.length;
							System.arraycopy(ses.theSeries, componentLength, seriesStates, 0, count);
							System.arraycopy(ses.theEdges, componentLength, seriesEdges, 0, count - 1);
							componentLength += count;
						} else
							seriesStates[componentLength++] = nodeEdges[0].getValue();
						seriesEdges[componentLength - 1] = node.getValue();
						if (series0) {
							SeriesEdgeState ses = (SeriesEdgeState) nodeEdges[1].getValue();
							int count = ses.theSeries.length;
							System.arraycopy(ses.theSeries, componentLength, seriesStates, 0, count);
							System.arraycopy(ses.theEdges, componentLength, seriesEdges, 0, count - 1);
							componentLength += count;
						} else
							seriesStates[componentLength++] = nodeEdges[1].getValue();
						SeriesEdgeState seriesEdge = new SeriesEdgeState(seriesEdges, seriesStates);
						edgeStates.addEdge(nodeEdges[0].getOtherEnd(node), nodeEdges[1].getOtherEnd(node), true, seriesEdge);
						edgeStates.removeNode(node);
					}

					// Search for parallels
					Iterator<? extends Graph.Edge<Edge, LayoutEdgeState>> edgeIter = new ArrayList<>(edgeStates.getEdges()).iterator();
					while (edgeIter.hasNext()) {
						Graph.Edge<Edge, LayoutEdgeState> edge = edgeIter.next();
						Graph.Edge<Edge, LayoutEdgeState>[] parallelEdges = checkParallel(edge);
						if (parallelEdges != null) {
							// TODO Replace the parallel edges with a single parallel edge
						}
					}
				}
			} while (modified);

			theEdges = edgeStates.immutable();
		}

		private Graph.Edge<Edge, LayoutEdgeState>[] checkSeries(Graph.Node<Edge, LayoutEdgeState> node) {
			if (node.getEdges().size() != 2)
				return null;
			Graph.Edge<Edge, LayoutEdgeState>[] nodeEdges = node.getEdges().toArray(new Graph.Edge[2]);
			if (nodeEdges[0].getOtherEnd(node) != nodeEdges[1].getOtherEnd(node))
				return nodeEdges;
			return null;
		}

		private Graph.Edge<Edge, LayoutEdgeState>[] checkParallel(Graph.Edge<Edge, LayoutEdgeState> edge) {
			// TODO
		}

		void initialize() {
			int todo = todo;
			// Set all edges to their preferred sizes
			// Calculate initial forces on all edges
		}

		void solve() {
			int todo = todo;
		}
	}

	public static class LayoutSystemSet {
		private final MutableGraph<LayoutSystem, EdgePair> theSystems;
		private final Map<Edge, LayoutSystem> theSystemsByEdge;
		final Map<Edge, int[]> thePositions;
		final LayoutSolution theSolution;

		LayoutSystemSet(LayoutSolver solver, LayoutGuideType sizeType) {
			this(solver);
			for (Graph.Node<LayoutSystem, EdgePair> system : theSystems.getNodes())
				system.getValue().initialize();
			int todo = todo; // Add tension to outer edges corresponding to size type
		}

		LayoutSystemSet(LayoutSolver solver, Dimension layoutSize) {
			this(solver);
			LayoutSystem hSystem = theSystemsByEdge.get(solver.getLeft());
			LayoutSystem vSystem = theSystemsByEdge.get(solver.getTop());
			((MutableGraph<Edge, LayoutEdgeLink>) hSystem.theEdges).addEdge(hSystem.theEdges.nodeFor(solver.getLeft()),
				hSystem.theEdges.nodeFor(solver.getRight()), true,
				new LayoutEdgeLink.SimpleSpringEdgeLink(new LayoutSpring.ConstSpring(layoutSize.width)));
			((MutableGraph<Edge, LayoutEdgeLink>) vSystem.theEdges).addEdge(hSystem.theEdges.nodeFor(solver.getTop()),
				hSystem.theEdges.nodeFor(solver.getBottom()), true,
				new LayoutEdgeLink.SimpleSpringEdgeLink(new LayoutSpring.ConstSpring(layoutSize.height)));
			for (Graph.Node<LayoutSystem, EdgePair> system : theSystems.getNodes())
				system.getValue().initialize();
		}

		private LayoutSystemSet(LayoutSolver solver) {
			theSystems = new DefaultGraph<>();
			thePositions = new HashMap<>();
			theSolution = new LayoutSolution() {
				@Override
				public Edge getBoundary(Orientation orient, End end) {
					return solver.getBoundary(orient, end);
				}

				@Override
				public int getPosition(Edge edge) {
					int[] pos = thePositions.get(edge);
					return pos == null ? -1 : pos[0];
				}

				@Override
				public boolean isInitialized(Edge edge) {
					return thePositions.containsKey(edge);
				}
			};
			theSystemsByEdge = new HashMap<>();

			// Derive independent sub-graphs in the layout--graphs which have no direct relationships between them
			Map<Edge, MutableGraph<Edge, LayoutEdgeLink>> subGraphsByEdge = new HashMap<>();
			List<MutableGraph<Edge, LayoutEdgeLink>> subGraphs = solver.theHSprings.split(node -> new DefaultGraph<>());
			for (MutableGraph<Edge, LayoutEdgeLink> subGraph : subGraphs) {
				for (Graph.Node<Edge, LayoutEdgeLink> edge : subGraph.getNodes())
					subGraphsByEdge.put(edge.getValue(), subGraph);
			}
			// Opposite edges of the overall layout must be in the same sub-graph
			MutableGraph<Edge, LayoutEdgeLink> leftSys = subGraphsByEdge.get(solver.getLeft());
			MutableGraph<Edge, LayoutEdgeLink> rightSys = subGraphsByEdge.get(solver.getRight());
			if (leftSys != rightSys) {
				leftSys.addAll(rightSys);
				subGraphs.remove(rightSys);
			}
			MutableGraph<Edge, LayoutEdgeLink> topSys = subGraphsByEdge.get(solver.getTop());
			MutableGraph<Edge, LayoutEdgeLink> bottomSys = subGraphsByEdge.get(solver.getBottom());
			if (topSys != bottomSys) {
				topSys.addAll(bottomSys);
				subGraphs.remove(bottomSys);
			}

			// Compile LayoutSystem graph
			for (MutableGraph<Edge, LayoutEdgeLink> subGraph : subGraphs) {
				LayoutSystem system = new LayoutSystem(this, subGraph);
				theSystems.addNode(system);
				for (Graph.Node<Edge, LayoutEdgeLink> node : subGraph.getNodes())
					theSystemsByEdge.put(node.getValue(), system);
			}
			for (Map.Entry<EdgePair, EdgePair> couples : solver.theDependencies.entrySet()) {
				LayoutSystem sys11 = theSystemsByEdge.get(couples.getKey().edge1);
				LayoutSystem sys21 = theSystemsByEdge.get(couples.getValue().edge1);
				if (theSystemsByEdge.get(couples.getKey().edge1) != theSystemsByEdge.get(couples.getKey().edge2)
					|| theSystemsByEdge.get(couples.getValue().edge1) != theSystemsByEdge.get(couples.getValue().edge2))
					throw new IllegalStateException(
						"Edges within same dimension in a dependency must be constrained together or be constrained to common edges");
				theSystems.addEdge(theSystems.nodeFor(sys11), theSystems.nodeFor(sys21), true, couples.getValue());
				theSystems.addEdge(theSystems.nodeFor(sys21), theSystems.nodeFor(sys11), true, couples.getValue());
			}
		}

		private void moveAll(MutableGraph<Edge, LayoutEdgeLink> fromSys, MutableGraph<Edge, LayoutEdgeLink> toSys) {
			for (Graph.Node<Edge, LayoutEdgeLink> edge : fromSys.getNodes())
				toSys.addNode(edge.getValue());
			for (Graph.Edge<Edge, LayoutEdgeLink> link : fromSys.getEdges())
				toSys.addEdge(toSys.nodeFor(link.getStart().getValue()), toSys.nodeFor(link.getEnd().getValue()), //
					link.isDirected(), link.getValue());
		}

		public LayoutSolution solve() {}
	}

	private final MutableGraph<Edge, LayoutEdgeLink> theHSprings;
	private final MutableGraph<Edge, LayoutEdgeLink> theVSprings;
	private final BiMap<EdgePair, EdgePair> theDependencies;
	private final Map<String, NamedEdge> theNamedEdges;
	private final Map<Object, ValueEdgeBox<?>> theValueBoxes;

	public LayoutSolver() {
		theHSprings = new DefaultGraph<>();
		theVSprings = new DefaultGraph<>();
		theDependencies = HashBiMap.create();
		theNamedEdges = new LinkedHashMap<>();
		theValueBoxes = new LinkedHashMap<>();

		for (Orientation orient : Orientation.values())
			for (End end : End.values())
				getEdgeLinks(orient).addNode(getBoundary(orient, end));
	}

	public <V> ValueEdgeBox<V> boxFor(V value) {
		return (ValueEdgeBox<V>) theValueBoxes.computeIfAbsent(value, this::addBoxFor);
	}

	private <V> ValueEdgeBox<V> addBoxFor(V value) {
		ValueEdgeBox<V> box = new ValueEdgeBox<>(value);
		theHSprings.addNode(box.getLeft());
		theHSprings.addNode(box.getRight());
		theVSprings.addNode(box.getTop());
		theVSprings.addNode(box.getBottom());
		return box;
	}

	public <V extends QuickElement> ValueEdgeBox<V> boxForElement(QuickElement element) {
		return (ValueEdgeBox<V>) theValueBoxes.computeIfAbsent(element, v -> addBoxForElement((QuickElement) v));
	}

	private <V extends QuickElement> ValueEdgeBox<V> addBoxForElement(V value) {
		ValueEdgeBox<V> box = new ValueEdgeBox<>(value);
		theHSprings.addNode(box.getLeft());
		theHSprings.addNode(box.getRight());
		theVSprings.addNode(box.getTop());
		theVSprings.addNode(box.getBottom());
		depends(box.getLeft(), box.getRight(), box.getTop(), box.getBottom());
		constrain(box.getLeft(), box.getRight(),
			new LayoutEdgeLink.SizeGuideEdgeLink(value.bounds().get(Orientation.horizontal).getGuide(), box.getTop(), box.getBottom()));
		constrain(box.getTop(), box.getBottom(),
			new LayoutEdgeLink.SizeGuideEdgeLink(value.bounds().get(Orientation.horizontal).getGuide(), box.getLeft(), box.getRight()));
		return box;
	}

	public NamedEdge edgeFor(String name, Orientation orient) {
		NamedEdge edge = theNamedEdges.computeIfAbsent(name, n -> {
			NamedEdge newEdge = new NamedEdge(n, orient);
			getEdgeLinks(orient).addNode(newEdge);
			return newEdge;
		});
		if (edge.getOrientation() != orient)
			throw new IllegalArgumentException("Edge " + name + " exists and is not oriented " + orient);
		return edge;
	}

	public LayoutSolver constrain(Edge from, Edge to, LayoutEdgeLink constraint) {
		if (from.getOrientation() != to.getOrientation())
			throw new IllegalArgumentException("Edges must have the same orientation place a constraint between them");
		MutableGraph<Edge, LayoutEdgeLink> springs = getEdgeLinks(from.getOrientation());
		Graph.Node<Edge, LayoutEdgeLink> fromNode = springs.nodeFor(from);
		Graph.Node<Edge, LayoutEdgeLink> toNode = springs.nodeFor(to);
		if (fromNode == null || toNode == null)
			throw new IllegalArgumentException("Only edges defined in this solver may be constrained");
		springs.addEdge(fromNode, toNode, false, constraint);
		return this;
	}

	public LayoutSolver depends(Edge edge11, Edge edge12, Edge edge21, Edge edge22) {
		if (edge11.getOrientation() != edge12.getOrientation() || edge21.getOrientation() != edge22.getOrientation()
			|| edge11.getOrientation() == edge21.getOrientation())
			throw new IllegalArgumentException(
				"Dependencies must be between 2 edges of one orientation and 2 edges of the opposite orientation");
		EdgePair hEdges, vEdges;
		if (edge11.getOrientation().isVertical()) {
			hEdges = new EdgePair(edge21, edge22);
			vEdges = new EdgePair(edge11, edge12);
		} else {
			hEdges = new EdgePair(edge11, edge12);
			vEdges = new EdgePair(edge21, edge22);
		}
		if (!theHSprings.getNodes().contains(hEdges.edge1))
			throw new IllegalArgumentException("Unrecognized horizontal edge: " + hEdges.edge1);
		if (!theHSprings.getNodes().contains(hEdges.edge2))
			throw new IllegalArgumentException("Unrecognized horizontal edge: " + hEdges.edge2);
		if (!theVSprings.getNodes().contains(vEdges.edge1))
			throw new IllegalArgumentException("Unrecognized vertical edge: " + vEdges.edge1);
		if (!theVSprings.getNodes().contains(vEdges.edge2))
			throw new IllegalArgumentException("Unrecognized vertical edge: " + vEdges.edge2);
		theDependencies.put(hEdges, vEdges);
		return this;
	}

	private MutableGraph<Edge, LayoutEdgeLink> getEdgeLinks(Orientation orient) {
		return orient.isVertical() ? theVSprings : theHSprings;
	}

	public LayoutSystemSet forSizeType(LayoutGuideType sizeType) {
		return new LayoutSystemSet(this, sizeType);
	}

	public LayoutSystemSet forLayoutSize(Dimension layoutSize) {
		return new LayoutSystemSet(this, layoutSize);
	}
}
