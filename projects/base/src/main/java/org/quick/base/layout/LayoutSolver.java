package org.quick.base.layout;

import java.awt.Dimension;
import java.util.*;

import org.qommons.collect.DefaultGraph;
import org.qommons.collect.Graph;
import org.qommons.collect.MutableGraph;
import org.quick.core.QuickElement;
import org.quick.core.layout.End;
import org.quick.core.layout.Orientation;

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

	public static class ElementEdgeBox extends EdgeBox.SimpleEdgeBox {
		private final QuickElement theElement;
		private final Dimension theSize;

		public ElementEdgeBox(QuickElement element) {
			theElement = element;
			theSize = new Dimension();
		}

		public int getSize(Orientation orient) {
			return orient.isVertical() ? theSize.height : theSize.width;
		}
	}

	public static class LayoutSystem {
		private final Graph<Edge, LayoutSpring> theSprings;
	}

	public static class LayoutSystemSet {

	}

	private final Map<QuickElement, ElementEdgeBox> theChildren;
	private final Map<String, NamedEdge> theNamedEdges;
	private final MutableGraph<Edge, LayoutSpring> theHSprings;
	private final MutableGraph<Edge, LayoutSpring> theVSprings;

	public LayoutSolver(Collection<? extends QuickElement> children) {
		theChildren = new IdentityHashMap<>(children.size());
		theNamedEdges = new LinkedHashMap<>();
		theHSprings = new DefaultGraph<>();
		theVSprings = new DefaultGraph<>();

		for (Orientation orient : Orientation.values())
			for (End end : End.values())
				getSprings(orient).addNode(getBoundary(orient, end));

		for (QuickElement child : children) {
			ElementEdgeBox box = new ElementEdgeBox(child);
			theChildren.put(child, box);
			for (Orientation orient : Orientation.values()) {
				Graph.Node<Edge, LayoutSpring> lead = getSprings(orient).addNode(box.getBoundary(orient, End.leading));
				Graph.Node<Edge, LayoutSpring> trail = getSprings(orient).addNode(box.getBoundary(orient, End.trailing));
				LayoutSpring spring = new LayoutSpring.SizeGuideSpring(child.bounds().get(orient).getGuide(), () -> box.getSize(orient));
				getSprings(orient).addEdge(lead, trail, false, spring);
			}
		}
	}

	public ElementEdgeBox boxFor(QuickElement element) {
		return theChildren.get(element);
	}

	public NamedEdge edge(String name, Orientation orient) {
		NamedEdge edge = theNamedEdges.computeIfAbsent(name, n -> {
			NamedEdge newEdge = new NamedEdge(n, orient);
			getSprings(orient).addNode(newEdge);
			return newEdge;
		});
		if (edge.getOrientation() != orient)
			throw new IllegalArgumentException("Edge " + name + " exists and is not oriented " + orient);
		return edge;
	}

	private MutableGraph<Edge, LayoutSpring> getSprings(Orientation orient) {
		return orient.isVertical() ? theVSprings : theHSprings;
	}

	public LayoutSolver constrain(Edge from, Edge to, LayoutSpring constraint) {
		if (from.getOrientation() != to.getOrientation())
			throw new IllegalArgumentException("Edges must have the same orientation place a constraint between them");
		MutableGraph<Edge, LayoutSpring> springs = getSprings(from.getOrientation());
		Graph.Node<Edge, LayoutSpring> fromNode = springs.nodeFor(from);
		Graph.Node<Edge, LayoutSpring> toNode = springs.nodeFor(to);
		if (fromNode == null || toNode == null)
			throw new IllegalArgumentException("Only edges defined in this solver may be constrained");
		springs.addEdge(fromNode, toNode, false, constraint);
		return this;
	}

	public LayoutSystemSet compile() {}
}
