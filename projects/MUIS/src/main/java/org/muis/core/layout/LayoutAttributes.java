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

/** MUIS {@link MuisAttribute attributes} dealing with layouts in MUIS */
public class LayoutAttributes {
	/** An attribute specifying a position */
	public static class PositionAttribute extends MuisAttribute<Position> {
		/**
		 * @param name The name of the attribute
		 * @param orient The orientation of the dimension that this attribute specifies something about
		 * @param end Which direction along the orientation that this attribute deals with
		 * @param type This attribute's guide type
		 */
		public PositionAttribute(String name, Orientation orient, End end, LayoutGuideType type) {
			super(name, PositionPropertyType.instance);
		}
	}

	/** An attribute specifying a size */
	public static class SizeAttribute extends MuisAttribute<Size> {
		/**
		 * @param name The name of the attribute
		 * @param orient The orientation of the dimension that this attribute specifies something about
		 * @param type This attribute's guide type
		 */
		public SizeAttribute(String name, Orientation orient, LayoutGuideType type) {
			super(name, SizePropertyType.instance);
		}
	}

	/** Specifies the distance between the left edge of the container and that of the component */
	public static final PositionAttribute left = new PositionAttribute("left", horizontal, leading, null);

	/** Specifies the minimum distance between the left edge of the container and that of the component */
	public static final PositionAttribute minLeft = new PositionAttribute("min-left", horizontal, leading, min);

	/** Specifies the maximum distance between the left edge of the container and that of the component */
	public static final PositionAttribute maxLeft = new PositionAttribute("max-left", horizontal, leading, max);

	/** Specifies the distance between the right edge of the container and that of the component */
	public static final PositionAttribute right = new PositionAttribute("right", horizontal, trailing, null);

	/** Specifies the minimum distance between the right edge of the container and that of the component */
	public static final PositionAttribute minRight = new PositionAttribute("min-right", horizontal, trailing, min);

	/** Specifies the maximum distance between the right edge of the container and that of the component */
	public static final PositionAttribute maxRight = new PositionAttribute("max-right", horizontal, trailing, max);

	/** Specifies the distance between the top edge of the container and that of the component */
	public static final PositionAttribute top = new PositionAttribute("top", vertical, leading, null);

	/** Specifies the minimum distance between the top edge of the container and that of the component */
	public static final PositionAttribute minTop = new PositionAttribute("min-top", vertical, leading, min);

	/** Specifies the maximum distance between the top edge of the container and that of the component */
	public static final PositionAttribute maxTop = new PositionAttribute("max-top", vertical, leading, max);

	/** Specifies the distance between the bottom edge of the container and that of the component */
	public static final PositionAttribute bottom = new PositionAttribute("bottom", vertical, trailing, null);

	/** Specifies the minimum distance between the bottom edge of the container and that of the component */
	public static final PositionAttribute minBottom = new PositionAttribute("min-bottom", vertical, trailing, min);

	/** Specifies the maximum distance between the bottom edge of the container and that of the component */
	public static final PositionAttribute maxBottom = new PositionAttribute("max-bottom", vertical, trailing, max);

	/** Specifies the distance between the left edge and the right edge of the component */
	public static final SizeAttribute width = new SizeAttribute("width", horizontal, null);

	/** Specifies the minimum distance between the left edge and the right edge of the component */
	public static final SizeAttribute minWidth = new SizeAttribute("min-width", horizontal, min);

	/** Specifies the maximum distance between the left edge and the right edge of the component */
	public static final SizeAttribute maxWidth = new SizeAttribute("max-width", horizontal, max);

	/** Specifies the distance between the top edge and the bottom edge of the component */
	public static final SizeAttribute height = new SizeAttribute("height", vertical, null);

	/** Specifies the minimum distance between the top edge and the bottom edge of the component */
	public static final SizeAttribute minHeight = new SizeAttribute("min-height", vertical, min);

	/** Specifies the maximum distance between the top edge and the bottom edge of the component */
	public static final SizeAttribute maxHeight = new SizeAttribute("max-height", vertical, max);

	/**
	 * @param orient The orientation for the attribute
	 * @param end The end for the attribute
	 * @param type The guide type for the attribute
	 * @return The attribute with the given orientation, end, and type
	 * @throws IllegalArgumentException If no such attribute exists (preferred layout guide types)
	 */
	public static PositionAttribute getPosAtt(Orientation orient, End end, LayoutGuideType type) throws IllegalArgumentException {
		switch (orient) {
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
		throw new IllegalArgumentException("Unrecognized layout orientation type: " + orient);
	}

	/**
	 * @param orient The orientation for the attribute
	 * @param type The guide type for the attribute
	 * @return The attribute with the given orientation, end, and type, or null if no such attribute exists (preferred layout guide types)
	 */
	public static SizeAttribute getSizeAtt(Orientation orient, LayoutGuideType type) {
		switch (orient) {
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
				return null;
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
				return null;
			}
			throw new IllegalArgumentException("Unrecognized layout guide type: " + type);
		}
		throw new IllegalArgumentException("Unrecognized layout orientation type: " + orient);
	}

	/** @return All standard size attributes */
	public static SizeAttribute [] getSizeAttributes() {
		return new SizeAttribute[] {width, minWidth, maxWidth, height, minHeight, maxHeight};
	}

	/**
	 * @param element The element to get the position value for
	 * @param attr The position attribute to get the position value for
	 * @param totalLength The total length of the container along the attribute's orientation axis
	 * @param defVal Default value, in pixels, to return if the attribute is not set for the element
	 * @return The position value for the attribute on the element, in pixels
	 */
	public static int getPositionValue(org.muis.core.MuisElement element, PositionAttribute attr, int totalLength, int defVal) {
		Position ret = element.atts().get(attr);
		if(ret != null)
			return ret.evaluate(totalLength);
		else
			return defVal;
	}

	/** Direction for a layout or component (left-to-right, right-to-left, top-to-bottom, or bottom-to-top */
	public static final MuisAttribute<Direction> direction = new MuisAttribute<>("direction", new MuisProperty.MuisEnumProperty<>(
		Direction.class));

	/** Orientation for a layout or component */
	public static final MuisAttribute<Orientation> orientation = new MuisAttribute<>("orientation", new MuisProperty.MuisEnumProperty<>(
		Orientation.class));

	/** Alignment of components within a container along the major axis of the container or layout */
	public static final MuisAttribute<Alignment> alignment = new MuisAttribute<>("align", new MuisProperty.MuisEnumProperty<>(
		Alignment.class));

	/** Alignment of components within a container along the minor axis of the container or layout */
	public static final MuisAttribute<Alignment> crossAlignment = new MuisAttribute<>("cross-align", new MuisProperty.MuisEnumProperty<>(
		Alignment.class));

	/** Edge (or center) of a container */
	public static final MuisAttribute<Region> region = new MuisAttribute<>("region", new MuisProperty.MuisEnumProperty<>(Region.class));

	/**
	 * <ul>
	 * <li><b>If true:</b> The container's maximum size will be related to the sizes of its contents. These contents will fill out the
	 * container's available space even if the container up to its maximum.</li>
	 * <li><b>If false:</b> Typically the default. The container's maximum size is infinite. Its contents will be sized no larger than their
	 * {@link LayoutGuideType#maxPref preferred maximum} sizes even if the container's size allows much more room.</li>
	 * </ul>
	 */
	public static final MuisAttribute<Boolean> fillContainer = new MuisAttribute<>("fill-container", MuisProperty.boolAttr);
}
