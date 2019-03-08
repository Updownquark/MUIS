package org.quick.base.widget;

import org.quick.core.layout.Orientation;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.prop.QuickPropertyType;
import org.quick.core.tags.AcceptAttribute;
import org.quick.core.tags.QuickElementType;

/** A widget with the capability to rotate and/or reflect its contents */
@QuickElementType(//
	attributes = { //
		@AcceptAttribute(declaringClass = Transform.class, field = "flip"), //
		@AcceptAttribute(declaringClass = Transform.class, field = "rotate"), //
		@AcceptAttribute(declaringClass = Transform.class, field = "scale"), //
		@AcceptAttribute(declaringClass = Transform.class, field = "scaleX"), //
		@AcceptAttribute(declaringClass = Transform.class, field = "scaleY"),//
	})
public class Transform extends SimpleContainer {
	/** The attribute allowing the user to reflect this widget's contents across either the x or the y axis */
	public static final QuickAttribute<Orientation> flip = QuickAttribute.build("flip", QuickPropertyType.forEnum(Orientation.class))
		.build();

	/** The attribute allowing the user to rotate this widget's contents. In clockwise degrees. */
	public static final QuickAttribute<Double> rotate = QuickAttribute.build("rotate", QuickPropertyType.floating).build();

	/** The attribute allowing the user to scale this widget's contents */
	public static final QuickAttribute<Double> scale = QuickAttribute.build("scale", QuickPropertyType.floating).build();

	/** The attribute allowing the user to scale this widget's contents' width */
	public static final QuickAttribute<Double> scaleX = QuickAttribute.build("scale-x", QuickPropertyType.floating).build();

	/** The attribute allowing the user to scale this widget's contents' height */
	public static final QuickAttribute<Double> scaleY = QuickAttribute.build("scale-y", QuickPropertyType.floating).build();

	/** @return This widget's contents block */
	public Block getContents() {
		return (Block) getElement(getTemplate().getAttachPoint("contents")).get();
	}
}
