package org.quick.base.layout.generic;

import java.util.LinkedHashMap;
import java.util.Map;

import org.quick.core.layout.*;

public class BoxLayoutSolver<B> {
	public interface State<B> {
		BoxLayoutSolver<B> getSolver();

		BoxState getBounds();

		EdgeState getEdge(EdgeDef edge);

		BoxState getBox(BoxDef box);

		State<B> stretchHorizontal(float hTension, int height, boolean heightMax);

		State<B> stretchVertical(float vTension, int width, boolean widthMax);

		State<B> layout(int width, boolean max);
	}

	private static final LayoutSpringEvaluator DEFAULT_MARGIN = LayoutSpringEvaluator.forSizer(() -> 0,
		new SimpleSizeGuide(0, 0, 2, 10000, 10000));
	private static final LayoutSpringEvaluator DEFAULT_PADDING = LayoutSpringEvaluator.forSizer(() -> 0,
		new SimpleSizeGuide(0, 0, 3, 10000, 10000));

	private final Orientation theAxis;

	private final BoxDefImpl<B> theBounds;
	private final Map<B, BoxDefImpl<B>> theBoxes;
	private Alignment theMajorAlignment;
	private Alignment theMinorAlignment;
	private LayoutSpringEvaluator theMargin;
	private LayoutSpringEvaluator thePadding;

	public BoxLayoutSolver(Orientation axis) {
		theAxis = axis;
		theBoxes = new LinkedHashMap<>();
		theMajorAlignment = Alignment.begin;
		theMinorAlignment = Alignment.center;
		theMargin = DEFAULT_MARGIN;
		thePadding = DEFAULT_PADDING;
		theBounds = new BoxDefImpl<>((B) "bounds", LayoutSpringEvaluator.BOUNDS_TENSION, LayoutSpringEvaluator.BOUNDS_TENSION);
	}

	public BoxLayoutSolver<B> withMargin(LayoutSpringEvaluator margin) {
		theMargin = margin;
		return this;
	}

	public BoxLayoutSolver<B> withPadding(LayoutSpringEvaluator padding) {
		thePadding = padding;
		return this;
	}

	public BoxDef getOrCreateBox(B boxObject, LayoutSpringEvaluator width, LayoutSpringEvaluator height) {
		return theBoxes.computeIfAbsent(boxObject, bo -> new BoxDefImpl<>(boxObject, width, height));
	}

	public State<B> use() {
		return new BoxLayoutStateImpl();
	}

	class BoxLayoutStateImpl implements State<B> {
		private final BoxStateImpl<B> theBoundState;
		private final Map<B, BoxStateImpl<B>> theBoxStates;

		BoxLayoutStateImpl() {
			theBoundState = new BoxStateImpl<>(theBounds, null);
			theBoxStates = new LinkedHashMap<>();
			for (BoxDefImpl<B> box : theBoxes.values())
				theBoxStates.put(box.value, new BoxStateImpl<>(box, theBoundState));
		}

		@Override
		public BoxLayoutSolver<B> getSolver() {
			return BoxLayoutSolver.this;
		}

		@Override
		public BoxState getBounds() {
			return theBoundState;
		}

		@Override
		public EdgeState getEdge(EdgeDef edge) {
			BoxStateImpl<B> boxState = (BoxStateImpl<B>) getBox(((EdgeDefImpl) edge).box);
			if (edge == boxState.def.left)
				return boxState.left;
			else if (edge == boxState.def.right)
				return boxState.right;
			else if (edge == boxState.def.top)
				return boxState.top;
			else
				return boxState.bottom;
		}

		@Override
		public BoxState getBox(BoxDef box) {
			if (box == theBounds)
				return theBoundState;
			else
				return theBoxStates.get(((BoxDefImpl<B>) box).value);
		}

		@Override
		public State<B> stretchHorizontal(float hTension, int height, boolean heightMax) {
			if (theAxis.isVertical())
				stretchMinor(hTension, height, heightMax);
			else
				stretchMajor(hTension, height, heightMax);
			return this;
		}

		@Override
		public State<B> stretchVertical(float vTension, int width, boolean widthMax) {
			if (theAxis.isVertical())
				stretchMajor(vTension, width, widthMax);
			else
				stretchMinor(vTension, width, widthMax);
			return this;
		}

		@Override
		public State<B> layout(int width, boolean max) {
			int todo = todo;// TODO Auto-generated method stub
			return this;
		}

		private void stretchMajor(float tension, int minorSize, boolean sizeMax) {

			int todo = todo;// TODO Auto-generated method stub
		}

