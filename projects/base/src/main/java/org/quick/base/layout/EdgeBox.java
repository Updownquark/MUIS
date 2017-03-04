package org.quick.base.layout;

import org.quick.core.layout.End;
import org.quick.core.layout.Orientation;

interface EdgeBox {
	public class SimpleEdgeBox implements EdgeBox {
		private final Edge[][] theEdges;

		public SimpleEdgeBox() {
			theEdges = new Edge[Orientation.values().length][End.values().length];
			for (Orientation orient : Orientation.values())
				for (End end : End.values())
					theEdges[orient.ordinal()][end.ordinal()] = new Edge.SimpleEdge(orient);
		}

		@Override
		public Edge getBoundary(Orientation orient, End end) {
			return theEdges[orient.ordinal()][end.ordinal()];
		}
	}

	public class ValueEdgeBox<V> extends SimpleEdgeBox {
		private final V theValue;

		public ValueEdgeBox(V value) {
			theValue = value;
		}

		public V getValue() {
			return theValue;
		}

		@Override
		public String toString() {
			return "box for " + getValue();
		}
	}

	default Edge getLeft() {
		return getBoundary(Orientation.horizontal, End.leading);
	}

	default Edge getRight() {
		return getBoundary(Orientation.horizontal, End.trailing);
	}

	default Edge getTop() {
		return getBoundary(Orientation.vertical, End.leading);
	}

	default Edge getBottom() {
		return getBoundary(Orientation.vertical, End.trailing);
	}

	Edge getBoundary(Orientation orient, End end);
}