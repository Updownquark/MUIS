package org.quick;

import java.awt.Color;
import java.awt.Cursor;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.junit.Assert;
import org.junit.Test;
import org.observe.*;
import org.quick.core.QuickEnvironment;
import org.quick.core.QuickParseEnv;
import org.quick.core.model.DefaultQuickModel;
import org.quick.core.model.ModelAttributes;
import org.quick.core.parser.QuickParseException;
import org.quick.core.parser.QuickPropertyParser;
import org.quick.core.parser.SimpleParseEnv;
import org.quick.core.prop.*;
import org.quick.core.style.*;

import com.google.common.reflect.TypeToken;

/** Tests property parsing */
public class PropertyTest {
	private final QuickAttribute<Duration> durationAtt = QuickAttribute.build("duration", QuickPropertyType.duration).build();
	private final QuickAttribute<Double> doubleAtt = QuickAttribute.build("double", QuickPropertyType.floating).build();
	private final QuickAttribute<Integer> intAtt = QuickAttribute.build("int", QuickPropertyType.integer).build();
	private final QuickProperty<Color> colorAtt = BackgroundStyle.color;

	/*@Test
	public void testEnumFunctionBug() {
		try {
			ExpressionFunction.build(makeEnumFunction(Direction.class));
			Assert.fail("The InternalError thrown from this method must have been fixed."
				+ "  Remove the ExpressionFunction.build(TypeToken, ...) methods");
		} catch (InternalError error) {
		}
	}

	private <T extends Enum<T>> Function<String, T> makeEnumFunction(Class<T> enumType) {
		return new Function<String, T>() {
			@Override
			public T apply(String name) {
				return Enum.valueOf(enumType, name);
			}
		};
	}*/

