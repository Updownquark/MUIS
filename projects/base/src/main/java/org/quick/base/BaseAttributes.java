package org.quick.base;

import org.quick.base.model.QuickFormatter;
import org.quick.base.model.Validator;
import org.quick.core.QuickAttribute;
import org.quick.core.QuickProperty;
import org.quick.core.model.QuickDocumentModel;

/** Constant class containing attributes used in the base project */
public class BaseAttributes {
	/** Allows specification of the format used by the text field */
	public static final QuickAttribute<QuickFormatter<?>> format = new QuickAttribute<>("format", new QuickProperty.QuickTypeInstanceProperty<>(
		(Class<QuickFormatter<?>>) (Class<?>) QuickFormatter.class));

	/** The attribute allowing the user to specify a label that parses rich text */
	public static final QuickAttribute<Boolean> rich = new QuickAttribute<>("rich", org.quick.core.QuickProperty.boolAttr);

	/** Allows the user to specify the model whose content is displayed in this text field */
	public static final QuickAttribute<QuickDocumentModel> document = new QuickAttribute<>("doc", new QuickProperty.QuickTypeInstanceProperty<>(
		QuickDocumentModel.class));

	/** Allows specification of the format used by the text field */
	public static final QuickAttribute<Validator<?>> validator = new QuickAttribute<>("validator",
		new QuickProperty.QuickTypeInstanceProperty<>((Class<Validator<?>>) (Class<?>) Validator.class));
}
