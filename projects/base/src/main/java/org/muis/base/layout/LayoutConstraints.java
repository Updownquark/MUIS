package org.muis.base.layout;

import static org.muis.core.layout.Orientation.horizontal;
import static org.muis.core.layout.Orientation.vertical;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;

import org.muis.base.layout.GeneticLayout.LayoutConstraint;
import org.muis.base.layout.GeneticLayout.LayoutScratchPad;
import org.muis.core.MuisElement;
import org.muis.core.style.LayoutStyles;

public class LayoutConstraints {
    public static abstract class ExpressionConstraint implements LayoutConstraint, Iterable<LayoutConstraint> {
        private LayoutConstraint[] theComponentConstraints;

        public ExpressionConstraint(LayoutConstraint... constraints) {
            theComponentConstraints = constraints;
        }

        @Override
        public Iterator<LayoutConstraint> iterator() {
            return prisms.util.ArrayUtils.iterator(theComponentConstraints, true);
        }

        public ExpressionConstraint not() {
            return new NotExpression(this);
        }

        public AndConstraint and(LayoutConstraint constraint) {
            return new AndConstraint(this, constraint);
        }

        public OrConstraint or(LayoutConstraint constraint) {
            return new OrConstraint(this, constraint);
        }
    }

    public static class NotExpression extends ExpressionConstraint {
        public NotExpression(LayoutConstraint constraint) {
            super(constraint);
        }

        public LayoutConstraint getComponent() {
            return iterator().next();
        }

        @Override
        public boolean isViolated(MuisElement container, Dimension size, LayoutScratchPad layout) {
            return !getComponent().isViolated(container, size, layout);
        }

        @Override
        public float getViolation(MuisElement container, Dimension size, LayoutScratchPad layout) {
            return 1 / getComponent().getViolation(container, size, layout);
        }
    }

    public static class OrConstraint extends ExpressionConstraint {
        public OrConstraint(LayoutConstraint... constraints) {
            super(constraints);
        }

        @Override
        public boolean isViolated(MuisElement container, Dimension size, LayoutScratchPad layout) {
            for (LayoutConstraint constraint : this)
                if (constraint.isViolated(container, size, layout))
                    return true;
            return false;
        }

        @Override
        public float getViolation(MuisElement container, Dimension size, LayoutScratchPad layout) {
            float max = 0;
            boolean hasConstraint = false;
            for (LayoutConstraint constraint : this) {
                hasConstraint = true;
                float violation = constraint.getViolation(container, size, layout);
                if (violation > max)
                    max = violation;
            }
            if (!hasConstraint)
                return 0;
            return max;
        }
    }

    public static class AndConstraint extends ExpressionConstraint {
        public AndConstraint(LayoutConstraint... constraints) {
            super(constraints);
        }

        @Override
        public boolean isViolated(MuisElement container, Dimension size, LayoutScratchPad layout) {
            for (LayoutConstraint constraint : this)
                if (!constraint.isViolated(container, size, layout))
                    return false;
            return true;
        }

        @Override
        public float getViolation(MuisElement container, Dimension size, LayoutScratchPad layout) {
            float min = Float.POSITIVE_INFINITY;
            boolean hasConstraint = false;
            for (LayoutConstraint constraint : this) {
                hasConstraint = true;
                float violation = constraint.getViolation(container, size, layout);
                if (violation < min)
                    min = violation;
            }
            if (!hasConstraint)
                return 0;
            return min;
        }
    }

