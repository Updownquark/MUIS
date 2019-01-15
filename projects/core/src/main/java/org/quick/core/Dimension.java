package org.quick.core;

public class Dimension {
	public final int width;
	public final int height;

	public Dimension(int w, int h) {
		if (w < 0)
			throw new IllegalArgumentException("Width must be >=0");
		if (h < 0)
			throw new IllegalArgumentException("Height must be >=0");
		this.width = w;
		this.height = h;
	}

	@Override
	public int hashCode() {
		return height * 7 + width;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		else if (!(o instanceof Dimension))
			return false;
		Dimension d = (Dimension) o;
		return d.width == width && d.height == height;
	}

	@Override
	public String toString() {
		return new StringBuilder().append('[').append(width).append("x").append(height).append(']').toString();
	}
}
