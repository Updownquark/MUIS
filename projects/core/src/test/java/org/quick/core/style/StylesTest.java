package org.quick.core.style;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.quick.core.QuickConstants.States.CLICK;
import static org.quick.core.style.BackgroundStyle.cornerRadius;
import static org.quick.core.style.LengthUnit.pixels;

import org.junit.Test;
import org.observe.BiTuple;
import org.observe.ObservableValue;
import org.observe.ObservableValueEvent;
import org.observe.Observer;
import org.observe.collect.ObservableCollection;
import org.observe.collect.ObservableList;
import org.observe.collect.impl.ObservableArrayList;
import org.observe.collect.impl.ObservableHashSet;
import org.observe.util.ObservableUtils;
import org.quick.core.BodyElement;
import org.quick.core.QuickElement;
import org.quick.core.mgr.QuickState;
import org.quick.core.style.MutableStyle;
import org.quick.core.style.Size;
import org.quick.core.style.StyleAttribute;
import org.quick.core.style.sheet.*;
import org.quick.core.style.stateful.*;

import prisms.lang.Type;

/** Tests style classes in org.quick.core.style.* packages */
public class StylesTest {
	private static class TestStatefulStyle extends AbstractInternallyStatefulStyle implements MutableStatefulStyle, MutableStyle {
		TestStatefulStyle() {
			super(new ObservableArrayList<>(new Type(StatefulStyle.class)), new ObservableHashSet<>(new Type(QuickState.class)));
		}

		@Override
		public <T> TestStatefulStyle set(StyleAttribute<T> attr, T value) throws ClassCastException, IllegalArgumentException {
			super.set(attr, value);
			return this;
		}

		@Override
		public <T> TestStatefulStyle set(StyleAttribute<T> attr, StateExpression exp, T value) throws ClassCastException,
			IllegalArgumentException {
			super.set(attr, exp, value);
			return this;
		}

		@Override
		public TestStatefulStyle clear(StyleAttribute<?> attr) {
			super.clear(attr);
			return this;
		}

		@Override
		public TestStatefulStyle clear(StyleAttribute<?> attr, StateExpression exp) {
			super.clear(attr, exp);
			return this;
		}
	}

	private static class TestStyleSheet extends AbstractStyleSheet implements MutableStyleSheet {
		@SuppressWarnings("unused")
		final ObservableList<StyleSheet> dependControl;

		public TestStyleSheet() {
			super(ObservableUtils.control(new ObservableArrayList<>(new Type(StyleSheet.class))));
			dependControl = ObservableUtils.getController(getConditionalDependencies());
		}

		@Override
		public <T> TestStyleSheet set(StyleAttribute<T> attr, StateGroupTypeExpression<?> exp, T value) throws ClassCastException,
			IllegalArgumentException {
			super.set(attr, exp, value);
			return this;
		}

		@Override
		public TestStyleSheet clear(StyleAttribute<?> attr) {
			super.clear(attr);
			return this;
		}

		@Override
		public TestStyleSheet clear(StyleAttribute<?> attr, StateGroupTypeExpression<?> exp) {
			super.clear(attr, exp);
			return this;
		}
	}