	/** Tests basic property parsing--literal values, no variables */
	@Test
	public void testBasicProperties() {
		QuickEnvironment env = QuickEnvironment.build().withDefaults().build();
		QuickPropertyParser propParser = env.getPropertyParser();
		QuickProperty<Integer> intProp = QuickAttribute.build("int", QuickPropertyType.integer).build();
		QuickProperty<Long> longProp = QuickAttribute.build("long", QuickPropertyType.build("long", TypeToken.of(Long.class)).build())
			.build();
		QuickProperty<Double> doubleProp = BackgroundStyle.transparency;
		QuickProperty<Float> floatProp = QuickAttribute.build("float", QuickPropertyType.build("float", TypeToken.of(Float.class)).build())
			.build();
		QuickProperty<Boolean> boolProp = FontStyle.strike;
		QuickProperty<String> stringProp = QuickAttribute.build("string", QuickPropertyType.string).build();
		QuickProperty<Character> charProp = QuickAttribute
			.build("char", QuickPropertyType.build("char", TypeToken.of(Character.class)).build()).build();
		try {
			// Literals
			Assert.assertEquals(Integer.valueOf(0), propParser.parseProperty(intProp, env, "0").get());
			Assert.assertEquals(Long.valueOf(0xff1L), propParser.parseProperty(longProp, env, "0xff1l").get());
			Assert.assertEquals(Double.valueOf(0.5), propParser.parseProperty(doubleProp, env, "0.5").get());
			Assert.assertEquals(Float.valueOf(0x1.ffp-1f), propParser.parseProperty(floatProp, env, "0x1.ffp-1f").get());
			Assert.assertEquals(Boolean.TRUE, propParser.parseProperty(boolProp, env, "true").get());
			Assert.assertEquals("\"\"", propParser.parseProperty(stringProp, env, "\"\"").get());
			Assert.assertEquals("", propParser.parseProperty(stringProp, env, "${\"\"}").get());
			Assert.assertEquals("A string\u00b0\t8 4", propParser.parseProperty(stringProp, env, "${\"A string\\u00b0\\t8 4\"}").get());
			Assert.assertEquals(Character.valueOf('"'), propParser.parseProperty(charProp, env, "${'\"'}").get());
			Assert.assertEquals(Character.valueOf('\u00b0'), propParser.parseProperty(charProp, env, "${'\\u00b0'}").get());
			Assert.assertEquals(2.0, //
				propParser.parseProperty(doubleProp, env, "2")//
					.get().doubleValue(),
				0);

			Assert.assertEquals(Colors.green, //
				propParser.parseProperty(BackgroundStyle.color, env, "green")//
					.get());

			Assert.assertEquals(new Color(128, 196, 255), //
				propParser.parseProperty(BackgroundStyle.color, env, "rgb(128, 196, 255)")//
					.get());

			Assert.assertEquals(Color.getHSBColor(.5f, .4f, .8f), //
				propParser.parseProperty(BackgroundStyle.color, env, "hsb(.5, .4, .8)")//
					.get());

			Assert.assertEquals(new Size(2, LengthUnit.pixels), //
				propParser.parseProperty(BackgroundStyle.cornerRadius, env, "2")//
					.get());

			Assert.assertEquals(new Size(2, LengthUnit.pixels), //
				propParser.parseProperty(BackgroundStyle.cornerRadius, env, "2px")//
					.get());

			Assert.assertEquals(new Size(2, LengthUnit.lexips), //
				propParser.parseProperty(BackgroundStyle.cornerRadius, env, "2xp")//
					.get());

			try {
				propParser.parseProperty(BackgroundStyle.cornerRadius, env, "\"\"").get();
				throw new IllegalStateException("Should have thrown a QuickParseException");
			} catch (QuickParseException e) {
			}

			// Parsing an invalid value is allowed; validation is done outside of parsing
			propParser.parseProperty(BackgroundStyle.cornerRadius, env, "-2").get();

			Assert.assertEquals(new Size(10, LengthUnit.percent), //
				propParser.parseProperty(BackgroundStyle.cornerRadius, env, "10%")//
					.get());

			Assert.assertEquals(Cursor.getPredefinedCursor(BackgroundStyle.PreDefinedCursor.northResize.type), //
				propParser.parseProperty(BackgroundStyle.cursor, env, "nResize")//
					.get());

			Assert.assertEquals(BaseTexture.class, //
				propParser.parseProperty(BackgroundStyle.texture, env, "no-texture")//
					.get().getClass());

			Assert.assertEquals(.25, //
				propParser.parseProperty(FontStyle.weight, env, "0.25")//
					.get().doubleValue(),
				0);

			Assert.assertEquals(FontStyle.bold, //
				propParser.parseProperty(FontStyle.weight, env, "bold")//
					.get().doubleValue(),
				0);

			Assert.assertEquals(Duration.of(10, ChronoUnit.DAYS), //
				propParser.parseProperty(durationAtt, env, "10d")//
					.get());

			Assert.assertEquals(Duration.of(1000, ChronoUnit.MILLIS), //
				propParser.parseProperty(durationAtt, env, "1000")//
					.get());

			Assert.assertEquals(Duration.of(10, ChronoUnit.DAYS).plus(Duration.of(5, ChronoUnit.HOURS)), //
				propParser.parseProperty(durationAtt, env, "10d 5h")//
					.get());

			Assert.assertEquals(Colors.blue, //
				propParser.parseProperty(colorAtt, env, "${colors.blue}")//
					.get());
		} catch (QuickParseException e) {
			throw new IllegalStateException(e);
		}
	}

	/** A little more advanced property parsing, including directives and functions */
	@Test
	public void testDirectives() {
		QuickEnvironment env = QuickEnvironment.build().withDefaults().build();
		QuickPropertyParser propParser = env.getPropertyParser();
		try {
			Assert.assertEquals(Duration.of(10, ChronoUnit.DAYS).plus(Duration.of(5, ChronoUnit.HOURS)), //
				propParser.parseProperty(durationAtt, env, "${ #{10d} + #{5h} }")//
					.get());

			Assert.assertEquals(new Color(128, 196, 255), //
				propParser.parseProperty(BackgroundStyle.color, env, "${rgb(128, 196, 255)}")//
					.get());

			Assert.assertEquals(new Color(128, 196, 255), //
				propParser.parseProperty(BackgroundStyle.color, env, "rgb(${100+28}, ${200-4}, ${256-1})")//
					.get());

			Assert.assertEquals(Integer.valueOf(5), //
				propParser.parseProperty(intAtt, env, "13%8")//
					.get());
		} catch (QuickParseException e) {
			throw new IllegalStateException(e);
		}
	}

