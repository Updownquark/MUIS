package org.quick.base.layout;

import org.quick.base.layout.LayoutSolver.LayoutSystemSet;

/**
 * <p>
 * Represents a set of edge positions for a layout.
 * </p>
 *
 * <p>
 * During computation for the layout, {@link #isInitialized(Edge)} should be used in conjunction with queries to determine if the edge
 * has been initialized yet. After the solution is returned by {@link LayoutSystemSet#solve()}, all edges will have been initialized.
 * </p>
 */
public interface LayoutSolution extends EdgeBox {
	boolean isInitialized(Edge edge);

	int getPosition(Edge edge);
}