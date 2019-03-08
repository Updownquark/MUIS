package org.quick.widget.base.layout.generic;

public interface SpringState extends SpringDef {
	@Override
	EdgeState getSource();

	@Override
	EdgeState getDest();

	default int getLength() {
		return getDest().getPosition() - getSource().getPosition();
	}

	float getTension();
}