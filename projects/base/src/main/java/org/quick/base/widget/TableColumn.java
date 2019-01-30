package org.quick.base.widget;

import org.quick.core.QuickConstants.CoreStage;
import org.quick.core.QuickElement;
import org.quick.core.QuickTemplate;
import org.quick.core.layout.SimpleSizeGuide;
import org.quick.core.model.ModelAttributes;
import org.quick.core.tags.AcceptAttribute;
import org.quick.core.tags.QuickElementType;
import org.quick.core.tags.Template;

@QuickElementType(attributes = { //
	@AcceptAttribute(declaringClass = ModelAttributes.class, field = "name", required = false)//
})
@Template(location = "../../../../table-column.qts")
public class TableColumn extends QuickTemplate {
	private AttachPoint<QuickElement> theRendererAP;
	private AttachPoint<QuickElement> theEditorAP;

	public TableColumn() {
		life().runWhen(() -> {
			theRendererAP = (AttachPoint<QuickElement>) getTemplate().getAttachPoint("renderer");
			theEditorAP = (AttachPoint<QuickElement>) getTemplate().getAttachPoint("editor");
		}, CoreStage.INIT_CHILDREN, 1);
	}

	protected QuickElement getRenderer() {
		return getElement(theRendererAP).get();
	}

	protected QuickElement getEditor() {
		return getElement(theEditorAP).get();
	}

	public SimpleSizeGuide getCellSize(Object row) {
	}
}
