package org.quick.base;

import org.quick.base.model.QuickFormatter;
import org.quick.base.model.Validator;
import org.quick.core.model.QuickDocumentModel;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.prop.QuickPropertyType;

/** Constant class containing attributes used in the base project */
public class BaseAttributes {
	/** Allows specification of the format used by the text field */
	public static final QuickAttribute<QuickFormatter<?>> format = new QuickAttribute<>("format",
		QuickPropertyType.forTypeInstance((Class<QuickFormatter<?>>) (Class<?>) QuickFormatter.class));

	/** The attribute allowing the user to specify a label that parses rich text */
	public static final QuickAttribute<Boolean> rich = new QuickAttribute<>("rich", QuickPropertyType.boole);

	/** Allows the user to specify the model whose content is displayed in this text field */
	public static final QuickAttribute<QuickDocumentModel> document = new QuickAttribute<>("doc",
		QuickPropertyType.forTypeInstance(QuickDocumentModel.class));

	/** Allows specification of the format used by the text field */
	public static final QuickAttribute<Validator<?>> validator = new QuickAttribute<>("validator",
		QuickPropertyType.forTypeInstance((Class<Validator<?>>) (Class<?>) Validator.class));
}