    /**
     * A layout constraint preventing elements from being layed out on top of one another. The variable cost is the total area of
     * intersection of elements.
     */
    public static final LayoutConstraint interference = new LayoutConstraint() {
        @Override
        public boolean isViolated(MuisElement container, Dimension size, LayoutScratchPad layout) {
            ArrayList<Rectangle> bounds = new ArrayList<>();
            for (MuisElement element : layout) {
                Rectangle bound = layout.get(element);
                for (Rectangle b : bounds) {
                    if (b.intersects(bound))
                        return true;
                }
                bounds.add(bound);
            }
            return false;
        }

        @Override
        public float getViolation(MuisElement container, Dimension size, LayoutScratchPad layout) {
            float ret = 0;
            ArrayList<Rectangle> bounds = new ArrayList<>();
            for (MuisElement element : layout) {
                Rectangle bound = layout.get(element);
                for (Rectangle b : bounds) {
                    if (b.intersects(bound)) {
                        Rectangle intersection = b.intersection(bound);
                        ret += intersection.width * intersection.height;
                    }
                }
                bounds.add(bound);
            }
            return ret;
        }
    };

    /** A layout constraint encouraging sizes within the preferred range */
    public static final LayoutConstraint prefSize = new LayoutConstraint() {
        @Override
        public boolean isViolated(MuisElement container, Dimension size, LayoutScratchPad layout) {
            for (MuisElement child : layout) {
                Rectangle bounds = layout.get(child);
                if (bounds.width < child.bounds().get(horizontal).getGuide().getMinPreferred(bounds.height))
                    return true;
                if (bounds.width > child.bounds().get(horizontal).getGuide().getMaxPreferred(bounds.height))
                    return true;
                if (bounds.height < child.bounds().get(vertical).getGuide().getMinPreferred(bounds.width))
                    return true;
                if (bounds.height > child.bounds().get(vertical).getGuide().getMaxPreferred(bounds.width))
                    return true;
            }
            return false;
        }

        @Override
        public float getViolation(MuisElement container, Dimension size, LayoutScratchPad layout) {
            float ret = 0;
            for (MuisElement child : layout) {
                Rectangle bounds = layout.get(child);
                if (bounds.width < child.bounds().get(horizontal).getGuide().getMinPreferred(bounds.height))
                    ret += child.bounds().get(horizontal).getGuide().getMinPreferred(bounds.height) - bounds.width;
                if (bounds.width > child.bounds().get(horizontal).getGuide().getMaxPreferred(bounds.height))
                    ret += bounds.width - child.bounds().get(horizontal).getGuide().getMaxPreferred(bounds.height);
                if (bounds.height < child.bounds().get(vertical).getGuide().getMinPreferred(bounds.width))
                    ret += child.bounds().get(vertical).getGuide().getMinPreferred(bounds.width) - bounds.height;
                if (bounds.height > child.bounds().get(vertical).getGuide().getMaxPreferred(bounds.width))
                    ret += bounds.height - child.bounds().get(vertical).getGuide().getMaxPreferred(bounds.width);
            }
            return ret;
        }
    };

    /** A layout constraint causing components to stay within the container's bounds */
    public static final LayoutConstraint containerBounds = new LayoutConstraint() {
        @Override
        public boolean isViolated(MuisElement container, Dimension size, LayoutScratchPad layout) {
            for (MuisElement child : layout) {
                Rectangle bounds = layout.get(child);
                if (bounds.x < 0)
                    return true;
                if (bounds.y < 0)
                    return true;
                int overlap = bounds.x + bounds.width - size.width;
                if (overlap > 0)
                    return true;
                overlap = bounds.y + bounds.height - size.height;
                if (overlap > 0)
                    return true;
            }
            return false;
        }

        @Override
        public float getViolation(MuisElement container, Dimension size, LayoutScratchPad layout) {
            float ret = 0;
            for (MuisElement child : layout) {
                Rectangle bounds = layout.get(child);
                if (bounds.x < 0)
                    ret += -bounds.x;
                if (bounds.y < 0)
                    ret += -bounds.y;
                int overlap = bounds.x + bounds.width - size.width;
                if (overlap > 0)
                    ret += overlap;
                overlap = bounds.y + bounds.height - size.height;
                if (overlap > 0)
                    ret += overlap;
            }
            return ret;
        }
    };

