package org.quick.widget.base.layout;

import static org.quick.core.layout.LayoutAttributes.bottom;
import static org.quick.core.layout.LayoutAttributes.height;
import static org.quick.core.layout.LayoutAttributes.left;
import static org.quick.core.layout.LayoutAttributes.maxHeight;
import static org.quick.core.layout.LayoutAttributes.maxWidth;
import static org.quick.core.layout.LayoutAttributes.minHeight;
import static org.quick.core.layout.LayoutAttributes.minWidth;
import static org.quick.core.layout.LayoutAttributes.right;
import static org.quick.core.layout.LayoutAttributes.top;
import static org.quick.core.layout.LayoutAttributes.width;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.observe.Observable;
import org.quick.core.layout.End;
import org.quick.core.layout.LayoutAttributes;
import org.quick.core.layout.LayoutGuideType;
import org.quick.core.layout.LayoutSize;
import org.quick.core.layout.Orientation;
import org.quick.core.style.LengthUnit;
import org.quick.core.style.Position;
import org.quick.core.style.Size;
import org.quick.util.CompoundListener;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.layout.LayoutUtils;
import org.quick.widget.core.layout.QuickWidgetLayout;
import org.quick.widget.core.layout.SizeGuide;

/**
 * A very simple layout that positions and sizes children independently using positional ({@link LayoutAttributes#left},
 * {@link LayoutAttributes#right}, {@link LayoutAttributes#top}, {@link LayoutAttributes#bottom}) and size ({@link LayoutAttributes#width}
 * and {@link LayoutAttributes#height}) attributes or sizers.
 */
public class SimpleLayout implements QuickWidgetLayout {
	private final CompoundListener<QuickWidget> theListener;

	/** Creates a simple layout */
	public SimpleLayout() {
		theListener = CompoundListener.<QuickWidget> buildFromQDW()//
			.child(childBuilder -> {
				childBuilder.acceptAll(left, right, top, bottom, width, height, minWidth, maxWidth, minHeight, maxHeight)
				.onEvent(sizeNeedsChanged);
			})//
			.build();
	}

	@Override
	public void install(QuickWidget parent, Observable<?> until) {
		theListener.listen(parent, parent, until);
	}

	@Override
	public SizeGuide getSizer(QuickWidget parent, Iterable<? extends QuickWidget> children, Orientation orientation) {
		return new SizeGuide.GenericSizeGuide() {
			@Override
			public int get(LayoutGuideType type, int crossSize, boolean csMax) {
				List<QuickWidget> ch = new ArrayList<>();
				for (QuickWidget child : children)
					ch.add(child);
				if (ch.isEmpty()) {
					if (type == LayoutGuideType.max)
						return Integer.MAX_VALUE;
					return 0;
				}
				int[] res = new int[ch.size()];
				for (int i = 0; i < res.length; i++) {
					// Need to take into account constraints on the opposite orientation.
					int childCrossSize = childCrossSize(crossSize, ch.get(i), orientation);
					res[i] = getSize(ch.get(i), type, childCrossSize, csMax);
				}
				switch (type) {
				case min:
				case minPref:
					return Arrays.stream(res).max().getAsInt();
				case pref:
					return (int) Math.round(Arrays.stream(res).average().getAsDouble());
				case maxPref:
				case max:
					return Arrays.stream(res).min().getAsInt();
				}
				throw new IllegalStateException("Unrecognized layout guide type: " + type);
			}

			private int getSize(QuickWidget child, LayoutGuideType type, int crossSize, boolean csMax) {
				if (!LayoutUtils.checkLayoutChild(child.getElement()))
					return 0;

				Position lead = child.getElement().atts().get(LayoutAttributes.getPosAtt(orientation, End.leading, null)).get();
				Position trail = child.getElement().atts().get(LayoutAttributes.getPosAtt(orientation, End.trailing, null)).get();

				Size[] layoutSizes;
				if (lead != null && trail != null && lead.getUnit() == LengthUnit.lexips && trail.getUnit() == LengthUnit.pixels)
					layoutSizes = LayoutUtils.getLayoutSize(child, orientation, type.opposite(), crossSize, csMax);
				else
					layoutSizes = LayoutUtils.getLayoutSize(child, orientation, type, crossSize, csMax);
				if (layoutSizes.length == 1)
					return getSize(type, lead, trail, layoutSizes[0]);
				else {
					int prefSize = getSize(type, lead, trail, layoutSizes[1]);
					if (layoutSizes[2] != null) {
						int maxSize = getSize(type, lead, trail, layoutSizes[2]);
						if (prefSize > maxSize)
							prefSize = maxSize;
					}
					if (layoutSizes[0] != null) {
						int minSize = getSize(type, lead, trail, layoutSizes[0]);
						if (prefSize < minSize)
							prefSize = minSize;
					}
					return prefSize;
				}
			}

			private int getSize(LayoutGuideType type, Position lead, Position trail, Size layoutWidth) {
				Sandbox sandbox = new Sandbox();
				Sandbox.Edge front = sandbox.createEdge();
				Sandbox.Edge end = sandbox.createEdge();
				if (lead != null) {
					switch (lead.getUnit()) {
					case pixels:
					case percent:
						sandbox.createSpace(sandbox.getLeft(), front, new Size(lead.getValue(), lead.getUnit()));
						break;
					case lexips:
						sandbox.createSpace(front, sandbox.getRight(), new Size(lead.getValue(), LengthUnit.pixels));
						break;
					}
				} else
					sandbox.createSpace(sandbox.getLeft(), front, new Size(0, LengthUnit.pixels));
				if (trail != null) {
					switch (trail.getUnit()) {
					case pixels:
					case percent:
						sandbox.createSpace(sandbox.getLeft(), end, new Size(trail.getValue(), trail.getUnit()));
						break;
					case lexips:
						sandbox.createSpace(end, sandbox.getRight(), new Size(trail.getValue(), LengthUnit.pixels));
						break;
					}
				}

				sandbox.createSpace(front, end, layoutWidth);

				sandbox.resolve();
				if (type == LayoutGuideType.max && sandbox.getRight().getConstraints().isEmpty())
					return Integer.MAX_VALUE;
				LayoutSize endPos = sandbox.getEnd();
				if (endPos.getPercent() == 0)
					return endPos.getPixels();
				else if (endPos.getPixels() == 0)
					return 0;
				else if (endPos.getPercent() >= 100)
					throw new IllegalStateException("Total length>100%");
				else
					return (int) (endPos.getPixels() / (100 - endPos.getPercent()) * 100);
			}

			@Override
			public int getBaseline(int size) {
				boolean first = true;
				Position initLeading = null, initTrailing = null;
				for (QuickWidget child : children) {
					if (first) {
						first = false;
						initLeading = child.getElement().atts().get(LayoutAttributes.getPosAtt(orientation, End.leading, null)).get();
						initTrailing = child.getElement().atts().get(LayoutAttributes.getPosAtt(orientation, End.trailing, null)).get();
					}
					int childSize = LayoutUtils.getSize(child, orientation, LayoutGuideType.pref, size, Integer.MAX_VALUE, true, null);
					int ret = child.bounds().get(orientation).getGuide().getBaseline(childSize);
					if (ret < 0)
						continue;
					Position pos = initLeading;
					if (pos != null) {
						return ret + pos.evaluate(size);
					}
					pos = initTrailing;
					if (pos != null) {
						return size - pos.evaluate(size) - childSize + ret;
					}
					return ret;
				}
				return 0;
			}
		};
	}

