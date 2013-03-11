package org.muis.core.layout;

import static org.muis.core.layout.End.leading;
import static org.muis.core.layout.End.trailing;
import static org.muis.core.layout.LayoutGuideType.max;
import static org.muis.core.layout.LayoutGuideType.min;
import static org.muis.core.layout.Orientation.horizontal;
import static org.muis.core.layout.Orientation.vertical;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisProperty;
import org.muis.core.style.Position;
import org.muis.core.style.PositionPropertyType;
import org.muis.core.style.Size;
import org.muis.core.style.SizePropertyType;

public class LayoutAttributes {
	public static class PositionAttribute extends MuisAttribute<Position> {
		public PositionAttribute(String name, Orientation orientation, End end, LayoutGuideType type) {
			super(name, PositionPropertyType.instance);
		}
	}

	public static class SizeAttribute extends MuisAttribute<Size> {
		public SizeAttribute(String name, Orientation orientation, LayoutGuideType type) {
			super(name, SizePropertyType.instance);
		}
	}

	public static final PositionAttribute left = new PositionAttribute("left", horizontal, leading, null);

	public static final PositionAttribute minLeft = new PositionAttribute("min-left", horizontal, leading, min);

	public static final PositionAttribute maxLeft = new PositionAttribute("max-left", horizontal, leading, max);

	public static final PositionAttribute right = new PositionAttribute("right", horizontal, trailing, null);

	public static final PositionAttribute minRight = new PositionAttribute("min-right", horizontal, trailing, min);

	public static final PositionAttribute maxRight = new PositionAttribute("max-right", horizontal, trailing, max);

	public static final PositionAttribute top = new PositionAttribute("top", vertical, leading, null);

	public static final PositionAttribute minTop = new PositionAttribute("min-top", vertical, leading, min);

	public static final PositionAttribute maxTop = new PositionAttribute("max-top", vertical, leading, max);

	public static final PositionAttribute bottom = new PositionAttribute("bottom", vertical, trailing, null);

	public static final PositionAttribute minBottom = new PositionAttribute("min-bottom", vertical, trailing, min);

	public static final PositionAttribute maxBottom = new PositionAttribute("max-bottom", vertical, trailing, max);

	public static final SizeAttribute width = new SizeAttribute("width", horizontal, null);

	public static final SizeAttribute minWidth = new SizeAttribute("min-width", horizontal, min);

	public static final SizeAttribute maxWidth = new SizeAttribute("max-width", horizontal, max);

	public static final SizeAttribute height = new SizeAttribute("height", vertical, null);

	public static final SizeAttribute minHeight = new SizeAttribute("min-height", vertical, min);

	public static final SizeAttribute maxHeight = new SizeAttribute("max-height", vertical, max);

	public static PositionAttribute getPosAtt(Orientation orientation, End end, LayoutGuideType type) {
		switch (orientation) {
		case horizontal:
			switch (end) {
			case leading:
				if(type == null)
					return left;
				switch (type) {
				case min:
					return minLeft;
				case max:
					return maxLeft;
				case minPref:
				case pref:
				case maxPref:
					throw new IllegalArgumentException("No position attribute for layout guide type: " + type);
				}
				throw new IllegalArgumentException("Unrecognized layout guide type: " + type);
			case trailing:
				if(type == null)
					return right;
				switch (type) {
				case min:
					return minRight;
				case max:
					return maxRight;
				case minPref:
				case pref:
				case maxPref:
					throw new IllegalArgumentException("No position attribute for layout guide type: " + type);
				}
				throw new IllegalArgumentException("Unrecognized layout guide type: " + type);
			}
			throw new IllegalArgumentException("Unrecognized layout end type: " + end);
		case vertical:
			switch (end) {
			case leading:
				if(type == null)
					return top;
				switch (type) {
				case min:
					return minTop;
				case max:
					return maxTop;
				case minPref:
				case pref:
				case maxPref:
					throw new IllegalArgumentException("No position attribute for layout guide type: " + type);
				}
				throw new IllegalArgumentException("Unrecognized layout guide type: " + type);
			case trailing:
				if(type == null)
					return bottom;
				switch (type) {
				case min:
					return minBottom;
				case max:
					return maxBottom;
				case minPref:
				case pref:
				case maxPref:
					throw new IllegalArgumentException("No position attribute for layout guide type: " + type);
				}
				throw new IllegalArgumentException("Unrecognized layout guide type: " + type);
			}
			throw new IllegalArgumentException("Unrecognized layout end type: " + end);
		}
		throw new IllegalArgumentException("Unrecognized layout orientation type: " + orientation);
	}

	public static SizeAttribute getSizeAtt(Orientation orientation, LayoutGuideType type) {
		switch (orientation) {
		case horizontal:
			if(type == null)
				return width;
			switch (type) {
			case min:
				return minWidth;
			case max:
				return maxWidth;
			case minPref:
			case pref:
			case maxPref:
				throw new IllegalArgumentException("No position attribute for layout guide type: " + type);
			}
			throw new IllegalArgumentException("Unrecognized layout guide type: " + type);
		case vertical:
			if(type == null)
				return height;
			switch (type) {
			case min:
				return minHeight;
			case max:
				return maxHeight;
			case minPref:
			case pref:
			case maxPref:
				throw new IllegalArgumentException("No position attribute for layout guide type: " + type);
			}
			throw new IllegalArgumentException("Unrecognized layout guide type: " + type);
		}
		throw new IllegalArgumentException("Unrecognized layout orientation type: " + orientation);
	}

	public static int getPositionValue(org.muis.core.MuisElement element, PositionAttribute attr, int totalLength, int defVal) {
		Position ret = element.atts().get(attr);
		if(ret != null)
			return ret.evaluate(totalLength);
		else
			return defVal;
	}

	public static final MuisAttribute<Direction> direction = new MuisAttribute<Direction>("direction", new MuisProperty.MuisEnumProperty<>(
		Direction.class));

	public static final MuisAttribute<Alignment> alignment = new MuisAttribute<Alignment>("align", new MuisProperty.MuisEnumProperty<>(
		Alignment.class));

	public static final MuisAttribute<Region> region = new MuisAttribute<Region>("region",
		new MuisProperty.MuisEnumProperty<>(Region.class));

	public static final MuisAttribute<Size> margin = new MuisAttribute<>("margin", SizePropertyType.instance);

	public static final MuisAttribute<Size> padding = new MuisAttribute<>("padding", SizePropertyType.instance);

	public static final MuisAttribute<Boolean> maxInf = new MuisAttribute<Boolean>("max-inf", MuisProperty.boolAttr);
}
