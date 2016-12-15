package org.quick.base.widget;

import org.quick.core.QuickTemplate;
import org.quick.core.layout.LayoutAttributes;
import org.quick.core.tags.AcceptAttribute;
import org.quick.core.tags.QuickElementType;
import org.quick.core.tags.Template;

/** The base class for several widgets that are at their core simple containers with additional functionality */
@Template(location = "../../../../simple-container.qts")
@QuickElementType(//
	attributes = { //
		@AcceptAttribute(declaringClass = LayoutAttributes.class, field = "direction", defaultValue = "down"), //
		@AcceptAttribute(declaringClass = LayoutAttributes.class, field = "alignment", defaultValue = "begin"), //
		@AcceptAttribute(declaringClass = LayoutAttributes.class, field = "crossAlignment", defaultValue = "center")//
	})
public abstract class SimpleContainer extends QuickTemplate {
}