	/** Property parsing with injected variables and actions */
	@Test
	public void testVariables() {
		QuickEnvironment env = QuickEnvironment.build().withDefaults().build();
		QuickPropertyParser propParser = env.getPropertyParser();

		TypeToken<Double> dType = TypeToken.of(Double.class);
		SimpleSettableValue<Double> var1 = new SimpleSettableValue<>(dType, false);
		var1.set(1d, null);
		SimpleSettableValue<Double> var2 = new SimpleSettableValue<>(dType, false);
		var2.set(2d, null);
		QuickParseEnv parseEnv = new SimpleParseEnv(env.cv(), env.msg(),
			DefaultExpressionContext.build().withParent(env.getContext())//
				.withValue("constVar", ObservableValue.constant(1))//
				.withValue("var1", var1)//
				.withValueGetter(name -> {
					if (name.equals("var2"))
						return var2;
					else
						return null;
				})//
				.withFunction("square",
					ExpressionFunction.build(dType).withArgs(dType)
						.withApply(args -> ((Double) args.get(0)) * ((Double) args.get(0))).build())//
				.withUnit("sqrt", dType, dType, v -> Math.sqrt(v), v -> v * v)//
				.build());

		try {
			ObservableValue<Double> result = propParser.parseProperty(doubleAtt, parseEnv, "var1");
			if (!(result instanceof SettableValue))
				throw new IllegalStateException("Result should be settable");
			assertSameObservable(var1, result);

			result = propParser.parseProperty(doubleAtt, parseEnv, "constVar");
			Assert.assertEquals(1d, result.get(), 0);

			result = propParser.parseProperty(doubleAtt, parseEnv, "var2");
			if (!(result instanceof SettableValue))
				throw new IllegalStateException("Result should be settable");
			assertSameObservable(var2, result);

			result = propParser.parseProperty(doubleAtt, parseEnv, "var1+5");
			if (!(result instanceof SettableValue))
				throw new IllegalStateException("Result should be settable");
			assertSameObservable(var1.mapV(dType, v -> v + 5, v -> v - 5, true), result);

			result = propParser.parseProperty(doubleAtt, parseEnv, "square(var1)");
			assertSameObservable(var1.mapV(dType, v -> v * v, v -> Math.sqrt(v), true), result);

			result = propParser.parseProperty(doubleAtt, parseEnv, "var1 sqrt");
			if (!(result instanceof SettableValue))
				throw new IllegalStateException("Result should be settable");
			assertSameObservable(var1.mapV(dType, v -> Math.sqrt(v), v -> v * v, true), result);
		} catch (QuickParseException e) {
			throw new IllegalStateException(e);
		}
	}

