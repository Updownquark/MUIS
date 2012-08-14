package org.muis.base.layout;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisProperty;
import org.muis.core.style.Position;
import org.muis.core.style.PositionPropertyType;
import org.muis.core.style.Size;
import org.muis.core.style.SizePropertyType;

public class LayoutConstants {
	public static int getPositionValue(org.muis.core.MuisElement element, MuisAttribute<Position> attr, int totalLength, int defVal) {
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

	public static final MuisAttribute<Boolean> maxInf = new MuisAttribute<Boolean>("max-inf", MuisProperty.boolAttr);

	public static final MuisAttribute<Position> left = new MuisAttribute<Position>("left", PositionPropertyType.instance);

	public static final MuisAttribute<Position> minLeft = new MuisAttribute<Position>("min-left", PositionPropertyType.instance);

	public static final MuisAttribute<Position> maxLeft = new MuisAttribute<Position>("max-left", PositionPropertyType.instance);

	public static final MuisAttribute<Position> right = new MuisAttribute<Position>("right", PositionPropertyType.instance);

	public static final MuisAttribute<Position> minRight = new MuisAttribute<Position>("min-right", PositionPropertyType.instance);

	public static final MuisAttribute<Position> maxRight = new MuisAttribute<Position>("max-right", PositionPropertyType.instance);

	public static final MuisAttribute<Position> top = new MuisAttribute<Position>("top", PositionPropertyType.instance);

	public static final MuisAttribute<Position> minTop = new MuisAttribute<Position>("min-top", PositionPropertyType.instance);

	public static final MuisAttribute<Position> maxTop = new MuisAttribute<Position>("max-top", PositionPropertyType.instance);

	public static final MuisAttribute<Position> bottom = new MuisAttribute<Position>("bottom", PositionPropertyType.instance);

	public static final MuisAttribute<Position> minBottom = new MuisAttribute<Position>("min-bottom", PositionPropertyType.instance);

	public static final MuisAttribute<Position> maxBottom = new MuisAttribute<Position>("max-bottom", PositionPropertyType.instance);

	public static final MuisAttribute<Size> width = new MuisAttribute<Size>("width", SizePropertyType.instance);

	public static final MuisAttribute<Size> minWidth = new MuisAttribute<Size>("min-width", SizePropertyType.instance);

	public static final MuisAttribute<Size> maxWidth = new MuisAttribute<Size>("max-width", SizePropertyType.instance);

	public static final MuisAttribute<Size> height = new MuisAttribute<Size>("height", SizePropertyType.instance);

	public static final MuisAttribute<Size> minHeight = new MuisAttribute<Size>("min-height", SizePropertyType.instance);

	public static final MuisAttribute<Size> maxHeight = new MuisAttribute<Size>("max-height", SizePropertyType.instance);
}
