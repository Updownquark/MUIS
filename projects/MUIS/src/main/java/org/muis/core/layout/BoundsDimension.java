package org.muis.core.layout;

public interface BoundsDimension {
	int getPosition();

	void setPosition(int pos);

	int getSize();

	void setSize(int size);

	SizeGuide getGuide();
}