	/** Tests functionality of stateful styles in org.quick.core.style.stateful */
	@Test
	public void testStatefulStyles() {
		TestStatefulStyle style = new TestStatefulStyle();

		Size [] reported = new Size[1];
		int [] changes = new int[1];
		int lastChanges = 0;
		style.get(cornerRadius, false).value().act(value -> reported[0] = value);
		style.allChanges().act(event -> changes[0]++);
		ObservableCollection<BiTuple<StyleAttribute<?>, ObservableValue<?>>> values = style.localAttributes().map(
			attr -> new BiTuple<>(attr, style.getLocal(attr)));
		values.onElement(element -> {
			System.out.println("New attribute " + element.get().getValue1() + "=" + element.get().getValue2().get());
			element.subscribe(new Observer<ObservableValueEvent<BiTuple<StyleAttribute<?>, ObservableValue<?>>>>() {
				@Override
				public <V2 extends ObservableValueEvent<BiTuple<StyleAttribute<?>, ObservableValue<?>>>> void onNext(V2 value2) {
					value2.getValue().getValue2().value().noInit().takeUntil(element.noInit()).act(value3 -> {
						System.out.println(value2.getValue().getValue1() + "=" + value3);
					});
				}

				@Override
				public <V2 extends ObservableValueEvent<BiTuple<StyleAttribute<?>, ObservableValue<?>>>> void onCompleted(V2 value2) {
					if(value2.getOldValue() != null) // Don't know why this is fired twice, once with null
						System.out.println(value2.getOldValue().getValue1() + " removed");
				}
			});
		});

		assertEquals(null, style.get(cornerRadius, false).get());
		assertEquals(cornerRadius.getDefault(), style.get(cornerRadius, true).get());
		assertEquals(0, changes[0]);

		Size clickSize = new Size(100, pixels);
		style.set(cornerRadius, new StateExpression.Simple(CLICK), clickSize);
		assertEquals(null, style.get(cornerRadius, false).get());
		assertEquals(cornerRadius.getDefault(), style.get(cornerRadius, true).get());
		assertEquals(null, reported[0]);
		assertEquals(0, changes[0]);

		style.getState().add(CLICK); // 2 fold events, no Local events
		assertEquals(clickSize, style.get(cornerRadius, false).get());
		assertEquals(clickSize, reported[0]);
		assertTrue(changes[0] > lastChanges);
		lastChanges = changes[0];
		style.getState().remove(CLICK);
		assertEquals(null, style.get(cornerRadius, false).get());
		assertEquals(null, reported[0]);
		assertTrue(changes[0] > lastChanges);
		lastChanges = changes[0];

		Size noClickSize = new Size(1000, pixels);
		style.set(cornerRadius, new StateExpression.Simple(CLICK).not(), noClickSize);
		assertEquals(noClickSize, style.get(cornerRadius, false).get());
		assertEquals(noClickSize, reported[0]);
		assertTrue(changes[0] > lastChanges);
		style.getState().add(CLICK);
		assertEquals(clickSize, style.get(cornerRadius, false).get());
		assertEquals(clickSize, reported[0]);
		assertTrue(changes[0] > lastChanges);
		lastChanges = changes[0];
		style.getState().remove(CLICK);
		assertEquals(noClickSize, style.get(cornerRadius, false).get());
		assertEquals(noClickSize, reported[0]);
		assertTrue(changes[0] > lastChanges);
		lastChanges = changes[0];
	}

	/** Tests functionality of style sheets in org.quick.core.style.sheet */
	@Test
	public void testStyleSheet() {
		// Create the styles and supporting collections
		ObservableHashSet<QuickState> state = new ObservableHashSet<>(new Type(QuickState.class));
		ObservableHashSet<TemplateRole> roles = new ObservableHashSet<>(new Type(TemplateRole.class));
		TestStyleSheet sheet = new TestStyleSheet();
		FilteredStyleSheet<QuickElement> filter = new FilteredStyleSheet<>(sheet, null, QuickElement.class, roles);
		StatefulStyleSample sample = new StatefulStyleSample(filter, state);
		FilteredStyleSheet<BodyElement> bodyFilter = new FilteredStyleSheet<>(sheet, null, BodyElement.class, roles);
		StatefulStyleSample bodySample = new StatefulStyleSample(bodyFilter, state);

		// Test default values
		Size [] reported = new Size[1];
		Size [] bodyReported = new Size[1];
		sample.get(cornerRadius, false).value().act(value -> reported[0] = value);
		bodySample.get(cornerRadius, false).value().act(value -> bodyReported[0] = value);
		assertEquals(null, sample.get(cornerRadius, false).get());
		assertEquals(cornerRadius.getDefault(), sample.get(cornerRadius, true).get());
		assertEquals(null, bodySample.get(cornerRadius, false).get());
		assertEquals(cornerRadius.getDefault(), bodySample.get(cornerRadius, true).get());

		// Set up values
		Size clickSize = new Size(100, pixels);
		sheet.set(cornerRadius, new StateGroupTypeExpression<>(StateExpression.forState(CLICK), null, QuickElement.class, null), clickSize);
		Size noClickSize = new Size(1000, pixels);
		sheet.set(cornerRadius, new StateGroupTypeExpression<>(StateExpression.forState(CLICK).not(), null, QuickElement.class, null),
			noClickSize);
		Size bodyClickSize = new Size(101, pixels);
		sheet.set(cornerRadius, new StateGroupTypeExpression<>(StateExpression.forState(CLICK), null, BodyElement.class, null),
			bodyClickSize);

		// Test clean values (no states or roles set)
		assertEquals(noClickSize, sample.get(cornerRadius, false).get());
		assertEquals(noClickSize, bodySample.get(cornerRadius, false).get());

		// Test stateful properties of style sheets
		state.add(CLICK);
		assertEquals(clickSize, sample.get(cornerRadius, false).get());
		assertEquals(clickSize, reported[0]);
		assertEquals(bodyClickSize, bodySample.get(cornerRadius, false).get());
		assertEquals(bodyClickSize, bodyReported[0]);
	}

	/** Tests style inheritance */
	// @Test
	public void testDependencies() {
	}

	/** A more thorough test of style functionality in org.quick.core.style.* */
	// @Test
	public void testAllThrough() {
	}
}
