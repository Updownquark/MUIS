package org.quick.widget.core;

public class Point {
	public final int x;
	public final int y;

	public Point(int x, int y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public int hashCode() {
		return y * 7 + x;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		else if (!(o instanceof Point))
			return false;
		Point p = (Point) o;
		return p.x == x && p.y == y;
	}

	@Override
	public String toString() {
		return new StringBuilder().append('(').append(x).append(", ").append(y).append(')').toString();
	}
}
