package org.muis.core.layout;

/** Represents the bounds of an object along a single dimension (axis) */
public interface BoundsDimension {
	/** @return The object's position along the axis */
	int getPosition();

	/** @param pos The position along the axis to set for the object */
	void setPosition(int pos);

	/** @return The object's size along the axis */
	int getSize();

	/** @param size The size along the axis to set for the object */
	void setSize(int size);

	/** @return The size for the object along the axis */
	SizeGuide getGuide();
}
