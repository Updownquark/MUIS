package org.quick.widget.core.mgr;

import java.util.concurrent.locks.Lock;
import java.util.function.Function;

import org.observe.ObservableValue;
import org.observe.VetoableSettableValue;
import org.observe.util.TypeTokens;
import org.quick.core.Dimension;
import org.quick.core.Point;
import org.quick.core.Rectangle;
import org.quick.core.layout.Orientation;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.layout.Bounds;
import org.quick.widget.core.layout.SizeGuide;

/** Bounds for an element. Contains some extra methods for easy access. */
public class ElementBounds extends VetoableSettableValue<Rectangle> implements Bounds {
	private final QuickWidget theWidget;

	private ElementBoundsDimension theHorizontalBounds;
	private ElementBoundsDimension theVerticalBounds;

	/** @param widget The element to create the bounds for */
	public ElementBounds(QuickWidget widget) {
		super(TypeTokens.get().of(Rectangle.class), false, widget.getElement().getAttributeLocker());
		set(new Rectangle(0, 0, 0, 0), null);
		theWidget = widget;
		theHorizontalBounds = new ElementBoundsDimension(false);
		theVerticalBounds = new ElementBoundsDimension(true);
	}

	/** @return The horizontal bounds */
	public ElementBoundsDimension getHorizontal() {
		return theHorizontalBounds;
	}

	/** @return The horizontal bounds */
	public ElementBoundsDimension h() {
		return theHorizontalBounds;
	}

	/** @return The vertical bounds */
	public ElementBoundsDimension getVertical() {
		return theVerticalBounds;
	}

	/** @return The vertical bounds */
	public ElementBoundsDimension v() {
		return theVerticalBounds;
	}

	@Override
	public ElementBoundsDimension get(Orientation orientation) {
		switch (orientation) {
		case horizontal:
			return theHorizontalBounds;
		case vertical:
			return theVerticalBounds;
		}
		throw new IllegalStateException("Unrecognized orientation: " + orientation);
	}

	/** @return {@link #getHorizontal()}.{@link org.quick.widget.core.layout.BoundsDimension#getPosition() getPosition()} */
	public int getX() {
		return get().x;
	}

	private ElementBounds update(Function<Rectangle, Rectangle> update) {
		Lock lock = theWidget.getElement().getAttributeLocker().writeLock();
		lock.lock();
		try {
			Rectangle updated = update.apply(get());
			if (updated != null)
				set(updated, null);
		} finally {
			lock.unlock();
		}
		return this;
	}

	/**
	 * @param x See {@link #getHorizontal()}.{@link org.quick.widget.core.layout.BoundsDimension#setPosition(int) setPosition(int)}
	 * @return This bounds
	 */
	public ElementBounds setX(int x) {
		return update(r -> r.x == x ? null : new Rectangle(x, r.y, r.width, r.height));
	}

	/** @return {@link #getVertical()}.{@link org.quick.widget.core.layout.BoundsDimension#getPosition() getPosition()} */
	public int getY() {
		return get().y;
	}

	/**
	 * @param y See {@link #getVertical()}.{@link org.quick.widget.core.layout.BoundsDimension#setPosition(int) setPosition(int)}
	 * @return This bounds
	 */
	public ElementBounds setY(int y) {
		return update(r -> r.y == y ? null : new Rectangle(r.x, y, r.width, r.height));
	}

	/** @return The element's 2-dimensional position */
	public Point getPosition() {
		return get().getPosition();
	}

	/**
	 * @param x See {@link #getHorizontal()}.{@link org.quick.widget.core.layout.BoundsDimension#setPosition(int) setPosition(int)}
	 * @param y See {@link #getVertical()}.{@link org.quick.widget.core.layout.BoundsDimension#setPosition(int) setPosition(int)}
	 * @return This bounds
	 */
	public ElementBounds setPosition(int x, int y) {
		return update(r -> (r.x == x && r.y == y) ? null : new Rectangle(x, y, r.width, r.height));
	}

	/**
	 * @param pos The position for this bounds
	 * @return This bounds
	 */
	public ElementBounds setPosition(Point pos) {
		return setPosition(pos.x, pos.y);
	}

	/** @return {@link #getHorizontal()}.{@link org.quick.widget.core.layout.BoundsDimension#getSize() getSize()} */
	public int getWidth() {
		return get().width;
	}

	/**
	 * @param width See {@link #getHorizontal()}.{@link org.quick.widget.core.layout.BoundsDimension#setSize(int) setSize(int)}
	 * @return This bounds
	 */
	public ElementBounds setWidth(int width) {
		return update(r -> r.width == width ? null : new Rectangle(r.x, r.y, width, r.height));
	}

	/** @return {@link #getVertical()}.{@link org.quick.widget.core.layout.BoundsDimension#getSize() getSize()} */
	public int getHeight() {
		return get().height;
	}

