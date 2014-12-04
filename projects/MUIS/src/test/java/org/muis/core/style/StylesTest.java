package org.muis.core.style;

import static org.junit.Assert.assertEquals;
import static org.muis.core.MuisConstants.States.CLICK;
import static org.muis.core.style.BackgroundStyle.cornerRadius;
import static org.muis.core.style.LengthUnit.pixels;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.muis.core.MuisElement;
import org.muis.core.mgr.MuisState;
import org.muis.core.rx.DefaultObservableList;
import org.muis.core.rx.DefaultObservableSet;
import org.muis.core.rx.ObservableSet;
import org.muis.core.style.sheet.*;
import org.muis.core.style.stateful.AbstractInternallyStatefulStyle;
import org.muis.core.style.stateful.MutableStatefulStyle;
import org.muis.core.style.stateful.StateExpression;
import org.muis.core.style.stateful.StatefulStyle;

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
		style.get(cornerRadius, false).value().act(value -> reported[0] = value);
		assertEquals(null, style.get(cornerRadius, false).get());
		assertEquals(cornerRadius.getDefault(), style.get(cornerRadius, true).get());

		Size clickSize = new Size(100, pixels);
		style.set(cornerRadius, new StateExpression.Simple(CLICK), clickSize);
		assertEquals(null, style.get(cornerRadius, false).get());
		assertEquals(cornerRadius.getDefault(), style.get(cornerRadius, true).get());
		assertEquals(null, reported[0]);

		style.stateControl.add(CLICK);
		assertEquals(clickSize, style.get(cornerRadius, false).get());
		assertEquals(clickSize, reported[0]);
		style.stateControl.remove(CLICK);
		assertEquals(null, style.get(cornerRadius, false).get());
		assertEquals(null, reported[0]);

		Size noClickSize = new Size(1000, pixels);
		style.set(cornerRadius, new StateExpression.Simple(CLICK).not(), noClickSize);
		assertEquals(noClickSize, style.get(cornerRadius, false).get());
		assertEquals(noClickSize, reported[0]);
		style.stateControl.add(CLICK);
		assertEquals(clickSize, style.get(cornerRadius, false).get());
		assertEquals(clickSize, reported[0]);
		style.stateControl.remove(CLICK);
		assertEquals(noClickSize, style.get(cornerRadius, false).get());
		assertEquals(noClickSize, reported[0]);
	}

	/** Tests functionality of style sheets in org.muis.core.style.sheet */
	// @Test
	public void testStyleSheet() {
		TestStyleSheet sheet = new TestStyleSheet();
		FilteredStyleSheet<MuisElement> filter = new FilteredStyleSheet<>(sheet, null, MuisElement.class,
			ObservableSet.constant(new Type(TemplateRole.class)));
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
