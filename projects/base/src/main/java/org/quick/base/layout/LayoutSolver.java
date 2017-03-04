package org.quick.base.layout;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.qommons.BiTuple;
import org.qommons.collect.DefaultGraph;
import org.qommons.collect.Graph;
import org.qommons.collect.MutableGraph;
import org.quick.core.QuickElement;
import org.quick.core.layout.*;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class LayoutSolver extends EdgeBox.SimpleEdgeBox {
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

	public interface LayoutEdgeLink extends Cloneable {
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
		private final LayoutSystemSet theSystemSet;
		private final Graph<Edge, LayoutEdgeLink> theEdges;
		private boolean isDirty;
		private int theSolveCount;

		LayoutSystem(LayoutSystemSet systemSet, Graph<Edge, LayoutEdgeLink> edges) {
			theSystemSet = systemSet;
			theEdges = edges;
			isDirty = true;
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
			int todo = todo; // Initialization
			for (Graph.Node<LayoutSystem, EdgePair> system : theSystems.getNodes())
				system.getValue().initialize();
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
			for (MutableGraph<Edge, LayoutEdgeLink> subGraph : solver.theHSprings.split(node -> new DefaultGraph<>())) {
				LayoutSystem system = new LayoutSystem(this, subGraph);
				for (Graph.Node<Edge, LayoutEdgeLink> edge : subGraph.getNodes())
					theSystemsByEdge.put(edge.getValue(), system);
				theSystems.addNode(system);
			}
			// Opposite edges of the overall layout must be in the same sub-graph
			LayoutSystem leftSys = theSystemsByEdge.get(solver.getLeft());
			LayoutSystem rightSys = theSystemsByEdge.get(solver.getRight());
			if (leftSys != rightSys)
				moveAll(rightSys, leftSys);
			LayoutSystem topSys = theSystemsByEdge.get(solver.getTop());
			LayoutSystem bottomSys = theSystemsByEdge.get(solver.getBottom());
			if (topSys != bottomSys)
				moveAll(bottomSys, topSys);
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

		private void moveAll(LayoutSystem fromSys, LayoutSystem toSys) {
			for (Graph.Node<Edge, LayoutEdgeLink> edge : fromSys.theEdges.getNodes()) {
				((MutableGraph<Edge, LayoutEdgeLink>) toSys.theEdges).addNode(edge.getValue());
				theSystemsByEdge.put(edge.getValue(), toSys);
			}
			for (Graph.Edge<Edge, LayoutEdgeLink> link : fromSys.theEdges.getEdges())
				((MutableGraph<Edge, LayoutEdgeLink>) toSys.theEdges).addEdge(toSys.theEdges.nodeFor(link.getStart().getValue()), //
					toSys.theEdges.nodeFor(link.getEnd().getValue()), //
					link.isDirected(), link.getValue());
			Graph.Node<LayoutSystem, EdgePair> rightNode = theSystems.nodeFor(fromSys);
			for (Graph.Edge<LayoutSystem, EdgePair> depend : rightNode.getEdges()) {
				if (depend.getStart() == rightNode)
					theSystems.addEdge(rightNode, theSystems.nodeFor(depend.getEnd().getValue()), depend.isDirected(), depend.getValue());
			}
			theSystems.removeNode(rightNode);
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
