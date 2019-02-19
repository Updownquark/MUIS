package org.quick.base.layout.generic;

import org.quick.core.layout.End;
import org.quick.core.layout.Orientation;

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