		private void stretchMinor(float tension, int majorSize, boolean sizeMax) {
			int todo = todo;// TODO Auto-generated method stub
		}

		private void doLayout() {
			// We'll assume here that preferred sizes will never be huge (i.e. wrap around to negative on addition)
			LayoutSize majorMargin = theMargin.addSize(new LayoutSize(false), 0);
			LayoutSize major = new LayoutSize(majorMargin);
			LayoutSize temp = new LayoutSize();
			for (BoxStateImpl<B> box : theBoxStates.values()) {
				if (Float.isNaN(theMinorTension)) {
					// TODO
				} else {
					// Don't care about the margins, just the box's minor size
					box.getEdge(theAxis.opposite(), End.leading).setPosition(0);
					box.getEdge(theAxis.opposite(), End.trailing)
						.setPosition(box.def.getLength(theAxis.opposite()).addSize(temp.clear(), theMinorTension));

				}
			}
		}

		private void stretch() {
		}
	}

	static class BoxDefImpl<B> implements BoxDef {
		final B value;
		final LayoutSpringEvaluator width;
		final LayoutSpringEvaluator height;
		final EdgeDefImpl left;
		final EdgeDefImpl right;
		final EdgeDefImpl top;
		final EdgeDefImpl bottom;

		BoxDefImpl(B value, LayoutSpringEvaluator width, LayoutSpringEvaluator height) {
			this.value = value;
			this.width = width;
			this.height = height;
			left = new EdgeDefImpl(this, Orientation.horizontal, End.leading);
			right = new EdgeDefImpl(this, Orientation.horizontal, End.trailing);
			top = new EdgeDefImpl(this, Orientation.vertical, End.leading);
			bottom = new EdgeDefImpl(this, Orientation.vertical, End.trailing);
		}

		LayoutSpringEvaluator getLength(Orientation axis) {
			return axis.isVertical() ? height : width;
		}

		@Override
		public EdgeDef getLeft() {
			return left;
		}

		@Override
		public EdgeDef getRight() {
			return right;
		}

		@Override
		public EdgeDef getTop() {
			return top;
		}

		@Override
		public EdgeDef getBottom() {
			return bottom;
		}
	}

	static class EdgeDefImpl implements EdgeDef {
		final BoxDefImpl<?> box;
		final Orientation orientation;
		final End end;

		EdgeDefImpl(BoxDefImpl<?> box, Orientation orientation, End end) {
			this.box = box;
			this.orientation = orientation;
			this.end = end;
		}

		@Override
		public Orientation getOrientation() {
			return orientation;
		}
	}

	static class BoxStateImpl<B> implements BoxState {
		final BoxDefImpl<B> def;
		final EdgeStateImpl left;
		final EdgeStateImpl right;
		final EdgeStateImpl top;
		final EdgeStateImpl bottom;

		BoxStateImpl(BoxDefImpl<B> def, BoxState bounds) {
			this.def = def;
			left = new EdgeStateImpl(def.left, bounds);
			right = new EdgeStateImpl(def.right, bounds);
			top = new EdgeStateImpl(def.top, bounds);
			bottom = new EdgeStateImpl(def.bottom, bounds);
		}

		@Override
		public EdgeStateImpl getEdge(Orientation orientation, End end) {
			return (EdgeStateImpl) BoxState.super.getEdge(orientation, end);
		}

		@Override
		public EdgeState getLeft() {
			return left;
		}

		@Override
		public EdgeState getRight() {
			return right;
		}

		@Override
		public EdgeState getTop() {
			return top;
		}

		@Override
		public EdgeState getBottom() {
			return bottom;
		}
	}

	static class EdgeStateImpl implements EdgeState {
		final EdgeDefImpl def;
		final BoxState bounds;
		int pixels;
		float percent;

		EdgeStateImpl(EdgeDefImpl def, BoxState bounds) {
			this.def = def;
			this.bounds = bounds;
		}

		void setPosition(int pixels) {
			this.pixels = pixels;
			percent = 0;
		}

		void setPosition(LayoutSize size) {
			pixels = size.getPixels();
			percent = size.getPercent();
		}

		@Override
		public Orientation getOrientation() {
			return def.orientation;
		}

		@Override
		public int getPosition() {
			int position = pixels;
			if (percent != 0f)
				position += bounds.getEdge(def.getOrientation(), End.trailing).getPosition();
			return position;
		}

		@Override
		public float getForce() {
			return 0; // We don't do force this way here
		}
	}
}