	/**
	 * @param height See {@link #getVertical()}.{@link org.quick.widget.core.layout.BoundsDimension#setSize(int) setSize(int)}
	 * @return This bounds
	 */
	public ElementBounds setHeight(int height) {
		return update(r -> r.height == height ? null : new Rectangle(r.x, r.y, r.width, height));
	}

	/** @return The element's 2-dimensional size */
	public Dimension getSize() {
		return get().getSize();
	}

	/**
	 * @param width See {@link #getHorizontal()}.{@link org.quick.widget.core.layout.BoundsDimension#setSize(int) setSize(int)}
	 * @param height See {@link #getVertical()}.{@link org.quick.widget.core.layout.BoundsDimension#setSize(int) setSize(int)}
	 * @return This bounds
	 */
	public ElementBounds setSize(int width, int height) {
		return update(r -> (r.width == width && r.height == height) ? null : new Rectangle(r.x, r.y, width, height));
	}

	/**
	 * @param size The size for this bounds
	 * @return This bounds
	 */
	public ElementBounds setSize(Dimension size) {
		return setSize(size.width, size.height);
	}

	/** @return The element's rectangle bounds */
	public Rectangle getBounds() {
		return get();
	}

	/**
	 * @param x The x position relative to this bounds' coordinate system
	 * @param y The y position relative to this bounds' coordinate system
	 * @return Whether this bounds overlaps the given coordinate
	 */
	public boolean contains(int x, int y) {
		return get().contains(x, y);
	}

	/** @return Whether this bounds has zero area */
	public boolean isEmpty() {
		return get().isEmpty();
	}

	/** @return An observable value for this bounds' x-coordinate. Equivalent to <code>mapV(bounds->{return bounds.x;})</code>. */
	public ObservableValue<Integer> observeX() {
		return map(TypeTokens.get().of(int.class), r -> r.x);
	}

	/** @return An observable value for this bounds' y-coordinate. Equivalent to <code>mapV(bounds->{return bounds.y;})</code>. */
	public org.observe.ObservableValue<Integer> observeY() {
		return map(TypeTokens.get().of(int.class), r -> r.y);
	}

	/** @return An observable value for this bounds' width. Equivalent to <code>mapV(bounds->{return bounds.width;})</code>. */
	public org.observe.ObservableValue<Integer> observeW() {
		return map(TypeTokens.get().of(int.class), r -> r.width);
	}

	/** @return An observable value for this bounds' height. Equivalent to <code>mapV(bounds->{return bounds.height;})</code>. */
	public org.observe.ObservableValue<Integer> observeH() {
		return map(TypeTokens.get().of(int.class), r -> r.height);
	}

	/**
	 *
	 * @param x See {@link #getHorizontal()}.{@link org.quick.widget.core.layout.BoundsDimension#setPosition(int) setPosition(int)}
	 * @param y See {@link #getVertical()}.{@link org.quick.widget.core.layout.BoundsDimension#setPosition(int) setPosition(int)}
	 * @param width See {@link #getHorizontal()}.{@link org.quick.widget.core.layout.BoundsDimension#setSize(int) setSize(int)}
	 * @param height See {@link #getVertical()}.{@link org.quick.widget.core.layout.BoundsDimension#setSize(int) setSize(int)}
	 * @return This bounds
	 */
	public ElementBounds setBounds(int x, int y, int width, int height) {
		return update(r -> (r.x == x && r.y == y && r.width == width && r.height == height) ? null : new Rectangle(x, y, width, height));
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (theWidget.getElement().getTagName() != null)
			builder.append(theWidget.getElement().getTagName()).append(' ');
		return builder.append("bounds=").append(get()).toString();
	}

	/** A BoundsDimension for an element along one axis */
	public class ElementBoundsDimension implements org.quick.widget.core.layout.BoundsDimension {
		private final boolean isVertical;

		ElementBoundsDimension(boolean vertical) {
			isVertical = vertical;
		}

		@Override
		public int getPosition() {
			return isVertical ? get().y : get().x;
		}

		@Override
		public void setPosition(int pos) {
			if (isVertical)
				setY(pos);
			else
				setX(pos);
		}

		@Override
		public int getSize() {
			return isVertical ? get().height : get().width;
		}

		@Override
		public void setSize(int size) {
			if (isVertical)
				setHeight(size);
			else
				setWidth(size);
		}

		@Override
		public SizeGuide getGuide() {
			return theWidget.getSizer(Orientation.of(isVertical));
		}

		@Override
		public String toString() {
			return new StringBuilder(theWidget.getElement().getTagName()).append(" ").append(isVertical ? "v" : "h").append("-bounds=(")
				.append(getPosition()).append("){").append(getSize()).append("}").toString();
		}
	}
}
