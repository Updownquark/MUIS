package org.quick.base.layout;

import java.awt.Dimension;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.observe.collect.ObservableList;
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
	public interface LayoutEdgeLink extends LayoutSpring {
		boolean recalculate(LayoutSolution current);

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
			public boolean recalculate(LayoutSolution current) {
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
				return theSpring.recalculate(layoutLength, crossSize, csAvailable);
			}

			@Override
			public int get(LayoutGuideType type) {
				return theSpring.get(type);
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
			public boolean recalculate(LayoutSolution current) {
				return false;
			}

			@Override
			public int get(LayoutGuideType type) {
				return spring.get(type);
			}

			@Override
			public SimpleSpringEdgeLink copy() {
				return this; // Stateless
			}
		}

		public static abstract class ComponentizedEdgeLink implements LayoutEdgeLink {
			private final List<? extends LayoutEdgeLink> theComponents;
			private final LayoutSpring.CompositeSpring theSpring;

			public ComponentizedEdgeLink(List<? extends LayoutEdgeLink> components) {
				if (components.getClass().getSimpleName().contains("Unmodifiable")
					&& !components.getClass().getSimpleName().contains("Immutable"))
					components = Collections.unmodifiableList(components);
				theComponents = components;
				theSpring = aggregate(theComponents);
			}

			public List<? extends LayoutEdgeLink> getComponents() {
				return theComponents;
			}

			protected abstract LayoutSpring.CompositeSpring aggregate(List<? extends LayoutEdgeLink> components);

			@Override
			public boolean recalculate(LayoutSolution current) {
				boolean recalculated = false;
				for (LayoutEdgeLink link : theComponents)
					recalculated |= link.recalculate(current);
				if (recalculated)
					theSpring.clearCache();
				return recalculated;
			}

			@Override
			public int get(LayoutGuideType type) {
				return theSpring.get(type);
			}
		}

		public static class SeriesEdgeLink extends ComponentizedEdgeLink {
			public SeriesEdgeLink(List<? extends LayoutEdgeLink> components) {
				super(components);
			}

			@Override
			protected LayoutSpring.SeriesSpring aggregate(List<? extends LayoutEdgeLink> components) {
				return new LayoutSpring.SeriesSpring(components);
			}

			@Override
			public SeriesEdgeLink copy() {
				return new SeriesEdgeLink(map(getComponents(), c -> c.copy()));
			}
		}

		public static class ParallelEdgeLink extends ComponentizedEdgeLink {
			public ParallelEdgeLink(List<? extends LayoutEdgeLink> components) {
				super(components);
			}

			@Override
			protected LayoutSpring.ParallelSpring aggregate(List<? extends LayoutEdgeLink> components) {
				return new LayoutSpring.ParallelSpring(components);
			}

			@Override
			public SeriesEdgeLink copy() {
				return new SeriesEdgeLink(map(getComponents(), c -> c.copy()));
			}
		}
	}

	private static <T, V> List<V> map(List<? extends T> list, Function<? super T, V> map) {
		if (list instanceof ObservableList) // TODO Change to Qollection when ready
			return ((ObservableList<? extends T>) list).map(map);
		else
			return list.stream().map(map).collect(Collectors.toList());
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

	private static class LayoutSolutionImpl extends EdgeBox.SimpleEdgeBox implements LayoutSolution {
		private final Map<Edge, int[]> thePositions;
		private final Map<Edge, float[]> theForces;

		LayoutSolutionImpl() {
			thePositions = new HashMap<>();
			theForces = new HashMap<>();
		}

		@Override
		public boolean isInitialized(Edge edge) {
		}

		@Override
		public int getPosition(Edge edge) {
			return thePositions.get(edge)[0];
		}
	}

	public static class LayoutSystem {
		public static abstract class CompiledLayoutEdge {
			private final LayoutEdgeLink theLink;
			private int theLength;

			CompiledLayoutEdge(LayoutEdgeLink link) {
				theLink = link;
			}

			public LayoutEdgeLink getLink() {
				return theLink;
			}

			boolean recalculate(LayoutSolutionImpl solution) {
				return theLink.recalculate(solution);
			}
		}

		private static class SimpleEdge extends CompiledLayoutEdge {
			SimpleEdge(LayoutEdgeLink link) {
				super(link);
			}
		}

		private static class SeriesEdge extends CompiledLayoutEdge {
			private final List<Edge> theEdges;
			private final List<CompiledLayoutEdge> theComponents;
			private int theCachedLength;

			SeriesEdge(List<Edge> edges, List<CompiledLayoutEdge> series) {
				super(new LayoutEdgeLink.SeriesEdgeLink(map(series, edge -> edge.getLink())));
				theEdges = edges;
				theComponents = series;
				theCachedLength = -1;
			}

			@Override
			boolean recalculate(LayoutSolutionImpl current) {
				boolean recalculated = super.recalculate(current);
				if (recalculated) {
					for (CompiledLayoutEdge link : theComponents)
						link.recalculate(current);
				}
				int length = current.getPosition(theEdges.get(theEdges.size() - 1)) - current.getPosition(theEdges.get(0));
				if (recalculated || length != theCachedLength) {
					theCachedLength = length;
					// TODO calculate positions for internal edges, insert into solution
				}
				return recalculated;
			}
		}

		private static class ParallelEdgeState extends CompiledLayoutEdge {
			final List<CompiledLayoutEdge> theComponents;

			ParallelEdgeState(List<CompiledLayoutEdge> series) {
				super(new LayoutEdgeLink.ParallelEdgeLink(map(series, edge -> edge.getLink())));
				theComponents = series;
			}

			@Override
			boolean recalculate(LayoutSolutionImpl current) {
				boolean recalculated = super.recalculate(current);
				if (recalculated) {
					for (CompiledLayoutEdge link : theComponents)
						link.recalculate(current);
				}
				return recalculated;
			}
		}

		private final Orientation theOrientation;
		private final LayoutSystemSet theSystemSet;
		private final Graph<Edge, CompiledLayoutEdge> theEdges;

		LayoutSystem(LayoutSystemSet systemSet, MutableGraph<Edge, LayoutEdgeLink> edges) {
			theSystemSet = systemSet;
			theOrientation = edges.getNodes().iterator().next().getValue().getOrientation();
			MutableGraph<Edge, CompiledLayoutEdge> edgeStates = new DefaultGraph<>();
			for (Graph.Node<Edge, LayoutEdgeLink> node : edges.getNodes())
				edgeStates.addNode(node.getValue());
			for (Graph.Edge<Edge, LayoutEdgeLink> edge : edges.getEdges())
				edgeStates.addEdge(edgeStates.nodeFor(edge.getStart().getValue()), edgeStates.nodeFor(edge.getEnd().getValue()),
					edge.isDirected(), new SimpleEdge(edge.getValue()));
			boolean modified;
			do {
				modified = false;

				// Search for series
				Iterator<? extends Graph.Node<Edge, CompiledLayoutEdge>> nodeIter = new ArrayList<>(edgeStates.getNodes()).iterator();
				while (nodeIter.hasNext()) {
					Graph.Node<Edge, CompiledLayoutEdge> node = nodeIter.next();
					Graph.Edge<Edge, CompiledLayoutEdge>[] nodeEdges = checkSeries(node);
					if (nodeEdges != null) {
						// Replace the edge-node-edge combination with a single series edge
						modified = true;
						nodeIter.remove();
						List<Edge> seriesEdges=new ArrayList<>();
						List<CompiledLayoutEdge> seriesStates = new ArrayList<>();
						if (nodeEdges[0].getValue() instanceof SeriesEdge) {
							SeriesEdge ses = (SeriesEdge) nodeEdges[0].getValue();
							seriesEdges.addAll(ses.theEdges);
							seriesStates.addAll(ses.theComponents);
						} else
							seriesStates.add(nodeEdges[0].getValue());
						seriesEdges.add(node.getValue());
						if (nodeEdges[1].getValue() instanceof SeriesEdge) {
							SeriesEdge ses = (SeriesEdge) nodeEdges[1].getValue();
							seriesEdges.addAll(ses.theEdges);
							seriesStates.addAll(ses.theComponents);
						} else
							seriesStates.add(nodeEdges[1].getValue());
						SeriesEdge seriesEdge = new SeriesEdge(seriesEdges, seriesStates);
						edgeStates.addEdge(nodeEdges[0].getOtherEnd(node), nodeEdges[1].getOtherEnd(node), true, seriesEdge);
						edgeStates.removeNode(node);
					}

					// Search for parallels
					List<Graph.Edge<Edge, CompiledLayoutEdge>> allEdges = new ArrayList<>(edgeStates.getEdges());
					Iterator<? extends Graph.Edge<Edge, CompiledLayoutEdge>> edgeIter = allEdges.iterator();
					while (edgeIter.hasNext()) {
						Graph.Edge<Edge, CompiledLayoutEdge> edge = edgeIter.next();
						List<Graph.Edge<Edge, CompiledLayoutEdge>> parallelEdges = checkParallel(edge);
						if (parallelEdges != null) {
							modified=true;
							List<CompiledLayoutEdge> parallelStates = new ArrayList<>();
							for (Graph.Edge<Edge, CompiledLayoutEdge> parallelEdge : parallelEdges) {
								if (parallelEdge.getValue() instanceof ParallelEdgeState) {
									parallelStates.addAll(((ParallelEdgeState) parallelEdge.getValue()).theComponents);
								} else
									parallelStates.add(parallelEdge.getValue());
								edgeStates.removeEdge(parallelEdge);
							}
							edgeStates.addEdge(edge.getStart(), edge.getEnd(), false, new ParallelEdgeState(parallelStates));
							allEdges.removeAll(parallelEdges);
							edgeIter = allEdges.iterator();
						}
					}
				}
			} while (modified);

			theEdges = edgeStates.immutable();
		}

		public LayoutSystemSet getSystemSet() {
			return theSystemSet;
		}

		public Orientation getOrientation() {
			return theOrientation;
		}

		private Graph.Edge<Edge, CompiledLayoutEdge>[] checkSeries(Graph.Node<Edge, CompiledLayoutEdge> node) {
			if (node.getEdges().size() != 2)
				return null;
			Graph.Edge<Edge, CompiledLayoutEdge>[] nodeEdges = node.getEdges().toArray(new Graph.Edge[2]);
			if (nodeEdges[0].getOtherEnd(node) != nodeEdges[1].getOtherEnd(node))
				return nodeEdges;
			return null;
		}

		private List<Graph.Edge<Edge, CompiledLayoutEdge>> checkParallel(Graph.Edge<Edge, CompiledLayoutEdge> edge) {
			List<Graph.Edge<Edge, CompiledLayoutEdge>> parallelEdges = null;
			for (Graph.Edge<Edge, CompiledLayoutEdge> fromEdge : edge.getStart().getEdges()) {
				if (fromEdge != edge && fromEdge.getOtherEnd(edge.getStart()) == edge.getEnd()) {
					if (parallelEdges == null) {
						parallelEdges = new ArrayList<>(4);
						parallelEdges.add(edge);
					}
					parallelEdges.add(fromEdge);
				}
			}
			return parallelEdges;
		}

		void initialize(LayoutSolutionImpl solution, Orientation primary, int layoutLength, int crossLength, boolean crossAvailable) {
			int todo = todo;
			// Set all edges to their preferred sizes
			// Calculate initial forces on all edges
		}

		void solve(LayoutSolutionImpl solution) {
			int todo = todo;
		}
	}

	public static class LayoutSystemSet {
		private final EdgeBox theBounds;
		private final Graph<LayoutSystem, EdgePair> theSystems;
		private final Map<Edge, LayoutSystem> theSystemsByEdge;

		private LayoutSystemSet(LayoutSolver solver) {
			theBounds = solver;

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
			MutableGraph<LayoutSystem, EdgePair> systems = new DefaultGraph<>();
			Map<Edge, LayoutSystem> systemsByEdge = new HashMap<>();
			for (MutableGraph<Edge, LayoutEdgeLink> subGraph : subGraphs) {
				LayoutSystem system = new LayoutSystem(this, subGraph);
				systems.addNode(system);
				for (Graph.Node<Edge, LayoutEdgeLink> node : subGraph.getNodes())
					systemsByEdge.put(node.getValue(), system);
			}
			for (Map.Entry<EdgePair, EdgePair> couples : solver.theDependencies.entrySet()) {
				LayoutSystem sys11 = systemsByEdge.get(couples.getKey().edge1);
				LayoutSystem sys21 = systemsByEdge.get(couples.getValue().edge1);
				if (systemsByEdge.get(couples.getKey().edge1) != systemsByEdge.get(couples.getKey().edge2)
					|| systemsByEdge.get(couples.getValue().edge1) != systemsByEdge.get(couples.getValue().edge2))
					throw new IllegalStateException(
						"Edges within same dimension in a dependency must be constrained together or be constrained to common edges");
				systems.addEdge(systems.nodeFor(sys11), systems.nodeFor(sys21), true, couples.getValue());
				systems.addEdge(systems.nodeFor(sys21), systems.nodeFor(sys11), true, couples.getValue());
			}
			theSystems = systems.immutable();
			theSystemsByEdge = Collections.unmodifiableMap(systemsByEdge);
		}

		public LayoutSolution solveSize(LayoutGuideType sizeType, Orientation primary, int layoutLength, int crossLength,
			boolean crossAvailable) {
			LayoutSolutionImpl solution = initSolution(primary, layoutLength, crossLength, crossAvailable);
			// Add a force to the far edge corresponding to the size type
			Edge farEdge = theBounds.getBoundary(primary, End.trailing);
			solution.theForces.get(farEdge)[0] += LayoutSpring.getTensionFor(sizeType);
			solve(solution);
			return solution;
		}

		public LayoutSolution solveLayout(Orientation primary, int layoutLength, int crossLength) {
			LayoutSolutionImpl solution = initSolution(primary, layoutLength, crossLength, false);
			// TODO
			solve(solution);
			return solution;
		}

		private LayoutSolutionImpl initSolution(Orientation primary, int layoutLength, int crossLength, boolean crossAvailable) {
			LayoutSolutionImpl solution = new LayoutSolutionImpl();
			for (Graph.Node<LayoutSystem, EdgePair> system : theSystems.getNodes())
				system.getValue().initialize(solution, primary, layoutLength, crossLength, crossAvailable);
			return solution;
		}

		private void solve(LayoutSolutionImpl solution) {
			// TODO Auto-generated method stub

		}
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

	public LayoutSystemSet compile() {
		return new LayoutSystemSet(this);
	}
}
