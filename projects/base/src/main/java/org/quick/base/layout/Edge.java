package org.quick.base.layout;

import org.quick.core.layout.Orientation;

interface Edge {
	public class SimpleEdge implements Edge {
		private final Orientation theOrient;

		public SimpleEdge(Orientation orient) {
			theOrient = orient;
		}

		@Override
		public Orientation getOrientation() {
			return theOrient;
		}
	}

	Orientation getOrientation();
}