	/** Tests parsing with injected models */
	@Test
	public void testModels() {
		QuickEnvironment env = QuickEnvironment.build().withDefaults().build();
		QuickPropertyParser propParser = env.getPropertyParser();

		TypeToken<Double> dType = TypeToken.of(Double.class);
		SimpleSettableValue<Double> var1 = new SimpleSettableValue<>(dType, false);
		var1.set(1d, null);
		SimpleSettableValue<Double> var2 = new SimpleSettableValue<>(dType, false);
		var2.set(5d, null);
		ObservableAction<Double> incVar1 = var1.assignmentTo(var1.mapV(v -> v + 1));
		DefaultQuickModel nested = DefaultQuickModel.build()//
			.with("var2", var2)//
			.build();
		DefaultQuickModel model = DefaultQuickModel.build()//
			.with("var1", var1)//
			.with("incVar1", incVar1)//
			.with("nested", nested)//
			.build();

		QuickParseEnv parseEnv = new SimpleParseEnv(env.cv(), env.msg(),
			DefaultExpressionContext.build().withParent(env.getContext())//
				.withValue("model", ObservableValue.constant(model))//
				.build());

		try {
			ObservableAction<Double> action = (ObservableAction<Double>) propParser
				.parseProperty(ModelAttributes.action, parseEnv, "model.incVar1()").get();
			double pre = var1.get();
			double acted = action.act(null);
			double post = var1.get();
			Assert.assertEquals(pre + 1, post, 0.000000001);
			Assert.assertEquals(post, acted, 0.000000001);

			pre = var2.get();
			Assert.assertEquals(pre, propParser.parseProperty(ModelAttributes.action, parseEnv, "model.nested.var2++").get().act(null));
			Assert.assertEquals(pre + 1, var2.get(), 0.000000001);
			pre = var2.get();
			Assert.assertEquals(pre + 1, propParser.parseProperty(ModelAttributes.action, parseEnv, "++model.nested.var2").get().act(null));
			Assert.assertEquals(pre + 1, var2.get(), 0.000000001);
			pre = var2.get();
			Assert.assertEquals(pre + 10,
				propParser.parseProperty(ModelAttributes.action, parseEnv, "model.nested.var2+=10").get().act(null));
			Assert.assertEquals(pre + 10, var2.get(), 0.000000001);
		} catch (QuickParseException e) {
			throw new IllegalStateException(e);
		}
	}

	private static void assertSameObservable(SettableValue<Double> expected, ObservableValue<Double> actual) {
		Double previous=expected.get();
		Assert.assertEquals(expected.get(), actual.get(), 0.0000000001);

		Double[] expectedValue = new Double[1];
		Double[] actualValue = new Double[1];
		Subscription sub = actual.act(v -> actualValue[0] = v.getValue());

		expectedValue[0] = 5d;
		expected.set(expectedValue[0], null);
		Assert.assertEquals(expectedValue[0], actualValue[0], 0.0000000001);

		expectedValue[0] = 10d;
		expected.set(expectedValue[0], null);
		Assert.assertEquals(expectedValue[0], actualValue[0], 0.0000000001);

		expectedValue[0] = 0d;
		expected.set(expectedValue[0], null);
		Assert.assertEquals(expectedValue[0], actualValue[0], 0.0000000001);

		expectedValue[0] = 25E8;
		expected.set(expectedValue[0], null);
		Assert.assertEquals(expectedValue[0], actualValue[0], 0.0000000001);

		expectedValue[0] = 0.001d;
		expected.set(expectedValue[0], null);
		Assert.assertEquals(expectedValue[0], actualValue[0], 0.0000000001);

		expectedValue[0] = Double.POSITIVE_INFINITY;
		expected.set(expectedValue[0], null);
		Assert.assertEquals(expectedValue[0], actualValue[0], 0.0000000001);

		sub.unsubscribe();

		if (actual instanceof SettableValue) {
			SettableValue<Double> actualS = (SettableValue<Double>) actual;
			sub = expected.act(v -> expectedValue[0] = v.getValue());

			actualValue[0] = 5d;
			actualS.set(actualValue[0], null);
			Assert.assertEquals(actualValue[0], expectedValue[0], 0.0000000001);

			actualValue[0] = 10d;
			actualS.set(actualValue[0], null);
			Assert.assertEquals(actualValue[0], expectedValue[0], 0.0000000001);

			actualValue[0] = 0d;
			actualS.set(actualValue[0], null);
			Assert.assertEquals(actualValue[0], expectedValue[0], 0.0000000001);

			actualValue[0] = 25E8d;
			actualS.set(actualValue[0], null);
			Assert.assertEquals(actualValue[0], expectedValue[0], 0.0000000001);

			actualValue[0] = 0.001d;
			actualS.set(actualValue[0], null);
			Assert.assertEquals(actualValue[0], expectedValue[0], 0.0000000001);

			actualValue[0] = Double.POSITIVE_INFINITY;
			actualS.set(actualValue[0], null);
			Assert.assertEquals(actualValue[0], expectedValue[0], 0.0000000001);

			sub.unsubscribe();
		}

		expected.set(previous, null);
	}
}
