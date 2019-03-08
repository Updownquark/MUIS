package org.quick.widget.base;

import org.quick.base.widget.Image;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.QuickWidgetDocument;

public class ImageWidget extends GenericImageWidget {
	public ImageWidget(QuickWidgetDocument doc, Image element, QuickWidget parent) {
		super(doc, element, parent);
	}

	@Override
	public Image getElement() {
		return (Image) super.getElement();
	}
}
