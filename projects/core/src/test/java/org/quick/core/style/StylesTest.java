package org.quick.core.style;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.observe.ObservableValueTester;
import org.observe.SimpleSettableValue;
import org.observe.collect.impl.ObservableHashSet;
import org.quick.core.*;
import org.quick.core.QuickCache.CacheException;
import org.quick.core.QuickConstants.States;
import org.quick.core.QuickTemplate.AttachPoint;
import org.quick.core.mgr.QuickState;
import org.quick.core.parser.QuickDocumentStructure;

import com.google.common.reflect.TypeToken;

/** Tests style classes in the org.quick.core.style package */
public class StylesTest {
	class StyleConditionInstanceBacking {
		final ObservableHashSet<QuickState> state;
		final ObservableHashSet<String> groups;
		final ObservableHashSet<List<AttachPoint<?>>> rolePaths;

		StyleConditionInstanceBacking() {
			state = new ObservableHashSet<>(TypeToken.of(QuickState.class));
			groups = new ObservableHashSet<>(TypeToken.of(String.class));
			rolePaths = new ObservableHashSet<>(new TypeToken<List<AttachPoint<?>>>() {});
		}
	}

	@Test
	public void testImmutableStyle() {
		SimpleSettableValue<Double> v1 = new SimpleSettableValue<>(Double.class, false);
		v1.set(0d, null);
		ImmutableStyle style = ImmutableStyle.build(null)//
			.set(BackgroundStyle.transparency, v1)//
			.build();

		assertEquals(v1.get(), style.get(BackgroundStyle.transparency).get(), 0.0000001);
		v1.set(1d, null);
		assertEquals(v1.get(), style.get(BackgroundStyle.transparency).get(), 0.0000001);
		assertEquals(FontStyle.size.getDefault(), style.get(FontStyle.size).get(), 0.000000);
		// TODO More!
	}

	@Test
	public void testSimpleStyleSheet() {
		SimpleSettableValue<Double> v1 = new SimpleSettableValue<>(Double.class, false);
		v1.set(0d, null);
		SimpleStyleSheet sheet = new SimpleStyleSheet(null);
		sheet.set(BackgroundStyle.transparency, v1);

		class DeepElement extends QuickTextElement {}
		StyleConditionInstanceBacking baseBacking = new StyleConditionInstanceBacking();
		StyleConditionInstanceBacking deepBacking = new StyleConditionInstanceBacking();
		StyleConditionInstance<QuickElement> baseCondition = StyleConditionInstance.build(QuickElement.class)//
			.withState(baseBacking.state)//
			.withGroups(baseBacking.groups)//
			.withRolePaths(baseBacking.rolePaths).build();
		StyleConditionInstance<DeepElement> deepCondition = StyleConditionInstance.build(DeepElement.class)//
			.withState(deepBacking.state)//
			.withGroups(deepBacking.groups)//
			.withRolePaths(deepBacking.rolePaths).build();

		ObservableValueTester<Double> baseTester = new ObservableValueTester<>(
			sheet.get(baseCondition, BackgroundStyle.transparency, false), 0.0000001);
		ObservableValueTester<Double> deepTester = new ObservableValueTester<>(
			sheet.get(deepCondition, BackgroundStyle.transparency, false), 0.0000001);

		baseTester.check(v1.get(), 1);
		deepTester.check(v1.get(), 1);

		v1.set(1d, null);
		baseTester.check(v1.get(), 1);
		deepTester.check(v1.get(), 1);

		// Throw in these to test default values
		assertEquals(null, sheet.get(baseCondition, FontStyle.size, false).get());
		assertEquals(FontStyle.size.getDefault(), sheet.get(baseCondition, FontStyle.size, true).get(), 0.000000);

		// Check stateful style
		SimpleSettableValue<Double> v2 = new SimpleSettableValue<>(Double.class, false);
		v2.set(0.5, null);
		sheet.set(BackgroundStyle.transparency, //
			StyleCondition.build(QuickElement.class)//
				.setState(StateCondition.forState(States.CLICK))//
				.build(), //
			v2);
		// Doesn't apply to either
		baseTester.check(v1.get(), 0);
		deepTester.check(v1.get(), 0);

		baseBacking.state.add(States.CLICK);
		baseTester.check(v2.get(), 1);
		deepTester.check(v1.get(), 0);

		sheet.clear(BackgroundStyle.transparency, //
			StyleCondition.build(QuickElement.class)//
				.setState(StateCondition.forState(States.CLICK))//
				.build());
		baseTester.check(v1.get(), 1);
		deepTester.check(v1.get(), 0);

		sheet.set(BackgroundStyle.transparency, //
			StyleCondition.build(QuickElement.class)//
				.setState(StateCondition.forState(States.CLICK))//
				.build(), //
			v2);
		baseTester.check(v2.get(), 1);
		deepTester.check(v1.get(), 0);

		v2.set(0.25, null);
		baseTester.check(v2.get(), 1);
		deepTester.check(v1.get(), 0);

		// Check type-specific style
		SimpleSettableValue<Double> v3 = new SimpleSettableValue<>(Double.class, false);
		v3.set(0.5, null);
		sheet.set(BackgroundStyle.transparency, //
			StyleCondition.build(QuickTextElement.class)//
				.build(), //
			v3);
		baseTester.check(v2.get(), 0);
		deepTester.check(v3.get(), 1);

		v3.set(0.1, null);
		baseTester.check(v2.get(), 0);
		deepTester.check(v3.get(), 1);

		SimpleSettableValue<Double> v4 = new SimpleSettableValue<>(Double.class, false);
		v4.set(0.5, null);
		sheet.set(BackgroundStyle.transparency, //
			StyleCondition.build(DeepElement.class)//
				.build(), //
			v4);
		baseTester.check(v2.get(), 0);
		deepTester.check(v4.get(), 1);

		v4.set(0.4, null);
		baseTester.check(v2.get(), 0);
		deepTester.check(v4.get(), 1);

		sheet.clear(BackgroundStyle.transparency, //
			StyleCondition.build(QuickTextElement.class)//
				.build());
		baseTester.check(v2.get(), 0);
		deepTester.check(v4.get(), 0);

		sheet.set(BackgroundStyle.transparency, //
			StyleCondition.build(QuickTextElement.class)//
				.build(), //
			v3);
		baseTester.check(v2.get(), 0);
		deepTester.check(v4.get(), 0);

		sheet.clear(BackgroundStyle.transparency, //
			StyleCondition.build(DeepElement.class)//
				.build());
		baseTester.check(v2.get(), 0);
		deepTester.check(v3.get(), 1);

		// Test group styles
		SimpleSettableValue<Double> v5 = new SimpleSettableValue<>(Double.class, false);
		v5.set(0.5, null);
		sheet.set(BackgroundStyle.transparency, //
			StyleCondition.build(QuickElement.class)//
				.forGroup("group1")//
				.build(),
			v5);
		baseTester.check(v2.get(), 0);
		deepTester.check(v3.get(), 0);

		deepBacking.groups.add("group2");
		deepTester.check(v3.get(), 0);
		deepBacking.groups.add("group1");
		deepTester.check(v5.get(), 1); // Groups override type styles

		sheet.set(BackgroundStyle.transparency, //
			StyleCondition.build(QuickElement.class)//
				.forGroup("group2")//
				.build(),
			v5);
		deepTester.check(v5.get(), 0);

		deepBacking.groups.remove("group1");
		deepTester.check(v5.get(), 0, 1); // In this case, we'll tolerate an event even though the value hasn't changed

		deepBacking.groups.remove("group2");
		deepTester.check(v3.get(), 0, 1); // In this case, we'll tolerate an event even though the value hasn't changed
	}

