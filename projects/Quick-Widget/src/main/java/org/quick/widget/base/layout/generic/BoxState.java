package org.quick.widget.base.layout.generic;

import org.quick.core.layout.End;
import org.quick.core.layout.Orientation;
import org.quick.widget.core.Point;

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