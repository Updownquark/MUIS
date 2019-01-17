package org.quick.core;

/** An immutable x,y width*height class */
public class Rectangle {
	public final int x;
	public final int y;
	public final int width;
	public final int height;

	public Rectangle(int x, int y, int w, int h) {
		if (w < 0)
			throw new IllegalArgumentException("Width must be >=0");
		if (h < 0)
			throw new IllegalArgumentException("Height must be >=0");
		this.x = x;
		this.y = y;
		this.width = w;
		this.height = h;
	}

	public Rectangle(Point pos, Dimension size) {
		x = pos.x;
		y = pos.y;
		width = size.width;
		height = size.height;
	}

	public Point getPosition() {
		return new Point(x, y);
	}

	public Dimension getSize() {
		return new Dimension(width, height);
	}

	public int getMaxX() {
		return x + width;
	}

	public int getMaxY() {
		return y + height;
	}

	public boolean isEmpty() {
		return width == 0 && height == 0;
	}

	public boolean contains(int x, int y) {
		int relX = x - this.x;
		if (relX < 0 || relX >= width)
			return false;
		int relY = y - this.y;
		if (relY < 0 || relY >= height)
			return false;
		return true;
	}

	public boolean contains(Rectangle r) {
		int X = r.x, Y = r.y, W = r.width, H = r.height;
		int w = this.width;
		int h = this.height;
		if ((w | h | W | H) < 0) {
			// At least one of the dimensions is negative...
			return false;
		}
		// Note: if any dimension is zero, tests below must return false...
		int x = this.x;
		int y = this.y;
		if (X < x || Y < y) {
			return false;
		}
		w += x;
		W += X;
		if (W <= X) {
			// X+W overflowed or W was zero, return false if...
			// either original w or W was zero or
			// x+w did not overflow or
			// the overflowed x+w is smaller than the overflowed X+W
			if (w >= x || W > w)
				return false;
		} else {
			// X+W did not overflow and W was not zero, return false if...
			// original w was zero or
			// x+w did not overflow and x+w is smaller than X+W
			if (w >= x && W > w)
				return false;
		}
		h += y;
		H += Y;
		if (H <= Y) {
			if (h >= y || H > h)
				return false;
		} else {
			if (h >= y && H > h)
				return false;
		}
		return true;
	}

	public Rectangle union(Rectangle r) {
		int minX = Math.min(x, r.x);
		int minY = Math.min(y, r.y);
		int maxX = Math.max(x + width, r.x + r.width);
		int maxY = Math.max(y + height, r.y + r.height);
		return new Rectangle(minX, minY, maxX - minX, maxY - minY);
	}

	public boolean intersects(int x2, int y2, int width2, int height2) {
		int minX = Math.min(x, x2);
		int maxX = Math.max(x + width, x2 + width2);
		if (minX >= maxX)
			return false;
		int minY = Math.min(y, y2);
		int maxY = Math.max(y + height, y2 + height2);
		if (minY >= maxY)
			return false;
		return true;
	}

	public Rectangle intersection(Rectangle r) {
		int minX = Math.min(x, r.x);
		int maxX = Math.max(x + width, r.x + r.width);
		if (minX > maxX)
			return null;
		int minY = Math.min(y, r.y);
		int maxY = Math.max(y + height, r.y + r.height);
		if (minY > maxY)
			return null;
		return new Rectangle(minX, minY, maxX - minX, maxY - minY);
	}

	public java.awt.Rectangle toAwt() {
		return new java.awt.Rectangle(x, y, width, height);
	}

	public static Rectangle fromAwt(java.awt.Rectangle r) {
		return new Rectangle(r.x, r.y, r.width, r.height);
	}

	@Override
	public int hashCode() {
		return y * 17 + x * 11 + height * 7 + width;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		else if (!(o instanceof Rectangle))
			return false;
		Rectangle r = (Rectangle) o;
		return r.x == x && r.y == y && r.width == width && r.height == height;
	}

	@Override
	public String toString() {
		return new StringBuilder().append('(').append(x).append(", ").append(y).append(")[").append(width).append("x").append(height)
			.append(']').toString();
	}
}
