package org.wam.layout;

import org.wam.core.WamAttribute;

public interface LayoutConstants
{
	public static final WamAttribute<Length> left = new WamAttribute<Length>("left", LengthAttributeType.instance);

	public static final WamAttribute<Length> minLeft = new WamAttribute<Length>("min-left",
		LengthAttributeType.instance);

	public static final WamAttribute<Length> maxLeft = new WamAttribute<Length>("max-left",
		LengthAttributeType.instance);

	public static final WamAttribute<Length> right = new WamAttribute<Length>("right", LengthAttributeType.instance);

	public static final WamAttribute<Length> minRight = new WamAttribute<Length>("min-right",
		LengthAttributeType.instance);

	public static final WamAttribute<Length> maxRight = new WamAttribute<Length>("max-right",
		LengthAttributeType.instance);

	public static final WamAttribute<Length> top = new WamAttribute<Length>("top", LengthAttributeType.instance);

	public static final WamAttribute<Length> minTop = new WamAttribute<Length>("min-top", LengthAttributeType.instance);

	public static final WamAttribute<Length> maxTop = new WamAttribute<Length>("max-top", LengthAttributeType.instance);

	public static final WamAttribute<Length> bottom = new WamAttribute<Length>("bottom", LengthAttributeType.instance);

	public static final WamAttribute<Length> minBottom = new WamAttribute<Length>("min-bottom",
		LengthAttributeType.instance);

	public static final WamAttribute<Length> maxBottom = new WamAttribute<Length>("max-bottom",
		LengthAttributeType.instance);

	public static final WamAttribute<Length> width = new WamAttribute<Length>("width", LengthAttributeType.instance);

	public static final WamAttribute<Length> minWidth = new WamAttribute<Length>("min-width",
		LengthAttributeType.instance);

	public static final WamAttribute<Length> maxWidth = new WamAttribute<Length>("max-width",
		LengthAttributeType.instance);

	public static final WamAttribute<Length> height = new WamAttribute<Length>("height", LengthAttributeType.instance);

	public static final WamAttribute<Length> minHeight = new WamAttribute<Length>("min-height",
		LengthAttributeType.instance);

	public static final WamAttribute<Length> maxHeight = new WamAttribute<Length>("max-height",
		LengthAttributeType.instance);

	public static final WamAttribute<Boolean> included = new WamAttribute<Boolean>("included", WamAttribute.boolAttr);
}
