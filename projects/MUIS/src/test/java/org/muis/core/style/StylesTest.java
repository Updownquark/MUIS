package org.muis.core.style;

import static org.junit.Assert.assertEquals;
import static org.muis.core.MuisConstants.States.CLICK;
import static org.muis.core.style.BackgroundStyle.cornerRadius;
import static org.muis.core.style.LengthUnit.pixels;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.muis.core.mgr.MuisState;
import org.muis.core.rx.DefaultObservableList;
import org.muis.core.rx.DefaultObservableSet;
import org.muis.core.rx.ObservableCollection;
import org.muis.core.rx.ObservableList;
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

	/** Tests functionality of stateful styles in org.muis.core.style.stateful */
	@Test
	public void testStatefulStyles() {
		TestStatefulStyle style = new TestStatefulStyle();

		Size [] reported=new Size[1];
		int [] changes = new int[1];
		style.get(cornerRadius, false).value().act(value->reported[0]=value);
		style.allChanges().act(event -> changes[0]++);
		style.localChanges().act(event -> System.out.println("Local event"));
		ObservableCollection.fold(ObservableList.constant(new Type(Object.class), style.getLocal(cornerRadius).skip(1))).act(
			event -> System.out.println("fold event"));

		assertEquals(null, style.get(cornerRadius, false).get());
		assertEquals(cornerRadius.getDefault(), style.get(cornerRadius, true).get());
		assertEquals(0, changes[0]);

		Size clickSize = new Size(100, pixels);
		style.set(cornerRadius, new StateExpression.Simple(CLICK), clickSize);
		assertEquals(null, style.get(cornerRadius, false).get());
		assertEquals(cornerRadius.getDefault(), style.get(cornerRadius, true).get());
		assertEquals(null, reported[0]);
		assertEquals(0, changes[0]);

		style.stateControl.add(CLICK); // TODO 2 fold events, no Local events
		assertEquals(clickSize, style.get(cornerRadius, false).get());
		assertEquals(clickSize, reported[0]);
		assertEquals(1, changes[0]);
		style.stateControl.remove(CLICK);
		assertEquals(null, style.get(cornerRadius, false).get());
		assertEquals(null, reported[0]);
		assertEquals(2, changes[0]);

		Size noClickSize = new Size(1000, pixels);
		style.set(cornerRadius, new StateExpression.Simple(CLICK).not(), noClickSize);
		assertEquals(noClickSize, style.get(cornerRadius, false).get());
		assertEquals(noClickSize, reported[0]);
		assertEquals(3, changes[0]);
		style.stateControl.add(CLICK);
		assertEquals(clickSize, style.get(cornerRadius, false).get());
		assertEquals(clickSize, reported[0]);
		assertEquals(4, changes[0]);
		style.stateControl.remove(CLICK);
		assertEquals(noClickSize, style.get(cornerRadius, false).get());
		assertEquals(noClickSize, reported[0]);
		assertEquals(5, changes[0]);
	}

	/** Tests functionality of style sheets in org.muis.core.style.sheet */
	// @Test
	public void testStyleSheet() {
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
