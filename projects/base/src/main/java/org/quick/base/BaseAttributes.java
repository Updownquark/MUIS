package org.quick.base;

import java.awt.Color;

import org.observe.ObservableValue;
import org.quick.base.model.Formats;
import org.quick.base.model.QuickFormatter;
import org.quick.base.model.Validator;
import org.quick.core.model.QuickDocumentModel;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.prop.QuickPropertyType;

import com.google.common.reflect.TypeToken;

/** Constant class containing attributes used in the base project */
public class BaseAttributes {
	/** Allows specification of the format used by a text field */
	public static final QuickAttribute<QuickFormatter<?>> format = QuickAttribute.build("format", QuickPropertyType
		.forTypeInstance((Class<QuickFormatter<?>>) (Class<?>) QuickFormatter.class, builder -> builder.buildContext(ctx -> {
			ctx.withValue("string", ObservableValue.constant(new TypeToken<QuickFormatter<String>>() {}, Formats.string))
				.withValue("number", ObservableValue.constant(new TypeToken<QuickFormatter<Number>>() {}, Formats.number))
				.withValue("integer", ObservableValue.constant(new TypeToken<QuickFormatter<Integer>>() {}, Formats.integer))
				.withValue("color", ObservableValue.constant(new TypeToken<QuickFormatter<Color>>() {}, Formats.color))
				.withValue("default", ObservableValue.constant(new TypeToken<QuickFormatter<Object>>() {}, Formats.def));
		}))).build();

	/** Allows specification of the factory to create the format used by a text field */
	public static final QuickAttribute<QuickFormatter.Factory<?>> formatFactory = QuickAttribute.build("format-factory",
		QuickPropertyType.forTypeInstance((Class<QuickFormatter.Factory<?>>) (Class<?>) QuickFormatter.Factory.class, null)).build();

	/** The attribute allowing the user to specify a label that parses rich text */
	public static final QuickAttribute<Boolean> rich = QuickAttribute.build("rich", QuickPropertyType.boole).build();

	/** Allows the user to specify the model whose content is displayed in a text field */
	public static final QuickAttribute<QuickDocumentModel> document = QuickAttribute
		.build("document", QuickPropertyType.forTypeInstance(QuickDocumentModel.class, null)).build();

	/** Allows specification of the format used by a text field */
	public static final QuickAttribute<Validator<?>> validator = QuickAttribute
		.build("validator", QuickPropertyType.forTypeInstance((Class<Validator<?>>) (Class<?>) Validator.class, null)).build();
}
