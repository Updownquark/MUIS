package org.muis.layout;

import org.muis.core.MuisAttribute;

public class LayoutConstants
{
	public static int getLayoutValue(org.muis.core.MuisElement element, MuisAttribute<Length> attr, int totalLength, int defVal)
	{
		Length ret = element.getAttribute(attr);
		if(ret != null)
			return ret.evaluate(totalLength);
		else
			return defVal;
	}

	public static final MuisAttribute<Length> left = new MuisAttribute<Length>("left", LengthAttributeType.instance);

	public static final MuisAttribute<Length> minLeft = new MuisAttribute<Length>("min-left", LengthAttributeType.instance);

	public static final MuisAttribute<Length> maxLeft = new MuisAttribute<Length>("max-left", LengthAttributeType.instance);

	public static final MuisAttribute<Length> right = new MuisAttribute<Length>("right", LengthAttributeType.instance);

	public static final MuisAttribute<Length> minRight = new MuisAttribute<Length>("min-right", LengthAttributeType.instance);

	public static final MuisAttribute<Length> maxRight = new MuisAttribute<Length>("max-right", LengthAttributeType.instance);

	public static final MuisAttribute<Length> top = new MuisAttribute<Length>("top", LengthAttributeType.instance);

	public static final MuisAttribute<Length> minTop = new MuisAttribute<Length>("min-top", LengthAttributeType.instance);

	public static final MuisAttribute<Length> maxTop = new MuisAttribute<Length>("max-top", LengthAttributeType.instance);

	public static final MuisAttribute<Length> bottom = new MuisAttribute<Length>("bottom", LengthAttributeType.instance);

	public static final MuisAttribute<Length> minBottom = new MuisAttribute<Length>("min-bottom", LengthAttributeType.instance);

	public static final MuisAttribute<Length> maxBottom = new MuisAttribute<Length>("max-bottom", LengthAttributeType.instance);

	public static final MuisAttribute<Length> width = new MuisAttribute<Length>("width", LengthAttributeType.instance);

	public static final MuisAttribute<Length> minWidth = new MuisAttribute<Length>("min-width", LengthAttributeType.instance);

	public static final MuisAttribute<Length> maxWidth = new MuisAttribute<Length>("max-width", LengthAttributeType.instance);

	public static final MuisAttribute<Length> height = new MuisAttribute<Length>("height", LengthAttributeType.instance);

	public static final MuisAttribute<Length> minHeight = new MuisAttribute<Length>("min-height", LengthAttributeType.instance);

	public static final MuisAttribute<Length> maxHeight = new MuisAttribute<Length>("max-height", LengthAttributeType.instance);

	public static final MuisAttribute<Boolean> included = new MuisAttribute<Boolean>("included", MuisAttribute.boolAttr);
}
