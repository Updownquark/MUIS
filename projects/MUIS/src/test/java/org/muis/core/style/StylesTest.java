package org.muis.core.style;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.muis.core.MuisConstants.States.CLICK;
import static org.muis.core.style.BackgroundStyle.cornerRadius;
import static org.muis.core.style.LengthUnit.pixels;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.muis.core.BodyElement;
import org.muis.core.MuisElement;
import org.muis.core.mgr.MuisState;
import org.muis.core.style.sheet.*;
import org.muis.core.style.stateful.*;
import org.observe.BiTuple;
import org.observe.ObservableValue;
import org.observe.ObservableValueEvent;
import org.observe.Observer;
import org.observe.collect.DefaultObservableList;
import org.observe.collect.DefaultObservableSet;
import org.observe.collect.ObservableCollection;
import org.observe.collect.ObservableElement;

import prisms.lang.Type;

/** Tests style classes in org.muis.core.style.* packages */
public class StylesTest {
	private static class TestStatefulStyle extends AbstractInternallyStatefulStyle implements MutableStatefulStyle, MutableStyle {
		@SuppressWarnings("unused")
		final List<StatefulStyle> dependControl;
		final Set<MuisState> stateControl;

		TestStatefulStyle() {
			super(new DefaultObservableList<>(new Type(StatefulStyle.class)), new DefaultObservableSet<>(new Type(MuisState.class)));
			dependControl = ((DefaultObservableList<StatefulStyle>) getConditionalDependencies()).control(null);
			stateControl = ((DefaultObservableSet<MuisState>) getState()).control(null);
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
		final List<StyleSheet> dependControl;

		public TestStyleSheet() {
			super(new DefaultObservableList<>(new Type(StyleSheet.class)));
			dependControl = ((DefaultObservableList<StyleSheet>) getConditionalDependencies()).control(null);
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

	/** Tests functionality of stateful styles in org.muis.core.style.stateful */
	@Test
	public void testStatefulStyles() {
		TestStatefulStyle style = new TestStatefulStyle();

		Size [] reported = new Size[1];
		int [] changes = new int[1];
		int lastChanges = 0;
		style.get(cornerRadius, false).value().act(value -> reported[0] = value);
		style.allChanges().act(event -> changes[0]++);
		ObservableCollection<BiTuple<StyleAttribute<?>, ObservableValue<?>>> values = style.localAttributes().mapC(
			attr -> new BiTuple<>(attr, style.getLocal(attr)));
		values.internalSubscribe(new Observer<ObservableElement<BiTuple<StyleAttribute<?>, ObservableValue<?>>>>() {
			@Override
			public <V extends ObservableElement<BiTuple<StyleAttribute<?>, ObservableValue<?>>>> void onNext(V value) {
				System.out.println("New attribute " + value.get().getValue1() + "=" + value.get().getValue2().get());
				value.internalSubscribe(new Observer<ObservableValueEvent<BiTuple<StyleAttribute<?>, ObservableValue<?>>>>() {
					@Override
					public <V2 extends ObservableValueEvent<BiTuple<StyleAttribute<?>, ObservableValue<?>>>> void onNext(V2 value2) {
						value2.getValue().getValue2().value().noInit().takeUntil(value.noInit()).act(value3 -> {
							System.out.println(value2.getValue().getValue1() + "=" + value3);
						});
					}

					@Override
					public <V2 extends ObservableValueEvent<BiTuple<StyleAttribute<?>, ObservableValue<?>>>> void onCompleted(V2 value2) {
						if(value2.getOldValue() != null) // Don't know why this is fired twice, once with null
							System.out.println(value2.getOldValue().getValue1() + " removed");
					}
				});
			}
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

		style.stateControl.add(CLICK); // 2 fold events, no Local events
		assertEquals(clickSize, style.get(cornerRadius, false).get());
		assertEquals(clickSize, reported[0]);
		assertTrue(changes[0] > lastChanges);
		lastChanges = changes[0];
		style.stateControl.remove(CLICK);
		assertEquals(null, style.get(cornerRadius, false).get());
		assertEquals(null, reported[0]);
		assertTrue(changes[0] > lastChanges);
		lastChanges = changes[0];

		Size noClickSize = new Size(1000, pixels);
		style.set(cornerRadius, new StateExpression.Simple(CLICK).not(), noClickSize);
		assertEquals(noClickSize, style.get(cornerRadius, false).get());
		assertEquals(noClickSize, reported[0]);
		assertTrue(changes[0] > lastChanges);
		style.stateControl.add(CLICK);
		assertEquals(clickSize, style.get(cornerRadius, false).get());
		assertEquals(clickSize, reported[0]);
		assertTrue(changes[0] > lastChanges);
		lastChanges = changes[0];
		style.stateControl.remove(CLICK);
		assertEquals(noClickSize, style.get(cornerRadius, false).get());
		assertEquals(noClickSize, reported[0]);
		assertTrue(changes[0] > lastChanges);
		lastChanges = changes[0];
	}

	/** Tests functionality of style sheets in org.muis.core.style.sheet */
	@Test
	public void testStyleSheet() {
		// Create the styles and supporting collections
		DefaultObservableSet<MuisState> state = new DefaultObservableSet<>(new Type(MuisState.class));
		Set<MuisState> stateControl = state.control(null);
		DefaultObservableSet<TemplateRole> roles = new DefaultObservableSet<>(new Type(TemplateRole.class));
		@SuppressWarnings("unused")
		Set<TemplateRole> roleControl = roles.control(null);
		TestStyleSheet sheet = new TestStyleSheet();
		FilteredStyleSheet<MuisElement> filter = new FilteredStyleSheet<>(sheet, null, MuisElement.class, roles);
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
		sheet.set(cornerRadius, new StateGroupTypeExpression<>(StateExpression.forState(CLICK), null, MuisElement.class, null), clickSize);
		Size noClickSize = new Size(1000, pixels);
		sheet.set(cornerRadius, new StateGroupTypeExpression<>(StateExpression.forState(CLICK).not(), null, MuisElement.class, null),
			noClickSize);
		Size bodyClickSize = new Size(101, pixels);
		sheet.set(cornerRadius, new StateGroupTypeExpression<>(StateExpression.forState(CLICK), null, BodyElement.class, null),
			bodyClickSize);

		// Test clean values (no states or roles set)
		assertEquals(noClickSize, sample.get(cornerRadius, false).get());
		assertEquals(noClickSize, bodySample.get(cornerRadius, false).get());

		// Test stateful properties of style sheets
		stateControl.add(CLICK);
		assertEquals(clickSize, sample.get(cornerRadius, false).get());
		assertEquals(clickSize, reported[0]);
		assertEquals(bodyClickSize, bodySample.get(cornerRadius, false).get());
		assertEquals(bodyClickSize, bodyReported[0]);
	}

	/** Tests style inheritance */
	// @Test
	public void testDependencies() {
	}

	/** A more thorough test of style functionality in org.muis.core.style.* */
	// @Test
	public void testAllThrough() {
	}
}
