package org.muis.core.style;

import java.util.List;
import java.util.Set;

import org.muis.core.mgr.MuisState;
import org.muis.core.rx.DefaultObservableList;
import org.muis.core.rx.DefaultObservableSet;
import org.muis.core.style.stateful.AbstractInternallyStatefulStyle;
import org.muis.core.style.stateful.MutableStatefulStyle;
import org.muis.core.style.stateful.StateExpression;
import org.muis.core.style.stateful.StatefulStyle;

import prisms.lang.Type;

/** Tests style classes in org.muis.core.style.* packages */
public class StylesTest {
	private static class TestStatefulStyle extends AbstractInternallyStatefulStyle implements MutableStatefulStyle, MutableStyle {
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

	public void testStatefulStyles() {
		TestStatefulStyle style = new TestStatefulStyle();
	}

	public void testStyleSheet() {
	}

	public void testDependencies() {
	}

	public void testAllThrough() {
	}
}
