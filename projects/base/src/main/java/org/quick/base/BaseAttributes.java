package org.quick.base;

import org.quick.base.model.QuickFormatter;
import org.quick.base.model.Validator;
import org.quick.core.model.QuickDocumentModel;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.prop.QuickPropertyType;

/** Constant class containing attributes used in the base project */
public class BaseAttributes {
	/** Allows specification of the format used by the text field */
	public static final QuickAttribute<QuickFormatter<?>> format = QuickAttribute
		.build("format", QuickPropertyType.forTypeInstance((Class<QuickFormatter<?>>) (Class<?>) QuickFormatter.class)).build();

	/** The attribute allowing the user to specify a label that parses rich text */
	public static final QuickAttribute<Boolean> rich = QuickAttribute.build("rich", QuickPropertyType.boole).build();

	/** Allows the user to specify the model whose content is displayed in this text field */
	public static final QuickAttribute<QuickDocumentModel> document = QuickAttribute
		.build("doc", QuickPropertyType.forTypeInstance(QuickDocumentModel.class)).build();

	/** Allows specification of the format used by the text field */
	public static final QuickAttribute<Validator<?>> validator = QuickAttribute
		.build("validator", QuickPropertyType.forTypeInstance((Class<Validator<?>>) (Class<?>) Validator.class)).build();
}
