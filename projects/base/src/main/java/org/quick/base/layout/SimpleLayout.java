package org.quick.base.layout;

import static org.quick.core.layout.LayoutAttributes.*;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.observe.Observable;
import org.quick.core.QuickElement;
import org.quick.core.QuickLayout;
import org.quick.core.layout.*;
import org.quick.core.style.LengthUnit;
import org.quick.core.style.Position;
import org.quick.core.style.Size;
import org.quick.util.CompoundListener;

/**
 * A very simple layout that positions and sizes children independently using positional ({@link LayoutAttributes#left},
 * {@link LayoutAttributes#right}, {@link LayoutAttributes#top}, {@link LayoutAttributes#bottom}) and size ({@link LayoutAttributes#width}
 * and {@link LayoutAttributes#height}) attributes or sizers.
 */
public class SimpleLayout implements QuickLayout {
	private final CompoundListener theListener;

	/** Creates a simple layout */
	public SimpleLayout() {
		theListener = CompoundListener.build()//
			.child(childBuilder -> {
				childBuilder.acceptAll(left, right, top, bottom, width, height, minWidth, maxWidth, minHeight, maxHeight)
					.onEvent(CompoundListener.sizeNeedsChanged);
			})//
			.build();
	}

	@Override
	public void install(QuickElement parent, Observable<?> until) {
		theListener.listen(parent, parent, until);
	}

	@Override
	public SizeGuide getWSizer(QuickElement parent, QuickElement[] children) {
		return getSizer(children, Orientation.horizontal);
	}

	@Override
	public SizeGuide getHSizer(QuickElement parent, QuickElement[] children) {
		return getSizer(children, Orientation.vertical);
	}

