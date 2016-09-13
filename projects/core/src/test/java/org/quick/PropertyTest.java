package org.quick;

import java.awt.Color;
import java.awt.Cursor;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.junit.Assert;
import org.junit.Test;
import org.observe.ObservableValue;
import org.observe.SettableValue;
import org.observe.SimpleSettableValue;
import org.observe.Subscription;
import org.quick.core.QuickEnvironment;
import org.quick.core.QuickParseEnv;
import org.quick.core.parser.QuickParseException;
import org.quick.core.parser.QuickPropertyParser;
import org.quick.core.parser.SimpleParseEnv;
import org.quick.core.prop.DefaultExpressionContext;
import org.quick.core.prop.ExpressionFunction;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.prop.QuickPropertyType;
import org.quick.core.style.*;

import com.google.common.reflect.TypeToken;

/** Tests property parsing */
public class PropertyTest {
	private final QuickAttribute<Duration> durationAtt = QuickAttribute.build("duration", QuickPropertyType.duration).build();
	private final QuickAttribute<Double> doubleAtt = QuickAttribute.build("double", QuickPropertyType.floating).build();

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
		try {
			Assert.assertEquals(2.5, //
				propParser.parseProperty(BackgroundStyle.transparency, env, "2.5")//
					.get().doubleValue(),
				0);

			Assert.assertEquals(2.0, //
				propParser.parseProperty(BackgroundStyle.transparency, env, "2")//
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

			try {
				propParser.parseProperty(BackgroundStyle.cornerRadius, env, "2xp").get();
				throw new IllegalStateException("Should have thrown an IllegalArgumentException");
			} catch (IllegalArgumentException e) {
			}

			// Parsing an invalid value is allowed; validation is done outside of parsing
			propParser.parseProperty(BackgroundStyle.cornerRadius, env, "-2").get();

			Assert.assertEquals(new Size(10, LengthUnit.percent), //
				propParser.parseProperty(BackgroundStyle.cornerRadius, env, "10%")//
					.get());

			Assert.assertEquals(Cursor.getPredefinedCursor(BackgroundStyle.PreDefinedCursor.northResize.type), //
				propParser.parseProperty(BackgroundStyle.cursor, env, "n-resize")//
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