	@Override
	public void layout(QuickWidget parent, List<? extends QuickWidget> children) {
		Rectangle bounds = new Rectangle();
		for (QuickWidget child : children) {
			layout(parent, child, Orientation.horizontal, bounds);
			layout(parent, child, Orientation.vertical, bounds);
			child.bounds().setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
		}
	}

	private static void layout(QuickWidget parent, QuickWidget child, Orientation orient, Rectangle bounds) {
		Position lead = child.getElement().atts().get(LayoutAttributes.getPosAtt(orient, End.leading, null)).get();
		Position trail = child.getElement().atts().get(LayoutAttributes.getPosAtt(orient, End.trailing, null)).get();
		int parentLength = parent.bounds().get(orient).getSize();
		int pos;
		int length;
		if (lead != null && trail != null) {
			pos = lead.evaluate(parentLength);
			int pos2 = trail.evaluate(parentLength);
			if (pos2 < pos)
				length = 0;
			else
				length = pos2 - pos;
		} else {
			Size[] sizes = LayoutUtils.getLayoutSize(child, orient, LayoutGuideType.pref,
				childCrossSize(parent.bounds().get(orient.opposite()).getSize(), child, orient), false);
			if (sizes.length == 1)
				length = sizes[0].evaluate(parentLength);
			else {
				length = sizes[1].evaluate(parentLength);
				if (sizes[2] != null) {
					int maxLength = sizes[2].evaluate(parentLength);
					if (length < maxLength)
						length = maxLength;
				}
				if (sizes[0] != null) {
					int minLength = sizes[0].evaluate(parentLength);
					if (length < minLength)
						length = minLength;
				}
			}
			if (lead != null)
				pos = lead.evaluate(parentLength);
			else if (trail != null)
				pos = trail.evaluate(parentLength) - length;
			else
				pos = 0;
		}
		switch (orient) {
		case horizontal:
			bounds.x = pos;
			bounds.width = length;
			break;
		case vertical:
			bounds.y = pos;
			bounds.height = length;
			break;
		}
	}

