package org.muis.base;

import org.muis.base.model.MuisFormatter;
import org.muis.base.model.Validator;
import org.muis.core.MuisAttribute;
import org.muis.core.MuisProperty;
import org.muis.core.model.MuisDocumentModel;

/** Constant class containing attributes used in the base project */
public class BaseAttributes {
	/** Allows specification of the format used by the text field */
	public static final MuisAttribute<MuisFormatter<?>> format = new MuisAttribute<>("format", new MuisProperty.MuisTypeInstanceProperty<>(
		(Class<MuisFormatter<?>>) (Class<?>) MuisFormatter.class));

	/** The attribute allowing the user to specify a label that parses rich text */
	public static final MuisAttribute<Boolean> rich = new MuisAttribute<>("rich", org.muis.core.MuisProperty.boolAttr);

	/** Allows the user to specify the model whose content is displayed in this text field */
	public static final MuisAttribute<MuisDocumentModel> document = new MuisAttribute<>("doc", new MuisProperty.MuisTypeInstanceProperty<>(
		MuisDocumentModel.class));

	/** Allows specification of the format used by the text field */
	public static final MuisAttribute<Validator<?>> validator = new MuisAttribute<>("validator",
		new MuisProperty.MuisTypeInstanceProperty<>((Class<Validator<?>>) (Class<?>) Validator.class));
}