    /**
     * A layout constraint enforcing the {@link LayoutStyles#margin} style. This constraint also enforces the same rules as
     * {@link #containerBounds}.
     */
    public static final LayoutConstraint margin = new LayoutConstraint() {
        @Override
        public boolean isViolated(MuisElement container, Dimension size, LayoutScratchPad layout) {
            int wMargin = container.getStyle().getSelf().get(LayoutStyles.margin).evaluate(size.width);
            int hMargin = container.getStyle().getSelf().get(LayoutStyles.margin).evaluate(size.height);
            for (MuisElement child : layout) {
                Rectangle bounds = layout.get(child);
                if (bounds.x < wMargin)
                    return true;
                if (bounds.y < hMargin)
                    return true;
                int overlap = bounds.x + bounds.width - (size.width - wMargin);
                if (overlap > 0)
                    return true;
                overlap = bounds.y + bounds.height - (size.height - hMargin);
                if (overlap > 0)
                    return true;
            }
            return false;
        }

        @Override
        public float getViolation(MuisElement container, Dimension size, LayoutScratchPad layout) {
            int wMargin = container.getStyle().getSelf().get(LayoutStyles.margin).evaluate(size.width);
            int hMargin = container.getStyle().getSelf().get(LayoutStyles.margin).evaluate(size.height);
            float ret = 0;
            for (MuisElement child : layout) {
                Rectangle bounds = layout.get(child);
                if (bounds.x < wMargin)
                    ret += wMargin - bounds.x;
                if (bounds.y < hMargin)
                    ret += hMargin - bounds.y;
                int overlap = bounds.x + bounds.width - (container.bounds().getWidth() - wMargin);
                if (overlap > 0)
                    ret += overlap;
                overlap = bounds.y + bounds.height - (container.bounds().getHeight() - hMargin);
                if (overlap > 0)
                    ret += overlap;
            }
            return ret;
        }
    };

    public static final LayoutConstraint padding = new LayoutConstraint() {
        @Override
        public boolean isViolated(MuisElement container, Dimension size, LayoutScratchPad layout) {
            return getViolation(container, size, layout, false) != 0;
        }

        @Override
        public float getViolation(MuisElement container, Dimension size, LayoutScratchPad layout) {
            return getViolation(container, size, layout, true);
        }

        private float getViolation(MuisElement container, Dimension size, LayoutScratchPad layout, boolean variable) {
            int wPadding = container.getStyle().getSelf().get(LayoutStyles.padding).evaluate(size.width);
            int hPadding = container.getStyle().getSelf().get(LayoutStyles.padding).evaluate(size.height);
            float ret = 0;
            ArrayList<Rectangle> bounds = new ArrayList<>();
            for (MuisElement child : layout) {
                Rectangle bound = layout.get(child);
                for (Rectangle b : bounds) {
                    int x0Dist = bound.x - (b.x + b.width + wPadding); // Distance between left edge of bound and right edge of b
                    int x1Dist = bound.x + bound.width + wPadding - b.x; // Distance between right edge of bound and left edge of b
                    int y0Dist = bound.y - (b.y + b.height + hPadding); // Distance between top edge of bound and bottom edge of b
                    int y1Dist = bound.y + bound.height + hPadding - b.y; // Distance between bottom edge of bound and top edge of b
                    if (Math.signum(x0Dist) != Math.signum(x1Dist) && Math.signum(y0Dist) != Math.signum(y1Dist)) {
                        if (!variable)
                            return 1;
                        int intersectW = x1Dist - x0Dist;
                        if (intersectW < 0)
                            intersectW = -intersectW;
                        if (intersectW > bound.width + wPadding * 2)
                            intersectW = bound.width + wPadding * 2;
                        if (intersectW > b.width + wPadding * 2)
                            intersectW = b.width + wPadding * 2;
                        int intersectH = y1Dist - y0Dist;
                        if (intersectH < 0)
                            intersectH = -intersectH;
                        if (intersectH > bound.height + wPadding * 2)
                            intersectH = bound.height + wPadding * 2;
                        if (intersectH > b.height + wPadding * 2)
                            intersectH = b.height + wPadding * 2;
                        ret += intersectW * intersectH;
                    }
                }
            }
            return ret;
        }
    };
}