	private static int childCrossSize(int crossSize, QuickWidget child, Orientation orient) {
		int childCrossSize;
		Size size = child.getElement().atts().get(LayoutAttributes.getSizeAtt(orient.opposite(), null)).get();
		Size maxSize = child.getElement().atts().get(LayoutAttributes.getSizeAtt(orient.opposite(), LayoutGuideType.max)).get();
		Position lead = child.getElement().atts().get(LayoutAttributes.getPosAtt(orient.opposite(), End.leading, null)).get();
		Position trail = child.getElement().atts().get(LayoutAttributes.getPosAtt(orient.opposite(), End.trailing, null)).get();
		if (size != null)
			childCrossSize = size.evaluate(crossSize);
		else if (lead != null && trail != null && lead.getUnit() == LengthUnit.lexips && trail.getUnit() == LengthUnit.pixels) {
			childCrossSize = lead.evaluate(crossSize) + trail.evaluate(crossSize) - crossSize;
			if (childCrossSize < 0)
				childCrossSize = 0;
		} else if (maxSize != null)
			childCrossSize = maxSize.evaluate(crossSize);
		else {
			childCrossSize = crossSize;
			if (lead != null)
				childCrossSize -= lead.evaluate(crossSize);
			if (trail != null)
				childCrossSize -= trail.evaluate(crossSize);
		}
		return childCrossSize;
	}

	@Override
	public String toString() {
		return "simple";
	}

	private static class Sandbox {
		static class Edge {
			private final boolean isLeftAllowed;
			private final boolean isRightAllowed;
			private final List<Space> theConstraints;
			private boolean isCached;
			private LayoutSize thePosition;

			Edge(boolean allowsLeft, boolean allowsRight) {
				isLeftAllowed = allowsLeft;
				isRightAllowed = allowsRight;
				theConstraints = new LinkedList<>();
			}

			List<Space> getConstraints() {
				return theConstraints;
			}

			void add(Space space) {
				if (!isLeftAllowed && space.right == this)
					throw new IllegalArgumentException("Cannot add spaces to the left of the left edge of the sandbox");
				if (!isRightAllowed && space.left == this)
					throw new IllegalArgumentException("Cannot add spaces to the right of the right edge of the sandbox");
				uncache();
				theConstraints.add(space);
			}

			private void uncache() {
				if (!isCached)
					return;
				isCached = false;
				for (Space constraint : theConstraints) {
					if (constraint.left != this)
						constraint.left.uncache();
					if (constraint.right != this)
						constraint.right.uncache();
				}
			}

			@SuppressWarnings("unused")
			public LayoutSize getPosition() {
				return thePosition;
			}

			void resolve(Space calling) {
				if (!isLeftAllowed && calling != null) {
					thePosition = new LayoutSize();
					isCached = true;
					return;
				}
				if (!isCached) {
					for (Space constraint : theConstraints) {
						if (constraint == calling)
							continue;
						if (constraint.left != null && constraint.left != this) {
							constraint.left.resolve(constraint);
							thePosition = new LayoutSize(constraint.left.thePosition).add(constraint.size);
						} else {
							constraint.right.resolve(constraint);
							thePosition = new LayoutSize(constraint.right.thePosition).minus(constraint.size);
						}
					}
					if (thePosition == null)
						thePosition = new LayoutSize();
					if (!isLeftAllowed) {
						if (!thePosition.isZero()) {
							for (Space constraint : theConstraints)
								constraint.right.add(thePosition.negate(), constraint);
						}
						thePosition = new LayoutSize();
					}
					isCached = true;
				}
			}

			private void add(LayoutSize size, Space calling) {
				thePosition.add(size);
				for (Space constraint : theConstraints) {
					if (constraint == calling)
						continue;
					if (constraint.left != null && constraint.left != this) {
						constraint.left.add(size, constraint);
					} else {
						constraint.right.add(size, constraint);
					}
				}
			}

			LayoutSize getEnd(Space calling, LayoutSize size) {
				if (!isLeftAllowed && calling != null)
					return size;
				size.add(thePosition);
				for (Space constraint : theConstraints) {
					if (constraint == calling)
						continue;
					if (constraint.left != null && constraint.left != this) {
						return constraint.left.getEnd(constraint, size);
					} else {
						return constraint.right.getEnd(constraint, size);
					}
				}
				return size;
			}

			@Override
			public String toString() {
				return String.valueOf(thePosition);
			}
		}

		static class Space {
			@SuppressWarnings("hiding")
			public final Edge left;
			@SuppressWarnings("hiding")
			public final Edge right;
			public final Size size;

			Space(Edge left, Edge right, Size size) {
				this.left = left;
				this.right = right;
				this.size = size;
				left.add(this);
				right.add(this);
			}
		}

		private final Edge theLeft;
		private final Edge theRight;

		Sandbox() {
			theLeft = new Edge(false, true);
			theRight = new Edge(true, false);
		}

		public Edge getLeft() {
			return theLeft;
		}

		public Edge getRight() {
			return theRight;
		}

		public Edge createEdge() {
			return new Edge(true, true);
		}

		@SuppressWarnings("hiding")
		public Space createSpace(Edge left, Edge right, Size size) {
			return new Space(left, right, size);
		}

		public void resolve() {
			theRight.resolve(null);
			theLeft.resolve(null);
		}

		public LayoutSize getEnd() {
			LayoutSize size = new LayoutSize(true);
			theLeft.getEnd(null, size);
			theRight.getEnd(null, size);
			return size;
		}
	}
}