	/**
	 * Gets a sizer for a container in one dimension
	 *
	 * @param children The children to lay out
	 * @param orient The orientation to get the sizer for
	 * @return The size policy for the container of the given children in the given dimension
	 */
	protected SizeGuide getSizer(final QuickElement[] children, final Orientation orient) {
		return new SizeGuide() {
			@Override
			public int getMin(int crossSize, boolean csMax) {
				return get(LayoutGuideType.min, crossSize, csMax);
			}

			@Override
			public int getMinPreferred(int crossSize, boolean csMax) {
				return get(LayoutGuideType.minPref, crossSize, csMax);
			}

			@Override
			public int getPreferred(int crossSize, boolean csMax) {
				return get(LayoutGuideType.pref, crossSize, csMax);
			}

			@Override
			public int getMaxPreferred(int crossSize, boolean csMax) {
				return get(LayoutGuideType.maxPref, crossSize, csMax);
			}

			@Override
			public int getMax(int crossSize, boolean csMax) {
				return get(LayoutGuideType.max, crossSize, csMax);
			}

			@Override
			public int get(LayoutGuideType type, int crossSize, boolean csMax) {
				if (children.length == 0) {
					if (type == LayoutGuideType.max)
						return Integer.MAX_VALUE;
					return 0;
				}
				int[] res = new int[children.length];
				for (int i = 0; i < res.length; i++)
					res[i] = getSize(children[i], type, crossSize, csMax);
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

			private int getSize(QuickElement child, LayoutGuideType type, int crossSize, boolean csMax) {
				if (!LayoutUtils.checkLayoutChild(child))
					return 0;
				Sandbox sandbox = new Sandbox();
				Position lead = child.atts().get(LayoutAttributes.getPosAtt(orient, End.leading, null));
				Position trail = child.atts().get(LayoutAttributes.getPosAtt(orient, End.trailing, null));
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
				if (lead != null && trail != null && lead.getUnit() == LengthUnit.lexips && trail.getUnit() == LengthUnit.pixels)
					sandbox.createSpace(front, end, LayoutUtils.getLayoutSize(child, orient, type.opposite(), crossSize, csMax));
				else
					sandbox.createSpace(front, end, LayoutUtils.getLayoutSize(child, orient, type, crossSize, csMax));

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
					return (int) (endPos.getPixels() / (100 - endPos.getPercent()));
			}

			@Override
			public int getBaseline(int size) {
				if (children.length == 0)
					return 0;
				for (QuickElement child : children) {
					int childSize = LayoutUtils.getSize(child, orient, LayoutGuideType.pref, size, Integer.MAX_VALUE, true, null);
					int ret = child.bounds().get(orient).getGuide().getBaseline(childSize);
					if (ret < 0)
						continue;
					Position pos = children[0].atts().get(LayoutAttributes.getPosAtt(orient, End.leading, null));
					if (pos != null) {
						return ret + pos.evaluate(size);
					}
					pos = children[0].atts().get(LayoutAttributes.getPosAtt(orient, End.trailing, null));
					if (pos != null) {
						return size - pos.evaluate(size) - childSize + ret;
					}
					return ret;
				}
				return -1;
			}
		};
	}

	@Override
	public void layout(QuickElement parent, QuickElement[] children) {
		for (QuickElement child : children)
			layout(parent, child);
	}

	private void layout(QuickElement parent, QuickElement child) {
		Position pos1 = child.atts().get(LayoutAttributes.left);
		Position pos2 = child.atts().get(LayoutAttributes.right);
		int x, w;
		if (pos1 != null) {
			x = pos1.evaluate(parent.bounds().getWidth());
			if (pos2 != null)
				w = pos2.evaluate(parent.bounds().getWidth()) - x;
			else
				w = LayoutUtils.getSize(child, Orientation.horizontal, LayoutGuideType.pref, parent.bounds().getWidth(),
					parent.bounds().getHeight(), false, null);
		} else if (pos2 != null) {
			w = LayoutUtils.getSize(child, Orientation.horizontal, LayoutGuideType.pref, parent.bounds().getWidth(),
				parent.bounds().getHeight(), false, null);
			x = pos2.evaluate(parent.bounds().getWidth()) - w;
		} else {
			x = 0;
			w = LayoutUtils.getSize(child, Orientation.horizontal, LayoutGuideType.pref, parent.bounds().getWidth(),
				parent.bounds().getHeight(), false, null);
		}

		pos1 = child.atts().get(LayoutAttributes.top);
		pos2 = child.atts().get(LayoutAttributes.bottom);
		int y, h;
		if (pos1 != null) {
			y = pos1.evaluate(parent.bounds().getHeight());
			if (pos2 != null) {
				h = pos2.evaluate(parent.bounds().getHeight()) - y;
			} else
				h = LayoutUtils.getSize(child, Orientation.vertical, LayoutGuideType.pref, parent.bounds().getHeight(),
					parent.bounds().getWidth(), false, null);
		} else if (pos2 != null) {
			h = LayoutUtils.getSize(child, Orientation.vertical, LayoutGuideType.pref, parent.bounds().getHeight(),
				parent.bounds().getWidth(), false, null);
			y = pos2.evaluate(parent.bounds().getHeight()) - h;
		} else {
			y = 0;
			h = LayoutUtils.getSize(child, Orientation.vertical, LayoutGuideType.pref, parent.bounds().getHeight(),
				parent.bounds().getWidth(), false, null);
		}
		child.bounds().setBounds(x, y, w, h);
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

			public LayoutSize getPosition() {
				return thePosition;
			}

			void resolve(Space calling) {
				if (!isLeftAllowed && calling != null) {
					thePosition = new LayoutSize();
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
		}

		static class Space {
			public final Edge left;
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

		public Space createSpace(Edge left, Edge right, Size size) {
			return new Space(left, right, size);
		}

		public void resolve() {
			theRight.resolve(null);
			theLeft.resolve(null);
		}

		public LayoutSize getEnd() {
			if (!theRight.theConstraints.isEmpty())
				return theRight.getPosition();
			else
				return theLeft.getEnd(null, new LayoutSize(true));
		}
	}
}