	@Test
	public void testRolePathStyles() throws org.quick.core.parser.QuickParseException, java.io.IOException, CacheException {
		// Check role-path styles
		// TODO This is hard because the QuickTemplate class is written to prevent synthesizing attach points out of nowhere.
		// They have to be built in the standard way, which requires an environment, etc. Maybe need to do some mocking. :-(

		QuickEnvironment env = QuickEnvironment.build().withDefaults().build();
		env.msg().addListener(msg -> {
			switch (msg.type) {
			case FATAL:
			case ERROR:
			case WARNING:
				throw new IllegalStateException(msg.toString(), msg.exception);
			case INFO:
			case DEBUG:
				System.out.println(msg);
				break;
			}
		});
		URL testDoc = StylesTest.class.getResource("testRolePathStyles.qml");
		QuickDocumentStructure docStruct = env.getDocumentParser().parseDocument(testDoc,
			new java.io.InputStreamReader(testDoc.openStream()), env.cv(), env.msg());
		QuickHeadSection head = env.getContentCreator().createHeadFromStructure(docStruct.getHead(), env.getPropertyParser(), env);
		QuickDocument doc = new QuickDocument(env, docStruct.getLocation(), head, docStruct.getContent().getClassView());
		env.getContentCreator().fillDocument(doc, docStruct.getContent());

		QuickToolkit testTK = doc.cv().getToolkit("test");
		QuickTemplate.TemplateStructure template1Struct;
		QuickTemplate.TemplateStructure template2Struct;
		try {
			template1Struct = QuickTemplate.TemplateStructure.getTemplateStructure(env,
				testTK.loadClass(testTK.getMappedClass("template1"), QuickTemplate.class));
			template2Struct = QuickTemplate.TemplateStructure.getTemplateStructure(env,
				testTK.loadClass(testTK.getMappedClass("template2"), QuickTemplate.class));
		} catch (QuickException e) {
			throw new IllegalStateException(e);
		}

		AttachPoint<?> attach1 = template1Struct.getAttachPoint("attach1");
		AttachPoint<?> attach2 = template2Struct.getAttachPoint("attach2");

		QuickTemplate template1 = (QuickTemplate) doc.getRoot().getLogicalChildren().last();
		QuickTemplate template2 = (QuickTemplate) template1.getLogicalChildren().last();
		QuickTextElement text = (QuickTextElement) template2.getLogicalChildren().last();

		StyleCondition shallowCondition = StyleCondition.build(QuickElement.class)//
			.forPath(attach1)//
			.build();
		StyleConditionInstance<?> temp2CI = StyleConditionInstance.of(template2);
		assertEquals(setOf(asList(attach1)), temp2CI.getRolePaths());
		assertTrue(shallowCondition.matches(temp2CI).get());

		StyleCondition deepCondition = StyleCondition.build(QuickElement.class)//
			.forPath(attach1, attach2)//
			.build();
		StyleConditionInstance<?> textCI = StyleConditionInstance.of(text);
		assertEquals(setOf(asList(attach2), asList(attach1, attach2)), textCI.getRolePaths());
		assertTrue(deepCondition.matches(textCI).get());
		assertFalse(deepCondition.matches(temp2CI).get());
		assertFalse(shallowCondition.matches(textCI).get());
	}

	private static <T> Set<T> setOf(T... values) {
		return new HashSet<>(asList(values));
	}
}
