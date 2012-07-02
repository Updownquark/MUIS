package org.muis.base.layout;

import org.muis.core.MuisAttribute;
import org.muis.core.style.Position;
import org.muis.core.style.PositionAttributeType;
import org.muis.core.style.Size;
import org.muis.core.style.SizeAttributeType;

public class LayoutConstants
{
	public static int getPositionValue(org.muis.core.MuisElement element, MuisAttribute<Position> attr, int totalLength, int defVal)
	{
		Position ret = element.getAttribute(attr);
		if(ret != null)
			return ret.evaluate(totalLength);
		else
			return defVal;
	}

	public static final MuisAttribute<Direction> direction = new MuisAttribute<Direction>("direction",
		new MuisAttribute.MuisEnumAttribute<>(Direction.class));

	public static final MuisAttribute<Alignment> alignment = new MuisAttribute<Alignment>("align", new MuisAttribute.MuisEnumAttribute<>(
		Alignment.class));

	public static final MuisAttribute<Region> region = new MuisAttribute<Region>("region", new MuisAttribute.MuisEnumAttribute<>(
		Region.class));

	public static final MuisAttribute<Boolean> maxInf = new MuisAttribute<Boolean>("max-inf", MuisAttribute.boolAttr);

	public static final MuisAttribute<Position> left = new MuisAttribute<Position>("left", PositionAttributeType.instance);

	public static final MuisAttribute<Position> minLeft = new MuisAttribute<Position>("min-left", PositionAttributeType.instance);

	public static final MuisAttribute<Position> maxLeft = new MuisAttribute<Position>("max-left", PositionAttributeType.instance);

	public static final MuisAttribute<Position> right = new MuisAttribute<Position>("right", PositionAttributeType.instance);

	public static final MuisAttribute<Position> minRight = new MuisAttribute<Position>("min-right", PositionAttributeType.instance);

	public static final MuisAttribute<Position> maxRight = new MuisAttribute<Position>("max-right", PositionAttributeType.instance);

	public static final MuisAttribute<Position> top = new MuisAttribute<Position>("top", PositionAttributeType.instance);

	public static final MuisAttribute<Position> minTop = new MuisAttribute<Position>("min-top", PositionAttributeType.instance);

	public static final MuisAttribute<Position> maxTop = new MuisAttribute<Position>("max-top", PositionAttributeType.instance);

	public static final MuisAttribute<Position> bottom = new MuisAttribute<Position>("bottom", PositionAttributeType.instance);

	public static final MuisAttribute<Position> minBottom = new MuisAttribute<Position>("min-bottom", PositionAttributeType.instance);

	public static final MuisAttribute<Position> maxBottom = new MuisAttribute<Position>("max-bottom", PositionAttributeType.instance);

	public static final MuisAttribute<Size> width = new MuisAttribute<Size>("width", SizeAttributeType.instance);

	public static final MuisAttribute<Size> minWidth = new MuisAttribute<Size>("min-width", SizeAttributeType.instance);

	public static final MuisAttribute<Size> maxWidth = new MuisAttribute<Size>("max-width", SizeAttributeType.instance);

	public static final MuisAttribute<Size> height = new MuisAttribute<Size>("height", SizeAttributeType.instance);

	public static final MuisAttribute<Size> minHeight = new MuisAttribute<Size>("min-height", SizeAttributeType.instance);

	public static final MuisAttribute<Size> maxHeight = new MuisAttribute<Size>("max-height", SizeAttributeType.instance);
}
